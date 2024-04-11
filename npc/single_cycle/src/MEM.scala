package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._
import scala.annotation.varargs
import chisel3.experimental.BundleLiterals._
import os.read

class MEMOutputIO(width: Int) extends Bundle {
  val pc          = Output(UInt(width.W))
  val nextPC      = Output(UInt(width.W))
  val ctrlsignals = Output(new ctrlSignals)
  val csrvalue    = Output(UInt(width.W))
  val alures      = Output(UInt(width.W))
  val rdata       = Output(UInt(width.W))
  val rd = Output(UInt(width.W))
}

class LSU extends Module {
  val id2lsuIn      = IO(Flipped(Decoupled(new IDUOutputIO)))
  val lsuAxiOut     = IO(AxiLiteMaster(width, width))
  val lsu2wbOut     = IO(Decoupled(new MEMOutputIO(width)))
  val axiController = Module(AxiController(width, width))
  val alu           = Module(new Alu(width))

  // 定义ID 到 LSU 之间的流水线寄存器, 并赋予初值
  val id2lsuReg = RegInit(
    (new IDUOutputIO).Lit(
      _.rs1 -> 0.U,
      _.rs2 -> 0.U,
      _.rd -> 0.U,
      _.immediate -> 0.U,
      _.ctrlsignals -> (new ctrlSignals).Lit(
        _.pcsel -> 0.U,
        _.writeEn -> false.B,
        _.immsel -> 0.U,
        _.asel -> false.B,
        _.bsel -> false.B,
        _.alusel -> 0.U,
        _.memRW -> false.B,
        _.memEnable -> false.B,
        _.WBsel -> 0.U,
        _.optype -> 0.U,
        _.csrsWriteEn -> false.B,
        _.mepcWriteEn -> false.B,
        _.mcauseWriteEn -> false.B
      ),
      _.pc -> 0.U,
      _.inst -> 0.U,
      _.csrvalue -> 0.U,
      _.mepc -> 0.U,
      _.mtvec -> 0.U
    )
  )

  // 当同时检测到 valid 和 ready 信号时, 将上一个阶段传过来的数据保存在寄存器中
  // 有一些特定的 technique 但是我不太清楚
  // 在检测到这些握手信号之前，这些寄存器的值都是 0, 要确定初值不会触发什么奇怪的错误。

  when(id2lsuIn.valid && id2lsuIn.ready) {
    id2lsuReg.rs1         := id2lsuIn.bits.rs1
    id2lsuReg.rs2         := id2lsuIn.bits.rs2
    id2lsuReg.rd          := id2lsuIn.bits.rd
    id2lsuReg.immediate   := id2lsuIn.bits.immediate
    id2lsuReg.ctrlsignals := id2lsuIn.bits.ctrlsignals
    id2lsuReg.pc          := id2lsuIn.bits.pc
    id2lsuReg.inst        := id2lsuIn.bits.inst
    id2lsuReg.csrvalue    := id2lsuIn.bits.csrvalue
    id2lsuReg.mepc        := id2lsuIn.bits.mepc
    id2lsuReg.mtvec       := id2lsuIn.bits.mtvec
  }

  // 定义一个寄存器保存 nextPC
  val lsuNextpcReg = RegNext(lsu2wbOut.bits.nextPC, config.startPC.U)

  lsuNextpcReg := MuxCase(
    0.U,
    Seq(
      (id2lsuReg.ctrlsignals.pcsel === 0.U) -> (id2lsuReg.pc + config.XLEN.U),
      (id2lsuReg.ctrlsignals.pcsel === 1.U) -> alu.io.res,
      (id2lsuReg.ctrlsignals.pcsel === 2.U) -> id2lsuReg.mepc,
      (id2lsuReg.ctrlsignals.pcsel === 3.U) -> id2lsuReg.mtvec
    )
  )

  // Dpi-itrace 跟踪指令
  val itrace = Module(new Dpi_itrace)
  itrace.io.pc     := id2lsuReg.pc
  itrace.io.inst   := id2lsuReg.inst
  itrace.io.nextpc := lsuNextpcReg

  // EX
  alu.io.alusel := id2lsuReg.ctrlsignals.alusel
  // 0 for rs1, 1 for pc
  alu.io.A := Mux(!id2lsuReg.ctrlsignals.asel, id2lsuReg.rs1, id2lsuReg.pc)
  // 0 for rs2, 1 for imm
  alu.io.B := Mux(!id2lsuReg.ctrlsignals.bsel, id2lsuReg.rs2, id2lsuReg.immediate)

  // MEM
  lsuAxiOut <> axiController.axiOut

