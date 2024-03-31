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
  val sIDLE, s_waitReady = Value
}

class IFU(memoryFile: String) extends Module {
  val wb2if_in      = IO(Flipped(Decoupled(new WBOutputIO)))
  val if2id_out     = IO(Decoupled(new IFUOutputIO))
  val axiController = Module(AxiController(width, width))
  val sram          = Module(new SRAM)
  // val instMem   = Module(new InstMem(memoryFile = memoryFile))
  sram.in <> axiController.axi

  import stageState._
  val ifu_state = RegInit(sIDLE)
  val updatePC = (ifu_state === sIDLE) && wb2if_in.valid
  if2id_out.bits.pc := RegEnable(wb2if_in.bits.wb_nextpc, config.startPC.U, updatePC)

  // if2id_out.bits.inst := Cat(instMem.io.inst)
  // instMem.io.pc       := if2id_out.bits.pc

  // after fetching pc, we may want to latch the pc value until
  // the instruction is ready to be sent to the next stage


  switch (ifu_state) {
    is (sIDLE) {
      when (wb2if_in.valid) {
        wb2if_in.ready := 1.U
        ifu_state := s_waitReady
      }
    }
    is (s_waitReady) {
      axiController.in.externalMemEn   := 1.U
      axiController.in.externalValid   := 1.U

      when (axiController.transactionEnded) {
        ifu_state := sIDLE
      }
    }
  }
  
  axiController.in.externalAddress := if2id_out.bits.pc
  axiController.in.externalMemRW   := 0.U
  axiController.in.externalData    := DontCare
  axiController.in.externalWmask   := DontCare
  if2id_out.bits.inst              := axiController.axi.readData.bits.data

  if2id_out.valid := axiController.transactionEnded


  val next_inst = Module(new Next_inst)
  next_inst.io.ready := if2id_out.ready
  next_inst.io.valid := if2id_out.valid
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
  val mepc     = Output(UInt(width.W))
  val mtvec    = Output(UInt(width.W))
}

class IDU extends Module {
  val data           = IO(Input(UInt(width.W)))
  val regfileWriteEn = IO(Input(Bool()))
  val csrsWriteEn    = IO(Input(Bool()))
  val mepcWriteEn    = IO(Input(Bool()))
  val mcauseWriteEn  = IO(Input(Bool()))
  val if2id_in       = IO(Flipped(Decoupled(new IFUOutputIO)))
  val id2ex_out      = IO(DecoupledIO(new IDUOutputIO))

  val regfile   = Module(new Regfile(num = regsNum, width = width))
  val ctrlLogic = Module(new controlLogic(width))
  val immgen    = Module(new ImmGen(width))
  val csr       = Module(new CSR(10, width))

  // 输入的 ready 跟随 valid
  if2id_in.ready := if2id_in.valid

  // valid 信号
  val idu_valid_reg = RegInit(0.U)
  val idu_inst_reg  = RegInit(UInt(32.W), config.NOP)
  val idu_pc_reg    = RegInit(UInt(32.W), 0.U)

  id2ex_out.valid := idu_valid_reg

  when(if2id_in.valid) {
    idu_valid_reg := 1.U
  }.elsewhen(id2ex_out.valid && id2ex_out.ready) {
    idu_valid_reg := 0.U
  }

  when(if2id_in.valid) {
    idu_inst_reg := if2id_in.bits.inst
    idu_pc_reg   := if2id_in.bits.pc
  }

  // 寄存器文件的连接
  regfile.io.readreg1 := idu_inst_reg(19, 15)
  regfile.io.readreg2 := idu_inst_reg(24, 20)
  regfile.io.writereg := idu_inst_reg(11, 7)
  regfile.io.writeEn  := regfileWriteEn
  regfile.io.data     := data

  // 控制逻辑的连接
  ctrlLogic.io.inst := idu_inst_reg
  ctrlLogic.io.rs1  := regfile.io.rs1
  ctrlLogic.io.rs2  := regfile.io.rs2

  // 立即数生成器的连接
  immgen.io.inst   := idu_inst_reg
  immgen.io.immsel := ctrlLogic.io.ctrlsignals.immsel

  // csr 寄存器文件的连接
  csr.io.csrsWriteEn := csrsWriteEn
  csr.io.csrNo       := immgen.io.imm
  // 只考虑 csrw, 所以直接把 rs1 寄存器的值写入 CSRs[csr_no]
  csr.io.data := regfile.io.rs1
  // 需要写回寄存器文件的值
  csr.io.mcauseData    := 0xb.U
  csr.io.mcauseWriteEn := mcauseWriteEn
  csr.io.mepcData      := idu_pc_reg
  csr.io.mepcWriteEn   := mepcWriteEn

