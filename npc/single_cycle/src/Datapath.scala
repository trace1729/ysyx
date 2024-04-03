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

  val ifu     = Module(new IFU(memoryFile))
  val idu     = Module(new IDU)
  val ex      = Module(new EX)
  val mem     = Module(new LSU)
  val wb      = Module(new WB)
  val arbiter = Module(new Arbiter)

  val sram = Module(new SRAM)

  ifu.if2idOut <> idu.if2idIn
  idu.id2exOut <> ex.id2exIn
  ex.ex2memOut <> mem.ex2lsuIn
  mem.lsu2wbOut <> wb.lsu2wbIn
  wb.wb2ifuOut <> ifu.wb2ifIn

  // for axi interface
  arbiter.ifuIn <> ifu.ifuAxiOut
  arbiter.lsuIn <> mem.lsuAxiOut
  arbiter.out <> sram.in

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

object ArbiterState extends ChiselEnum {
  val sIDLE, sIFU, sLSU = Value
}

class Arbiter extends Module {
  val ifuIn = IO(Flipped(AxiLiteMaster(width, width)))
  val lsuIn = IO(Flipped(AxiLiteMaster(width, width)))
  val out   = IO(AxiLiteMaster(width, width))

  out := DontCare

  // 默认将 ar, wr, w 的 ready 置为 false
  // r, b 的 valid 置为 false
  // 其他信号不关心

  ifuIn.readAddr.ready  := false.B
  ifuIn.writeAddr.ready := false.B
  ifuIn.writeData.ready := false.B

  ifuIn.readData.valid  := false.B
  ifuIn.readData.bits   := DontCare
  ifuIn.writeResp.valid := false.B
  ifuIn.writeResp.bits  := false.B

  lsuIn.readAddr.ready  := false.B
  lsuIn.writeAddr.ready := false.B
  lsuIn.writeData.ready := false.B

  lsuIn.readData.valid  := false.B
  lsuIn.readData.bits   := DontCare
  lsuIn.writeResp.valid := false.B
  lsuIn.writeResp.bits  := false.B

  import ArbiterState._
  val arbiterState = RegInit(sIDLE)


  switch (arbiterState) {
    is (sIDLE) {
      when(ifuIn.writeAddr.valid || ifuIn.writeData.valid || ifuIn.readAddr.valid) {
        arbiterState := sIFU
      }.elsewhen(lsuIn.writeAddr.valid || lsuIn.writeData.valid || lsuIn.readAddr.valid) {
        arbiterState := sLSU
      }
    }
    is (sIFU) {
      out <> ifuIn
      when(ifuIn.readData.valid && ifuIn.readData.ready) {
        arbiterState := sIDLE
      }
    }
    is (sLSU) {
      out <> lsuIn
      when(lsuIn.readData.valid && lsuIn.readData.ready) {
        arbiterState := sIDLE
      }
      when (lsuIn.writeResp.valid && lsuIn.writeResp.ready) {
        arbiterState := sIDLE
      }
    }
  }


}
