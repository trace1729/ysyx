package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._
import scala.annotation.varargs
import chisel3.experimental.BundleLiterals._

class MEMOutputIO(width: Int) extends Bundle {
  val pc          = Output(UInt(width.W))
  val ctrlsignals = Output(new ctrlSignals)
  val csrvalue    = Output(UInt(width.W))
  val alures      = Output(UInt(width.W))
  val rdata       = Output(UInt(width.W))
  val rd          = Output(UInt(width.W))

  val npc = Output(UInt(width.W))
}

class LSU extends Module {
  val jump = IO(Output(Bool()))

  val id2lsuIn      = IO(Flipped(Decoupled(new IDUOutputIO)))
  val lsuAxiOut     = IO(AxiLiteMaster(width, dataWidth))
  val lsu2wbOut     = IO(Decoupled(new MEMOutputIO(width)))
  val axiController = Module(AxiController(width, dataWidth))
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
  lsu2wbOut.bits.npc := MuxCase(
    0.U,
    Seq(
      (id2lsuReg.ctrlsignals.pcsel === 0.U) -> (id2lsuReg.pc + config.XLEN.U),
      (id2lsuReg.ctrlsignals.pcsel === 1.U) -> alu.io.res,
      (id2lsuReg.ctrlsignals.pcsel === 2.U) -> id2lsuReg.mepc,
      (id2lsuReg.ctrlsignals.pcsel === 3.U) -> id2lsuReg.mtvec
    )
  )
  jump := (id2lsuReg.ctrlsignals.pcsel =/= 0.U) && lsu2wbOut.valid && lsu2wbOut.ready

  // Dpi-itrace 跟踪指令
  val itrace = Module(new Dpi_itrace)
  itrace.io.pc     := id2lsuReg.pc
  itrace.io.inst   := id2lsuReg.inst
  itrace.io.nextpc := lsu2wbOut.bits.npc

  // EX
  alu.io.alusel := id2lsuReg.ctrlsignals.alusel
  // 0 for rs1, 1 for pc
  alu.io.A := Mux(!id2lsuReg.ctrlsignals.asel, id2lsuReg.rs1, id2lsuReg.pc)
  // 0 for rs2, 1 for imm
  alu.io.B := Mux(!id2lsuReg.ctrlsignals.bsel, id2lsuReg.rs2, id2lsuReg.immediate)

  // MEM
  lsuAxiOut <> axiController.axiOut

  // activate the axiController
  axiController.stageInput.ar.valid  := false.B
  axiController.stageInput.w.valid := false.B
  axiController.stageInput.aw.valid := false.B

  axiController.stageInput.w.bits.data := id2lsuReg.rs2
  axiController.stageInput.w.bits.strb := Mux(
    !id2lsuReg.ctrlsignals.memRW,
    0.U,
    wmaskGen(id2lsuReg.inst(14, 12), alu.io.res(1, 0))
  )

  axiController.stageInput.aw.bits.addr := alu.io.res
  axiController.stageInput.ar.bits.addr  := alu.io.res

  // 处理 writeAck 请求 (`after` wvalid and wready is both asserted)
  axiController.stageInput.b.ready := axiController.stageInput.b.valid && (~axiController.stageInput.w.valid)  
  // 处理 read ack 请求 (`after` arvalid and arready is both asserted) 
  axiController.stageInput.r.ready := axiController.stageInput.r.valid && (~axiController.stageInput.ar.valid)
  axiController.stageInput.ar.bits.id := 1.U
  axiController.stageInput.ar.bits.len := 0.U
  axiController.stageInput.ar.bits.size := id2lsuReg.inst(14, 12)
  axiController.stageInput.ar.bits.burst := 1.U
  axiController.stageInput.aw.bits.id := 1.U
  axiController.stageInput.aw.bits.len := 0.U
  axiController.stageInput.aw.bits.size := id2lsuReg.inst(14, 12) 
  axiController.stageInput.aw.bits.burst := 1.U
  axiController.stageInput.w.bits.last := 1.U

