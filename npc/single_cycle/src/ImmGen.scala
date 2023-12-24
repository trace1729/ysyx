
import chisel3._
import chisel3.util._

class ImmGen(width: Int) extends Module {
    val io = IO(new Bundle {
        val inst = Input(UInt(width.W))
        val immsel = Input(UInt(6.W))
        val imm = Output(UInt(width.W))
    })
    
    io.imm := MuxCase(0.U, Seq(
        (io.immsel === 0.U) -> io.inst(31, 20)
    ))
}