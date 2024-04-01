
package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._

/** *******************IDU***************************
  */

class IDUOutputIO extends Bundle {
  val rs1         = Output(UInt(width.W))
  val rs2         = Output(UInt(width.W))
  val immediate   = Output(UInt(width.W))
  val ctrlsignals = Output(new ctrlSignals)

  val pc       = Output(UInt(width.W))
  val inst     = Output(UInt(width.W))
  val csrvalue = Output(UInt(width.W))
  val mepc     = Output(UInt(width.W))
  val mtvec    = Output(UInt(width.W))
}

class IDU extends Module {
  val data           = IO(Input(UInt(width.W)))
  val regfileWriteEn = IO(Input(Bool()))
  val csrsWriteEn    = IO(Input(Bool()))
  val mepcWriteEn    = IO(Input(Bool()))
  val mcauseWriteEn  = IO(Input(Bool()))
  val if2id_in       = IO(Flipped(Decoupled(new IFUOutputIO)))
  val id2ex_out      = IO(DecoupledIO(new IDUOutputIO))

  val regfile   = Module(new Regfile(num = regsNum, width = width))
  val ctrlLogic = Module(new controlLogic(width))
  val immgen    = Module(new ImmGen(width))
  val csr       = Module(new CSR(10, width))

  // 输入的 ready 跟随 valid
  if2id_in.ready := if2id_in.valid

  // valid 信号
  val idu_valid_reg = RegInit(0.U)
  val idu_inst_reg  = RegInit(UInt(32.W), config.NOP)
  val idu_pc_reg    = RegInit(UInt(32.W), 0.U)

  id2ex_out.valid := idu_valid_reg

  when(if2id_in.valid) {
    idu_valid_reg := 1.U
  }.elsewhen(id2ex_out.valid && id2ex_out.ready) {
    idu_valid_reg := 0.U
  }

  when(if2id_in.valid) {
    idu_inst_reg := if2id_in.bits.inst
    idu_pc_reg   := if2id_in.bits.pc
  }

  // 寄存器文件的连接
  regfile.io.readreg1 := idu_inst_reg(19, 15)
  regfile.io.readreg2 := idu_inst_reg(24, 20)
  regfile.io.writereg := idu_inst_reg(11, 7)
  regfile.io.writeEn  := regfileWriteEn
  regfile.io.data     := data

  // 控制逻辑的连接
  ctrlLogic.io.inst := idu_inst_reg
  ctrlLogic.io.rs1  := regfile.io.rs1
  ctrlLogic.io.rs2  := regfile.io.rs2

  // 立即数生成器的连接
  immgen.io.inst   := idu_inst_reg
  immgen.io.immsel := ctrlLogic.io.ctrlsignals.immsel

  // csr 寄存器文件的连接
  csr.io.csrsWriteEn := csrsWriteEn
  csr.io.csrNo       := immgen.io.imm
  // 只考虑 csrw, 所以直接把 rs1 寄存器的值写入 CSRs[csr_no]
  csr.io.data := regfile.io.rs1
  // 需要写回寄存器文件的值
  csr.io.mcauseData    := 0xb.U
  csr.io.mcauseWriteEn := mcauseWriteEn
  csr.io.mepcData      := idu_pc_reg
  csr.io.mepcWriteEn   := mepcWriteEn

  // 生成控制信号
  id2ex_out.bits.ctrlsignals := ctrlLogic.io.ctrlsignals

  // idu 模块的输出
  id2ex_out.bits.rs1       := regfile.io.rs1
  id2ex_out.bits.rs2       := regfile.io.rs2
  id2ex_out.bits.immediate := immgen.io.imm
  id2ex_out.bits.pc        := idu_pc_reg
  id2ex_out.bits.inst      := idu_inst_reg
  id2ex_out.bits.csrvalue  := csr.io.csrValue

  id2ex_out.bits.mepc  := csr.io.mepc
  id2ex_out.bits.mtvec := csr.io.mtvec

}
