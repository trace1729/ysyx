package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._
import chisel3.experimental.BundleLiterals._

/** *******************WB***************************
  */

class WBOutputIO extends Bundle {
  // 暂时不太清楚 wb 需要输出什么
  val rd             = Output(UInt(5.W))
  val wbData         = Output(UInt(32.W))
  val regfileWriteEn = Output(Bool())
  val csrsWriteEn    = Output(Bool())
  val mepcWriteEn    = Output(Bool())
  val mcauseWriteEn  = Output(Bool())
  // 目前没有必要把 write_rd 也传过来, 因为这个写入地址是不会变的
}

class WB extends Module {
  val lsu2wbIn  = IO(Flipped(Decoupled(new MEMOutputIO(width))))
  val wb2ifuOut = IO(Decoupled(new WBOutputIO))

  // 一定能在一个周期内完成写入，所以一直为 1
  lsu2wbIn.ready := 1.U

  val lsu2wbReg = RegInit(
    (new MEMOutputIO(config.width)).Lit(
      _.rd -> 0.U,
      _.ctrlsignals -> (new ctrlSignals).Lit(
        _.pcsel -> 0.U,
        _.writeEn -> false.B,
        _.immsel -> 0.U,
        _.asel -> false.B,
        _.bsel -> false.B,
        _.alusel -> 0.U,
        _.memRW -> false.B,
        _.memEnable -> false.B,
        _.WBsel -> 0.U,
        _.optype -> 0.U,
        _.csrsWriteEn -> false.B,
        _.mepcWriteEn -> false.B,
        _.mcauseWriteEn -> false.B
      ),
      _.pc -> 0.U,
      _.csrvalue -> 0.U,
      _.alures -> 0.U,
      _.rdata -> 0.U
    )
  )

  // 当同时检测到 valid 和 ready 信号时, 将上一个阶段传过来的数据保存在寄存器中

  when(lsu2wbIn.valid && lsu2wbIn.ready) {
    lsu2wbReg.rd          := lsu2wbIn.bits.rd
    lsu2wbReg.ctrlsignals := lsu2wbIn.bits.ctrlsignals
    lsu2wbReg.pc          := lsu2wbIn.bits.pc
    lsu2wbReg.csrvalue    := lsu2wbIn.bits.csrvalue
    lsu2wbReg.alures      := lsu2wbIn.bits.alures
    lsu2wbReg.rdata       := lsu2wbIn.bits.rdata
  }

  // wb 阶段的状态转移
  import stageState._
  val wbState = RegInit(sIDLE)

  switch(wbState) {
    is(sIDLE) {
      when(lsu2wbIn.valid && lsu2wbIn.ready) {
        wbState := sACK
      }
    }
    is(sACK) {
      when(wb2ifuOut.valid && wb2ifuOut.ready) {
        wbState := sIDLE
      }
    }
  }

  wb2ifuOut.bits.wbData := MuxCase(
    0.U,
    Seq(
      (lsu2wbReg.ctrlsignals.WBsel === 0.U) -> lsu2wbReg.alures,
      (lsu2wbReg.ctrlsignals.WBsel === 1.U) -> (lsu2wbReg.pc + config.XLEN.U),
      (lsu2wbReg.ctrlsignals.WBsel === 2.U) -> lsu2wbReg.rdata,
      (lsu2wbReg.ctrlsignals.WBsel === 3.U) -> lsu2wbReg.csrvalue
    )
  )

  wb2ifuOut.valid := wbState === sACK

  wb2ifuOut.bits.regfileWriteEn := wb2ifuOut.valid & lsu2wbIn.bits.ctrlsignals.writeEn
  wb2ifuOut.bits.csrsWriteEn    := wb2ifuOut.valid & lsu2wbIn.bits.ctrlsignals.csrsWriteEn
  wb2ifuOut.bits.mepcWriteEn    := wb2ifuOut.valid & lsu2wbIn.bits.ctrlsignals.mepcWriteEn
  wb2ifuOut.bits.mcauseWriteEn  := wb2ifuOut.valid & lsu2wbIn.bits.ctrlsignals.mcauseWriteEn

  wb2ifuOut.bits.rd := lsu2wbReg.rd

  // 写回总是能够一周期内结束，所以设计 ready 信号为 1
  // 延迟一周期触发 difftest
  val instEnded = RegInit(false.B)
  instEnded := wb2ifuOut.valid
  
  val next_inst = Module(new Next_inst)
  next_inst.io.ready := instEnded
  next_inst.io.valid := instEnded
}

class Next_inst extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val valid = Input(Bool())
    val ready = Input(Bool())
  })
  addResource("/Next_inst.sv")
}