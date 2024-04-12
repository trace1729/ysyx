package cpu

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import cpu.config._
import cpu.utils._

/** *******************IDU***************************
  */

class IDUOutputIO extends Bundle {
  val rs1        = Output(UInt(width.W))
  val rs2        = Output(UInt(width.W))
  val rd         = Output(UInt(width.W))

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

  val backwardRd = IO(Input(UInt(width.W)))
  val if2idIn        = IO(Flipped(Decoupled(new IFUOutputIO)))
  val id2lsuOut      = IO(DecoupledIO(new IDUOutputIO))

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

  // 理想情况西 idu 总是能在一周期内完成译码，所以将 ready 恒置为 1
  if2idIn.ready := 1.U

  // 当握手成功时，将数据锁存到寄存器中
  when(if2idIn.valid && if2idIn.ready) {
    if2idReg.inst := if2idIn.bits.inst
    if2idReg.pc   := if2idIn.bits.pc
  }

  import stageState._

  val iduState = RegInit(sIDLE)

  switch(iduState) {
    // 这里需要状态转化是因为需要等 数据存入寄存器中
    is(sIDLE) {
      when(if2idIn.valid && if2idIn.ready) {
        iduState := sACK
      }
    }
    is(sACK) {
      when(id2lsuOut.valid && id2lsuOut.ready) {
        iduState := sIDLE
      }
    }
  }

  id2lsuOut.valid := (iduState === sACK)

  // 寄存器文件的连接
  regfile.io.readreg1 := if2idReg.inst(19, 15)
  regfile.io.readreg2 := if2idReg.inst(24, 20)
  regfile.io.writereg := backwardRd
  regfile.io.writeEn  := regfileWriteEn
  regfile.io.data     := data

  // 控制逻辑的连接
  ctrlLogic.io.inst := if2idReg.inst
  ctrlLogic.io.rs1  := regfile.io.rs1
  ctrlLogic.io.rs2  := regfile.io.rs2

  // 立即数生成器的连接
  immgen.io.inst   := if2idReg.inst
  immgen.io.immsel := ctrlLogic.io.ctrlsignals.immsel

  // csr 寄存器文件的连接
  csr.io.csrsWriteEn := csrsWriteEn
  csr.io.csrNo       := immgen.io.imm
  // 只考虑 csrw, 所以直接把 rs1 寄存器的值写入 CSRs[csr_no]
  csr.io.data := regfile.io.rs1
  // 需要写回寄存器文件的值
  csr.io.mcauseData    := 0xb.U
  csr.io.mcauseWriteEn := mcauseWriteEn
  csr.io.mepcData      := if2idReg.pc
  csr.io.mepcWriteEn   := mepcWriteEn

  // 生成控制信号
  id2lsuOut.bits.ctrlsignals := ctrlLogic.io.ctrlsignals

  // idu 模块的输出
  id2lsuOut.bits.rs1       := regfile.io.rs1
  id2lsuOut.bits.rs2       := regfile.io.rs2
  id2lsuOut.bits.rd        := if2idReg.inst(11, 7)
  id2lsuOut.bits.immediate := immgen.io.imm
  id2lsuOut.bits.pc        := if2idReg.pc
  id2lsuOut.bits.inst      := if2idReg.inst
  id2lsuOut.bits.csrvalue  := csr.io.csrValue

  id2lsuOut.bits.mepc  := csr.io.mepc
  id2lsuOut.bits.mtvec := csr.io.mtvec

}
