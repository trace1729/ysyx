import chisel3._
import chisel3.util._

class top extends Module {
  val io = IO(new Bundle {
    val sw   = Input(UInt(9.W))
    val ledr = Output(Vec(4, UInt(1.W)))
    val seg0 = Output(UInt(8.W))
  })
  val hotvalue = Wire(UInt(3.W))

  hotvalue := Mux(
    io.sw(7, 0) === 0.U,
    0.U,
    PriorityMux(
      Seq(
        io.sw(0) -> 0.U,
        io.sw(1) -> 1.U,
        io.sw(2) -> 2.U,
        io.sw(3) -> 3.U,
        io.sw(4) -> 4.U,
        io.sw(5) -> 5.U,
        io.sw(6) -> 6.U,
        io.sw(7) -> 7.U
      )
    )
  )

  val seg = Module(new Seg)

  seg.io.enable := io.sw(8)
  seg.io.seg_in := hotvalue
  io.seg0       := seg.io.seg_out

  io.ledr(0) := hotvalue(0)
  io.ledr(1) := hotvalue(1)
  io.ledr(2) := hotvalue(2)
  io.ledr(3) := Mux(hotvalue === 0.U, 0.U, 1.U)
}

class Seg extends Module {
  val io = IO(new Bundle {
    val enable  = Input(Bool())
    val seg_in  = Input(UInt(3.W))
    val seg_out = Output(UInt(8.W))
  })

  io.seg_out := Mux(!io.enable, 
      0xff.U,
      ~MuxCase(
      0.U(8.W),
      Seq(
        (io.seg_in === 0.U) -> 0xfd.U,
        (io.seg_in === 1.U) -> 0x60.U,
        (io.seg_in === 2.U) -> 0xda.U,
        (io.seg_in === 3.U) -> 0xf2.U,
        (io.seg_in === 4.U) -> 0x66.U,
        (io.seg_in === 5.U) -> 0xb6.U,
        (io.seg_in === 6.U) -> 0xbe.U,
        (io.seg_in === 7.U) -> 0xe0.U
      ))
  )
}
