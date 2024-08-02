package cpu

import chisel3._
import chisel3.util._
import cpu.config._

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
  val ifuIn = IO(AxiLiteSlave(width, dataWidth))
  val lsuIn = IO(AxiLiteSlave(width, dataWidth))
  val xbar = IO(AxiLiteMaster(width, dataWidth))
  val rtc  = IO(AxiLiteMaster(width, dataWidth))

  xbar := DontCare
  rtc  := DontCare

  rtc.ar.valid := false.B
  xbar.w.valid := false.B
  xbar.aw.valid := false.B
  xbar.ar.valid := false.B

  // 默认将 ar, wr, w 的 ready 置为 false, ready 信号由 sram 或其他外设进行设置
  // r, b 的 valid 置为 false
  // 其他信号不关心

  ifuIn.ar.ready  := false.B
  ifuIn.aw.ready := false.B
  ifuIn.w.ready := false.B

  ifuIn.r.valid  := false.B
  ifuIn.r.bits   := DontCare
  ifuIn.b.valid := false.B
  ifuIn.b.bits.resp  := false.B

  lsuIn.ar.ready  := false.B
  lsuIn.aw.ready := false.B
  lsuIn.w.ready := false.B

  lsuIn.r.valid  := false.B
  lsuIn.r.bits   := DontCare
  lsuIn.b.valid := false.B
  lsuIn.b.bits.resp  := false.B

  lsuIn.b.bits.id := DontCare
  ifuIn.b.bits.id := DontCare

  import ArbiterState._
  val arbiterState = RegInit(sIDLE)

  switch(arbiterState) {
    is(sIDLE) {
      when(ifuIn.aw.valid || ifuIn.w.valid || ifuIn.ar.valid) {
        arbiterState := sIFU
      }.elsewhen(lsuIn.aw.valid || lsuIn.w.valid || lsuIn.ar.valid) {
        arbiterState := MuxCase(
          sLSU,
          Seq(
            (lsuIn.aw.bits.addr >= "h1000_0000".asUInt
              && lsuIn.aw.bits.addr <= "h1000_ffff".asUInt) -> sUART,
            (lsuIn.ar.bits.addr === config.RTC_MNIO.U) -> sRTC,
            (lsuIn.ar.bits.addr === (config.RTC_MNIO + 4).U) -> sRTC
          )
        )
      }
    }
    is(sIFU) {
      xbar <> ifuIn
      when(ifuIn.r.valid && ifuIn.r.ready) {
        arbiterState := sIDLE
      }
    }
    is(sLSU) {
      xbar <> lsuIn
      when(lsuIn.r.valid && lsuIn.r.ready) {
        arbiterState := sIDLE
      }
      when(lsuIn.b.valid && lsuIn.b.ready) {
        arbiterState := sIDLE
      }
    }
    is(sUART) {
      xbar <> lsuIn
      when(lsuIn.b.valid && lsuIn.b.ready) {
        arbiterState := sIDLE
      }
    }
    is(sRTC) {
      rtc <> lsuIn
      when(lsuIn.r.valid && lsuIn.r.ready) {
        arbiterState := sIDLE
      }
    }
  }
}

object deviceState extends ChiselEnum {
  val aIDLE, awriteDataAddr, awriteData, awriteAddr, aREAD, aWriteACK, aReadACK, aUART, aRTC, aUARTACK, aRTCACK = Value
}

class Uart extends Module {

  val in = IO(AxiLiteSlave(width, dataWidth))
  import deviceState._
  val state = RegInit(aIDLE)

  // 不关心
  in.ar.ready := false.B
  in.r.valid := false.B
  in.r.bits  := DontCare

  // 有关
  in.b.valid    := false.B
  in.b.bits.resp     := 1.U
  in.b.bits.id := DontCare

  // ready follows valid
  in.aw.ready := in.aw.valid
  in.w.ready := in.w.valid

  // using a state machine would elegantly represent
  // the whole axi interface communicating process
  switch(state) {
    is(aIDLE) {
      // received write data and address concurrently
      when(in.aw.ready && in.aw.valid && in.w.valid && in.w.ready) {
        state := aUART
      }
    }
    is(aUART) {
      printf("%c", in.w.bits.data(7, 0))
      state := aUARTACK
    }
    is(aUARTACK) {
      in.b.valid := true.B
      in.b.bits.resp  := 0.U
      when(in.b.ready && in.b.valid) {
        state := aIDLE
      }
    }
  }
}

class RTC extends Module {

  val in = IO(AxiLiteSlave(width, dataWidth))
  import deviceState._
  val state = RegInit(aIDLE)


  // 不关心的
  in.b.bits.resp    := DontCare
  in.b.valid    := false.B
  in.b.bits.id := DontCare
  in.w.ready    := false.B
  in.aw.ready := false.B

  // 有关的
  in.ar.ready := in.ar.valid
  in.r.valid     := false.B
  in.r.bits.resp := 1.U
  in.r.bits.last := 1.U
  in.r.bits.id := in.ar.bits.id // rid should match arid

  val mtime = RegInit(UInt(64.W), 0.U)
  mtime := mtime + 1.U

  in.r.bits.data := MuxCase(
    0.U,
    Seq(
      (in.ar.bits.addr === config.RTC_MNIO.U) -> mtime(31, 0),
      (in.ar.bits.addr === (config.RTC_MNIO + 4).U) -> mtime(63, 32)
    )
  )

  // using a state machine would elegantly represent
  // the whole axi interface communicating process
  switch(state) {
    is(aIDLE) {
      when(in.ar.ready && in.ar.valid) {
        state := aRTC
      }
    }
    is(aRTC) {
      state := aRTCACK
    }
    is(aRTCACK) {
      in.r.valid     := 1.U
      in.r.bits.resp := 0.U
      when(in.r.ready && in.r.valid) {
        state := aIDLE
      }
    }
  }
}
