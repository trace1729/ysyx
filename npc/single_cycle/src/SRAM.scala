package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._

// val lsfr = Module(new LSFR(4))
// 如何拿到 lsfr 的数据，让他在 sram 读取期间保存这个延迟不改变呢

// By using Value, you're telling Scala to automatically assign ordinal values to these members.
// By default, aIDLE will have the value 0, aWRITE will have the value 1,
//   aREAD will have the value 2, and aACK will have the value 3.

object SRAMState extends ChiselEnum {
  val aIDLE, awriteDataAddr, awriteData, awriteAddr, aREAD, aWriteACK, aReadACK, aUART, aRTC, aUARTACK, aRTCACK = Value
}

class SRAM extends Module {

  val in   = IO(AxiLiteSlave(width, width))
  val dmem = Module(new Dmem(width))

  // ready follows valid

  in.aw.ready := in.aw.valid
  in.w.ready := in.w.valid
  in.ar.ready  := in.ar.valid

  import SRAMState._

  // the data and data address are indepentdent of each other
  //   the axi controller pass the data to SRAM when valid and ready are both asserted
  //   the sram tries to write data into the rom
  //   then set the state to ack state

  val state = RegInit(aIDLE)

  // 直接设置一个计数器，来模拟延迟，每一条指令的执行周期都不一样，这样也算模拟了随机延迟了。
  // TODO 为 valid / ready 信号设置一个随机延迟
  // val timer = RegInit(0.U(32.W))
  // timer := Mux(timer === 10.U, 0.U, timer + 1.U)

  // TODO 这里应该把数据锁存在寄存器里，不应该直连
  dmem.io.raddr := RegEnable(in.ar.bits.addr, in.ar.valid && in.ar.ready)
  dmem.io.waddr := RegEnable(in.aw.bits.addr, in.aw.valid && in.aw.ready)
  dmem.io.wdata := RegEnable(in.w.bits.data, in.w.valid && in.w.ready)
  dmem.io.wmask := RegEnable(in.w.bits.strb, in.w.valid && in.w.ready)

  dmem.io.memRW     := 0.U
  dmem.io.memEnable := false.B

  in.r.bits.data := dmem.io.rdata

  in.b.valid    := false.B
  in.r.valid     := false.B
  in.r.bits.resp := 1.U
  in.b.bits     := 1.U

  // using a state machine would elegantly represent
  // the whole axi interface communicating process
  switch(state) {
    is(aIDLE) {
      // received write data and address concurrently
      when(in.aw.ready && in.aw.valid && in.w.valid && in.w.ready) {
        state := awriteDataAddr
      }.elsewhen(in.w.ready && in.w.valid) {
        state := awriteData
      }.elsewhen(in.aw.ready && in.aw.valid) {
        state := awriteAddr
      }
      // receive read address
      when(in.ar.ready && in.ar.valid) {
        state := aREAD
      }
    }
    // only received write addr
    is(awriteData) {
      when(in.aw.ready && in.aw.valid) {
        state := awriteDataAddr
      }
    }
    // only received write data
    is(awriteAddr) {
      when(in.w.ready && in.w.valid) {
        state := awriteDataAddr
      }
    }
    // 假设在 一周期内完成读写，然后在 ack 阶段阻塞数据
    // ready to write
    is(awriteDataAddr) {
      state := aWriteACK
    }
    // ready to read
    is(aREAD) {
      state := aReadACK
    }
    // finished write/read transaction
    is(aWriteACK) {
      // when (timer === 0.U) {
      dmem.io.memEnable  := true.B
      dmem.io.memRW      := 1.U
      in.b.valid := true.B
      in.b.bits  := 0.U
      when(in.b.ready && in.b.valid) {
        state := aIDLE
      }
      // }
    }
    is(aReadACK) {
      // when (timer === 0.U) {
      dmem.io.memRW         := 0.U
      dmem.io.memEnable     := true.B
      in.r.valid     := 1.U
      in.r.bits.resp := 0.U
      when(in.r.ready && in.r.valid) {
        state := aIDLE
      }
    }
    // }
  }
}

class MemIO(width: Int) extends Bundle {
  val raddr     = Input(UInt(width.W))
  val rdata     = Output(UInt(width.W))
  val wdata     = Input(UInt(width.W))
  val wmask     = Input(UInt(8.W))
  val memEnable = Input(Bool())
  val memRW     = Input(Bool())
  val waddr     = Input(UInt(width.W))
}

class Dmem(val width: Int) extends BlackBox with HasBlackBoxResource {
  val io = IO(new MemIO(width))
  addResource("/Dmem.sv")
}
