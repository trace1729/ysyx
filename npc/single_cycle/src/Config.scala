package config

import chisel3._
import chisel3.util._

object Config {
  val inst_len = 4.U
  val base     = "h80000000".asUInt
  val type_I :: type_IE :: type_IL :: type_IJ :: type_IS :: type_U :: type_S :: type_J :: type_R :: type_B :: type_N :: Nil = Enum(10)
}