
package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._


/** *******************WB***************************
  */

class WBOutputIO extends Bundle {
  // 暂时不太清楚 wb 需要输出什么
  val wb_data        = Output(UInt(32.W))
  val wb_nextpc      = Output(UInt(32.W))
  val regfileWriteEn = Output(Bool())
  val csrsWriteEn    = Output(Bool())
  val mepcWriteEn    = Output(Bool())
  val mcauseWriteEn  = Output(Bool())
  // 目前没有必要把 write_rd 也传过来, 因为这个写入地址是不会变的
}

class WB extends Module {
  val lsu2wb_in  = IO(Flipped(Decoupled(new MEMOutputIO(width))))
  val wb2ifu_out = IO(Decoupled(new WBOutputIO))

  val wb_data_reg   = RegNext(wb2ifu_out.bits.wb_data, 0.U)
  val wb_nextpc_reg = RegNext(wb2ifu_out.bits.wb_nextpc, config.startPC.U)

  wb2ifu_out.bits.wb_data   := wb_data_reg
  wb2ifu_out.bits.wb_nextpc := wb_nextpc_reg

  when(lsu2wb_in.valid) {
    wb_data_reg := MuxCase(
      0.U,
      Seq(
        (lsu2wb_in.bits.ctrlsignals.WBsel === 0.U) -> lsu2wb_in.bits.alures,
        (lsu2wb_in.bits.ctrlsignals.WBsel === 1.U) -> (lsu2wb_in.bits.pc + config.XLEN.U),
        (lsu2wb_in.bits.ctrlsignals.WBsel === 2.U) -> lsu2wb_in.bits.rdata,
        (lsu2wb_in.bits.ctrlsignals.WBsel === 3.U) -> lsu2wb_in.bits.csrvalue
      )
    )
    wb_nextpc_reg := MuxCase(
      0.U,
      Seq(
        (lsu2wb_in.bits.ctrlsignals.pcsel === 0.U) -> (lsu2wb_in.bits.pc + config.XLEN.U),
        (lsu2wb_in.bits.ctrlsignals.pcsel === 1.U) -> lsu2wb_in.bits.alures,
        (lsu2wb_in.bits.ctrlsignals.pcsel === 2.U) -> lsu2wb_in.bits.mepc,
        (lsu2wb_in.bits.ctrlsignals.pcsel === 3.U) -> lsu2wb_in.bits.mtvec
      )
    )
  }

  val itrace = Module(new Dpi_itrace)
  itrace.io.pc     := lsu2wb_in.bits.pc
  itrace.io.inst   := lsu2wb_in.bits.inst
  itrace.io.nextpc := wb_nextpc_reg

  lsu2wb_in.ready := lsu2wb_in.valid
  val wb_valid = RegInit(1.U)
  wb2ifu_out.valid := wb_valid

  when(lsu2wb_in.valid) {
    wb_valid := 1.U
  }.elsewhen(wb2ifu_out.valid && wb2ifu_out.ready) {
    wb_valid := 0.U
  }

  wb2ifu_out.bits.regfileWriteEn := wb_valid & lsu2wb_in.bits.ctrlsignals.writeEn
  wb2ifu_out.bits.csrsWriteEn    := wb_valid & lsu2wb_in.bits.ctrlsignals.csrsWriteEn
  wb2ifu_out.bits.mepcWriteEn    := wb_valid & lsu2wb_in.bits.ctrlsignals.mepcWriteEn
  wb2ifu_out.bits.mcauseWriteEn  := wb_valid & lsu2wb_in.bits.ctrlsignals.mcauseWriteEn

}

class Dpi_itrace extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val pc     = Input(UInt(32.W))
    val inst   = Input(UInt(32.W))
    val nextpc = Input(UInt(32.W))
  })
  addResource("/Dpi_itrace.sv")
}

