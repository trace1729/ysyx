package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._

class LSU extends Module {
  val in            = IO(Flipped(Decoupled(new EXOutputIO)))
  val lsu_axi_out   = IO(AxiLiteMaster(width, width))
  val out           = IO(Decoupled(new MEMOutputIO(width)))
  val axiController = Module(AxiController(width, width))

  lsu_axi_out <> axiController.axiOut

  // activate the axiController
  axiController.stageInput.readAddr.valid  := false.B
  axiController.stageInput.writeData.valid := false.B
  axiController.stageInput.writeAddr.valid := false.B

  axiController.stageInput.writeData.bits.data := in.bits.rs2
  axiController.stageInput.writeData.bits.strb := Mux(
    !in.bits.ctrlsignals.memRW,
    0.U,
    wmaskGen(in.bits.inst(14, 12), in.bits.alures(1, 0))
  )

  axiController.stageInput.writeAddr.bits.addr := in.bits.alures
  axiController.stageInput.readAddr.bits.addr  := in.bits.alures

  // valid 跟随 ready
  axiController.stageInput.writeResp.ready := axiController.stageInput.writeResp.valid
  axiController.stageInput.readData.ready  := axiController.stageInput.readData.valid

  import stageState._
  val lsu_state = RegInit(sIDLE)

  in.ready := in.valid

  switch(lsu_state) {
    is(sIDLE) {
      when(in.valid && in.bits.ctrlsignals.memEnable) {
        lsu_state := sWaitReady
      }
    }
    is(sWaitReady) {

      axiController.stageInput.readAddr.valid  := Mux(in.bits.ctrlsignals.memRW === 0.U, true.B, false.B)
      axiController.stageInput.writeAddr.valid := Mux(in.bits.ctrlsignals.memRW === 1.U, true.B, false.B)
      axiController.stageInput.writeData.valid := Mux(in.bits.ctrlsignals.memRW === 1.U, true.B, false.B)

      when(axiController.stageInput.readData.valid && axiController.stageInput.readData.ready) {
        lsu_state := sIDLE
      }

      when(axiController.stageInput.writeResp.valid && axiController.stageInput.writeResp.ready) {
        lsu_state := sIDLE
      }
    }
  }

  // 处理读取的数据
  val rmemdata = Wire(UInt(width.W))
  // if (mem.io.memRW) set wmask to 0b0000
  // mem.io.memRW = 0, read, set to 0
  val imm_byte = Wire(UInt(8.W))
  val imm_half = Wire(UInt(16.W))
  imm_byte := readDataGen(in.bits.alures(1, 0), 1, axiController.stageInput.readData.bits.data)
  imm_half := readDataGen(in.bits.alures(1, 0), 2, axiController.stageInput.readData.bits.data)
  rmemdata := Mux(
    in.bits.inst(14),
    // io.inst(14) == 1, unsigned 直接截断就好
    MuxCase(
      axiController.stageInput.readData.bits.data,
      Seq(
        (in.bits.inst(13, 12) === 0.U) -> imm_byte,
        (in.bits.inst(13, 12) === 1.U) -> imm_half
      )
    ),
    // io.inst(14) == 0, signed 还需符号扩展
    MuxCase(
      axiController.stageInput.readData.bits.data,
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

  // 如果该条指令有访问内存的阶段，那么看是读取还是写入，根据读写的 response 信号，来决定是否结束 mem 阶段
  val memtransActionEnded = MuxCase(
    0.U,
    Seq(
      (in.bits.ctrlsignals.memRW === 0.U) -> (axiController.stageInput.readData.valid && axiController.stageInput.readData.ready),
      (in.bits.ctrlsignals.memRW === 1.U) -> (axiController.stageInput.writeResp.valid && axiController.stageInput.writeResp.ready)
    )
  )

  // 处理握手信号
  val lsu_valid_reg = RegInit(0.U)
  // 对下一级握手信号的生成
  in.ready := in.valid
  when(in.valid) {
    lsu_valid_reg := 1.U
  }.elsewhen(out.valid && out.ready) {
    lsu_valid_reg := 0.U
  }
  out.valid := MuxCase(
    0.U,
    Seq(
      (in.bits.ctrlsignals.memEnable === 0.U) -> lsu_valid_reg,
      (in.bits.ctrlsignals.memEnable === 1.U) -> memtransActionEnded
    )
  )

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
