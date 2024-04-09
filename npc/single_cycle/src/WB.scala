package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._

/** *******************WB***************************
  */

class WBOutputIO extends Bundle {
  // 暂时不太清楚 wb 需要输出什么
  val wbData         = Output(UInt(32.W))
  val wbNextpc       = Output(UInt(32.W))
  val regfileWriteEn = Output(Bool())
  val csrsWriteEn    = Output(Bool())
  val mepcWriteEn    = Output(Bool())
  val mcauseWriteEn  = Output(Bool())
  // 目前没有必要把 write_rd 也传过来, 因为这个写入地址是不会变的
}

class WB extends Module {
  val lsu2wbIn  = IO(Flipped(Decoupled(new MEMOutputIO(width))))
  val wb2ifuOut = IO(Decoupled(new WBOutputIO))

  val wbDataReg   = RegNext(wb2ifuOut.bits.wbData, 0.U)
  val wbNextpcReg = RegNext(wb2ifuOut.bits.wbNextpc, config.startPC.U)

  wb2ifuOut.bits.wbData   := wbDataReg
  wb2ifuOut.bits.wbNextpc := wbNextpcReg

  wbDataReg := MuxCase(
    0.U,
    Seq(
      (lsu2wbIn.bits.ctrlsignals.WBsel === 0.U) -> lsu2wbIn.bits.alures,
      (lsu2wbIn.bits.ctrlsignals.WBsel === 1.U) -> (lsu2wbIn.bits.pc + config.XLEN.U),
      (lsu2wbIn.bits.ctrlsignals.WBsel === 2.U) -> lsu2wbIn.bits.rdata,
      (lsu2wbIn.bits.ctrlsignals.WBsel === 3.U) -> lsu2wbIn.bits.csrvalue
    )
  )
  wbNextpcReg := MuxCase(
    0.U,
    Seq(
      (lsu2wbIn.bits.ctrlsignals.pcsel === 0.U) -> (lsu2wbIn.bits.pc + config.XLEN.U),
      (lsu2wbIn.bits.ctrlsignals.pcsel === 1.U) -> lsu2wbIn.bits.alures,
      (lsu2wbIn.bits.ctrlsignals.pcsel === 2.U) -> lsu2wbIn.bits.mepc,
      (lsu2wbIn.bits.ctrlsignals.pcsel === 3.U) -> lsu2wbIn.bits.mtvec
    )
  )

  val itrace = Module(new Dpi_itrace)
  itrace.io.pc     := lsu2wbIn.bits.pc
  itrace.io.inst   := lsu2wbIn.bits.inst
  itrace.io.nextpc := wbNextpcReg

  lsu2wbIn.ready := lsu2wbIn.valid
  val wb_valid = RegInit(1.U)
  wb2ifuOut.valid := wb_valid

  when(lsu2wbIn.valid) {
    wb_valid := 1.U
  }.elsewhen(wb2ifuOut.valid && wb2ifuOut.ready) {
    wb_valid := 0.U
  }

  wb2ifuOut.bits.regfileWriteEn := wb_valid & lsu2wbIn.bits.ctrlsignals.writeEn
  wb2ifuOut.bits.csrsWriteEn    := wb_valid & lsu2wbIn.bits.ctrlsignals.csrsWriteEn
  wb2ifuOut.bits.mepcWriteEn    := wb_valid & lsu2wbIn.bits.ctrlsignals.mepcWriteEn
  wb2ifuOut.bits.mcauseWriteEn  := wb_valid & lsu2wbIn.bits.ctrlsignals.mcauseWriteEn

}

class Dpi_itrace extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val pc     = Input(UInt(32.W))
    val inst   = Input(UInt(32.W))
    val nextpc = Input(UInt(32.W))
  })
  addResource("/Dpi_itrace.sv")
}
