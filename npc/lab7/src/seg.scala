import chisel3._
import chisel3.util._
import upickle.implicits.key

class seg extends Module {
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
