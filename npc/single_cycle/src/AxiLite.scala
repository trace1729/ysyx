// referenced from https://github.com/nhynes/chisel3-axi.git
package cpu

import chisel3._
import chisel3.util._
import dataclass.data

class AxiLiteAddr(val addrWidth: Int) extends Bundle {
  val addr = UInt(addrWidth.W)
  val id = UInt(3.W)
  val len = UInt(8.W)
  val size = UInt(3.W)
  val burst = UInt(2.W)
}

object AxiLiteAddr {
  def apply(addrWidth: Int) = new AxiLiteAddr(addrWidth)
}

class AxiLiteWriteData(val dataWidth: Int) extends Bundle {
  require(dataWidth == 32 || dataWidth == 64, "AxiLite `dataWidth` must be 32 or 64")
  val data = UInt(dataWidth.W)
  val strb = UInt((dataWidth / 8).W)
  val last = Bool()
}

object AxiLiteWriteData {
  def apply(dataWidth: Int) = new AxiLiteWriteData(dataWidth)
}

class AxiLiteReadData(val dataWidth: Int) extends Bundle {
  require(dataWidth == 32 || dataWidth == 64, "AxiLite `dataWidth` must be 32 or 64")
  val data = UInt(dataWidth.W)
  val resp = UInt(2.W)
  val last = Bool()
  val id = UInt(4.W)
}

object AxiLiteReadData {
  def apply(dataWidth: Int) = new AxiLiteReadData(dataWidth)
}

class AxiLiteWriteResponse(val dataWidth: Int) extends Bundle {
  val resp = UInt(2.W)
  val id = UInt(4.W)
}

object AxiLiteWriteResponse {
  def apply(dataWidth: Int) = new AxiLiteWriteResponse(dataWidth)
}

class AxiLiteSlave(val addrWidth: Int, val dataWidth: Int) extends Bundle {
  val ar = Flipped(Decoupled(AxiLiteAddr(addrWidth)))
  val r = Decoupled(AxiLiteReadData(dataWidth))

  val aw = Flipped(Decoupled(AxiLiteAddr(addrWidth)))
  val w = Flipped(Decoupled(AxiLiteWriteData(dataWidth)))
  val b = Decoupled(AxiLiteWriteResponse(dataWidth))
}

object AxiLiteSlave {
  def apply(addrWidth: Int, dataWidth: Int) =
    new AxiLiteSlave(addrWidth = addrWidth, dataWidth = dataWidth)
}

class AxiLiteMaster(val addrWidth: Int, val dataWidth: Int) extends Bundle {
  val ar = Decoupled(AxiLiteAddr(addrWidth))
  val r = Flipped(Decoupled(AxiLiteReadData(dataWidth)))

  val aw = Decoupled(AxiLiteAddr(addrWidth))
  val w = Decoupled(AxiLiteWriteData(dataWidth))
  val b = Flipped(Decoupled(AxiLiteWriteResponse(dataWidth)))
}

object AxiLiteMaster {
  def apply(addrWidth: Int, dataWidth: Int) =
    new AxiLiteMaster(addrWidth = addrWidth, dataWidth = dataWidth)
}

object AxiState extends ChiselEnum {
  val aIDLE, aWRITE, aREAD, aACK = Value
}


object AxiController {
  def apply(addrWidth: Int, dataWidth: Int) = new AxiController(addrWidth, dataWidth)
}

class AxiController(addrWidth: Int, dataWidth: Int) extends Module {

  // 控制器的输入应该是可以通用化的
  val stageInput = IO(AxiLiteSlave(addrWidth, dataWidth))
  val axiOut = IO(AxiLiteMaster(addrWidth, dataWidth))

  stageInput <> axiOut
  

}
