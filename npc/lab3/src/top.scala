import chisel3._
import chisel3.util._

class top extends Module {

  val io = IO(new Bundle {
    val sw       = Input(UInt(11.W))
    val res      = Output(UInt(4.W))
    val overflow = Output(Bool())
    val carry    = Output(Bool())
    val zero     = Output(Bool())
  })

  val sel       = Wire(UInt(3.W))
  val operand_a = Wire(UInt(4.W))
  val operand_b = Wire(UInt(4.W))

  sel       := io.sw(10, 8) // 选择线
  operand_a := io.sw(7, 4)
  operand_b := io.sw(3, 0)

  /* 加法器 */
  val add       = Module(new adder)
  val adder_res = Wire(UInt(4.W))
  // 两个输入
  add.io.A   := operand_a
  add.io.B   := operand_b
  add.io.Cin := (sel(0) | sel(2)).asUInt
  // 输出
  io.overflow := add.io.overflow
  io.carry    := add.io.carry
  adder_res   := add.io.result
  io.zero     := ~VecInit(adder_res.asBools).reduce(_ | _)

  // 选择器
  io.res := MuxCase(
    0.U,
    Seq(
      (sel === 0.U) -> adder_res,
      (sel === 1.U) -> adder_res,
      (sel === 2.U) -> ~operand_a,
      (sel === 3.U) -> (operand_a | operand_b),
      (sel === 4.U) -> (operand_a & operand_b),
      (sel === 5.U) -> (operand_a ^ operand_b),
      (sel === 6.U) -> (adder_res(3) ^ io.overflow),
      (sel === 7.U) -> io.zero
    )
  )

}

class adder extends Module {
  val io = IO(new Bundle {
    val A        = Input(UInt(4.W))
    val B        = Input(UInt(4.W))
    val Cin      = Input(Bool())
    val result   = Output(UInt(4.W))
    val overflow = Output(Bool())
    val carry    = Output(Bool())
  })

  val complement_of_B   = Wire(UInt(4.W))
  val carry_with_result = Wire(UInt(5.W))

  complement_of_B   := io.B ^ Cat(Seq.fill(4)(io.Cin))
  carry_with_result := Seq(io.A, complement_of_B, io.Cin).reduce(_ +& _)

  io.result := carry_with_result(3, 0)
  io.carry  := carry_with_result(4)
  // 注意一定是 B 的补码
  io.overflow := (io.A(3) === complement_of_B(3)) && (io.result(3) =/= io.A(3))

}