  // 生成控制信号
  id2ex_out.bits.ctrlsignals := ctrlLogic.io.ctrlsignals

  // idu 模块的输出
  id2ex_out.bits.rs1       := regfile.io.rs1
  id2ex_out.bits.rs2       := regfile.io.rs2
  id2ex_out.bits.immediate := immgen.io.imm
  id2ex_out.bits.pc        := idu_pc_reg
  id2ex_out.bits.inst      := idu_inst_reg
  id2ex_out.bits.csrvalue  := csr.io.csrValue

  id2ex_out.bits.mepc  := csr.io.mepc
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
  val mepc        = Output(UInt(width.W))
  val mtvec       = Output(UInt(width.W))
}

class EX extends Module {
  val id2ex_in   = IO(Flipped(Decoupled(new IDUOutputIO)))
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

  ex2mem_out.bits.mepc  := id2ex_in.bits.mepc
  ex2mem_out.bits.mtvec := id2ex_in.bits.mtvec

  // ready, valid 信号全部设置成1
  // id2ex_in.ready  := 1.U
  // ex2mem_out.valid := 1.U

  id2ex_in.ready := id2ex_in.valid
  val exu_valid_reg = RegInit(0.U)
  ex2mem_out.valid := exu_valid_reg

  when(id2ex_in.valid) {
    exu_valid_reg := 1.U
  }.elsewhen(ex2mem_out.ready && ex2mem_out.valid) {
    exu_valid_reg := 0.U
  }

}

/** *******************MEM***************************
  */

class MEMOutputIO(width: Int) extends Bundle {
  val pc          = Output(UInt(width.W))
  val inst        = Output(UInt(width.W))
  val ctrlsignals = Output(new ctrlSignals)
  val csrvalue    = Output(UInt(width.W))
  val alures      = Output(UInt(width.W))
  val rdata       = Output(UInt(width.W))
  val mepc        = Output(UInt(width.W))
  val mtvec       = Output(UInt(width.W))
}

class LSU extends Module {
  val in            = IO(Flipped(Decoupled(new EXOutputIO)))
  val out           = IO(Decoupled(new MEMOutputIO(width)))
  val axiController = Module(AxiController(width, width))
  val sram          = Module(new SRAM)

  // 看看能不能在 dmem 上加一层 wrapper around, 这样不用修改代码，就可以完成 axi 总线的接入
  // hope we can do this!!

  // activate the axiController
  axiController.in.externalAddress := in.bits.alures
  axiController.in.externalMemRW   := in.bits.ctrlsignals.memRW
  axiController.in.externalMemEn   := in.bits.ctrlsignals.memEnable
  axiController.in.externalData    := in.bits.rs2
  axiController.in.externalWmask := Mux(
    !in.bits.ctrlsignals.memRW,
    0.U,
    wmaskGen(in.bits.inst(14, 12), in.bits.alures(1, 0))
  )
  axiController.in.externalValid := in.valid

  axiController.axi <> sram.in

  // 处理读取的数据
  val rmemdata = Wire(UInt(width.W))
  // if (mem.io.memRW) set wmask to 0b0000
  // mem.io.memRW = 0, read, set to 0
  val imm_byte = Wire(UInt(8.W))
  val imm_half = Wire(UInt(16.W))
  imm_byte := readDataGen(in.bits.alures(1, 0), 1, axiController.axi.readData.bits.data)
  imm_half := readDataGen(in.bits.alures(1, 0), 2, axiController.axi.readData.bits.data)
  rmemdata := Mux(
    in.bits.inst(14),
    // io.inst(14) == 1, unsigned 直接截断就好
    MuxCase(
      axiController.axi.readData.bits.data,
      Seq(
        (in.bits.inst(13, 12) === 0.U) -> imm_byte,
        (in.bits.inst(13, 12) === 1.U) -> imm_half
      )
    ),
    // io.inst(14) == 0, signed 还需符号扩展
    MuxCase(
      axiController.axi.readData.bits.data,
      Seq(
        (in.bits.inst(13, 12) === 0.U) -> Cat(padding(24, imm_byte(7)), imm_byte),
        (in.bits.inst(13, 12) === 1.U) -> Cat(padding(16, imm_half(15)), imm_half)
      )
    )
  )

