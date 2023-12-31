import chisel3._
import chisel3.util._

class top(width: Int = 32) extends Module {
  val io = IO(new Bundle {
    val pc   = Output(UInt(width.W))
    val inst = Input(UInt(width.W))

    // for testing purpose
    val x1           = Output(UInt(width.W))
    val x2           = Output(UInt(width.W))
    val x5           = Output(UInt(width.W))
    val x6           = Output(UInt(width.W))
    val x7           = Output(UInt(width.W))
    val x8           = Output(UInt(width.W))
    val x9           = Output(UInt(width.W))
    val x10          = Output(UInt(width.W))
    val test_alu_res = Output(UInt(width.W))
  })

  val cntlLogic = Module(new controlLogic(width))
  val regfile   = Module(new Regfile(width = width))
  val alu       = Module(new ALU(width))
  val immgen    = Module(new ImmGen(width))

  val pcvalue = Wire(UInt(32.W))
  pcvalue := Mux(!cntlLogic.io.pcsel, io.pc + top.inst_len, alu.io.res)
  io.pc := RegNext(pcvalue, top.base)

  cntlLogic.io.inst := io.inst

  regfile.io.readreg1 := io.inst(19, 15)
  regfile.io.readreg2 := io.inst(24, 20)
  regfile.io.writereg := io.inst(11, 7)
  regfile.io.writeEn  := cntlLogic.io.writeEn
  regfile.io.data := MuxCase(
    0.U,
    Seq(
      (cntlLogic.io.WBsel === 0.U) -> alu.io.res,
      (cntlLogic.io.WBsel === 1.U) -> (io.pc + top.inst_len)
    )
  )

  // for testing purpose
  io.x1           := regfile.io.x1
  io.x2           := regfile.io.x2
  io.x5           := regfile.io.x5
  io.x6           := regfile.io.x6
  io.x7           := regfile.io.x7
  io.x8           := regfile.io.x8
  io.x9           := regfile.io.x9
  io.x10          := regfile.io.x10
  io.test_alu_res := alu.io.res

  alu.io.alusel := cntlLogic.io.alusel
  // 0 for rs1, 1 for pc
  alu.io.A := Mux(!cntlLogic.io.asel, regfile.io.rs1, io.pc)
  // 0 for rs2, 1 for imm
  alu.io.B := Mux(!cntlLogic.io.bsel, regfile.io.rs2, immgen.io.imm)

  immgen.io.inst   := io.inst
  immgen.io.immsel := cntlLogic.io.immsel


}


object top {
  val inst_len = 4.U
  val base     = "h80000000".asUInt
}
