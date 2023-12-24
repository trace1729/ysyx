import chisel3._
import chisel3.util._
 
class Regfile (num: Int = 32, width: Int = 32) extends Module {
  val io = IO(new Bundle {
    val rs1 = Output(UInt(num.W))
    val rs2 = Output(UInt(num.W))

    val x1  = Output(UInt(num.W))
    val x2  = Output(UInt(num.W))
    val x5  = Output(UInt(num.W))
    val x6  = Output(UInt(num.W))
    val x7  = Output(UInt(num.W))
    val x8  = Output(UInt(num.W))
    val x9  = Output(UInt(num.W))
    val x10  = Output(UInt(num.W))
  

    val readreg1 = Input(UInt(5.W))
    val readreg2 = Input(UInt(5.W))
    val writereg = Input(UInt(5.W))
    val data     = Input(UInt(num.W))
    val writeEn  = Input(Bool())

  })

  val regs = RegInit(VecInit(Seq.fill(num)(0.U(width.W))))
  io.rs1 := 0.U
  io.rs2 := 0.U
  regs.zipWithIndex.foreach {
    case (reg, idx) =>
      // 永远向x0 写入 0
      when(io.writeEn && idx.U === io.writereg) { reg := Mux(idx.U === 0.U, 0.U, io.data) }
      // 读取
      when(idx.U === io.readreg1) { io.rs1 := reg }
      when(idx.U === io.readreg2) { io.rs2 := reg }
  }

  io.x1 := regs(1)
  io.x2 := regs(2)
  io.x5 := regs(5)
  io.x6 := regs(6)
  io.x7 := regs(7)
  io.x8 := regs(8)
  io.x9 := regs(9)
  io.x10 := regs(10)

}
