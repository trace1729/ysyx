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

class IFUInputIO extends Bundle {
  val pcsel     = Input(UInt(3.W))
  val alu_res   = Input(UInt(width.W))
  val csr_mepc  = Input(UInt(width.W))
  val csr_mtvec = Input(UInt(width.W))
}

class IFU(memoryFile: String) extends Module {
  val in      = IO(new IFUInputIO)
  val out     = IO(Decoupled(new IFUOutputIO))
  val pcvalue = Wire(UInt(32.W))
  val instMem = Module(new InstMem(memoryFile = memoryFile))
  pcvalue := MuxCase(
    0.U,
    Seq(
      (in.pcsel === 0.U) -> (out.bits.pc + config.instLen.U),
      (in.pcsel === 1.U) -> in.alu_res,
      (in.pcsel === 2.U) -> in.csr_mepc,
      (in.pcsel === 3.U) -> in.csr_mtvec
    )
  )
  instMem.io.pc := out.bits.pc
  out.bits.pc   := RegNext(pcvalue, config.startPC.U)
  out.bits.inst := Cat(instMem.io.inst)
  
  val itrace = Module(new Dpi_itrace)
  itrace.io.pc     := out.bits.pc
  itrace.io.inst   := out.bits.inst
  itrace.io.nextpc := pcvalue

  // ready, valid 信号全部设置成1
  out.valid := 1.U
}

/** *******************IDU***************************
  */

class IDUOutputIO extends Bundle {
  val rs1         = Output(UInt(width.W))
  val rs2         = Output(UInt(width.W))
  val immediate   = Output(UInt(width.W))
  val ctrlsignals = Output(new controlLogicIO(width))

  val pc       = Output(UInt(width.W))
  val inst     = Output(UInt(width.W))
  val csrvalue = Output(UInt(width.W))
}

class IDU extends Module {
  val data = IO(Input(UInt(width.W)))
  val in  = IO(Flipped(Decoupled(new IFUOutputIO)))
  val out = IO(DecoupledIO(new IDUOutputIO))
  val x10 = IO(Output(UInt(width.W)))

  val regfile   = Module(new Regfile(num = regsNum, width = width))
  val ctrlLogic = Module(new controlLogic(width))
  val immgen    = Module(new ImmGen(width))
  val csr       = Module(new CSR(10, width))

  // 寄存器文件的连接
  regfile.io.readreg1 := in.bits.inst(19, 15)
  regfile.io.readreg2 := in.bits.inst(24, 20)
  regfile.io.writereg := in.bits.inst(11, 7)
  regfile.io.writeEn  := ctrlLogic.io.writeEn
  regfile.io.data := data
  x10 := regfile.io.x10

  // 控制逻辑的连接
  ctrlLogic.io.inst := in.bits.inst
  ctrlLogic.io.rs1  := regfile.io.rs1
  ctrlLogic.io.rs2  := regfile.io.rs2

  // 立即数生成器的连接
  immgen.io.inst   := in.bits.inst
  immgen.io.immsel := ctrlLogic.io.immsel

  // csr 寄存器文件的连接
  csr.io.csrsWriteEn := ctrlLogic.io.csrsWriteEn
  csr.io.csrNo       := immgen.io.imm
  // 只考虑 csrw, 所以直接把 rs1 寄存器的值写入 CSRs[csr_no]
  csr.io.data := regfile.io.rs1
  // 需要写回寄存器文件的值
  csr.io.mcauseData    := 0xb.U
  csr.io.mcauseWriteEn := ctrlLogic.io.mcauseWriteEn

  csr.io.mepcData    := in.bits.pc
  csr.io.mepcWriteEn := ctrlLogic.io.mepcWriteEn

  // 输出信号到外部模块，肯定可以简化，之后看看
  out.bits.ctrlsignals :>= ctrlLogic.io
  out.bits.ctrlsignals.inst := DontCare
  out.bits.ctrlsignals.rs1 := DontCare
  out.bits.ctrlsignals.rs2 := DontCare

