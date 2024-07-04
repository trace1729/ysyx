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

  val io = IO(new Bundle {
    val interrupt = Input(Bool())
    val master    = AxiLiteMaster(width, width)
    val slave     = AxiLiteSlave(width, width)
  })

  io.slave := DontCare
  io.master := DontCare

  val ifu     = Module(new IFU(memoryFile))
  val idu     = Module(new IDU)
  val lsu     = Module(new LSU)
  val wb      = Module(new WB)
  val arbiter = Module(new myArbiter)
  val uart    = Module(new Uart)
  val rtc     = Module(new RTC)

  val sram = Module(new SRAM)

  ifu.if2idOut <> idu.if2idIn
  idu.id2lsuOut <> lsu.id2lsuIn
  lsu.lsu2wbOut <> wb.lsu2wbIn
  wb.wb2ifuOut <> ifu.wb2ifIn

  // for axi interface
  arbiter.ifuIn <> ifu.ifuAxiOut
  arbiter.lsuIn <> lsu.lsuAxiOut
  arbiter.sram <> sram.in
  arbiter.uart <> uart.in
  arbiter.rtc <> rtc.in

  // 诡异的连线，上面各阶段之间的握手突出一个毫无意义 (确定 pc 和 寄存器的写回值)
  // pc 值前递
  ifu.npc  := lsu.lsu2wbOut.bits.npc
  ifu.jump := lsu.jump
  // 寄存器写回
  idu.data           := wb.wb2ifuOut.bits.wbData
  idu.backwardRd     := wb.wb2ifuOut.bits.rd
  idu.regfileWriteEn := wb.wb2ifuOut.bits.regfileWriteEn
  // csr 寄存器写回
  idu.csrsWriteEn   := wb.wb2ifuOut.bits.csrsWriteEn
  idu.mcauseWriteEn := wb.wb2ifuOut.bits.mcauseWriteEn
  idu.mepcWriteEn   := wb.wb2ifuOut.bits.mepcWriteEn

  // abondon
  // datapath 的输出
  // io.inst := ifu.if2idOut.bits.inst
  // io.pc   := ifu.if2idOut.bits.pc
}
