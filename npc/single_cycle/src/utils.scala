package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import org.w3c.dom.DOMError

object utils {
  def padding(len: Int, v: UInt): UInt = Cat(Seq.fill(len)(v))
  // start means the least two significant bits of read addr
  def readDataGen(start: UInt, len: Int, data: UInt): UInt = MuxCase(
    // lh for addr end with '11' is invaild
    data(31, 24),
    Seq(
      (start === "b00".asUInt) -> data((0 + len) * 8 - 1, 0),
      (start === "b01".asUInt) -> data((1 + len) * 8 - 1, 8),
      (start === "b10".asUInt) -> data((2 + len) * 8 - 1, 16)
    )
  )

  def wmaskGen(func3: UInt, addr: UInt): UInt = MuxCase(
    "b1111".asUInt,
    Seq(
      (func3 === "b000".asUInt) -> (1.U << addr),
      (func3 === "b001".asUInt) -> (3.U << addr)
    )
  )
}

class LSFR(val len: Int) extends Module {
  val out = IO(Output(UInt(len.W)))

  val taps  = Seq.fill(len)(RegInit(0.U(1.W)))
  val const = Seq(1.U, 0.U, 1.U, 1.U, 1.U, 0.U, 0.U, 0.U)
  // 7 -> 6 - > 5 ...

  taps.tail.zip(taps).foreach {
    case (a, b) => b := a
  }

  // 每次都更新最高位
  when(taps.reduce(_ + _) =/= 0.U) {
    taps(len - 1) := taps
      .zip(const)
      .map {
        case (a, b) => a * b
      }
      .reduce(_ ^ _)
  }.elsewhen(taps.reduce(_ + _) === 0.U) {
    taps(0) := 1.U
  }

  out := Cat(taps.reverse)
}

object ArbiterState extends ChiselEnum {
  val sIDLE, sIFU, sLSU, sUART, sRTC = Value
}

class myArbiter extends Module {
  val ifuIn = IO(Flipped(AxiLiteMaster(width, width)))
  val lsuIn = IO(Flipped(AxiLiteMaster(width, width)))

  val sram = IO(AxiLiteMaster(width, width))
  val uart = IO(AxiLiteMaster(width, width))
  val rtc  = IO(AxiLiteMaster(width, width))

  sram := DontCare
  uart := DontCare
  rtc  := DontCare

  // 默认将 ar, wr, w 的 ready 置为 false
  // r, b 的 valid 置为 false
  // 其他信号不关心

  ifuIn.readAddr.ready  := false.B
  ifuIn.writeAddr.ready := false.B
  ifuIn.writeData.ready := false.B

  ifuIn.readData.valid  := false.B
  ifuIn.readData.bits   := DontCare
  ifuIn.writeResp.valid := false.B
  ifuIn.writeResp.bits  := false.B

  lsuIn.readAddr.ready  := false.B
  lsuIn.writeAddr.ready := false.B
  lsuIn.writeData.ready := false.B

  lsuIn.readData.valid  := false.B
  lsuIn.readData.bits   := DontCare
  lsuIn.writeResp.valid := false.B
  lsuIn.writeResp.bits  := false.B

  import ArbiterState._
  val arbiterState = RegInit(sIDLE)

  switch(arbiterState) {
    is(sIDLE) {
      when(ifuIn.writeAddr.valid || ifuIn.writeData.valid || ifuIn.readAddr.valid) {
        arbiterState := sIFU
      }.elsewhen(lsuIn.writeAddr.valid || lsuIn.writeData.valid || lsuIn.readAddr.valid) {
        arbiterState := MuxCase(
          sLSU,
          Seq(
            (lsuIn.writeAddr.bits.addr === config.SERAL_MNIO.U) -> sUART,
            (lsuIn.readAddr.bits.addr === config.RTC_MNIO.U) -> sRTC,
            (lsuIn.readAddr.bits.addr === (config.RTC_MNIO + 4).U) -> sRTC
          )
        )
      }
    }
    is(sIFU) {
      sram <> ifuIn
      when(ifuIn.readData.valid && ifuIn.readData.ready) {
        arbiterState := sIDLE
      }
    }
    is(sLSU) {
      sram <> lsuIn
      when(lsuIn.readData.valid && lsuIn.readData.ready) {
        arbiterState := sIDLE
      }
      when(lsuIn.writeResp.valid && lsuIn.writeResp.ready) {
        arbiterState := sIDLE
      }
    }
    is(sUART) {
      uart <> lsuIn
      when(lsuIn.writeResp.valid && lsuIn.writeResp.ready) {
        arbiterState := sIDLE
      }
    }
    is(sRTC) {
      rtc <> lsuIn
      when(lsuIn.readData.valid && lsuIn.readData.ready) {
        arbiterState := sIDLE
      }
    }
  }
}

