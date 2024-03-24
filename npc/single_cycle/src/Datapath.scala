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

class IFU(memoryFile: String) extends Module {
  val wb2if_in      = IO(Flipped(Decoupled(new WBOutputIO)))
  val if2id_out     = IO(Decoupled(new IFUOutputIO))
  val instMem = Module(new InstMem(memoryFile = memoryFile))


  instMem.io.pc := if2id_out.bits.pc
  if2id_out.bits.pc   := RegNext(wb2if_in.bits.wb_nextpc, config.startPC.U)
  if2id_out.bits.inst := Cat(instMem.io.inst)

  wb2if_in.ready := wb2if_in.valid

  val ifu_valid_reg = RegInit(1.U)

  if2id_out.valid := ifu_valid_reg
  
  when (wb2if_in.valid) {
    ifu_valid_reg := 1.U
  }.elsewhen(if2id_out.ready && if2id_out.valid) {
    ifu_valid_reg := 0.U
  }
}

/** *******************IDU***************************
  */

class IDUOutputIO extends Bundle {
  val rs1         = Output(UInt(width.W))
  val rs2         = Output(UInt(width.W))
  val immediate   = Output(UInt(width.W))
  val ctrlsignals = Output(new ctrlSignals)

  val pc       = Output(UInt(width.W))
  val inst     = Output(UInt(width.W))
  val csrvalue = Output(UInt(width.W))
  val mepc = Output(UInt(width.W))
  val mtvec = Output(UInt(width.W))
}

class IDU extends Module {
  val data = IO(Input(UInt(width.W)))
  val if2id_in   = IO(Flipped(Decoupled(new IFUOutputIO)))
  val id2ex_out  = IO(DecoupledIO(new IDUOutputIO))

  val regfile   = Module(new Regfile(num = regsNum, width = width))
  val ctrlLogic = Module(new controlLogic(width))
  val immgen    = Module(new ImmGen(width))
  val csr       = Module(new CSR(10, width))

  // 输入的 ready 跟随 valid
  if2id_in.ready := if2id_in.valid

  // valid 信号
  val idu_valid_reg = RegInit(0.U)
  val idu_inst_reg = RegInit(UInt(32.W), config.NOP)
  val idu_pc_reg = RegInit(UInt(32.W), 0.U)

  id2ex_out.valid := idu_valid_reg

  when (if2id_in.valid) {
    idu_valid_reg := 1.U
  }.elsewhen(id2ex_out.valid && id2ex_out.ready) {
    idu_valid_reg := 0.U
  }

  when (if2id_in.valid) {
    idu_inst_reg := if2id_in.bits.inst
    idu_pc_reg := if2id_in.bits.pc
  }  


  // 寄存器文件的连接
  regfile.io.readreg1 := idu_inst_reg(19, 15)
  regfile.io.readreg2 := idu_inst_reg(24, 20)
  regfile.io.writereg := idu_inst_reg(11, 7)
  regfile.io.writeEn  := ctrlLogic.io.ctrlsignals.writeEn
  regfile.io.data     := data

  // 控制逻辑的连接
  ctrlLogic.io.inst := idu_inst_reg
  ctrlLogic.io.rs1  := regfile.io.rs1
  ctrlLogic.io.rs2  := regfile.io.rs2

  // 立即数生成器的连接
  immgen.io.inst   := idu_inst_reg
  immgen.io.immsel := ctrlLogic.io.ctrlsignals.immsel

  // csr 寄存器文件的连接
  csr.io.csrsWriteEn := ctrlLogic.io.ctrlsignals.csrsWriteEn
  csr.io.csrNo       := immgen.io.imm
  // 只考虑 csrw, 所以直接把 rs1 寄存器的值写入 CSRs[csr_no]
  csr.io.data := regfile.io.rs1
  // 需要写回寄存器文件的值
  csr.io.mcauseData    := 0xb.U
  csr.io.mcauseWriteEn := ctrlLogic.io.ctrlsignals.mcauseWriteEn
  csr.io.mepcData      := idu_pc_reg
  csr.io.mepcWriteEn   := ctrlLogic.io.ctrlsignals.mepcWriteEn

  // 生成控制信号
  id2ex_out.bits.ctrlsignals := ctrlLogic.io.ctrlsignals

