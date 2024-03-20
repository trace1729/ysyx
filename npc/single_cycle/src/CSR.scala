package cpu

import chisel3._
import chisel3.util._
import cpu.config._

// reference riscv-mini https://github.com/ucb-bar/riscv-mini.git

object CSR {
  val mstatus = 0x300.U(12.W)
  val mtvec   = 0x305.U(12.W)
  val mepc    = 0x341.U(12.W)
  val mcause  = 0x342.U(12.W)
}

object csrInst {
  // CSR Access
  def CSRRW = BitPat("b?????????????????001?????1110011")
  /*
    对应到 rtl 就是设置 csr_no, 然后设置 rs1 为 data
   */
  def CSRRS = BitPat("b?????????????????010?????1110011")
  /*
    对应到 rtl 就是设置 csr_no, 然后设置 csrValue 为 写回寄存器的值
   */
  // Change Level
  def ECALL = BitPat("b00000000000000000000000001110011")
  /*
    cpu.csr[MEPC] = s->pc;
     对应到 rtl 就是将 csr_no 设置为 0x341, data 设置为 pc
    cpu.csr[MCAUSE] = 0xb;
     对应到 rtl 就是将 csr_no 设置为 0x342, data 设置为 0xb
    return cpu.csr[MTVEC];
      对应到 rtl a  pc 的值
   */
  def MRET = BitPat("b00110000001000000000000001110011")
  // 对应到 rtl 就是将 csr_no 设置为 0x341, 选择 csrValue 作为 pc 的值
}

object Cause {
  val InstAddrMisaligned  = 0x0.U
  val IllegalInst         = 0x2.U
  val Breakpoint          = 0x3.U
  val LoadAddrMisaligned  = 0x4.U
  val StoreAddrMisaligned = 0x6.U
  val Ecall               = 0x8.U
}

class CSR_IO extends Bundle {
  val csrNo = Flipped(UInt(12.W))

  val data    = Flipped(UInt(32.W))
  val csrsWriteEn = Flipped(Bool())

  val mepcData    = Flipped(UInt(32.W))
  val mepcWriteEn = Flipped(Bool())

  val mcauseData    = Flipped(UInt(32.W))
  val mcauseWriteEn = Flipped(Bool())

  val csrValue = UInt(32.W)
  val mepc = UInt(32.W)
  val mtvec = UInt(32.W)
}

class CSR(regNum: Int = 10, width: Int) extends Module {
  val io = IO(new CSR_IO)

  val mstatus = RegInit(0x1800.U(32.W))
  val mepc    = RegInit(0.U(32.W))
  val mtvec   = RegInit(0.U(32.W))
  val mcause  = RegInit(0.U(32.W))

  val csr_file = Seq(
    BitPat(CSR.mcause) -> mcause,
    BitPat(CSR.mtvec) -> mtvec,
    BitPat(CSR.mepc) -> mepc,
    BitPat(CSR.mstatus) -> mstatus
  )

  // t = csrs[csr_no]
  io.csrValue := Lookup(io.csrNo, 0.U, csr_file)
  io.mepc := mepc
  io.mtvec := mtvec

  // csrs[csr_no] = data
  when(io.csrsWriteEn) {
    switch(io.csrNo) {
      is(CSR.mtvec) {
        mtvec := io.data
      }
      is(CSR.mepc) {
        mepc := io.data
      }
      is(CSR.mcause) {
        mcause := io.data
      }
      is(CSR.mstatus) {
        mstatus := io.data
      }
    }
  }

  when(io.mepcWriteEn) {
    mepc := io.mepcData
  }

  when(io.mcauseWriteEn) {
    mcause := io.mcauseData
  }

  val csr_display = Module(new Csrs_display)
  csr_display.io.regs := VecInit(Seq(mstatus, mepc, mtvec, mcause))
}


class Csrs_display extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val regs = Input(Vec(4, UInt(32.W)))
  })
  addResource("/Csrs_display.sv")
}