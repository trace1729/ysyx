
package cpu

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
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
  val if2idIn       = IO(Flipped(Decoupled(new IFUOutputIO)))
  val id2exOut      = IO(DecoupledIO(new IDUOutputIO))

  val regfile   = Module(new Regfile(num = regsNum, width = width))
  val ctrlLogic = Module(new controlLogic(width))
  val immgen    = Module(new ImmGen(width))
  val csr       = Module(new CSR(10, width))


  // pipeline registers
  val if2idReg = RegInit(
    (new IFUOutputIO).Lit(
      _.inst -> config.NOP,
      _.pc -> 0.U
    )
  )
   


  // 输入的 ready 跟随 valid
  if2idIn.ready := if2idIn.valid

  // valid 信号
  val idu_valid_reg = RegInit(0.U)
  val idu_inst_reg  = RegInit(UInt(32.W), config.NOP)
  val idu_pc_reg    = RegInit(UInt(32.W), 0.U)

  id2exOut.valid := idu_valid_reg

  when(if2idIn.valid) {
    idu_valid_reg := 1.U
  }.elsewhen(id2exOut.valid && id2exOut.ready) {
    idu_valid_reg := 0.U
  }

  when(if2idIn.valid) {
    idu_inst_reg := if2idIn.bits.inst
    idu_pc_reg   := if2idIn.bits.pc
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
  id2exOut.bits.ctrlsignals := ctrlLogic.io.ctrlsignals

  // idu 模块的输出
  id2exOut.bits.rs1       := regfile.io.rs1
  id2exOut.bits.rs2       := regfile.io.rs2
  id2exOut.bits.immediate := immgen.io.imm
  id2exOut.bits.pc        := idu_pc_reg
  id2exOut.bits.inst      := idu_inst_reg
  id2exOut.bits.csrvalue  := csr.io.csrValue

  id2exOut.bits.mepc  := csr.io.mepc
  id2exOut.bits.mtvec := csr.io.mtvec

}
