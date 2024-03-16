import chisel3._
import chisel3.util._
import config.Config._

class controlLogic(width: Int = 32) extends Module {

  val io = IO(new Bundle {
    val inst = Input(UInt(width.W))
    val rs1 = Input(UInt(width.W))
    val rs2 = Input(UInt(width.W))
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
    val memEnable = Output(Bool())
    val WBsel = Output(UInt(3.W))
    val optype = Output(UInt(4.W))
  })
  // io.writeEn := 1.U
  // io.immsel  := 0.U
  // io.bsel    := 0.U
  // io.alusel  := 0.U
  
  // 定义指令类型
  val optype = Wire(UInt(4.W))

  // func3 7
  val func3 = Wire(UInt(3.W))
  func3 := io.inst(14,12)
  val func7 = Wire(UInt(7.W))
  func7 := io.inst(31, 25)

  optype := MuxCase(type_N, Seq(
    // csrrx
    (io.inst(6, 0) === "b1110011".asUInt) -> type_IE,
    // load 
    (io.inst(6, 0) ===  "b0000011".asUInt) -> type_IL,
    // jalr
    (io.inst(6, 0) ===  "b1100111".asUInt) -> type_IJ,
    // add or sll
    (io.inst(6, 0) ===  "b0010011".asUInt) -> Mux((func3 === "b001".asUInt || func3 === "b101".asUInt), type_IS, type_I),
    (io.inst(6, 0) ===  "b0110011".asUInt) -> type_R,
    (io.inst(6, 0) ===  "b0010111".asUInt) -> type_U,
    (io.inst(6, 0) ===  "b0110111".asUInt) -> type_U,
    (io.inst(6, 0) ===  "b0100011".asUInt) -> type_S,
    (io.inst(6, 0) ===  "b1101111".asUInt) -> type_J,
    (io.inst(6, 0) ===  "b1100011".asUInt) -> type_B,
  ))  

  io.optype := optype
  // default value
  io.pcsel := 0.U
  io.writeEn := 0.U
  io.immsel := 0.U
  io.asel := 0.U
  io.bsel := 0.U
  io.alusel := 0.U
  io.memRW := 0.U
  io.memEnable := 0.U
  io.WBsel := 0.U

  // 比较器
  val comparator = Module(new Comparator)
  comparator.io.BrUn := func3(1)
  comparator.io.rs1 := io.rs1
  comparator.io.rs2 := io.rs2

