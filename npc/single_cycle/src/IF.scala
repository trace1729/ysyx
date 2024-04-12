package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._
import os.read
import org.yaml.snakeyaml.events.Event.ID

/** ********************IFU**************************
  */

class IFUOutputIO extends Bundle {
  val pc   = Output(UInt(width.W))
  val inst = Output(UInt(width.W))
}

object stageState extends ChiselEnum {
  val sIDLE, sWaitReady, sACK, sCompleted = Value
}

// the axiController shall connected to the memArbiter
// how to? axiController is defined inside the ifu, how can it connect to

class IFU(memoryFile: String) extends Module {
  val nextPC = IO(Input(UInt(width.W)))
  val wb2ifIn   = IO(Flipped(Decoupled(new WBOutputIO)))
  val if2idOut  = IO(Decoupled(new IFUOutputIO))
  val ifuAxiOut = IO(AxiLiteMaster(width, width))

  val axiController = Module(AxiController(width, width))

  import stageState._
  val ifu_state = RegInit(sIDLE)

  ifuAxiOut <> axiController.axiOut

  // if2id_out.bits.inst := Cat(instMem.io.inst)
  // instMem.io.pc       := if2id_out.bits.pc

  // after fetching pc, we may want to latch the pc value until
  // the instruction is ready to be sent to the next stage

  axiController.stageInput.readAddr.valid := false.B
  axiController.stageInput.readData.ready := axiController.stageInput.readData.valid

  if2idOut.bits.pc := RegEnable(nextPC, config.startPC.U, wb2ifIn.valid)

  axiController.stageInput.writeAddr := DontCare
  axiController.stageInput.writeData := DontCare
  axiController.stageInput.writeResp := DontCare

  wb2ifIn.ready := wb2ifIn.valid

  switch(ifu_state) {
    is(sIDLE) {
      when(wb2ifIn.valid && wb2ifIn.ready) {
        ifu_state     := sWaitReady
      }
    }
    is(sWaitReady) {
      axiController.stageInput.readAddr.valid := true.B
      when(axiController.stageInput.readAddr.valid && axiController.stageInput.readAddr.ready) {
        ifu_state := sACK
      }
    }
    is(sACK) {
      when(axiController.stageInput.readData.valid && axiController.stageInput.readData.ready) {
        ifu_state := sCompleted
      }
    }
    // 为什么要多一个 sCompleted 状态 --> 为了延迟一个周期，这样可以把 从控制器拿到的数据放置在寄存器里 L83
    is(sCompleted) {
      when(if2idOut.valid && if2idOut.ready) {
        ifu_state := sIDLE
      }
    }
  }

  val readCompleted = axiController.stageInput.readData.valid && axiController.stageInput.readData.ready

  axiController.stageInput.readAddr.bits.addr := if2idOut.bits.pc
  if2idOut.bits.inst                          := RegEnable(axiController.stageInput.readData.bits.data, 0.U, readCompleted)
  if2idOut.valid                              := ifu_state === sCompleted

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
