import chisel3._
import chisel3.util._

class controlLogic(width: Int = 32) extends Module {
  val io = IO(new Bundle {
    val inst    = Input(UInt(width.W))
    val writeEn = Output(Bool())
    val immsel  = Output(UInt(6.W))
    val bsel    = Output(Bool())
    val alusel  = Output(UInt(4.W))
  })

  io.writeEn := 1.U
  io.immsel := 0.U
  io.bsel := 0.U
  io.alusel := 0.U
}
  
  
