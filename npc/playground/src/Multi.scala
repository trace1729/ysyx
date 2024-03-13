import chisel3._
import chisel3.util._

class Message extends Bundle {
  val inst = UInt(32.W)
  val pc   = UInt(32.W)
}

class IFU extends Module {
  val io = IO(new Bundle {
    val out = Decoupled(new Message)
  })

  // create 2 state
  val s_idle :: s_wait_ready :: Nil = Enum(2)
  // inital state is idle
  val state = RegInit(s_idle)

  val inst = RegInit(0.U(32.W))
  val pc = RegInit(0.U(32.W))
  
  // 假设进行一次指令的读取
  inst := inst + 1.U
  pc := pc + 4.U

  // 读取指令成功
  io.out.vaild := 1.U

  // 把状态机 和 

  when (state === s_wait_ready && io.out.ready) {
    io.out.bits.inst := inst
    io.out.bits.pc := pc
    }.otherwise {
      io.out.bits.inst := DontCare
      io.out.bits.pc := DontCare
    }


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
  })

  // create 3 state
  val s_block :: s_wait_valid :: Nil = Enum(2)
  // inital state is idle
  val state = RegInit(s_block)

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

  ifu.io.out.ready := 1.U
  idu.io.in <> ifu.io.out

}