  // out.bits.ctrlsignals.pcsel         := ctrlLogic.io.pcsel
  // out.bits.ctrlsignals.writeEn       := ctrlLogic.io.writeEn
  // out.bits.ctrlsignals.immsel        := ctrlLogic.io.immsel
  // out.bits.ctrlsignals.asel          := ctrlLogic.io.asel
  // out.bits.ctrlsignals.bsel          := ctrlLogic.io.bsel
  // out.bits.ctrlsignals.alusel        := ctrlLogic.io.alusel
  // out.bits.ctrlsignals.memRW         := ctrlLogic.io.memRW
  // out.bits.ctrlsignals.memEnable     := ctrlLogic.io.memEnable
  // out.bits.ctrlsignals.WBsel         := ctrlLogic.io.WBsel
  // out.bits.ctrlsignals.optype        := ctrlLogic.io.optype
  // out.bits.ctrlsignals.isCsrInst     := ctrlLogic.io.isCsrInst
  // out.bits.ctrlsignals.csrsWriteEn   := ctrlLogic.io.csrsWriteEn
  // out.bits.ctrlsignals.mepcWriteEn   := ctrlLogic.io.mepcWriteEn
  // out.bits.ctrlsignals.mcauseWriteEn := ctrlLogic.io.mcauseWriteEn

  out.bits.rs1       := regfile.io.rs1
  out.bits.rs2       := regfile.io.rs2
  out.bits.immediate := immgen.io.imm
  out.bits.pc        := in.bits.pc
  out.bits.inst      := in.bits.inst
  out.bits.csrvalue  := csr.io.csrValue

  // ready, valid 信号全部设置成1
  in.ready := 1.U
  out.valid := 1.U
}

/** *******************EX***************************
  */

class EXOutputIO extends Bundle {
  val overflow = Output(Bool())
  val carry    = Output(Bool())
  val zero     = Output(Bool())
  val inst     = Output(UInt(width.W))
  val rs2      = Output(UInt(width.W))

  val pc          = Output(UInt(width.W))
  val ctrlsignals = Output(new ctrlSignals)
  val csrvalue    = Output(UInt(width.W))
  val alures      = Output(UInt(width.W))
}

class EX extends Module {
  val in  = IO(Flipped(Decoupled(new IDUOutputIO)))
  val out = IO(Decoupled(new EXOutputIO))

  val alu = Module(new Alu(width))
  // 因为控制逻辑是贯穿五个阶段的，所以每一个阶段(除了ID)都会有控制信号的输入
  // 这样就比较怪了，那我当前的阶段需要将控制信号传递给之后的阶段

  alu.io.alusel := in.bits.ctrlsignals.alusel
  // 0 for rs1, 1 for pc
  alu.io.A := Mux(!in.bits.ctrlsignals.asel, in.bits.rs1, in.bits.pc)
  // 0 for rs2, 1 for imm
  alu.io.B := Mux(!in.bits.ctrlsignals.bsel, in.bits.rs2, in.bits.immediate)

  out.bits.carry       := alu.io.carry
  out.bits.overflow    := alu.io.overflow
  out.bits.alures      := alu.io.res
  out.bits.zero        := alu.io.zero
  out.bits.ctrlsignals := in.bits.ctrlsignals

  out.bits.pc       := in.bits.pc
  out.bits.inst     := in.bits.inst
  out.bits.csrvalue := in.bits.csrvalue
  out.bits.rs2      := in.bits.rs2

  // ready, valid 信号全部设置成1
  in.ready := 1.U
  out.valid := 1.U
}

/** *******************MEM***************************
  */

class MEMOutputIO(width: Int) extends Bundle {
  val pc          = Output(UInt(width.W))
  val ctrlsignals = Output(new ctrlSignals)
  val csrvalue    = Output(UInt(width.W))
  val alures      = Output(UInt(width.W))
  val rdata       = Output(UInt(width.W))
}

class MEM extends Module {
  val in  = IO(Flipped(Decoupled(new EXOutputIO)))
  val out = IO(Decoupled(new MEMOutputIO(width)))
  val mem = Module(new Dmem(width))

  val rmemdata = Wire(UInt(width.W))

