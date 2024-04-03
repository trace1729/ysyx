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
  val sIDLE, s_waitReady, sACK = Value
}

// the axiController shall connected to the memArbiter
// how to? axiController is defined inside the ifu, how can it connect to

class IFU(memoryFile: String) extends Module {
  val wb2if_in    = IO(Flipped(Decoupled(new WBOutputIO)))
  val if2id_out   = IO(Decoupled(new IFUOutputIO))
  val ifu_axi_out = IO(AxiLiteMaster(width, width))
  val ifu_enable  = IO(Output(Bool()))

  val axiController = Module(AxiController(width, width))

  ifu_axi_out <> axiController.axiOut

  import stageState._
  val ifu_state = RegInit(sIDLE)

  if2id_out.bits.pc := RegEnable(wb2if_in.bits.wb_nextpc, config.startPC.U, wb2if_in.valid)

  // if2id_out.bits.inst := Cat(instMem.io.inst)
  // instMem.io.pc       := if2id_out.bits.pc

  // after fetching pc, we may want to latch the pc value until
  // the instruction is ready to be sent to the next stage

  wb2if_in.ready                          := 0.U
  axiController.stageInput.readAddr.valid := false.B
  axiController.stageInput.readData.ready := axiController.stageInput.readData.valid
  axiController.stageInput.writeAddr      := DontCare
  axiController.stageInput.writeData      := DontCare
  axiController.stageInput.writeResp      := DontCare

  ifu_enable := false.B

  switch(ifu_state) {
    is(sIDLE) {
      when(wb2if_in.valid) {
        wb2if_in.ready := 1.U
        ifu_state      := s_waitReady
      }
    }
    is(s_waitReady) {
      ifu_enable                              := true.B
      axiController.stageInput.readAddr.valid := true.B
      when (axiController.stageInput.readAddr.valid && axiController.stageInput.readAddr.ready) {
        ifu_state := sACK
      }
    }
    is(sACK) {
      ifu_enable                              := true.B
      when(axiController.stageInput.readData.valid && axiController.stageInput.readData.ready) {
        ifu_state := sIDLE
      }
    }
  }

  axiController.stageInput.readAddr.bits.addr := if2id_out.bits.pc
  if2id_out.bits.inst                         := axiController.stageInput.readData.bits.data
  if2id_out.valid                             := axiController.stageInput.readData.valid && axiController.stageInput.readData.ready

  val next_inst = Module(new Next_inst)
  next_inst.io.ready := if2id_out.ready && (if2id_out.bits.pc =/= config.startPC.U)
  next_inst.io.valid := if2id_out.valid
}

class Next_inst extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val valid = Input(Bool())
    val ready = Input(Bool())
  })
  addResource("/Next_inst.sv")
}
