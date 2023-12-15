import chisel3._

class top extends Module {
  val io = IO(new Bundle {
    val sw = Input(UInt(10.W))
    val led = Output(UInt(2.W))
  })

  io.sw.suggestName("sw")
  io.led.suggestName("led")

  val selector = Module(new selector)
  selector.io.sel := io.sw(1, 0) 
  selector.io.x0 := io.sw(3, 2)
  selector.io.x1 := io.sw(5, 4)
  selector.io.x2 := io.sw(7, 6)
  selector.io.x3 := io.sw(9, 8)
  io.led := selector.io.out
}

class selector extends Module {
  val io = IO(new Bundle {
    val x0  = Input(UInt(2.W))
    val x1  = Input(UInt(2.W))
    val x2  = Input(UInt(2.W))
    val x3  = Input(UInt(2.W))
    val sel = Input(UInt(2.W))
    val out = Output(UInt(2.W))
  })

  when(io.sel === 0.U) {
    io.out := io.x0
  }.elsewhen(io.sel === 1.U) {
    io.out := io.x1
  }.elsewhen(io.sel === 2.U) {
    io.out := io.x2
  }.elsewhen(io.sel === 3.U) {
    io.out := io.x3
  }.otherwise {
    io.out := 0.U
  }
}
