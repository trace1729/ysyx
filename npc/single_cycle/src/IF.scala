package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._
import os.read

/** ********************IFU**************************
  */

class IFUOutputIO extends Bundle {
  val pc   = Output(UInt(width.W))
  val inst = Output(UInt(width.W))
}

object stageState extends ChiselEnum {
  val sIDLE, sWaitAXI, sWaitReady, sACK = Value
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
  import stageState._
  val ifu_state = RegInit(sWaitAXI)

  // passing syntax check
  wb2ifIn.ready := 1.U

  // 和 axi 控制器相连接
  ifuAxiOut <> axiController.axiOut

  val jump_r = RegInit(false.B)
  val npc_r = RegInit(NOP)

  // 当检测到 jump 信号时，将跳转信号和跳转pc锁存到寄存器中
  when (jump) {
    jump_r := jump
    npc_r := npc
  }

  // pc 生成逻辑
  val PC     = RegInit(config.startPC.U - config.XLEN.U)
  val nextPC = Wire(UInt(config.width.W))
  nextPC := PC + config.XLEN.U

  // 当if阶段和id阶段完成握手之后，就可以更新 PC 了
  // 阻塞赋值
  when(if2idOut.valid && if2idOut.ready) {
    PC := Mux(jump_r, npc_r - config.XLEN.U, nextPC)
    // flush 掉跳转信号
    jump_r := false.B
    npc_r := 0.U
  }

  // 为 axiController 设置默认值
  val readCompleted = axiController.stageInput.readData.valid && axiController.stageInput.readData.ready
  axiController.stageInput.writeAddr      := DontCare
  axiController.stageInput.writeData      := DontCare
  axiController.stageInput.writeResp      := DontCare
  axiController.stageInput.readAddr.valid := (ifu_state === stageState.sWaitAXI)
  // nextPC 作为取值的请求
  axiController.stageInput.readAddr.bits.addr := nextPC
  // 处理器 read ack 请求
  axiController.stageInput.readData.ready := axiController.stageInput.readData.valid

  // 处理 ifu 的状态转移
  switch(ifu_state) {
    is(sWaitAXI) {
      // 进入 sWaitReady 状态之后，设置置 ar Valid
      when (axiController.stageInput.readAddr.valid && axiController.stageInput.readAddr.ready) {
        ifu_state := sWaitReady
      }
    }
    is (sWaitReady) {
      when(readCompleted) {
        ifu_state := sACK
      }
    }
    is (sACK) {
      when (if2idOut.ready && if2idOut.valid) {
        ifu_state := sWaitAXI
      }
    }
  }

  // 处理输出
  if2idOut.bits.inst := RegEnable(Mux(jump || jump_r, NOP, axiController.stageInput.readData.bits.data), readCompleted)
  if2idOut.bits.pc   := Mux(jump || jump_r, 0.U, nextPC)
  if2idOut.valid     := ifu_state === sACK

}
