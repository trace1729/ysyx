package cpu

import chisel3._
import chisel3.util._
import cpu.config._

/** ********************IFU**************************
  */

class IFUInputIO extends Bundle {
  val pc   = Output(UInt(width.W))
  val inst = Output(UInt(width.W))
}

class IFU extends Bundle {
  val out = IO(Decoupled(new IFUInputIO))
}

/** *******************IDU***************************
  */

class IDUOutputIO extends Bundle {
  val rs1       = Output(UInt(width.W))
  val rs2       = Output(UInt(width.W))
  val immediate = Output(UInt(width.W))
  val ctrl = Output(new controlLogicIO(width))
}

class IDU extends Bundle {
  val in = IO(Flipped(Decoupled(new IFUInputIO)))
  val out = IO(DecoupledIO(new IDUOutputIO))
}


/*********************EX***************************
  */

class EXOutputIO extends Bundle {
    val alu = new AluIO(width)
}

class EX extends Bundle {
    val in = IO(Flipped(Decoupled(new IDUOutputIO)))
    val out = IO(Decoupled(new EXOutputIO))
}

/*********************MEM***************************
  */

class MEMOutputIO(width: Int) extends Bundle {
    val mem = new MemIO(width)
}

class MEM extends Bundle {
    val in = IO(Flipped(Decoupled(new EXOutputIO)))
    val out = IO(Decoupled(new MEMOutputIO(width)))
}

/*********************WB***************************
  */


class WBOutputIO extends Bundle {
    // 暂时不太清楚 wb 需要输出什么
    val wb = Output(Bool())
}

class WB extends Bundle {
    val in = IO(Flipped(Decoupled(new MEMOutputIO(width))))
    val out = IO(Decoupled(new WBOutputIO))
}
