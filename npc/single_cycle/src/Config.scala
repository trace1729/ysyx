package config

import chisel3._
import chisel3.util._

object Config {
  val inst_len = 4.U
  val base     = "h80000000".asUInt
}