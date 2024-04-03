package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._
import scala.annotation.varargs
import os.read
import org.yaml.snakeyaml.events.Event.ID

class LSU extends Module {
  val ex2lsuIn      = IO(Flipped(Decoupled(new EXOutputIO)))
  val lsuAxiOut     = IO(AxiLiteMaster(width, width))
  val lsu2wbOut     = IO(Decoupled(new MEMOutputIO(width)))
  val axiController = Module(AxiController(width, width))

  lsuAxiOut <> axiController.axiOut

  // activate the axiController
  axiController.stageInput.readAddr.valid  := false.B
  axiController.stageInput.writeData.valid := false.B
  axiController.stageInput.writeAddr.valid := false.B

  axiController.stageInput.writeData.bits.data := ex2lsuIn.bits.rs2
  axiController.stageInput.writeData.bits.strb := Mux(
    !ex2lsuIn.bits.ctrlsignals.memRW,
    0.U,
    wmaskGen(ex2lsuIn.bits.inst(14, 12), ex2lsuIn.bits.alures(1, 0))
  )

  axiController.stageInput.writeAddr.bits.addr := ex2lsuIn.bits.alures
  axiController.stageInput.readAddr.bits.addr  := ex2lsuIn.bits.alures

  // valid 跟随 ready
  axiController.stageInput.writeResp.ready := axiController.stageInput.writeResp.valid
  axiController.stageInput.readData.ready  := axiController.stageInput.readData.valid

  import stageState._
  val lsu_state = RegInit(sIDLE)

  ex2lsuIn.ready := ex2lsuIn.valid

  switch(lsu_state) {
    is(sIDLE) {
      when(ex2lsuIn.valid && ex2lsuIn.bits.ctrlsignals.memEnable) {
        lsu_state := sWaitReady
      }
    }
    is(sWaitReady) {

      axiController.stageInput.readAddr.valid  := Mux(ex2lsuIn.bits.ctrlsignals.memRW === 0.U, true.B, false.B)
      axiController.stageInput.writeAddr.valid := Mux(ex2lsuIn.bits.ctrlsignals.memRW === 1.U, true.B, false.B)
      axiController.stageInput.writeData.valid := Mux(ex2lsuIn.bits.ctrlsignals.memRW === 1.U, true.B, false.B)

      when(axiController.stageInput.readAddr.valid && axiController.stageInput.readAddr.ready) {
        lsu_state := Mux(ex2lsuIn.bits.ctrlsignals.memRW === 0.U, sACK, sIDLE)
      }

      when(
        axiController.stageInput.writeAddr.valid && axiController.stageInput.writeAddr.ready && axiController.stageInput.writeData.valid && axiController.stageInput.writeData.ready
      ) {
        lsu_state := Mux(ex2lsuIn.bits.ctrlsignals.memRW === 1.U, sACK, sIDLE)
      }

    }
    is(sACK) {
      when(axiController.stageInput.readData.valid && axiController.stageInput.readData.ready) {
        lsu_state := sCompleted
      }
      when(axiController.stageInput.writeResp.valid && axiController.stageInput.writeResp.ready) {
        lsu_state := sCompleted
      }
    }
    is(sCompleted) {
      when (lsu2wbOut.valid && lsu2wbOut.ready) {
        lsu_state := sIDLE
      }
    }
  }

  val readCompleted = axiController.stageInput.readData.valid && axiController.stageInput.readData.ready
  val readData      = RegEnable(axiController.stageInput.readData.bits.data, 0.U, readCompleted)

  // 处理读取的数据
  val rmemdata = Wire(UInt(width.W))
  // if (mem.io.memRW) set wmask to 0b0000
  // mem.io.memRW = 0, read, set to 0
  val imm_byte = Wire(UInt(8.W))
  val imm_half = Wire(UInt(16.W))
  imm_byte := readDataGen(ex2lsuIn.bits.alures(1, 0), 1, readData)
  imm_half := readDataGen(ex2lsuIn.bits.alures(1, 0), 2, readData)
  rmemdata := Mux(
    ex2lsuIn.bits.inst(14),
    // io.inst(14) == 1, unsigned 直接截断就好
    MuxCase(
      readData,
      Seq(
        (ex2lsuIn.bits.inst(13, 12) === 0.U) -> imm_byte,
        (ex2lsuIn.bits.inst(13, 12) === 1.U) -> imm_half
      )
    ),
    // io.inst(14) == 0, signed 还需符号扩展
    MuxCase(
      readData,
      Seq(
        (ex2lsuIn.bits.inst(13, 12) === 0.U) -> Cat(padding(24, imm_byte(7)), imm_byte),
        (ex2lsuIn.bits.inst(13, 12) === 1.U) -> Cat(padding(16, imm_half(15)), imm_half)
      )
    )
  )

  // 输出
  lsu2wbOut.bits.alures      := ex2lsuIn.bits.alures
  lsu2wbOut.bits.pc          := ex2lsuIn.bits.pc
  lsu2wbOut.bits.csrvalue    := ex2lsuIn.bits.csrvalue
  lsu2wbOut.bits.ctrlsignals := ex2lsuIn.bits.ctrlsignals
  lsu2wbOut.bits.rdata       := rmemdata
  lsu2wbOut.bits.inst        := ex2lsuIn.bits.inst

  //csr
  lsu2wbOut.bits.mepc  := ex2lsuIn.bits.mepc
  lsu2wbOut.bits.mtvec := ex2lsuIn.bits.mtvec

  // 如果该条指令有访问内存的阶段，那么看是读取还是写入，根据读写的 response 信号，来决定是否结束 mem 阶段

  // 处理握手信号
  val lsu_valid_reg = RegInit(0.U)
  // 对下一级握手信号的生成
  ex2lsuIn.ready := ex2lsuIn.valid
  when(ex2lsuIn.valid) {
    lsu_valid_reg := 1.U
  }.elsewhen(lsu2wbOut.valid && lsu2wbOut.ready) {
    lsu_valid_reg := 0.U
  }
  lsu2wbOut.valid := MuxCase(
    0.U,
    Seq(
      (ex2lsuIn.bits.ctrlsignals.memEnable === 0.U) -> lsu_valid_reg,
      (ex2lsuIn.bits.ctrlsignals.memEnable === 1.U) -> (lsu_state === sCompleted)
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
