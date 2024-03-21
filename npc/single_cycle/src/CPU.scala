package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._


class top(memoryFile: String = "") extends Module {
  val io = IO(new DatapathIO)
  val datapath = Module(new Datapath(memoryFile))
  io.inst := datapath.io.inst
  io.pc := datapath.io.pc
  io.x10 := datapath.io.x10

//   val io = IO(new Bundle {
//     val pc   = Output(UInt(width.W))
//     val inst = Output(UInt(width.W))

//     // for testing purpose
//     val x1           = Output(UInt(width.W))
//     val x2           = Output(UInt(width.W))
//     val x5           = Output(UInt(width.W))
//     val x6           = Output(UInt(width.W))
//     val x7           = Output(UInt(width.W))
//     val x8           = Output(UInt(width.W))
//     val x9           = Output(UInt(width.W))
//     val x10          = Output(UInt(width.W))
//     val writereg     = Output(UInt(5.W))
//     val test_alu_res = Output(UInt(width.W))
//   })

//   val instMem   = Module(new InstMem(memoryFile = memoryFile))
//   val cntlLogic = Module(new controlLogic(width))
//   val regfile   = Module(new Regfile(num=regsNum, width = width))
//   val alu       = Module(new Alu(width))
//   val mem       = Module(new Mem(width))
//   val immgen    = Module(new ImmGen(width))
//   val csr = Module(new CSR(10, width))

//   val pcvalue = Wire(UInt(32.W))
// //   pcvalue := Mux(!cntlLogic.io.pcsel, io.pc + top.inst_len, alu.io.res)
//   pcvalue := MuxCase(0.U, Seq(
//     (cntlLogic.io.pcsel === 0.U) -> (io.pc + config.instLen.U),
//     (cntlLogic.io.pcsel === 1.U) -> alu.io.res,
//     (cntlLogic.io.pcsel === 2.U) -> csr.io.mepc,
//     (cntlLogic.io.pcsel === 3.U) -> csr.io.mtvec,
//   ))
//   io.pc   := RegNext(pcvalue, config.startPC.U)

//   instMem.io.pc     := io.pc
//   cntlLogic.io.inst := Cat(instMem.io.inst)
//   cntlLogic.io.rs1  := regfile.io.rs1
//   cntlLogic.io.rs2  := regfile.io.rs2
//   io.inst           := Cat(instMem.io.inst)

//   val itrace = Module(new Dpi_itrace)
//   itrace.io.pc     := io.pc
//   itrace.io.inst   := io.inst
//   itrace.io.nextpc := pcvalue

//   val ftrace = Module(new Dpi_ftrace)
//   ftrace.io.optype := cntlLogic.io.optype
//   ftrace.io.rd := io.inst(11, 7)
//   ftrace.io.ref_jalr := type_IJ
//   ftrace.io.ref_jal := type_J
//   ftrace.io.src1 := regfile.io.rs1

//   // getInstruction.io.inst := instMem.io.inst

//   regfile.io.readreg1 := io.inst(19, 15)
//   regfile.io.readreg2 := io.inst(24, 20)
//   regfile.io.writereg := io.inst(11, 7)
//   io.writereg         := regfile.io.writereg
//   regfile.io.writeEn  := cntlLogic.io.writeEn

//   val rmemdata = Wire(UInt(width.W))

//   // write back stage
//   regfile.io.data := MuxCase(
//     0.U,
//     Seq(
//       (cntlLogic.io.WBsel === 0.U) -> alu.io.res,
//       (cntlLogic.io.WBsel === 1.U) -> (io.pc + config.instLen.U),
//       (cntlLogic.io.WBsel === 2.U) -> rmemdata,
//       (cntlLogic.io.WBsel === 3.U) -> csr.io.csrValue
//     )
//   )

//   // for testing purpose
//   io.x1           := regfile.io.x1
//   io.x2           := regfile.io.x2
//   io.x5           := regfile.io.x5
//   io.x6           := regfile.io.x6
//   io.x7           := regfile.io.x7
//   io.x8           := regfile.io.x8
//   io.x9           := regfile.io.x9
//   io.x10          := regfile.io.x10
//   io.test_alu_res := alu.io.res

//   alu.io.alusel := cntlLogic.io.alusel
//   // 0 for rs1, 1 for pc
//   alu.io.A := Mux(!cntlLogic.io.asel, regfile.io.rs1, io.pc)
//   // 0 for rs2, 1 for imm
//   alu.io.B := Mux(!cntlLogic.io.bsel, regfile.io.rs2, immgen.io.imm)

//   immgen.io.inst   := io.inst
//   immgen.io.immsel := cntlLogic.io.immsel

//   // mem
//   mem.io.addr := alu.io.res
//   // determined by control logic
//   mem.io.memEnable := cntlLogic.io.memEnable
//   mem.io.memRW     := cntlLogic.io.memRW
//   // if (mem.io.memRW) set wmask to 0b0000
//   // mem.io.memRW = 0, read, set to 0
//   mem.io.wmask := Mux(!mem.io.memRW, 0.U, wmaskGen(io.inst(14, 12), mem.io.addr(1, 0)))
//   mem.io.wdata := regfile.io.rs2
//   val imm_byte = Wire(UInt(8.W))
//   val imm_half = Wire(UInt(16.W))
//   imm_byte := readDataGen(mem.io.addr(1, 0), 1, mem.io.rdata)
//   imm_half := readDataGen(mem.io.addr(1, 0), 2, mem.io.rdata)
//   rmemdata := Mux(
//     io.inst(14),
//     // io.inst(14) == 1, unsigned 直接截断就好
//     MuxCase(
//       mem.io.rdata,
//       Seq(
//         (io.inst(13, 12) === 0.U) -> imm_byte,
//         (io.inst(13, 12) === 1.U) -> imm_half
//       )
//     ),
//     // io.inst(14) == 0, signed 还需符号扩展
//     MuxCase(
//       mem.io.rdata,
//       Seq(
//         (io.inst(13, 12) === 0.U) -> Cat(padding(24, imm_byte(7)), imm_byte),
//         (io.inst(13, 12) === 1.U) -> Cat(padding(16, imm_half(15)), imm_half)
//       )
//     )
//   )

//   // csr
//   csr.io.csrsWriteEn := cntlLogic.io.csrsWriteEn
//   csr.io.csrNo := immgen.io.imm
//   // 只考虑 csrw, 所以直接把 rs1 寄存器的值写入 CSRs[csr_no]
//   csr.io.data := regfile.io.rs1
//   // 需要写回寄存器文件的值
//   csr.io.mcauseData := 0xb.U
//   csr.io.mcauseWriteEn := cntlLogic.io.mcauseWriteEn

//   csr.io.mepcData := io.pc
//   csr.io.mepcWriteEn := cntlLogic.io.mepcWriteEn
  
}


