import chisel3._
import chisel3.util._
import axi.AxiLiteMaster
import axi.AxiLiteSlave

class ExternalInput extends Bundle {
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
  val ended           = IO(Output(Bool()))
  val axiController = Module(new AxiController)
  val sram          = Module(new SRAM)
  in <> axiController.in
  axiController.axi <> sram.in

  out := sram.out
  ended := axiController.transferEnded
}

class AxiController extends Module {
  val in  = IO(ExternalInput())
  val axi = IO(AxiLiteMaster(32, 32))
  val transferEnded = IO(Output(Bool()))

  // external data is stored in these two registers
  // when axiMaster.axi.valid and ready is both asserted,
  // these registers will update their value using external
  // address and data in the following rising edge
  // and since register's output is directly connected to
  // the sram, sram will receive the data once the register
  // updates its value.
  val mem_data_reg       = RegEnable(in.external_data, 0.U, axi.writeAddr.valid & axi.writeAddr.ready)
  val mem_addr_reg       = RegEnable(in.external_address, 0.U, axi.writeAddr.valid & axi.writeAddr.ready)
  val mem_wmask_reg      = RegEnable(in.external_wmask, 0.U, axi.writeData.valid & axi.writeData.ready)
  val mem_valid_data_reg = RegInit(1.U)
  val mem_valid_addr_reg = RegInit(1.U)

  axi.writeData.bits.data := mem_data_reg
  axi.writeData.bits.strb := mem_wmask_reg

  axi.writeAddr.bits.addr := mem_addr_reg
  axi.writeData.valid     := mem_valid_data_reg
  axi.writeAddr.valid     := mem_valid_addr_reg

  // the ready is always follows the valid signal
  axi.writeResp.ready := axi.writeResp.valid

  when(in.external_valid) {
    mem_valid_data_reg := 1.U
  }.elsewhen(axi.writeData.valid & axi.writeData.ready) {
    // transfer ended
    // Now the Lfu either do read or write, as such, when the we get the response,
    // we could just restore to idle state.
    // what does it means for idle state?
    mem_valid_data_reg := 0.U
  }
  when(in.external_valid) {
    mem_valid_addr_reg := 1.U
  }.elsewhen(axi.writeAddr.valid & axi.writeAddr.ready) {
    // transfer ended
    // Now the Lfu either do read or write, as such, when the we get the response,
    // we could just restore to idle state.
    // what does it means for idle state?
    mem_valid_addr_reg := 0.U
  }
  transferEnded := RegEnable(1.U, axi.writeResp.valid && axi.writeResp.ready)
  // 逐渐领会到状态机的写法

}

// By using Value, you're telling Scala to automatically assign ordinal values to these members.
// By default, aIDLE will have the value 0, aWRITE will have the value 1, 
//   aREAD will have the value 2, and aACK will have the value 3.

object AxiState extends ChiselEnum {
  val aIDLE, aWRITE, aREAD, aACK = Value
}

class SRAM extends Module {
  val in  = IO(AxiLiteSlave(32, 32))
  val out = IO(Output(UInt(32.W)))

  in.writeAddr.ready := in.writeAddr.valid
  in.writeData.ready := in.writeData.valid
  
  import AxiState._


  // the data and data address are indepentdent of each other,
  //   the axi controller pass the data to SRAM when valid and ready are both asserted
  //   the sram tries to write data into the rom
  //   then set the state to ack state
  
  val state = RegInit(aIDLE)

  val wen = state === aWRITE
  val data = RegEnable(in.writeData.bits.data, 0.U, wen)
  val hit = data =/= 0.U

  in.writeResp.valid := false.B
  in.writeResp.bits := 1.U

  switch (state) {
    is (aIDLE) {
      // 用状态转换的话，不太好表示同时写数据和地址
      // or we just use one unified state to 
      // represent write
      when (in.writeAddr.ready && in.writeAddr.valid) {
        state := aWRITE
      }
    }
    is (aWRITE) {
      when (hit) {
        state := aACK
        in.writeResp.valid := true.B
        in.writeResp.bits := 0.U
      }
    }
    is (aACK) {
      state := aIDLE
    }
  }

  out := data


  // the data will be available on the next rising edge after valid and ready are both asserted,
  // How to tell the SRAM this feature?
  // using wmask to distinguish

  // It is hard to set the writeResponse signal after the data is stored on the sram
  // how to signal the write to sram is completed

  // in.writeResp.valid := RegEnable(1.U, in.writeData.ready && in.writeData.valid && in.writeAddr.valid && in.writeAddr.ready)
  // in.writeResp.bits  := RegEnable(0.U, in.writeData.valid && in.writeData.ready)
  // out                := RegEnable(in.writeData.bits.data, in.writeData.valid)

}

class top extends Module {
  val in  = IO(ExternalInput())
  val ended = IO(Output(Bool()))
  val out = IO(Output(Bool()))

  val mem = Module(new Mem)
  // using input port to drive the submodule input is just fine
  mem.in <> in
  ended := mem.ended

  out := mem.out
}
