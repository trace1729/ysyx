package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._

/** ********************IFU**************************
  */

class IFUOutputIO extends Bundle {
  val pc   = Output(UInt(width.W))
  val inst = Output(UInt(width.W))
}

object stageState extends ChiselEnum {
  val sIDLE, sWaitReady, sACK = Value
}

// the axiController shall connected to the memArbiter
// how to? axiController is defined inside the ifu, how can it connect to

class IFU(memoryFile: String) extends Module {
  val wb2if_in    = IO(Flipped(Decoupled(new WBOutputIO)))
  val if2idOut   = IO(Decoupled(new IFUOutputIO))
  val ifu_axi_out = IO(AxiLiteMaster(width, width))
  val ifu_enable  = IO(Output(Bool()))

  val axiController = Module(AxiController(width, width))

  ifu_axi_out <> axiController.axiOut

  import stageState._
  val ifu_state = RegInit(sIDLE)

  if2idOut.bits.pc := RegEnable(wb2if_in.bits.wb_nextpc, config.startPC.U, wb2if_in.valid)

  // if2id_out.bits.inst := Cat(instMem.io.inst)
  // instMem.io.pc       := if2id_out.bits.pc

  // after fetching pc, we may want to latch the pc value until
  // the instruction is ready to be sent to the next stage

  wb2if_in.ready                          := 0.U
  axiController.stageInput.readAddr.valid := false.B
  axiController.stageInput.readData.ready := axiController.stageInput.readData.valid

  // DontCare 真的么问题吗
  axiController.stageInput.writeAddr := DontCare
  axiController.stageInput.writeData := DontCare
  axiController.stageInput.writeResp := DontCare

  axiController.ifuEnable := false.B

  switch(ifu_state) {
    is(sIDLE) {
      when(wb2if_in.valid) {
        wb2if_in.ready := 1.U
        ifu_state      := sWaitReady
      }
    }
    is(sWaitReady) {
      axiController.ifuEnable := true.B
      axiController.stageInput.readAddr.valid := true.B
      when(axiController.stageInput.readAddr.valid && axiController.stageInput.readAddr.ready) {
        ifu_state := sACK
      }
    }
    is(sACK) {
      axiController.ifuEnable := true.B
      when(axiController.stageInput.readData.valid && axiController.stageInput.readData.ready) {
        ifu_state := sIDLE
      }
    }
  }

  axiController.stageInput.readAddr.bits.addr := if2idOut.bits.pc
  if2idOut.bits.inst                         := axiController.stageInput.readData.bits.data
  if2idOut.valid                             := axiController.stageInput.readData.valid && axiController.stageInput.readData.ready

  val next_inst = Module(new Next_inst)
  next_inst.io.ready := if2idOut.ready && (if2idOut.bits.pc =/= config.startPC.U)
  next_inst.io.valid := if2idOut.valid
}

class Next_inst extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val valid = Input(Bool())
    val ready = Input(Bool())
  })
  addResource("/Next_inst.sv")
}
