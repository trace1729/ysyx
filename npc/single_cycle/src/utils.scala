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