  // 输出
  out.bits.alures      := in.bits.alures
  out.bits.pc          := in.bits.pc
  out.bits.csrvalue    := in.bits.csrvalue
  out.bits.ctrlsignals := in.bits.ctrlsignals
  out.bits.rdata       := rmemdata
  out.bits.inst        := in.bits.inst

  //csr
  out.bits.mepc  := in.bits.mepc
  out.bits.mtvec := in.bits.mtvec

  // 处理握手信号
  val lsu_valid_reg = RegInit(0.U)
  in.ready := in.valid
  out.valid := MuxCase(
    0.U,
    Seq(
      (in.bits.ctrlsignals.memEnable === 0.U) -> lsu_valid_reg,
      (in.bits.ctrlsignals.memEnable === 1.U) -> axiController.transactionEnded
    )
  )

  when(in.valid) {
    lsu_valid_reg := 1.U
  }.elsewhen(out.valid && out.ready) {
    lsu_valid_reg := 0.U
  }
}

/** *******************WB***************************
  */

class WBOutputIO extends Bundle {
  // 暂时不太清楚 wb 需要输出什么
  val wb_data        = Output(UInt(32.W))
  val wb_nextpc      = Output(UInt(32.W))
  val regfileWriteEn = Output(Bool())
  val csrsWriteEn    = Output(Bool())
  val mepcWriteEn    = Output(Bool())
  val mcauseWriteEn  = Output(Bool())
  // 目前没有必要把 write_rd 也传过来, 因为这个写入地址是不会变的
}

class WB extends Module {
  val lsu2wb_in  = IO(Flipped(Decoupled(new MEMOutputIO(width))))
  val wb2ifu_out = IO(Decoupled(new WBOutputIO))

  val wb_data_reg   = RegNext(wb2ifu_out.bits.wb_data, 0.U)
  val wb_nextpc_reg = RegNext(wb2ifu_out.bits.wb_nextpc, 0.U)

  wb2ifu_out.bits.wb_data   := wb_data_reg
  wb2ifu_out.bits.wb_nextpc := wb_nextpc_reg

  when(lsu2wb_in.valid) {
    wb_data_reg := MuxCase(
      0.U,
      Seq(
        (lsu2wb_in.bits.ctrlsignals.WBsel === 0.U) -> lsu2wb_in.bits.alures,
        (lsu2wb_in.bits.ctrlsignals.WBsel === 1.U) -> (lsu2wb_in.bits.pc + config.XLEN.U),
        (lsu2wb_in.bits.ctrlsignals.WBsel === 2.U) -> lsu2wb_in.bits.rdata,
        (lsu2wb_in.bits.ctrlsignals.WBsel === 3.U) -> lsu2wb_in.bits.csrvalue
      )
    )
    wb_nextpc_reg := MuxCase(
      0.U,
      Seq(
        (lsu2wb_in.bits.ctrlsignals.pcsel === 0.U) -> (lsu2wb_in.bits.pc + config.XLEN.U),
        (lsu2wb_in.bits.ctrlsignals.pcsel === 1.U) -> lsu2wb_in.bits.alures,
        (lsu2wb_in.bits.ctrlsignals.pcsel === 2.U) -> lsu2wb_in.bits.mepc,
        (lsu2wb_in.bits.ctrlsignals.pcsel === 3.U) -> lsu2wb_in.bits.mtvec
      )
    )
  }

  val itrace = Module(new Dpi_itrace)
  itrace.io.pc     := lsu2wb_in.bits.pc
  itrace.io.inst   := lsu2wb_in.bits.inst
  itrace.io.nextpc := wb_nextpc_reg

  lsu2wb_in.ready := lsu2wb_in.valid
  val wb_valid = RegInit(1.U)
  wb2ifu_out.valid := wb_valid

  when(lsu2wb_in.valid) {
    wb_valid := 1.U
  }.elsewhen(wb2ifu_out.valid && wb2ifu_out.ready) {
    wb_valid := 0.U
  }

  wb2ifu_out.bits.regfileWriteEn := wb_valid & lsu2wb_in.bits.ctrlsignals.writeEn
  wb2ifu_out.bits.csrsWriteEn    := wb_valid & lsu2wb_in.bits.ctrlsignals.csrsWriteEn
  wb2ifu_out.bits.mepcWriteEn    := wb_valid & lsu2wb_in.bits.ctrlsignals.mepcWriteEn
  wb2ifu_out.bits.mcauseWriteEn  := wb_valid & lsu2wb_in.bits.ctrlsignals.mcauseWriteEn

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
  mem.out <> wb.lsu2wb_in
  wb.wb2ifu_out <> ifu.wb2if_in

