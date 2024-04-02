// referenced from https://github.com/nhynes/chisel3-axi.git
package cpu

import chisel3._
import chisel3.util._

class AxiLiteAddr(val addrWidth: Int) extends Bundle {
  val addr = UInt(addrWidth.W)
}

object AxiLiteAddr {
  def apply(addrWidth: Int) = new AxiLiteAddr(addrWidth)
}

class AxiLiteWriteData(val dataWidth: Int) extends Bundle {
  require(dataWidth == 32 || dataWidth == 64, "AxiLite `dataWidth` must be 32 or 64")
  val data = UInt(dataWidth.W)
  val strb = UInt((dataWidth / 8).W)
}

object AxiLiteWriteData {
  def apply(dataWidth: Int) = new AxiLiteWriteData(dataWidth)
}

class AxiLiteReadData(val dataWidth: Int) extends Bundle {
  require(dataWidth == 32 || dataWidth == 64, "AxiLite `dataWidth` must be 32 or 64")
  val data = UInt(dataWidth.W)
  val resp = UInt(2.W)
}

object AxiLiteReadData {
  def apply(dataWidth: Int) = new AxiLiteReadData(dataWidth)
}

class AxiLiteSlave(val addrWidth: Int, val dataWidth: Int) extends Bundle {
  val readAddr = Flipped(Decoupled(AxiLiteAddr(addrWidth)))
  val readData = Decoupled(AxiLiteReadData(dataWidth))

  val writeAddr = Flipped(Decoupled(AxiLiteAddr(addrWidth)))
  val writeData = Flipped(Decoupled(AxiLiteWriteData(dataWidth)))
  val writeResp = Decoupled(UInt(2.W))
}

object AxiLiteSlave {
  def apply(addrWidth: Int, dataWidth: Int) =
    new AxiLiteSlave(addrWidth = addrWidth, dataWidth = dataWidth)
}

class AxiLiteMaster(val addrWidth: Int, val dataWidth: Int) extends Bundle {
  val readAddr = Decoupled(AxiLiteAddr(addrWidth))
  val readData = Flipped(Decoupled(AxiLiteReadData(dataWidth)))

  val writeAddr = Decoupled(AxiLiteAddr(addrWidth))
  val writeData = Decoupled(AxiLiteWriteData(dataWidth))
  val writeResp = Flipped(Decoupled(UInt(2.W)))
}

object AxiLiteMaster {
  def apply(addrWidth: Int, dataWidth: Int) =
    new AxiLiteMaster(addrWidth = addrWidth, dataWidth = dataWidth)
}

object AxiState extends ChiselEnum {
  val aIDLE, aWRITE, aREAD, aACK = Value
}

class ExternalInput extends Bundle {
  val externalMemRW   = Flipped(Bool())
  val externalMemEn   = Flipped(Bool())
  val externalValid   = Flipped(Bool())
  val externalData    = Flipped(UInt(32.W))
  val externalWmask   = Flipped(UInt(4.W))
  val externalAddress = Flipped(UInt(32.W))
}

object ExternalInput {
  def apply() = new ExternalInput
}

object AxiController {
  def apply(addrWidth: Int, dataWidth: Int) = new AxiController(addrWidth, dataWidth)
}

class AxiController(addrWidth: Int, dataWidth: Int) extends Module {
  // 那这样的话就必须重构 AxiController 了
  // 控制器的输入应该是可以通用化的
  val in  = IO(ExternalInput())
  val axi = IO(AxiLiteMaster(addrWidth, dataWidth))

  // external data is stored in these two registers
  // when axiMaster.axi.valid and ready is both asserted,
  // these registers will update their value using external
  // address and data in the following rising edge
  // and since register's output is directly connected to
  // the sram, sram will receive the data once the register
  // updates its value.
  import AxiState._

  // initial is idle state
  val axiState   = RegInit(aIDLE)
  // in one way or the other, you will going to learn how to build a finite state machine

  axi.writeAddr.valid := 0.U
  axi.writeData.valid := 0.U
  axi.readAddr.valid := 0.U

  switch(axiState) {
    is(aIDLE) {
      when(in.externalValid && in.externalMemEn) {
        axiState := Mux(in.externalMemRW, aWRITE, aREAD)
      }
    }
    is (aREAD) {
      axi.readAddr.valid := 1.U
      when (axi.readAddr.valid && axi.readAddr.ready) {
        axiState := aACK
      }
    }
    is(aWRITE) {
      axi.writeAddr.valid := 1.U
      axi.writeData.valid := 1.U
      when (axi.writeData.valid && axi.writeData.ready) {
        axiState := aACK
      }
    }
    is (aACK) {
      when (axi.readData.valid && axi.readData.ready) {
        axiState := aIDLE
      }
      when (axi.writeResp.ready && axi.writeResp.valid) {
        axiState := aIDLE
      }
    }

  }
  axi.readData.ready := axi.readData.valid
  axi.writeResp.ready     := axi.writeResp.valid
  axi.writeData.bits.data := in.externalData
  axi.writeData.bits.strb := in.externalWmask
  axi.writeAddr.bits.addr := in.externalAddress
  axi.readAddr.bits.addr := in.externalAddress

  // 逐渐领会到状态机的写法

}
