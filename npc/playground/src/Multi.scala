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

  val temp_valid = RegInit(0.U)
  io.out.valid := temp_valid

  io.out.bits.inst := 4.U
  io.out.bits.pc := 4.U

  // wb_valid 就取下一条指令
  // io.in <=> wb_v
  when(io.in) {
    temp_valid := 1.U
  }.elsewhen(io.out.valid && io.out.ready) {
    temp_valid := 0.U
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

  inst := Mux(io.in.valid, io.in.bits.inst, DontCare) 
  pc := Mux(io.in.valid, io.in.bits.pc, DontCare) 

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
