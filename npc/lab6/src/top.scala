import chisel3._
import chisel3.util._

class top extends Module {
  val seg0 = IO(Output(UInt(8.W)))
  val seg1 = IO(Output(UInt(8.W)))

  val timer = RegInit(0.U(32.W))
  timer := Mux(timer === 500000.U, 0.U, RegNext(timer + 1.U))

  val out  = RegInit(0.U(8.W))
  val taps  = Seq.fill(8)(RegInit(0.U(1.W)))
  val const = Seq(1.U, 0.U, 1.U, 1.U, 1.U, 0.U, 0.U, 0.U)
  // 7 -> 6 - > 5 ... 
  taps.tail.zip(taps).foreach {
    case (a, b) =>
      when(timer === 0.U) { b := a }
  }

  // 每次都更新最高位
  when(timer === 0.U && taps.reduce(_ + _) =/= 0.U) {
    taps(7) := taps
      .zip(const)
      .map {
        case (a, b) => a * b
      }
      .reduce(_ ^ _)
  }.elsewhen(taps.reduce(_ + _) === 0.U) {
    taps(0) := 1.U
  }

  out := Cat(taps.reverse)

  val s0 = Module(new Seg())
  val s1 = Module(new Seg())

  s0.io.enable := true.B
  s0.io.seg_in := out(3, 0)
  seg0         := s0.io.seg_out

  s1.io.enable := true.B
  s1.io.seg_in := out(7, 4)
  seg1         := s1.io.seg_out
}

class Seg extends Module {
  val io = IO(new Bundle {
    val enable  = Input(Bool())
    val seg_in  = Input(UInt(4.W))
    val seg_out = Output(UInt(8.W))
  })

  val led_encoding = Seq(
    "b11111101".asUInt,
    "b01100000".asUInt,
    "b11011010".asUInt,
    "b11110010".asUInt,
    "b01100110".asUInt,
    "b10110110".asUInt,
    "b10111110".asUInt,
    "b11100000".asUInt,
    "b11111110".asUInt,
    "b11100110".asUInt,
    "b11101111".asUInt,
    "b00111110".asUInt,
    "b10011100".asUInt,
    "b01111010".asUInt,
    "b10011110".asUInt,
    "b10001110".asUInt
  )

  io.seg_out := Mux(
    !io.enable,
    0xff.U,
    ~MuxCase(
      0.U,
      led_encoding.zipWithIndex.map {
        case (value, index) =>
          (io.seg_in === index.U) -> value
      }
    )
  )
}