  import stageState._
  val lsu_state = RegInit(sIDLE)

  // TODO IDU 的 ready ？
  id2lsuIn.ready := (lsu_state === sIDLE) && lsu2wbOut.ready

  val r_fire  = axiController.stageInput.r.valid && axiController.stageInput.r.ready
  val b_fire = axiController.stageInput.b.valid && axiController.stageInput.b.ready
  val rdata       = RegEnable(axiController.stageInput.r.bits.data, r_fire)

  switch(lsu_state) {
    is(sIDLE) {
      // 在 sIDLE 状态, 等待将上一阶段的值写入寄存器
      when(id2lsuIn.valid && id2lsuIn.ready) {
        lsu_state := sWaitAXI
      }
    }
    is(sWaitAXI) {
      // 在 waitReady 状态，数据已经保存到了 mem 的寄存器中

      // 下面三个大行设置 axiController 的 valid 信号
      // 如果 memEnable===0.U, 说明该条指令不涉及到访存操作，我们可以将 axiController 的valid 位全部置低
      axiController.stageInput.ar.valid := Mux(
        (id2lsuReg.ctrlsignals.memEnable === 1.U) && (id2lsuReg.ctrlsignals.memRW === 0.U),
        true.B,
        false.B
      )
      axiController.stageInput.aw.valid := Mux(
        (id2lsuReg.ctrlsignals.memEnable === 1.U) && (id2lsuReg.ctrlsignals.memRW === 1.U),
        true.B,
        false.B
      )
      axiController.stageInput.w.valid := Mux(
        (id2lsuReg.ctrlsignals.memEnable === 1.U) && (id2lsuReg.ctrlsignals.memRW === 1.U),
        true.B,
        false.B
      )

      // 握手成功之后，数据锁存到 sram 的寄存器中，然后就跳转到 ack 状态，拉低 valid 信号
      when(
        id2lsuReg.ctrlsignals.memEnable === 1.U && axiController.stageInput.ar.valid && axiController.stageInput.ar.ready
      ) {
        lsu_state := sWaitReady
      }

      when(
        id2lsuReg.ctrlsignals.memEnable === 1.U && axiController.stageInput.aw.valid && axiController.stageInput.aw.ready && axiController.stageInput.w.valid && axiController.stageInput.w.ready
      ) {
        lsu_state := sWaitReady
      }

      when(id2lsuReg.ctrlsignals.memEnable === 0.U) {
        lsu_state := sWaitReady
      }

    }
    is(sWaitReady) {
      when(r_fire) {
        lsu_state := sACK
      }
      when(b_fire) {
        lsu_state := sACK
      }
      when(id2lsuReg.ctrlsignals.memEnable === 0.U) {
        lsu_state := sACK
      }
    }
    is(sACK) {
      when(lsu2wbOut.valid && lsu2wbOut.ready) {
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
  imm_byte := readDataGen(alu.io.res(1, 0), 1, rdata)
  imm_half := readDataGen(alu.io.res(1, 0), 2, rdata)
  rmemdata := Mux(
    id2lsuReg.inst(14),
    // io.inst(14) == 1, unsigned 直接截断就好
    MuxCase(
      rdata,
      Seq(
        (id2lsuReg.inst(13, 12) === 0.U) -> imm_byte,
        (id2lsuReg.inst(13, 12) === 1.U) -> imm_half
      )
    ),
    // io.inst(14) == 0, signed 还需符号扩展
    MuxCase(
      rdata,
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
  lsu2wbOut.bits.rd          := id2lsuReg.rd
  lsu2wbOut.bits.rdata       := rmemdata

  lsu2wbOut.valid := lsu_state === sACK
  

}

class Dpi_itrace extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val pc     = Input(UInt(32.W))
    val inst   = Input(UInt(32.W))
    val nextpc = Input(UInt(32.W))
  })
  addResource("/Dpi_itrace.sv")
}
