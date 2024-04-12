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

  val in   = IO(Flipped(AxiLiteMaster(width, width)))
  val dmem = Module(new Dmem(width))

  // ready follows valid

  in.writeAddr.ready := in.writeAddr.valid
  in.writeData.ready := in.writeData.valid
  in.readAddr.ready  := in.readAddr.valid

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
  dmem.io.raddr := RegEnable(in.readAddr.bits.addr, in.readAddr.valid && in.readAddr.ready)
  dmem.io.waddr := RegEnable(in.writeAddr.bits.addr, in.writeAddr.valid && in.writeAddr.ready)
  dmem.io.wdata := RegEnable(in.writeData.bits.data, in.writeData.valid && in.writeData.ready)
  dmem.io.wmask := RegEnable(in.writeData.bits.strb, in.writeData.valid && in.writeData.ready)

  dmem.io.memRW     := 0.U
  dmem.io.memEnable := false.B

  in.readData.bits.data := dmem.io.rdata

  in.writeResp.valid    := false.B
  in.readData.valid     := false.B
  in.readData.bits.resp := 1.U
  in.writeResp.bits     := 1.U

  // using a state machine would elegantly represent
  // the whole axi interface communicating process
  switch(state) {
    is(aIDLE) {
      // received write data and address concurrently
      when(in.writeAddr.ready && in.writeAddr.valid && in.writeData.valid && in.writeData.ready) {
        state := awriteDataAddr
      }.elsewhen(in.writeData.ready && in.writeData.valid) {
        state := awriteData
      }.elsewhen(in.writeAddr.ready && in.writeAddr.valid) {
        state := awriteAddr
      }
      // receive read address
      when(in.readAddr.ready && in.readAddr.valid) {
        state := aREAD
      }
    }
    // only received write addr
    is(awriteData) {
      when(in.writeAddr.ready && in.writeAddr.valid) {
        state := awriteDataAddr
      }
    }
    // only received write data
    is(awriteAddr) {
      when(in.writeData.ready && in.writeData.valid) {
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
      in.writeResp.valid := true.B
      in.writeResp.bits  := 0.U
      when(in.writeResp.ready && in.writeResp.valid) {
        state := aIDLE
      }
      // }
    }
    is(aReadACK) {
      // when (timer === 0.U) {
      dmem.io.memRW         := 0.U
      dmem.io.memEnable     := true.B
      in.readData.valid     := 1.U
      in.readData.bits.resp := 0.U
      when(in.readData.ready && in.readData.valid) {
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
