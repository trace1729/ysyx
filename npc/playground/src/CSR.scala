import chisel3._
import chisel3.util._
object CSR {
  val mstatus = 0x300.U(12.W)
  val mtvec = 0x301.U(12.W)
  val mepc = 0x341.U(12.W)
  val mcause = 0x342.U(12.W)
}

object Cause {
  val InstAddrMisaligned = 0x0.U
  val IllegalInst = 0x2.U
  val Breakpoint = 0x3.U
  val LoadAddrMisaligned = 0x4.U
  val StoreAddrMisaligned = 0x6.U
  val Ecall = 0x8.U
}


class CSR_IO extends Bundle {
    val csr_no = Flipped(UInt(12.W))
    val data = Flipped(UInt(32.W))
    val csr_value = UInt(32.W)
}

class CSR (regNum: Int = 10, width: Int) extends Module {
    val io = IO(new CSR_IO)

    val mstatus = RegInit(0x1800.U(32.W))
    val mepc = RegInit  (0.U(32.W))
    val mtvec = RegInit (0.U(32.W))
    val mcause = RegInit(0.U(32.W))
    
    val csr_file = Seq(
        BitPat(CSR.mcause) -> mcause,
        BitPat(CSR.mtvec) -> mtvec,
        BitPat(CSR.mepc) -> mepc,
        BitPat(CSR.mstatus) -> mstatus
    )

    // t = csrs[csr_no]
    io.csr_value := Lookup(io.csr_no, 0.U, csr_file)

    // csrs[csr_no] = data
    when(io.csr_no === CSR.mtvec) {
        mtvec := io.data
    }
    when(io.csr_no === CSR.mepc) {
        mepc := io.data
    }
    when(io.csr_no === CSR.mcause) {
        mcause := io.data
    }
    when(io.csr_no === CSR.mstatus) {
        mstatus := io.data
    }
}