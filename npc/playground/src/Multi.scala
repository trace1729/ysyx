import chisel3._
import chisel3.util._

class Message extends Bundle {
  val inst = UInt(32.W)
  val pc   = UInt(32.W)
}

class IFU extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Bool())
    val out = Decoupled(new Message)
  })

  io.out.bits.inst := 4.U
  io.out.bits.pc := 4.U

  // valid 就取下一条指令
  // io.in <=> wb_v
  when (io.in) {
    // ifu_to_idu_valid
    io.out.valid := 1.U
    // ifu_to_idu_ready && ifu_to_idu_valid
  }.elsewhen(io.out.ready && io.out.valid) {
    io.out.valid := 0.U
  }.otherwise{
    io.out.valid := DontCare
  }

}

class IDU extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new Message))
    val out = new Message
  })
  
  val pc = RegInit(0.U(32.W))
  val inst = RegInit(0.U(32.W))

  io.in.ready := io.in.valid
  // valid 就取下一条指令
  // io.in <=> wb_v
  when (io.in.valid && io.in.ready) {
     pc := io.in.bits.pc
     inst := io.in.bits.inst
  }.otherwise{
      pc := DontCare
      inst := DontCare
  }
  io.out.pc := pc
  io.out.inst := inst
}

class AsyncBus extends Module {
  val io = IO(new Bundle {
    val out = new Message
  })

  val ifu = Module(new IFU)
  val idu = Module(new IDU)

  ifu.io.in := true.B
  ifu.io.out <> idu.io.in
  io.out := idu.io.out
}
