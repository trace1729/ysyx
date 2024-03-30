import chisel3._
import chisel3.util._
import axi.AxiLiteMaster
import axi.AxiLiteSlave

class ExternalInput extends Bundle {
  val external_memRW   = Flipped(Bool())
  val external_valid   = Flipped(Bool())
  val external_data    = Flipped(UInt(32.W))
  val external_wmask   = Flipped(UInt(4.W))
  val external_address = Flipped(UInt(32.W))
}

object ExternalInput {
  def apply() = new ExternalInput
}

// =================================
/*
对于内存来说
写数据：
 数据来自于 Regfile
 地址来自于 Alures
 不过我在改成多周期的时候，把来自于 regfile 的数据全部
 传到了 Alu 阶段，然后和地址一起传递给 Mem 模块。
 所以 Mem 的 awvalid 和 wvalid 都可以依赖于 alu.valid_reg
 */

class Mem extends Module {
  val in            = IO(ExternalInput())
  val out           = IO(Output(UInt(32.W)))
  val axiController = Module(new AxiController)
  val sram          = Module(new SRAM)
  in <> axiController.in
  axiController.axi <> sram.in

  out := sram.out
}

object AxiState extends ChiselEnum {
  val aIDLE, aWRITE, aREAD, aACK = Value
}

class AxiController extends Module {
  val in  = IO(ExternalInput())
  val axi = IO(AxiLiteMaster(32, 32))

  // external data is stored in these two registers
  // when axiMaster.axi.valid and ready is both asserted,
  // these registers will update their value using external
  // address and data in the following rising edge
  // and since register's output is directly connected to
  // the sram, sram will receive the data once the register
  // updates its value.
  import AxiState._

  // initial is idle state
  val state   = RegInit(aIDLE)
  val dataWen = (state === aWRITE)
  val addrWen = (state === aWRITE)

  // axi.writeData.bits.data := Re
  // in one way or the other, you will going to learn how to build a finite state machine

  axi.writeAddr.valid := 0.U
  axi.writeData.valid := 0.U

  switch(state) {
    is(aIDLE) {
      when(in.external_valid) {
        state := Mux(in.external_memRW, aWRITE, aREAD)
      }
    }
    is(aWRITE) {
      axi.writeAddr.valid := 1.U
      axi.writeData.valid := 1.U
      when(axi.writeResp.valid && axi.writeResp.ready) {
        state := aIDLE
      }
    }

  }
  axi.writeResp.ready     := axi.writeResp.valid
  axi.writeData.bits.data := in.external_data
  axi.writeData.bits.strb := in.external_wmask
  axi.writeAddr.bits.addr := in.external_address

  // 逐渐领会到状态机的写法

}

// By using Value, you're telling Scala to automatically assign ordinal values to these members.
// By default, aIDLE will have the value 0, aWRITE will have the value 1,
//   aREAD will have the value 2, and aACK will have the value 3.
object SRAMState extends ChiselEnum {
  val aIDLE, awriteDataAddr, awriteData, awriteAddr, aACK = Value
}

class SRAM extends Module {
  val in  = IO(AxiLiteSlave(32, 32))
  val out = IO(Output(UInt(32.W)))

  in.writeAddr.ready := in.writeAddr.valid
  in.writeData.ready := in.writeData.valid

  import SRAMState._

  // the data and data address are indepentdent of each other,
  //   the axi controller pass the data to SRAM when valid and ready are both asserted
  //   the sram tries to write data into the rom
  //   then set the state to ack state

  val state = RegInit(aIDLE)

  val dataWen = (state === awriteDataAddr) || (state === awriteData)
  val addrWen = (state === awriteDataAddr) || (state === awriteAddr)
  val data    = RegEnable(in.writeData.bits.data, 0.U, dataWen)
  val addr    = RegEnable(in.writeAddr.bits.addr, 0.U, addrWen)

  // dummy detected
  val hit = data =/= 0.U

  in.writeResp.valid := false.B
  in.writeResp.bits  := 1.U

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
    // ready to write
    is(awriteDataAddr) {
      when(hit) {
        state := aACK
      }
    }
    // finished write transaction
    is(aACK) {
      in.writeResp.valid := true.B
      in.writeResp.bits  := 0.U
      state              := aIDLE
    }
  }
  out := data

}

class top extends Module {
  val in  = IO(ExternalInput())
  val out = IO(Output(Bool()))

  val mem = Module(new Mem)
  // using input port to drive the submodule input is just fine
  mem.in <> in
  out := mem.out
}
