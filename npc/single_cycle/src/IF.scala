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
  val npc       = IO(Input(UInt(width.W)))
  val jump      = IO(Input(Bool()))
  val wb2ifIn   = IO(Flipped(Decoupled(new WBOutputIO)))
  val if2idOut  = IO(Decoupled(new IFUOutputIO))
  val ifuAxiOut = IO(AxiLiteMaster(width, width))

  val axiController = Module(AxiController(width, width))

  // 和 axi 控制器相连接
  ifuAxiOut <> axiController.axiOut

  // pc 生成逻辑
  val PC     = RegInit(config.startPC.U - config.XLEN.U)
  val nextPC = Wire(UInt(config.width.W))
  nextPC := Mux(jump, nextPC, PC + config.XLEN.U)

  // 当if阶段和id阶段完成一次握手之后，就可以更新 PC 了
  when(if2idOut.valid && if2idOut.ready) {
    PC := nextPC
  }

  // 为 axiController 设置默认值
  axiController.stageInput.writeAddr      := DontCare
  axiController.stageInput.writeData      := DontCare
  axiController.stageInput.writeResp      := DontCare
  axiController.stageInput.readAddr.valid := (ifu_state === stageState.sWaitReady)

  // 处理器 read ack 请求
  axiController.stageInput.readData.ready := axiController.stageInput.readData.valid
  val readCompleted = axiController.stageInput.readData.valid && axiController.stageInput.readData.ready

  import stageState._
  val ifu_state = RegInit(sIDLE)

  switch(ifu_state) {
    is(sIDLE) {
      ifu_state := sWaitReady
    }
    is(sWaitReady) {
      // 进入 sWaitReady 状态之后，设置置 ar Valid
      when(readCompleted) {
        ifu_state := sIDLE
      }
    }
  }

  axiController.stageInput.readAddr.bits.addr := nextPC
  if2idOut.bits.inst                          := axiController.stageInput.readData.bits.data
  if2idOut.bits.pc                            := PC
  if2idOut.valid                              := readCompleted

  val next_inst = Module(new Next_inst)
  next_inst.io.ready := if2idOut.ready
  next_inst.io.valid := if2idOut.valid
}

class Next_inst extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val valid = Input(Bool())
    val ready = Input(Bool())
  })
  addResource("/Next_inst.sv")
}
