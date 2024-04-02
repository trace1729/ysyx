
package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._

/** ********************IFU**************************
 *
  */

class IFUOutputIO extends Bundle {
  val pc   = Output(UInt(width.W))
  val inst = Output(UInt(width.W))
}

object stageState extends ChiselEnum {
  val sIDLE, s_waitReady = Value
}

// the axiController shall connected to the memArbiter
// how to? axiController is defined inside the ifu, how can it connect to

class IFU(memoryFile: String) extends Module {
  val wb2if_in      = IO(Flipped(Decoupled(new WBOutputIO)))
  val if2id_out     = IO(Decoupled(new IFUOutputIO))
  // val axi = IO(AxiLiteMaster(width, width))
   
  val axiController = Module(AxiController(width, width))
  val sram          = Module(new SRAM)
  // val instMem   = Module(new InstMem(memoryFile = memoryFile))
  sram.in <> axiController.axi

  import stageState._
  val ifu_state = RegInit(sIDLE)

  if2id_out.bits.pc := RegEnable(wb2if_in.bits.wb_nextpc, config.startPC.U, wb2if_in.valid)

  // if2id_out.bits.inst := Cat(instMem.io.inst)
  // instMem.io.pc       := if2id_out.bits.pc

  // after fetching pc, we may want to latch the pc value until
  // the instruction is ready to be sent to the next stage

  wb2if_in.ready                 := 0.U
  axiController.in.externalMemEn := 0.U
  axiController.in.externalValid := 0.U

  switch(ifu_state) {
    is(sIDLE) {
      when(wb2if_in.valid) {
        wb2if_in.ready := 1.U
        ifu_state      := s_waitReady
      }
    }
    is(s_waitReady) {
      axiController.in.externalMemEn := 1.U
      axiController.in.externalValid := 1.U

      when(axiController.axi.readData.valid && axiController.axi.readData.ready) {
        ifu_state := sIDLE
      }
    }
  }

  axiController.in.externalAddress := if2id_out.bits.pc
  axiController.in.externalMemRW   := 0.U
  axiController.in.externalData    := DontCare
  axiController.in.externalWmask   := DontCare
  if2id_out.bits.inst              := axiController.axi.readData.bits.data

  if2id_out.valid := axiController.axi.readData.valid && axiController.axi.readData.ready

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
