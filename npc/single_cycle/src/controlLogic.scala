import chisel3._
import chisel3.util._
class controlLogic(width: Int = 32) extends Module {

  val io = IO(new Bundle {
    val inst = Input(UInt(width.W))
    // selecting pc + 4 (0) or pc + imm (1)
    val pcsel   = Output(Bool())
    val writeEn = Output(Bool())
    // generate imm based on instruction type
    val immsel = Output(UInt(6.W))
    // selecting alu operand1 (readreg1 or pc)
    val asel = Output(Bool())
    // selecting alu operand1 (readreg2 or imm)
    val bsel   = Output(Bool())
    val alusel = Output(UInt(4.W))
    val memRW = Output(Bool())
    val WBsel = Output(UInt(3.W))
  })
  // io.writeEn := 1.U
  // io.immsel  := 0.U
  // io.bsel    := 0.U
  // io.alusel  := 0.U
  
  val optype = Wire(UInt(3.W))
  // 定义指令类型
  val type_I :: type_IS :: type_U :: type_S :: type_J :: type_R :: type_B :: type_N :: Nil = Enum(8)

  optype := MuxCase(type_N, Seq(
    (io.inst(6, 0) ===  "b0010011".asUInt) -> type_I,
    // jalr
    (io.inst(6, 0) ===  "b1100111".asUInt) -> type_I,
    // load 
    // (io.inst(6, 0) ===  "b0000011".asUInt) -> type_IL,
    (io.inst(6, 0) ===  "b0010111".asUInt) -> type_U,
    (io.inst(6, 0) ===  "b0110111".asUInt) -> type_U,
    (io.inst(6, 0) ===  "b0100011".asUInt) -> type_S,
    (io.inst(6, 0) ===  "b1101111".asUInt) -> type_J,
  ))  

  // default value
  io.pcsel := 0.U
  io.writeEn := 0.U
  io.immsel := 0.U
  io.asel := 0.U
  io.bsel := 0.U
  io.alusel := 0.U
  io.memRW := 0.U
  io.WBsel := 0.U

  // output control logic based on instruction type
  switch(optype) {
    // normally: alures = rs1 (0) + imm (1); R[rd] = alusel (0); pc = pc + 4 (0)
    // Specially: (jalr) alures = rs1 (0) + imm (1); R[rd] = pc + 4 (1); pc = alures (1)
    is (type_I) { 
      io.pcsel := io.inst(5)
      io.writeEn := 1.U
      io.immsel := 0.U
      io.asel := 0.U
      io.bsel := 1.U
      io.alusel := io.inst(14, 12)  // func3
      io.memRW := 0.U
      io.WBsel := io.inst(5)
    } 
    // // (lw) alures = rs1 (0) + imm (1); R[rd] = Mr(alures) (1); pc = pc + 4 (0)
    // is (type_IL) {
    //   io.pcsel := 0.U
    //   io.writeEn := 1.U
    //   io.immsel := 0.U
    //   io.asel := 0.U
    //   io.bsel := 1.U
    //   io.alusel := 0.U
    //   io.memRW := 0.U
    //   io.WBsel := 2.U
    // }
    // auipc: alures = pc + imm << 12; R[rd] = alusel
    // lui:   alures = imm << 12; R[rd] = alusel
    is (type_U) {
      io.pcsel := 0.U
      io.writeEn := 1.U
      io.immsel  := 1.U
      io.asel := 1.U
      io.bsel := 1.U
      io.alusel := 0.U(4.W) ^ Cat(Seq.fill(4)(io.inst(5)))
      io.memRW := 0.U
      io.WBsel := 0.U
    }
    // sw: alures = rs1 (0) + imm (1); Mr[alures] = rs2 (2); pc = pc + 4
    is (type_S) {
      io.pcsel := 0.U
      io.writeEn := 0.U
      io.immsel := 2.U
      io.asel := 0.U
      io.bsel := 1.U
      io.alusel := 0.U
      io.memRW := 1.U
      io.WBsel := 0.U
    }
    // alures = pc (1) + imm (1); R[rd] = pc + 4; pc = alures (1)
    is(type_J) {
      io.pcsel := 1.U
      io.writeEn := 1.U 
      io.immsel := 3.U
      io.asel := 1.U 
      io.bsel := 1.U 
      io.alusel := 0.U
      io.memRW := 0.U
      io.WBsel := 1.U 
    }
  }


  val stop = Module(new BlackBoxRealAdd)
  stop.io.inst_type := optype
  stop.io.inst_ref := type_N
  // 找规律

}

class BlackBoxRealAdd extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val inst_ref = Input(UInt(4.W))
    val inst_type = Input(UInt(4.W))
  })
  addResource("/halt_handler.v")
}
// ??????? ????? ????? ??? ????? 00101 11, auipc  , U
// ??????? ????? ????? ??? ????? 01101 11, lui    , U, R(rd) = imm)
// ??????? ????? ????? 100 ????? 00000 11, lbu    , I
// ??????? ????? ????? 000 ????? 01000 11, sb     , S
// 0000000 00001 00000 000 00000 11100 11, ebreak , N
// ??????? ????? ????? ??? ????? ????? ??, inv    , N
// ??????? ????? ????? 000 ????? 11001 11, jalr   , I, R(rd) = s->pc + 4, s->dnpc = src1 + imm
// ??????? ????? ????? 000 ????? 00100 11, addi   , I, R(rd) = src1 + imm);
// ??????? ????? ????? ??? ????? 11011 11, jal    , J, R(rd) = s->pc + 4, s->dnpc = s->pc + imm



