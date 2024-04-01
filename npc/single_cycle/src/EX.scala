
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
  val id2ex_in   = IO(Flipped(Decoupled(new IDUOutputIO)))
  val ex2mem_out = IO(Decoupled(new EXOutputIO))

  val alu = Module(new Alu(width))
  // 因为控制逻辑是贯穿五个阶段的，所以每一个阶段(除了ID)都会有控制信号的输入
  // 这样就比较怪了，那我当前的阶段需要将控制信号传递给之后的阶段

  alu.io.alusel := id2ex_in.bits.ctrlsignals.alusel
  // 0 for rs1, 1 for pc
  alu.io.A := Mux(!id2ex_in.bits.ctrlsignals.asel, id2ex_in.bits.rs1, id2ex_in.bits.pc)
  // 0 for rs2, 1 for imm
  alu.io.B := Mux(!id2ex_in.bits.ctrlsignals.bsel, id2ex_in.bits.rs2, id2ex_in.bits.immediate)

  ex2mem_out.bits.carry       := alu.io.carry
  ex2mem_out.bits.overflow    := alu.io.overflow
  ex2mem_out.bits.alures      := alu.io.res
  ex2mem_out.bits.zero        := alu.io.zero
  ex2mem_out.bits.ctrlsignals := id2ex_in.bits.ctrlsignals

  ex2mem_out.bits.pc       := id2ex_in.bits.pc
  ex2mem_out.bits.inst     := id2ex_in.bits.inst
  ex2mem_out.bits.csrvalue := id2ex_in.bits.csrvalue
  ex2mem_out.bits.rs2      := id2ex_in.bits.rs2

  ex2mem_out.bits.mepc  := id2ex_in.bits.mepc
  ex2mem_out.bits.mtvec := id2ex_in.bits.mtvec

  // ready, valid 信号全部设置成1
  // id2ex_in.ready  := 1.U
  // ex2mem_out.valid := 1.U

  id2ex_in.ready := id2ex_in.valid
  val exu_valid_reg = RegInit(0.U)
  ex2mem_out.valid := exu_valid_reg

  when(id2ex_in.valid) {
    exu_valid_reg := 1.U
  }.elsewhen(ex2mem_out.ready && ex2mem_out.valid) {
    exu_valid_reg := 0.U
  }

}
