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



class MemIO(width: Int) extends Bundle {
  // val raddr = Input(UInt(width.W))
  val addr      = Input(UInt(width.W))
  val rdata     = Output(UInt(width.W))
  val wdata     = Input(UInt(width.W))
  val wmask     = Input(UInt(8.W))
  val memEnable = Input(Bool())
  val memRW     = Input(Bool())
  // val waddr = Input(UInt(width.W))
}

//   val io = IO(new Bundle {
//     val pc   = Output(UInt(width.W))
//     val inst = Output(UInt(width.W))

//     // for testing purpose
//     val x1           = Output(UInt(width.W))
//     val x2           = Output(UInt(width.W))
//     val x5           = Output(UInt(width.W))
//     val x6           = Output(UInt(width.W))
//     val x7           = Output(UInt(width.W))
//     val x8           = Output(UInt(width.W))
//     val x9           = Output(UInt(width.W))
//     val x10          = Output(UInt(width.W))
//     val writereg     = Output(UInt(5.W))
//     val test_alu_res = Output(UInt(width.W))
//   })


//   val ftrace = Module(new Dpi_ftrace)
//   ftrace.io.optype := cntlLogic.io.optype
//   ftrace.io.rd := io.inst(11, 7)
//   ftrace.io.ref_jalr := type_IJ
//   ftrace.io.ref_jal := type_J
//   ftrace.io.src1 := regfile.io.rs1
// class Dpi_ftrace extends BlackBox with HasBlackBoxResource {
//   val io = IO(new Bundle {
//     val optype   = Input(UInt(4.W))
//     val ref_jal  = Input(UInt(4.W))
//     val ref_jalr = Input(UInt(4.W))
//     val rd       = Input(UInt(5.W))
//     val src1     = Input(UInt(32.W))
//   })
//   addResource("/Dpi_ftrace.sv")
// }
