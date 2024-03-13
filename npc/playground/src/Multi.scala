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

  val pc = RegInit(0.U(32.W))
  val inst = RegInit(0.U(32.W))

  // valid 就取下一条指令
  when (io.in) {
    pc := pc + 4.U
    inst := inst + 1.U
    io.out.valid := 1.U
  }.otherwise {
    io.out.valid := 0.U
  }

  // 当获取到下一个单元的 ready 信号时，将指令和程序计数器传递给下一个模块
  when (io.out.ready) {
    io.out.bits.inst := inst
    io.out.bits.pc := pc
    }.otherwise {
      io.out.bits.inst := DontCare
      io.out.bits.pc := DontCare
    }

}

class IDU extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new Message))
    val out = new Message
  })
  
  io.in.ready := io.in.valid

  when (io.in.valid) {
    io.out := io.in.bits
  }.otherwise{
    io.out := DontCare
  }


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
