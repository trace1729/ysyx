import chisel3._
import chisel3.util._

class CPU extends Module {
  val io = IO(new Bundle{
    val pc = Output(UInt(32.W))
    val inst = Input(UInt(32.W))
  })

  io.pc := RegNext(io.pc + top.inst_len, top.base)
  
}

object top {
  val inst_len = 4.U
  val base = "h80000000".asUInt
}

