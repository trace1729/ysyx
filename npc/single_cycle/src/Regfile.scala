import chisel3._
import chisel3.util._

class Regfile extends Module {
  val io = IO(new Bundle {
    val rs1 = Output(UInt(32.W))
    val rs2 = Output(UInt(32.W))

    val x0  = Output(UInt(32.W))
    val x1  = Output(UInt(32.W))
    val x2  = Output(UInt(32.W))
    val x3  = Output(UInt(32.W))
    val x4  = Output(UInt(32.W))
    val x5  = Output(UInt(32.W))
    val x6  = Output(UInt(32.W))

    val readreg1 = Input(UInt(5.W))
    val readreg2 = Input(UInt(5.W))
    val writereg = Input(UInt(5.W))
    val data     = Input(UInt(32.W))
    val writeEn  = Input(Bool())

  })

  val regs = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))
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

  io.x0 := regs(0)
  io.x1 := regs(1)
  io.x2 := regs(2)
  io.x3 := regs(3)
  io.x4 := regs(4)
  io.x5 := regs(5)
  io.x6 := regs(6)

}
