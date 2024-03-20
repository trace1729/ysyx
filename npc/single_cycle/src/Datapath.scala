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
  val out = IO(new IFUInputIO)
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
  val in = IO(Flipped(new IFUInputIO))
  val out = IO(new IDUOutputIO)
}


/*********************EX***************************
  */

class EXOutputIO extends Bundle {
    val alu_res = Output(UInt(width.W))
}

class EX extends Bundle {
    val in = IO(Flipped(new IDUOutputIO))
}

/*********************MEM***************************
  */

/*********************WB***************************
  */