// class Dpi_itrace extends BlackBox with HasBlackBoxResource {
//   val io = IO(new Bundle {
//     val pc     = Input(UInt(32.W))
//     val inst   = Input(UInt(32.W))
//     val nextpc = Input(UInt(32.W))
//   })
//   addResource("/Dpi_itrace.sv")
// }

// class Dpi_ftrace extends BlackBox with HasBlackBoxResource {
//   val io = IO(new Bundle {
//     val optype   = Input(UInt(4.W))
//     val ref_jal  = Input(UInt(4.W))
//     val ref_jalr = Input(UInt(4.W))
//     val rd       = Input(UInt(5.W))
//     val src1     = Input(UInt(32.W))
//   })
//   addResource("/Dpi_ftrace.sv")
// }

class MemIO(width: Int) extends Bundle {
  // val raddr = Input(UInt(width.W))
  val addr      = Input(UInt(width.W))
  val rdata     = Output(UInt(width.W))
  val wdata     = Input(UInt(width.W))
  val wmask     = Input(UInt(8.W))
  val memEnable = Input(Bool())
  val memRW     = Input(Bool())
  // val waddr = Input(UInt(width.W))
}

// class Mem(val width: Int) extends BlackBox with HasBlackBoxResource {
//   val io = IO(new MemIO(width))
//   addResource("/Mem.sv")
// }