  // output control logic based on instruction type
  switch(optype) {
    // normally: alures = rs1 (0) + imm (1); R[rd] = alusel (0); pc = pc + 4 (0)
    // Specially: (jalr) alures = rs1 (0) + imm (1); R[rd] = pc + 4 (1); pc = alures (1)
    is (type_I) { 
      io.pcsel := io.inst(5)
      io.writeEn := 1.U
      io.immsel := type_I
      io.asel := 0.U
      io.bsel := 1.U
      io.alusel := io.inst(14, 12)  // func3
      io.memRW := DontCare
      io.memEnable := 0.U
      io.WBsel := io.inst(5)
    } 
    is (type_IJ) {
      io.pcsel := io.inst(5)
      io.writeEn := 1.U
      io.immsel := type_I
      io.asel := 0.U
      io.bsel := 1.U
      io.alusel := io.inst(14, 12)  // func3
      io.memRW := DontCare
      io.memEnable := 0.U
      io.WBsel := io.inst(5)
    } 
    // (lw) alures = rs1 (0) + imm (1); R[rd] = Mr(alures) (2); pc = pc + 4 (0)
    is (type_IL) {
      io.pcsel := 0.U
      io.writeEn := 1.U
      io.immsel := type_I
      io.asel := 0.U
      io.bsel := 1.U
      io.alusel := 0.U
      io.memRW := 0.U
      io.memEnable := 1.U
      io.WBsel := 2.U
    }
    // (csrrw) alures = r     
    is (type_IE) {
      io.pcsel := 0.U
      io.writeEn := 1.U
      io.immsel := type_I
      io.asel := 0.U
      io.bsel := 1.U
      io.alusel := 0.U
      io.memRW := 0.U
      io.memEnable := 1.U
      io.WBsel := 2.U

    }
    is (type_IS) {
      io.pcsel := 0.U
      io.writeEn := 1.U
      io.immsel := type_IS
      io.asel := 0.U
      io.bsel := 1.U
      io.alusel := Cat(func7(5), func3)
      io.memRW := DontCare
      io.memEnable := 0.U
      io.WBsel := 0.U
    }
    // (add) alures = rs1(asel=0) operator(func3, func7) rs2(bsel=0); R[rd] = alures (wbsel=0)
    //   pc = pc + 4 (pcsle = 0)
    is (type_R) {
      io.pcsel := 0.U
      io.writeEn := 1.U
      io.asel := 0.U
      io.bsel := 0.U
      // 指令的 func 域 如果可以和 alu 的选择信号相对应，那么便是极好的
      // 只有 sub 是特殊的，其他的指令alu选择信号都可以用 func7 和 func3 拼接成
      io.alusel := Mux((func3 === 0.U) && (func7(5) === 1.U) ,"b1100".U, Cat(func7(0) | func7(5), func3)) // func3
      io.memRW := DontCare
      io.memEnable := 0.U
      io.WBsel := 0.U
    }
    // auipc: alures = pc + imm << 12; R[rd] = alusel
    // lui:   alures = imm << 12; R[rd] = alusel
    is (type_U) {
      io.pcsel := 0.U
      io.writeEn := 1.U
      io.immsel  := type_U
      io.asel := 1.U
      io.bsel := 1.U
      io.alusel := 0.U(4.W) ^ Cat(Seq.fill(4)(io.inst(5)))
      io.memRW := DontCare
      io.memEnable := 0.U
      io.WBsel := 0.U
    }
    // sw: alures = rs1 (0) + imm (1); Mr[alures] = rs2 (2); pc = pc + 4
    is (type_S) {
      io.pcsel := 0.U
      io.writeEn := 0.U
      io.immsel := type_S
      io.asel := 0.U
      io.bsel := 1.U
      io.alusel := 0.U
      io.memRW := 1.U
      io.memEnable := 1.U
      io.WBsel := 0.U
    }
    // alures = pc (1) + imm (1); R[rd] = pc + 4; pc = alures (1)
    is(type_J) {
      io.pcsel := 1.U
      io.writeEn := 1.U 
      io.immsel := type_J
      io.asel := 1.U 
      io.bsel := 1.U 
      io.alusel := 0.U
      io.memRW := DontCare
      io.memEnable := 0.U
      io.WBsel := 1.U 
    }
    // Beq alures = pc (1) + imm (1); pc = alures(1) / 4 (0)
    is (type_B) {
      // 首先区分是判断 相等还是大小 func3(2) = 0 比较相等
      io.pcsel := Mux(!func3(2), func3(0) ^ comparator.io.BrEq, func3(0) ^ comparator.io.BrLt)
      io.writeEn := 0.U 
      io.immsel := type_B
      io.asel := 1.U 
      io.bsel := 1.U 
      io.alusel := 0.U
      io.memRW := DontCare
      io.memEnable := 0.U
      io.WBsel := 1.U 
    }
  }


  val stop = Module(new BlackBoxRealAdd)
  stop.io.inst_type := optype
  stop.io.inst_ref := type_N
  // 找规律

}

class Comparator extends Module {
  val io = IO(new Bundle {
    val BrUn = Input(Bool())
    val rs1 = Input(UInt(32.W))
    val rs2 = Input(UInt(32.W))
    val BrEq = Output(Bool())
    val BrLt = Output(Bool())
  })
  // func3(1) 这一位表示 是无符号比较还是有符号比较
  io.BrEq := (io.rs1 === io.rs2)
  // io.BrUn (1 means unsigned) 
  io.BrLt := Mux(io.BrUn, (io.rs1 < io.rs2), (io.rs1.asSInt < io.rs2.asSInt))
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



