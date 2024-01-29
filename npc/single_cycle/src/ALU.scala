import chisel3._
import chisel3.util._

class ALU(width: Int = 32) extends Module {

  val io = IO(new Bundle {
    val alusel   = Input(UInt(4.W))
    val A        = Input(UInt(width.W))
    val B        = Input(UInt(width.W))
    val res      = Output(UInt(width.W))
    val overflow = Output(Bool())
    val carry    = Output(Bool())
    val zero     = Output(Bool())
  })

  /* 加法器 */
  val add       = Module(new adder(width))
  val adder_res = Wire(UInt(width.W))
  // 两个输入
  add.io.A := io.A
  add.io.B := io.B
  //
  add.io.Cin := (io.alusel(1) | io.alusel(2))
  // 输出
  io.overflow := add.io.overflow
  io.carry    := add.io.carry
  adder_res   := add.io.result
  io.zero     := ~VecInit(adder_res.asBools).reduce(_ | _)

  /* 比较器 */
  val less = Wire(Bool())
  // 0: signed
  // 1: unsigned
  less := Mux(!io.alusel(0), adder_res(width - 1) ^ io.overflow, io.carry ^ add.io.Cin)

  /* 移位器 */
  val shifter_res = Wire(UInt(width.W))
  val shifter     = Module(new Shifter(width))
  shifter.io.left_right            := io.alusel(2)
  shifter.io.logical_or_arthimetic := io.alusel(3)
  shifter.io.in                    := io.A
  shifter.io.shamt                 := io.B(5, 0)
  shifter_res                      := shifter.io.out

  // 选择器
  io.res := MuxCase(
    0.U,
    Seq(
      (io.alusel === "b0000".asUInt) -> adder_res, // add
      (io.alusel === "b0001".asUInt) -> shifter_res, // left shift
      (io.alusel === "b0010".asUInt) -> less,
      (io.alusel === "b0011".asUInt) -> less,
      (io.alusel === "b0100".asUInt) -> (io.A ^ io.B), // xor
      (io.alusel === "b0101".asUInt) -> shifter_res, // logical r
      (io.alusel === "b0110".asUInt) -> (io.A | io.B),
      (io.alusel === "b0111".asUInt) -> (io.A & io.B),
      // (io.alusel === "b1000".asUInt) -> ,
      // (io.alusel === "b1001".asUInt) -> ,
      // (io.alusel === "b1010".asUInt) -> ,
      // (io.alusel === "b1011".asUInt) -> ,
      (io.alusel === "b1100".asUInt) -> adder_res, // sub
      (io.alusel === "b1101".asUInt) -> shifter_res, // arithmetic r
      (io.alusel === "b1110".asUInt) -> 0.U, // unused
      (io.alusel === "b1111".asUInt) -> io.B
    )
  )

}

class adder(width: Int = 32) extends Module {
  val io = IO(new Bundle {
    val A        = Input(UInt(width.W))
    val B        = Input(UInt(width.W))
    val Cin      = Input(Bool())
    val result   = Output(UInt(width.W))
    val overflow = Output(Bool())
    val carry    = Output(Bool())
  })

  val complement_of_B   = Wire(UInt(width.W))
  val carry_with_result = Wire(UInt((width + 1).W))

  complement_of_B   := io.B ^ Cat(Seq.fill(width)(io.Cin))
  carry_with_result := Seq(io.A, complement_of_B, io.Cin).reduce(_ +& _)

  io.result := carry_with_result(width - 1, 0)
  io.carry  := carry_with_result(width)
  // 注意一定是 B 的补码
  io.overflow := (io.A(width - 1) === complement_of_B(width - 1)) && (io.result(width - 1) =/= io.A(width - 1))

}

class Shifter(width: Int) extends Module {
  val io = IO(new Bundle {
    val logical_or_arthimetic = Input(Bool())
    val left_right            = Input(Bool())
    val in                    = Input(UInt(width.W))
    val shamt                 = Input(UInt(6.W))
    val out                   = Output(UInt(width.W))
  })
  // 实现一个高效的移位器
  // 先做一个 dumby version, 之后可以和使用桶形移位器的做法对比一下，看下
  // 综合出来的电路有无区别

  // rv32I 移位数的最高位需要为 0

  io.out := Mux(
    !io.left_right,
    io.in << io.shamt(4, 0),
    Mux(!io.logical_or_arthimetic, io.in >> io.shamt(4, 0), (io.in.asSInt >> io.shamt(4, 0)).asUInt)
  )


}

// io.out := DontCare
// // 0 -> left
// // 1 -> right
// when (io.left_right) {
//   // 0 -> logical
//   // 1 -> arthimetic
//   when (io.logical_or_arthimetic) {
//     io.out := ((io.in.asSInt) >> io.shamt).asUInt
//   }.otherwise {
//     io.out := io.in >> io.shamt
//   }
// }.otherwise {
//   io.out := io.in << io.shamt
// }