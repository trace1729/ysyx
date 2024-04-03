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

  out  := Cat(taps.reverse)
}


object ArbiterState extends ChiselEnum {
  val sIDLE, sIFU, sLSU = Value
}

class myArbiter extends Module {
  val ifuIn = IO(Flipped(AxiLiteMaster(width, width)))
  val lsuIn = IO(Flipped(AxiLiteMaster(width, width)))
  val out   = IO(AxiLiteMaster(width, width))

  out := DontCare

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


  switch (arbiterState) {
    is (sIDLE) {
      when(ifuIn.writeAddr.valid || ifuIn.writeData.valid || ifuIn.readAddr.valid) {
        arbiterState := sIFU
      }.elsewhen(lsuIn.writeAddr.valid || lsuIn.writeData.valid || lsuIn.readAddr.valid) {
        arbiterState := sLSU
      }
    }
    is (sIFU) {
      out <> ifuIn
      when(ifuIn.readData.valid && ifuIn.readData.ready) {
        arbiterState := sIDLE
      }
    }
    is (sLSU) {
      out <> lsuIn
      when(lsuIn.readData.valid && lsuIn.readData.ready) {
        arbiterState := sIDLE
      }
      when (lsuIn.writeResp.valid && lsuIn.writeResp.ready) {
        arbiterState := sIDLE
      }
    }
  }


}