object deviceState extends ChiselEnum {
  val aIDLE, awriteDataAddr, awriteData, awriteAddr, aREAD, aWriteACK, aReadACK, aUART, aRTC, aUARTACK, aRTCACK = Value
}

class Uart extends Module {

  val in = IO(Flipped(AxiLiteMaster(width, width)))
  import deviceState._
  val state = RegInit(aIDLE)

  // 不关心
  in.readAddr.ready := false.B
  in.readData.valid := false.B
  in.readData.bits  := DontCare

  // 有关
  in.writeResp.valid    := false.B
  in.writeResp.bits     := 1.U

  // ready follows valid
  in.writeAddr.ready := in.writeAddr.valid
  in.writeData.ready := in.writeData.valid

  // using a state machine would elegantly represent
  // the whole axi interface communicating process
  switch(state) {
    is(aIDLE) {
      // received write data and address concurrently
      when(in.writeAddr.ready && in.writeAddr.valid && in.writeData.valid && in.writeData.ready) {
        state := aUART
      }.elsewhen(in.writeData.ready && in.writeData.valid) {
        state := awriteData
      }.elsewhen(in.writeAddr.ready && in.writeAddr.valid) {
        state := awriteAddr
      }
    }
    // only received write addr
    is(awriteData) {
      when(in.writeAddr.ready && in.writeAddr.valid) {
        state := awriteDataAddr
      }
    }
    // only received write data
    is(awriteAddr) {
      when(in.writeData.ready && in.writeData.valid) {
        state := awriteDataAddr
      }
    }
    is(aUART) {
      printf("%c", in.writeData.bits.data(7, 0))
      state := aUARTACK
    }
    is(aUARTACK) {
      in.writeResp.valid := true.B
      in.writeResp.bits  := 0.U
      when(in.writeResp.ready && in.writeResp.valid) {
        state := aIDLE
      }
    }
  }
}

class RTC extends Module {

  val in = IO(Flipped(AxiLiteMaster(width, width)))
  import deviceState._
  val state = RegInit(aIDLE)


  // 不关心的
  in.writeResp.bits     := DontCare
  in.writeResp.valid    := false.B
  in.writeData.ready    := false.B
  in.writeAddr.ready := false.B

  // 有关的
  in.readAddr.ready := in.readAddr.valid
  in.readData.valid     := false.B
  in.readData.bits.resp := 1.U

  val mtime = RegInit(UInt(64.W), 0.U)
  mtime := mtime + 1.U

  in.readData.bits.data := MuxCase(
    0.U,
    Seq(
      (in.readAddr.bits.addr === config.RTC_MNIO.U) -> mtime(31, 0),
      (in.readAddr.bits.addr === (config.RTC_MNIO + 4).U) -> mtime(63, 32)
    )
  )

  // using a state machine would elegantly represent
  // the whole axi interface communicating process
  switch(state) {
    is(aIDLE) {
      when(in.readAddr.ready && in.readAddr.valid) {
        state := aRTC
      }
    }
    is(aRTC) {
      state := aRTCACK
    }
    is(aRTCACK) {
      in.readData.valid     := 1.U
      in.readData.bits.resp := 0.U
      when(in.readData.ready && in.readData.valid) {
        state := aIDLE
      }
    }
  }
}
