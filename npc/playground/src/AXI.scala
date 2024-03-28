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
  val in            = IO(Input(ExternalInput()))
  val axiMaster     = IO(Output(AxiLiteMaster(32, 32)))
  val sram          = Module(new SRAM)
  val mem_valid_reg = RegInit(1.U)

  axiMaster <> sram.in

  axiMaster.writeAddr.valid := mem_valid_reg
  axiMaster.writeData.valid := mem_valid_reg

  // external data is stored in these two registers
  // when axiMaster.valid and ready is both asserted,
  // these registers will update their value using external
  // address and data in the following rising edge
  // and since register's output is directly connected to
  // the sram, sram will receive the data once the register
  // updates its value.
  val mem_data_reg  = RegEnable(in.external_data, 0.U, axiMaster.writeAddr.valid & axiMaster.writeAddr.ready)
  val mem_addr_reg  = RegEnable(in.external_address, 0.U, axiMaster.writeAddr.valid & axiMaster.writeAddr.ready)
  val mem_wmask_reg = RegEnable(in.external_wmask, 0.U, axiMaster.writeAddr.valid & axiMaster.writeAddr.ready)

  axiMaster.writeData.bits.data := mem_data_reg
  axiMaster.writeAddr.bits.addr := mem_addr_reg

  when(in.external_valid) {
    mem_valid_reg := 1.U
  }
  // the ready is always follows the valid signal
  axiMaster.writeResp.ready := axiMaster.writeResp.valid
  when(axiMaster.writeResp.valid & axiMaster.writeResp.ready) {
    // transfer ended
    // Now the Lfu either do read or write, as such, when the we get the response,
    // we could just restore to idle state.
    // what does it means for idle state?
    mem_valid_reg := 0.U
  }

}

class SRAM extends Module {
  val in = IO(Output(AxiLiteSlave(32, 32)))

  in.writeAddr.ready := in.writeAddr.valid
  in.writeData.ready := in.writeData.valid

  // the data will be available on the next rising edge after valid and ready is both asserted,
  // How to tell the SRAM this feature?
  // using wmask to distinguish
  val ram = Wire(Vec(10, UInt(32.W)))
  ram.foreach {
    _ := 0.U
  }

  in.writeResp.valid := 0.U
  when(in.writeData.bits.strb =/= 0.U) {
    in.writeResp.valid          := 1.U
    ram(in.writeAddr.bits.addr) := in.writeData.bits.data
  }

}

class AxiTest extends Module {
  val in  = IO(Input(ExternalInput()))
  val mem = Module(new Mem)

  mem.in <> in

}
