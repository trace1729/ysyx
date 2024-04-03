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
  

  ifu.if2idOut <> idu.if2idIn
  idu.id2exOut <> ex.id2exIn
  ex.ex2memOut <> mem.ex2lsuIn
  mem.ex2lsuOut <> wb.lsu2wbIn
  wb.wb2ifuOut <> ifu.wb2ifIn

  // for axi interface
  
  ifu.ifuAxiOut <> sram.ifuIn
  mem.lsuAxiOut <> sram.lsuIn
  sram.ifuEnable := ifu.ifuEnable

  // 诡异的连线，上面各阶段之间的握手突出一个毫无意义 (确定 pc 和 寄存器的写回值)
  idu.data           := wb.wb2ifuOut.bits.wbData
  idu.regfileWriteEn := wb.wb2ifuOut.bits.regfileWriteEn
  idu.csrsWriteEn    := wb.wb2ifuOut.bits.csrsWriteEn
  idu.mcauseWriteEn  := wb.wb2ifuOut.bits.mcauseWriteEn
  idu.mepcWriteEn    := wb.wb2ifuOut.bits.mepcWriteEn

  // datapath 的输出
  io.inst := ifu.if2idOut.bits.inst
  io.pc   := ifu.if2idOut.bits.pc
}