  mem.io.addr := in.bits.alures
  // determined by control logic
  mem.io.memEnable := in.bits.ctrlsignals.memEnable
  mem.io.memRW     := in.bits.ctrlsignals.memRW
  // if (mem.io.memRW) set wmask to 0b0000
  // mem.io.memRW = 0, read, set to 0
  mem.io.wmask := Mux(!mem.io.memRW, 0.U, wmaskGen(in.bits.inst(14, 12), mem.io.addr(1, 0)))
  mem.io.wdata := in.bits.rs2
  val imm_byte = Wire(UInt(8.W))
  val imm_half = Wire(UInt(16.W))
  imm_byte := readDataGen(mem.io.addr(1, 0), 1, mem.io.rdata)
  imm_half := readDataGen(mem.io.addr(1, 0), 2, mem.io.rdata)
  rmemdata := Mux(
    in.bits.inst(14),
    // io.inst(14) == 1, unsigned 直接截断就好
    MuxCase(
      mem.io.rdata,
      Seq(
        (in.bits.inst(13, 12) === 0.U) -> imm_byte,
        (in.bits.inst(13, 12) === 1.U) -> imm_half
      )
    ),
    // io.inst(14) == 0, signed 还需符号扩展
    MuxCase(
      mem.io.rdata,
      Seq(
        (in.bits.inst(13, 12) === 0.U) -> Cat(padding(24, imm_byte(7)), imm_byte),
        (in.bits.inst(13, 12) === 1.U) -> Cat(padding(16, imm_half(15)), imm_half)
      )
    )
  )

  out.bits.alures      := in.bits.alures     
  out.bits.pc          := in.bits.pc         
  out.bits.csrvalue    := in.bits.csrvalue   
  out.bits.ctrlsignals := in.bits.ctrlsignals
  out.bits.rdata := rmemdata

  // ready, valid 信号全部设置成1
  in.ready := 1.U
  out.valid := 1.U
}

/** *******************WB***************************
  */

class WBOutputIO extends Bundle {
  // 暂时不太清楚 wb 需要输出什么
  val wb = Output(Bool())
}

class WB extends Module {
  val in   = IO(Flipped(Decoupled(new MEMOutputIO(width))))
  val out  = IO(new WBOutputIO)
  val data = IO(Output(UInt(width.W)))
  data := MuxCase(
    0.U,
    Seq(
      (in.bits.ctrlsignals.WBsel === 0.U) -> in.bits.alures,
      (in.bits.ctrlsignals.WBsel === 1.U) -> (in.bits.pc + config.instLen.U),
      (in.bits.ctrlsignals.WBsel === 2.U) -> in.bits.rdata,
      (in.bits.ctrlsignals.WBsel === 3.U) -> in.bits.csrvalue
    )
  )

  in.ready := 1.U
  out.wb := 1.U
}

/** ****************** 数据通路 ****************************
  */
class DatapathIO extends Bundle {
  val pc   = Output(UInt(width.W))
  val inst = Output(UInt(width.W))

}

class Datapath(memoryFile: String) extends Module {
  val io = IO(new DatapathIO)

  val ifu = Module(new IFU(memoryFile))
  val idu = Module(new IDU)
  val ex  = Module(new EX)
  val mem = Module(new MEM)
  val wb  = Module(new WB)

  ifu.out <> idu.in
  idu.out <> ex.in
  ex.out <> mem.in
  mem.out <> wb.in

  
  io.inst := ifu.out.bits.inst
  io.pc := ifu.out.bits.pc

  // 诡异的连线，上面执行阶段之间的握手突出一个毫无意义
  ifu.in.alu_res := ex.out.bits.alures
  ifu.in.pcsel := idu.out.bits.ctrlsignals.pcsel
  ifu.in.csr_mepc := 0.U
  ifu.in.csr_mtvec := 0.U
  // io.x10 := idu.x10
  
  idu.data := wb.data
}

class Dmem(val width: Int) extends BlackBox with HasBlackBoxResource {
  val io = IO(new MemIO(width))
  addResource("/Dmem.sv")
}

class Dpi_itrace extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val pc     = Input(UInt(32.W))
    val inst   = Input(UInt(32.W))
    val nextpc = Input(UInt(32.W))
  })
  addResource("/Dpi_itrace.sv")
}