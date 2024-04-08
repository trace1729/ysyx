
package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._

/** *******************EX***************************
  */

class EXOutputIO extends Bundle {
  val overflow = Output(Bool())
  val carry    = Output(Bool())
  val zero     = Output(Bool())
  val inst     = Output(UInt(width.W))
  val rs2      = Output(UInt(width.W))

  val pc          = Output(UInt(width.W))
  val ctrlsignals = Output(new ctrlSignals)
  val csrvalue    = Output(UInt(width.W))
  val alures      = Output(UInt(width.W))
  val mepc        = Output(UInt(width.W))
  val mtvec       = Output(UInt(width.W))
}

class EX extends Module {
  val ex2lsuIn   = IO(Flipped(Decoupled(new IDUOutputIO)))
  val ex2memOut = IO(Decoupled(new EXOutputIO))

  val alu = Module(new Alu(width))
  // 因为控制逻辑是贯穿五个阶段的，所以每一个阶段(除了ID)都会有控制信号的输入
  // 这样就比较怪了，那我当前的阶段需要将控制信号传递给之后的阶段

  alu.io.alusel := ex2lsuIn.bits.ctrlsignals.alusel
  // 0 for rs1, 1 for pc
  alu.io.A := Mux(!ex2lsuIn.bits.ctrlsignals.asel, ex2lsuIn.bits.rs1, ex2lsuIn.bits.pc)
  // 0 for rs2, 1 for imm
  alu.io.B := Mux(!ex2lsuIn.bits.ctrlsignals.bsel, ex2lsuIn.bits.rs2, ex2lsuIn.bits.immediate)

  ex2memOut.bits.carry       := alu.io.carry
  ex2memOut.bits.overflow    := alu.io.overflow
  ex2memOut.bits.alures      := alu.io.res
  ex2memOut.bits.zero        := alu.io.zero
  ex2memOut.bits.ctrlsignals := ex2lsuIn.bits.ctrlsignals

  ex2memOut.bits.pc       := ex2lsuIn.bits.pc
  ex2memOut.bits.inst     := ex2lsuIn.bits.inst
  ex2memOut.bits.csrvalue := ex2lsuIn.bits.csrvalue
  ex2memOut.bits.rs2      := ex2lsuIn.bits.rs2

  ex2memOut.bits.mepc  := ex2lsuIn.bits.mepc
  ex2memOut.bits.mtvec := ex2lsuIn.bits.mtvec

  // ready, valid 信号全部设置成1
  // id2ex_in.ready  := 1.U
  // ex2mem_out.valid := 1.U

  ex2lsuIn.ready := ex2lsuIn.valid
  val exu_valid_reg = RegInit(0.U)
  ex2memOut.valid := exu_valid_reg

  when(ex2lsuIn.valid) {
    exu_valid_reg := 1.U
  }.elsewhen(ex2memOut.ready && ex2memOut.valid) {
    exu_valid_reg := 0.U
  }

}
