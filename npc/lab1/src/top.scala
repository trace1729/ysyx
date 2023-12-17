import chisel3._
import chisel3.util._

class top extends Module {
  val io = IO(new Bundle {
    val sw = Input(UInt(10.W))
    val led = Output(UInt(2.W))
  })

  val selector = Module(new selector)
  selector.io.in(0) := io.sw(3, 2)
  selector.io.in(1) := io.sw(5, 4)
  selector.io.in(2) := io.sw(7, 6)
  selector.io.in(3) := io.sw(9, 8)

  selector.io.in(4) := io.sw(1, 0)  // sel
  io.led := selector.io.out
}

class selector extends Module {
  val io = IO(new Bundle {
    val in = Input(Vec(5, UInt(2.W)))
    val out = Output(UInt(2.W))
  })

  io.out := MuxCase(io.in(0), Array(
    (io.in(4) === 0.U) -> io.in(0),
    (io.in(4) === 1.U) -> io.in(1),
    (io.in(4) === 2.U) -> io.in(2),
    (io.in(4) === 3.U) -> io.in(3)
    ))

}
