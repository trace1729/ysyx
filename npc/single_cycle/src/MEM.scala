
package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._

class LSU extends Module {
  val in            = IO(Flipped(Decoupled(new EXOutputIO)))
  val out           = IO(Decoupled(new MEMOutputIO(width)))
  val axiController = Module(AxiController(width, width))
  val sram          = Module(new SRAM)

  // 看看能不能在 dmem 上加一层 wrapper around, 这样不用修改代码，就可以完成 axi 总线的接入
  // hope we can do this!!

  // activate the axiController
  axiController.in.externalAddress := in.bits.alures
  axiController.in.externalMemRW   := in.bits.ctrlsignals.memRW
  axiController.in.externalMemEn   := in.bits.ctrlsignals.memEnable
  axiController.in.externalData    := in.bits.rs2
  axiController.in.externalWmask := Mux(
    !in.bits.ctrlsignals.memRW,
    0.U,
    wmaskGen(in.bits.inst(14, 12), in.bits.alures(1, 0))
  )
  axiController.in.externalValid := in.valid

  axiController.axi <> sram.in

  // 处理读取的数据
  val rmemdata = Wire(UInt(width.W))
  // if (mem.io.memRW) set wmask to 0b0000
  // mem.io.memRW = 0, read, set to 0
  val imm_byte = Wire(UInt(8.W))
  val imm_half = Wire(UInt(16.W))
  imm_byte := readDataGen(in.bits.alures(1, 0), 1, axiController.axi.readData.bits.data)
  imm_half := readDataGen(in.bits.alures(1, 0), 2, axiController.axi.readData.bits.data)
  rmemdata := Mux(
    in.bits.inst(14),
    // io.inst(14) == 1, unsigned 直接截断就好
    MuxCase(
      axiController.axi.readData.bits.data,
      Seq(
        (in.bits.inst(13, 12) === 0.U) -> imm_byte,
        (in.bits.inst(13, 12) === 1.U) -> imm_half
      )
    ),
    // io.inst(14) == 0, signed 还需符号扩展
    MuxCase(
      axiController.axi.readData.bits.data,
      Seq(
        (in.bits.inst(13, 12) === 0.U) -> Cat(padding(24, imm_byte(7)), imm_byte),
        (in.bits.inst(13, 12) === 1.U) -> Cat(padding(16, imm_half(15)), imm_half)
      )
    )
  )

  // 输出
  out.bits.alures      := in.bits.alures
  out.bits.pc          := in.bits.pc
  out.bits.csrvalue    := in.bits.csrvalue
  out.bits.ctrlsignals := in.bits.ctrlsignals
  out.bits.rdata       := rmemdata
  out.bits.inst        := in.bits.inst

  //csr
  out.bits.mepc  := in.bits.mepc
  out.bits.mtvec := in.bits.mtvec

  // 处理握手信号
  val lsu_valid_reg = RegInit(0.U)
  in.ready := in.valid
  out.valid := MuxCase(
    0.U,
    Seq(
      (in.bits.ctrlsignals.memEnable === 0.U) -> lsu_valid_reg,
      (in.bits.ctrlsignals.memEnable === 1.U) -> axiController.transactionEnded
    )
  )

  when(in.valid) {
    lsu_valid_reg := 1.U
  }.elsewhen(out.valid && out.ready) {
    lsu_valid_reg := 0.U
  }
}
class MEMOutputIO(width: Int) extends Bundle {
  val pc          = Output(UInt(width.W))
  val inst        = Output(UInt(width.W))
  val ctrlsignals = Output(new ctrlSignals)
  val csrvalue    = Output(UInt(width.W))
  val alures      = Output(UInt(width.W))
  val rdata       = Output(UInt(width.W))
  val mepc        = Output(UInt(width.W))
  val mtvec       = Output(UInt(width.W))
}