  // 诡异的连线，上面各阶段之间的握手突出一个毫无意义 (确定 pc 和 寄存器的写回值)
  idu.data           := wb.wb2ifu_out.bits.wb_data
  idu.regfileWriteEn := wb.wb2ifu_out.bits.regfileWriteEn
  idu.csrsWriteEn    := wb.wb2ifu_out.bits.csrsWriteEn
  idu.mcauseWriteEn  := wb.wb2ifu_out.bits.mcauseWriteEn
  idu.mepcWriteEn    := wb.wb2ifu_out.bits.mepcWriteEn

  // datapath 的输出
  io.inst := ifu.if2id_out.bits.inst
  io.pc   := ifu.if2id_out.bits.pc
}

// By using Value, you're telling Scala to automatically assign ordinal values to these members.
// By default, aIDLE will have the value 0, aWRITE will have the value 1,
//   aREAD will have the value 2, and aACK will have the value 3.
object SRAMState extends ChiselEnum {
  val aIDLE, awriteDataAddr, awriteData, awriteAddr, aREAD, aACK = Value
}

class SRAM extends Module {
  val in   = IO(AxiLiteSlave(width, width))
  val dmem = Module(new Dmem(width))

  // ready follows the ready
  in.writeAddr.ready := in.writeAddr.valid
  in.writeData.ready := in.writeData.valid
  in.readAddr.ready  := in.readAddr.valid

  import SRAMState._

  // the data and data address are indepentdent of each other
  //   the axi controller pass the data to SRAM when valid and ready are both asserted
  //   the sram tries to write data into the rom
  //   then set the state to ack state

  val state = RegInit(aIDLE)

  dmem.io.raddr := in.readAddr.bits.addr
  dmem.io.waddr := in.writeAddr.bits.addr
  dmem.io.wdata := in.writeData.bits.data
  dmem.io.wmask := in.writeData.bits.strb
  dmem.io.memRW := MuxCase(
    0.U,
    Seq(
      (state === aREAD) -> 0.U,
      (state === awriteDataAddr) -> 1.U
    )
  )
  dmem.io.memEnable     := (state === aREAD) || (state === awriteDataAddr)
  in.readData.bits.data := dmem.io.rdata

  in.writeResp.valid    := false.B
  in.readData.valid     := false.B
  in.readData.bits.resp := 1.U
  in.writeResp.bits     := 1.U

  // using a state machine would elegantly represent
  // the whole axi interface communicating process

  switch(state) {
    is(aIDLE) {
      // received write data and address concurrently
      when(in.writeAddr.ready && in.writeAddr.valid && in.writeData.valid && in.writeData.ready) {
        state := awriteDataAddr
      }.elsewhen(in.writeData.ready && in.writeData.valid) {
        state := awriteData
      }.elsewhen(in.writeAddr.ready && in.writeAddr.valid) {
        state := awriteAddr
      }
      when(in.readAddr.ready && in.readAddr.valid) {
        state := aREAD
      }
    }
    // only received write addr
    is(awriteData) {
      when(in.writeAddr.ready && in.writeAddr.valid) {
        state := awriteDataAddr
      }
    }
    // only received write data
    is(awriteAddr) {
      when(in.writeData.ready && in.writeData.valid) {
        state := awriteDataAddr
      }
    }
    // ready to write
    is(awriteDataAddr) {
      in.writeResp.valid := true.B
      in.writeResp.bits  := 0.U
      state              := aIDLE
    }
    // ready to read
    is(aREAD) {
      in.readData.valid     := 1.U
      in.readData.bits.resp := 0.U
      state                 := aIDLE
    }
    // finished write/read transaction
  }
}

class MemIO(width: Int) extends Bundle {
  val raddr     = Input(UInt(width.W))
  val rdata     = Output(UInt(width.W))
  val wdata     = Input(UInt(width.W))
  val wmask     = Input(UInt(8.W))
  val memEnable = Input(Bool())
  val memRW     = Input(Bool())
  val waddr     = Input(UInt(width.W))
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

class Next_inst extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val valid = Input(Bool())
    val ready = Input(Bool())
  })
  addResource("/Next_inst.sv")
}
