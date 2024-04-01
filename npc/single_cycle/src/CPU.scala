package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._


class top(memoryFile: String = "") extends Module {
  val io = IO(new DatapathIO)
  val datapath = Module(new Datapath(memoryFile))
  io.inst := datapath.io.inst
  io.pc := datapath.io.pc
}


//   val ftrace = Module(new Dpi_ftrace)
//   ftrace.io.optype := cntlLogic.io.optype
//   ftrace.io.rd := io.inst(11, 7)
//   ftrace.io.ref_jalr := type_IJ
//   ftrace.io.ref_jal := type_J
//   ftrace.io.src1 := regfile.io.rs1

class Dpi_ftrace extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val optype   = Input(UInt(type_width.W))
    val ref_jal  = Input(UInt(type_width.W))
    val ref_jalr = Input(UInt(type_width.W))
    val rd       = Input(UInt(5.W))
    val src1     = Input(UInt(32.W))
  })
  addResource("/Dpi_ftrace.sv")
}
