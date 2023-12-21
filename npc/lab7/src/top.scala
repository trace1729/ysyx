import chisel3._
import chisel3.util._
import upickle.implicits.key

class top extends Module {
  val io = IO(new Bundle {
    val ps2_clk  = Input(UInt(1.W))
    val ps2_data = Input(UInt(1.W))
    val seg0     = Output(UInt(8.W))
    val seg1     = Output(UInt(8.W))
    val seg2     = Output(UInt(8.W))
    val seg3     = Output(UInt(8.W))
    val seg4     = Output(UInt(8.W))
    val seg5     = Output(UInt(8.W))
  })

  val buffer         = RegInit(0.U(10.W))
  val prev_data      = RegInit(0.U(8.W))
  val keystroke      = RegInit(0.U(8.W))
  val collected_data = RegInit(0.U(4.W))
  val count          = RegInit(0.U(8.W))
  val ps2_clk_sync   = RegInit(0.U(3.W))
  val ready          = RegInit(0.U(1.W))
  val key_received   = RegInit(0.U(1.W))

  ps2_clk_sync := Cat(ps2_clk_sync(1, 0), io.ps2_clk)
  val sampling = Wire(Bool())
  sampling := ps2_clk_sync(2) & (~ps2_clk_sync(1))

  val s0 = Module(new Seg)
  val s1 = Module(new Seg)
  val s2 = Module(new Seg)
  val s3 = Module(new Seg)
  val s4 = Module(new Seg)
  val s5 = Module(new Seg)

  val ascii = Wire(UInt(8.W))
  ascii := top.romContent(buffer(8, 1))

  s0.io.enable := key_received
  s0.io.seg_in := buffer(4, 1)
  io.seg0      := s0.io.seg_out

  s1.io.enable := key_received
  s1.io.seg_in := buffer(7, 4)
  io.seg1      := s1.io.seg_out

  s2.io.enable := key_received
  s2.io.seg_in := ascii(3, 0)
  io.seg2      := s2.io.seg_out

  s3.io.enable := key_received
  s3.io.seg_in := ascii(7, 4)
  io.seg3      := s3.io.seg_out

  s4.io.enable := 1.U
  s4.io.seg_in := count(3, 0)
  io.seg4      := s4.io.seg_out

  s5.io.enable := 1.U
  s5.io.seg_in := count(7, 4)
  io.seg5      := s5.io.seg_out

  when(sampling) {
    when(collected_data === 10.U) {
      collected_data := 0.U
      when(
        (buffer(0) === 0.U) &&
          (io.ps2_data === 1.U) &&
          (buffer(9, 1).asBools.reduce(_ ^ _) === 1.U)
      ) {
        printf(p"buffer = 0x${Hexadecimal(buffer(8, 1))}\n")
        printf(p"ascii = 0x${Hexadecimal(buffer(8, 1))}\n")
        when((prev_data =/= buffer(8, 1)) && (buffer(8, 1) =/= 0xf0.U)) {
          prev_data    := buffer(8, 1)
          key_received := 1.U
          keystroke    := keystroke + 1.U
          count        := count + 1.U
        }.otherwise {
          when(buffer(8, 1) === 0xf0.U) {
            key_received := 0.U
          }
        }
      }
    }.otherwise {
      val bools = VecInit(buffer.asBools)
      bools(collected_data) := io.ps2_data
      buffer := bools.asUInt
      collected_data := collected_data + 1.U
    }
  }

}

object top {
  def romContent(addr: UInt): UInt = {
    MuxCase(
      0.U,
      Seq(
        (addr === 0x8.U(8.W)) -> 0x41.U, // 'A'
        (addr === 0x32.U(8.W)) -> 0x42.U, // 'B'
        (addr === 0x33.U(8.W)) -> 0x43.U,
        (addr === 0x35.U(8.W)) -> 0x44.U,
        (addr === 0x36.U(8.W)) -> 0x45.U,
        (addr === 0x43.U(8.W)) -> 0x46.U,
        (addr === 0x52.U(8.W)) -> 0x47.U,
        (addr === 0x51.U(8.W)) -> 0x48.U,
        (addr === 0x67.U(8.W)) -> 0x49.U,
        (addr === 0x59.U(8.W)) -> 0x4a.U,
        (addr === 0x66.U(8.W)) -> 0x4b.U,
        (addr === 0x75.U(8.W)) -> 0x4c.U,
        (addr === 0x58.U(8.W)) -> 0x4d.U,
        (addr === 0x49.U(8.W)) -> 0x4e.U,
        (addr === 0x68.U(8.W)) -> 0x4f.U,
        (addr === 0x77.U(8.W)) -> 0x50.U,
        (addr === 0x21.U(8.W)) -> 0x51.U,
        (addr === 0x45.U(8.W)) -> 0x52.U,
        (addr === 0x27.U(8.W)) -> 0x53.U,
        (addr === 0x44.U(8.W)) -> 0x54.U,
        (addr === 0x60.U(8.W)) -> 0x55.U,
        (addr === 0x42.U(8.W)) -> 0x56.U,
        (addr === 0x29.U(8.W)) -> 0x57.U,
        (addr === 0x34.U(8.W)) -> 0x58.U,
        (addr === 0x53.U(8.W)) -> 0x59.U,
        (addr === 0x26.U(8.W)) -> 0x5a.U,
        (addr === 0x69.U(8.W)) -> 0x30.U,
        (addr === 0x22.U(8.W)) -> 0x31.U,
        (addr === 0x30.U(8.W)) -> 0x32.U,
        (addr === 0x38.U(8.W)) -> 0x33.U,
        (addr === 0x37.U(8.W)) -> 0x34.U,
        (addr === 0x46.U(8.W)) -> 0x35.U,
        (addr === 0x54.U(8.W)) -> 0x36.U,
        (addr === 0x61.U(8.W)) -> 0x37.U,
        (addr === 0x62.U(8.W)) -> 0x38.U,
        (addr === 0x70.U(8.W)) -> 0x39.U
      )
    )
  }
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
