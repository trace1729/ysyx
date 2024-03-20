package cpu

import chisel3._
import chisel3.util._
import cpu.config._

class IFU extends Bundle {
    val pc   = Output(UInt(config.width.W))
    val inst = Output(UInt(config.width.W))
}

class IDU extends Bundle {
}