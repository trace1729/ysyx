import chisel3._
import chisel3.util._

class Message extends Bundle {
  val inst = Output(UInt(32.W))
  val pc   = Output(UInt(32.W))
}

class IFU extends Module {
  val io = IO(new Bundle {
    val out = Decoupled(new Message)
  })

  // create 3 state
  val s_idle :: s_wait_ready :: Nil = Enum(2)
  // inital state is idle
  val state                         = RegInit(s_idle)

  // create a state machine
  state := MuxLookup(state, s_idle)(
    Seq(
      s_idle -> Mux(io.out.valid, s_wait_ready, s_idle),
      s_wait_ready -> Mux(io.out.ready, s_idle, s_wait_ready)
    )
  )
}

// 
class IDU extends Module {
    val io = IO(new Bundle {
        val in = Flipped(Decoupled(new Message))
    })
}