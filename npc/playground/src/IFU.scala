import chisel3._
import chisel3.util._

class Message extends Bundle {
  val inst = UInt(32.W)
  val pc   = UInt(32.W)
}

class IFU extends Module {
  val io = IO(new Bundle {
    val in = Flipped(new Message)
    val out = Decoupled(new Message)
  })

  when (io.out.ready) {
    io.out.valid := 1.U
  }.otherwise {
    io.out.valid := 0.U
  }

  // create 2 state
  val s_idle :: s_wait_ready :: Nil = Enum(2)
  // inital state is idle
  val state = RegInit(s_idle)

  // create a state machine
  state := MuxLookup(state, s_idle)(
    Seq(
      s_idle -> Mux(io.out.valid, s_wait_ready, s_idle),
      s_wait_ready -> Mux(io.out.ready, s_idle, s_wait_ready)
    )
  )
  
}

class IDU extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new Message))
    val out = new Message
  })

  // create 3 state
  val s_block :: s_wait_valid :: Nil = Enum(2)
  // inital state is idle
  val state = RegInit(s_block)

  when (io.in.valid) {
    io.in.ready := 1.U
  }.otherwise {
    io.in.ready := 0.U
  }

  // create a state machine
  state := MuxLookup(state, s_block)(
    Seq(
      s_block -> Mux(io.in.ready, s_wait_valid, s_block),
      s_wait_valid -> Mux(io.in.valid, s_block, s_wait_valid)
    )
  )
}

class AsyncBus extends Module {
  val io = IO(new Bundle {
    val in = Flipped(new Message)
    val out = new Message
  })

  val ifu = Module(new IFU)
  val idu = Module(new IDU)

  ifu.io.in := io.in
  ifu.io.out.ready := 1.U
  idu.io.in <> ifu.io.out
  io.out := idu.io.out

}