  // idu 模块的输出
  id2ex_out.bits.rs1       := regfile.io.rs1
  id2ex_out.bits.rs2       := regfile.io.rs2
  id2ex_out.bits.immediate := immgen.io.imm
  id2ex_out.bits.pc        := idu_pc_reg
  id2ex_out.bits.inst      := idu_inst_reg
  id2ex_out.bits.csrvalue  := csr.io.csrValue

  id2ex_out.bits.mepc := csr.io.mepc
  id2ex_out.bits.mtvec := csr.io.mtvec


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
  val mepc = Output(UInt(width.W))
  val mtvec = Output(UInt(width.W))
}

class EX extends Module {
  val id2ex_in  = IO(Flipped(Decoupled(new IDUOutputIO)))
  val ex2mem_out = IO(Decoupled(new EXOutputIO))

  val alu = Module(new Alu(width))
  // 因为控制逻辑是贯穿五个阶段的，所以每一个阶段(除了ID)都会有控制信号的输入
  // 这样就比较怪了，那我当前的阶段需要将控制信号传递给之后的阶段

  alu.io.alusel := id2ex_in.bits.ctrlsignals.alusel
  // 0 for rs1, 1 for pc
  alu.io.A := Mux(!id2ex_in.bits.ctrlsignals.asel, id2ex_in.bits.rs1, id2ex_in.bits.pc)
  // 0 for rs2, 1 for imm
  alu.io.B := Mux(!id2ex_in.bits.ctrlsignals.bsel, id2ex_in.bits.rs2, id2ex_in.bits.immediate)

  ex2mem_out.bits.carry       := alu.io.carry
  ex2mem_out.bits.overflow    := alu.io.overflow
  ex2mem_out.bits.alures      := alu.io.res
  ex2mem_out.bits.zero        := alu.io.zero
  ex2mem_out.bits.ctrlsignals := id2ex_in.bits.ctrlsignals

  ex2mem_out.bits.pc       := id2ex_in.bits.pc
  ex2mem_out.bits.inst     := id2ex_in.bits.inst
  ex2mem_out.bits.csrvalue := id2ex_in.bits.csrvalue
  ex2mem_out.bits.rs2      := id2ex_in.bits.rs2

  ex2mem_out.bits.mepc := id2ex_in.bits.mepc
  ex2mem_out.bits.mtvec := id2ex_in.bits.mtvec

  // ready, valid 信号全部设置成1
  // id2ex_in.ready  := 1.U
  // ex2mem_out.valid := 1.U

  id2ex_in.ready := id2ex_in.valid
  val exu_valid_reg = RegInit(0.U)
  ex2mem_out.valid := exu_valid_reg

  when (id2ex_in.valid) {
    exu_valid_reg := 1.U
  }.elsewhen(ex2mem_out.ready && ex2mem_out.valid) {
    exu_valid_reg := 0.U
  }
  
}

/** *******************MEM***************************
  */

class MEMOutputIO(width: Int) extends Bundle {
  val pc          = Output(UInt(width.W))
  val inst          = Output(UInt(width.W))
  val ctrlsignals = Output(new ctrlSignals)
  val csrvalue    = Output(UInt(width.W))
  val alures      = Output(UInt(width.W))
  val rdata       = Output(UInt(width.W))
  val mepc = Output(UInt(width.W))
  val mtvec = Output(UInt(width.W))
}

class LSU extends Module {
  val in  = IO(Flipped(Decoupled(new EXOutputIO)))
  val out = IO(Decoupled(new MEMOutputIO(width)))
  val dmem = Module(new Dmem(width))

  val rmemdata = Wire(UInt(width.W))
  val mem_valid_reg = RegInit(0.U)