  // activate the axiController
  axiController.stageInput.readAddr.valid  := false.B
  axiController.stageInput.writeData.valid := false.B
  axiController.stageInput.writeAddr.valid := false.B

  axiController.stageInput.writeData.bits.data := id2lsuReg.rs2
  axiController.stageInput.writeData.bits.strb := Mux(
    !id2lsuReg.ctrlsignals.memRW,
    0.U,
    wmaskGen(id2lsuReg.inst(14, 12), alu.io.res(1, 0))
  )

  axiController.stageInput.writeAddr.bits.addr := alu.io.res
  axiController.stageInput.readAddr.bits.addr  := alu.io.res

  // valid 跟随 ready
  axiController.stageInput.writeResp.ready := axiController.stageInput.writeResp.valid
  axiController.stageInput.readData.ready  := axiController.stageInput.readData.valid

  import stageState._
  val lsu_state = RegInit(sIDLE)

  // IDU 的 ready 跟随 valid
  id2lsuIn.ready := id2lsuIn.valid

  switch(lsu_state) {
    is(sIDLE) {
      // 在 sIDLE 状态, 没有任何输出
      when(id2lsuIn.valid) {
        lsu_state := sWaitReady
      }
    }
    is(sWaitReady) {
      // 在 waitReady 状态，数据已经保存到了 mem 的寄存器中

      // 如果 memEnable===0.U, 说明该条指令不涉及到访存操作，我们可以将 axiController 的valid 位全部置低
      axiController.stageInput.readAddr.valid := Mux(
        (id2lsuReg.ctrlsignals.memEnable === 1.U) && (id2lsuReg.ctrlsignals.memRW === 0.U),
        true.B,
        false.B
      )
      axiController.stageInput.writeAddr.valid := Mux(
        (id2lsuReg.ctrlsignals.memEnable === 1.U) && (id2lsuReg.ctrlsignals.memRW === 1.U),
        true.B,
        false.B
      )
      axiController.stageInput.writeData.valid := Mux(
        (id2lsuReg.ctrlsignals.memEnable === 1.U) && (id2lsuReg.ctrlsignals.memRW === 1.U),
        true.B,
        false.B
      )

      when(axiController.stageInput.readAddr.valid && axiController.stageInput.readAddr.ready) {
        lsu_state := Mux(id2lsuReg.ctrlsignals.memRW === 0.U, sACK, sIDLE)
      }

      when(
        axiController.stageInput.writeAddr.valid && axiController.stageInput.writeAddr.ready && axiController.stageInput.writeData.valid && axiController.stageInput.writeData.ready
      ) {
        lsu_state := Mux(id2lsuReg.ctrlsignals.memRW === 1.U, sACK, sIDLE)
      }

      when (id2lsuReg.ctrlsignals.memEnable === 0.U) {
        lsu_state := sCompleted
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
      when(lsu2wbOut.valid && lsu2wbOut.ready) {
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
  imm_byte := readDataGen(alu.io.res(1, 0), 1, readData)
  imm_half := readDataGen(alu.io.res(1, 0), 2, readData)
  rmemdata := Mux(
    id2lsuReg.inst(14),
    // io.inst(14) == 1, unsigned 直接截断就好
    MuxCase(
      readData,
      Seq(
        (id2lsuReg.inst(13, 12) === 0.U) -> imm_byte,
        (id2lsuReg.inst(13, 12) === 1.U) -> imm_half
      )
    ),
    // io.inst(14) == 0, signed 还需符号扩展
    MuxCase(
      readData,
      Seq(
        (id2lsuReg.inst(13, 12) === 0.U) -> Cat(padding(24, imm_byte(7)), imm_byte),
        (id2lsuReg.inst(13, 12) === 1.U) -> Cat(padding(16, imm_half(15)), imm_half)
      )
    )
  )

  // 输出
  lsu2wbOut.bits.alures      := alu.io.res
  lsu2wbOut.bits.pc          := id2lsuReg.pc
  lsu2wbOut.bits.csrvalue    := id2lsuReg.csrvalue
  lsu2wbOut.bits.ctrlsignals := id2lsuReg.ctrlsignals
  lsu2wbOut.bits.rd := id2lsuReg.rd
  lsu2wbOut.bits.rdata       := rmemdata
  lsu2wbOut.bits.nextPC      := lsuNextpcReg

  // 如果该条指令有访问内存的阶段，那么看是读取还是写入，根据读写的 response 信号，来决定是否结束 mem 阶段

  lsu2wbOut.valid := lsu_state === sCompleted

}

class Dpi_itrace extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val pc     = Input(UInt(32.W))
    val inst   = Input(UInt(32.W))
    val nextpc = Input(UInt(32.W))
  })
  addResource("/Dpi_itrace.sv")
}
