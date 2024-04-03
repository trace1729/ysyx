package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._

/** ****************** 数据通路 ****************************
  */
class DatapathIO extends Bundle {
  val pc   = Output(UInt(width.W))
  val inst = Output(UInt(width.W))

}

class Datapath(memoryFile: String) extends Module {
  val io = IO(new DatapathIO)

  val ifu = Module(new IFU(memoryFile))
  val idu = Module(new IDU)
  val ex  = Module(new EX)
  val mem = Module(new LSU)
  val wb  = Module(new WB)

  val sram = Module(new SRAM)
  

  ifu.if2idOut <> idu.if2id_in
  idu.id2ex_out <> ex.id2ex_in
  ex.ex2mem_out <> mem.in
  mem.out <> wb.lsu2wb_in
  wb.wb2ifu_out <> ifu.wb2if_in

  // for axi interface
  
  ifu.ifu_axi_out <> sram.ifuIn
  mem.lsu_axi_out <> sram.lsuIn
  sram.ifuEnable := ifu.ifu_enable

  // 诡异的连线，上面各阶段之间的握手突出一个毫无意义 (确定 pc 和 寄存器的写回值)
  idu.data           := wb.wb2ifu_out.bits.wb_data
  idu.regfileWriteEn := wb.wb2ifu_out.bits.regfileWriteEn
  idu.csrsWriteEn    := wb.wb2ifu_out.bits.csrsWriteEn
  idu.mcauseWriteEn  := wb.wb2ifu_out.bits.mcauseWriteEn
  idu.mepcWriteEn    := wb.wb2ifu_out.bits.mepcWriteEn

  // datapath 的输出
  io.inst := ifu.if2idOut.bits.inst
  io.pc   := ifu.if2idOut.bits.pc
}

