package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._


// the functionality of this class overlaps with the Datapath class
// abaondon now.
class ysyx() extends Module {
  val io = IO(new DatapathIO)
  val datapath = Module(new ysyx_23060107())
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

// class ysyx_00000000 extends BlackBox {
//   val io = IO(new Bundle {
//     val clock = Input(Clock())
//     val reset = Input(Reset())
//     val io_interrupt = Input(Bool())
//     val io_master = AXI4Bundle(CPUAXI4BundleParameters())
//     val io_slave = Flipped(AXI4Bundle(CPUAXI4BundleParameters()))
//   })
// }
