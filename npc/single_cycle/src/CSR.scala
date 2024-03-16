import chisel3._
import chisel3.util._
import config.Config._



class CSR_IO extends Bundle {
    val csr_no = Flipped(UInt(12.W))
    val data = Flipped(UInt(32.W))
    val csr_value = UInt(32.W)
    val writeBackData = UInt(32.W)
}

class CSR (regNum: Int = 10, width: Int) extends Module {
    val io = IO(new CSR_IO)

    val csrs = RegInit(VecInit(Seq.fill(regNum)(0.U(width.W))))

    csrs(io.csr_no) := io.data
    io.writeBackData := csrs(io.csr_no)
}