  dmem.io.addr := in.bits.alures
  // determined by control logic
  dmem.io.memEnable := mem_valid_reg & in.bits.ctrlsignals.memEnable
  dmem.io.memRW     := in.bits.ctrlsignals.memRW
  // if (mem.io.memRW) set wmask to 0b0000
  // mem.io.memRW = 0, read, set to 0
  dmem.io.wmask := Mux(!dmem.io.memRW, 0.U, wmaskGen(in.bits.inst(14, 12), dmem.io.addr(1, 0)))
  dmem.io.wdata := in.bits.rs2
  val imm_byte = Wire(UInt(8.W))
  val imm_half = Wire(UInt(16.W))
  imm_byte := readDataGen(dmem.io.addr(1, 0), 1, dmem.io.rdata)
  imm_half := readDataGen(dmem.io.addr(1, 0), 2, dmem.io.rdata)
  rmemdata := Mux(
    in.bits.inst(14),
    // io.inst(14) == 1, unsigned 直接截断就好
    MuxCase(
      dmem.io.rdata,
      Seq(
        (in.bits.inst(13, 12) === 0.U) -> imm_byte,
        (in.bits.inst(13, 12) === 1.U) -> imm_half
      )
    ),
    // io.inst(14) == 0, signed 还需符号扩展
    MuxCase(
      dmem.io.rdata,
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
  out.bits.rdata       := rmemdata
  out.bits.inst := in.bits.inst

  //csr
  out.bits.mepc := in.bits.mepc
  out.bits.mtvec := in.bits.mtvec

  // ready, valid 信号全部设置成1
  in.ready := in.valid
  out.valid := mem_valid_reg
  
  when (in.valid) {
    mem_valid_reg := 1.U
  }.elsewhen(out.valid && out.ready) {
    mem_valid_reg := 0.U
  }


}

/** *******************WB***************************
  */

class WBOutputIO extends Bundle {
  // 暂时不太清楚 wb 需要输出什么
  val wb_data = Output(UInt(32.W))
  val wb_nextpc = Output(UInt(32.W))
}

class WB extends Module {
  val mem2wb_in   = IO(Flipped(Decoupled(new MEMOutputIO(width))))
  val wb2ifu_out  = IO(Decoupled(new WBOutputIO))

  val wb_data_reg = RegNext(wb2ifu_out.bits.wb_data, 0.U)
  val wb_nextpc_reg = RegNext(wb2ifu_out.bits.wb_nextpc, 0.U)

  wb2ifu_out.bits.wb_data := wb_data_reg
  wb2ifu_out.bits.wb_nextpc := wb_nextpc_reg

  when (mem2wb_in.valid) {
    wb_data_reg := MuxCase(
      0.U,
      Seq(
        (mem2wb_in.bits.ctrlsignals.WBsel === 0.U) -> mem2wb_in.bits.alures,
        (mem2wb_in.bits.ctrlsignals.WBsel === 1.U) -> (mem2wb_in.bits.pc + config.XLEN.U),
        (mem2wb_in.bits.ctrlsignals.WBsel === 2.U) -> mem2wb_in.bits.rdata,
        (mem2wb_in.bits.ctrlsignals.WBsel === 3.U) -> mem2wb_in.bits.csrvalue
      )
    )
    wb_nextpc_reg := MuxCase(
      0.U,
      Seq(
        (mem2wb_in.bits.ctrlsignals.pcsel === 0.U) -> (mem2wb_in.bits.pc + config.XLEN.U),
        (mem2wb_in.bits.ctrlsignals.pcsel === 1.U) -> mem2wb_in.bits.alures,
        (mem2wb_in.bits.ctrlsignals.pcsel === 2.U) -> mem2wb_in.bits.mepc,
        (mem2wb_in.bits.ctrlsignals.pcsel === 3.U) -> mem2wb_in.bits.mtvec
      )
    )
  }

  val itrace = Module(new Dpi_itrace)
  itrace.io.pc     := mem2wb_in.bits.pc
  itrace.io.inst   := mem2wb_in.bits.inst
  itrace.io.nextpc := wb_nextpc_reg

  mem2wb_in.ready := mem2wb_in.valid
  val wb_valid = RegInit(0.U)
  wb2ifu_out.valid := wb_valid

  when (mem2wb_in.valid) {
    wb_valid := 1.U
  }.elsewhen(wb2ifu_out.valid && wb2ifu_out.ready) {
    wb_valid := 0.U
  }

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
  val mem = Module(new LSU)
  val wb  = Module(new WB)

  ifu.if2id_out <> idu.if2id_in
  idu.id2ex_out <> ex.id2ex_in
  ex.ex2mem_out <> mem.in
  mem.out <> wb.mem2wb_in
  wb.wb2ifu_out <> ifu.wb2if_in

  // 诡异的连线，上面各阶段之间的握手突出一个毫无意义 (确定 pc 和 寄存器的写回值)
  idu.data := wb.wb2ifu_out.bits.wb_data

  // datapath 的输出
  io.inst := ifu.if2id_out.bits.inst
  io.pc   := ifu.if2id_out.bits.pc
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
