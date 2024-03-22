package cpu

import chisel3._
import chisel3.util._

object config {
  val instLen = 4
  val regsNum = 32
  val startPC: String  = "h80000000" 
  val width: Int = 32
  val type_I :: type_I_CSRR :: type_I_CSRW :: type_ECALL :: type_MRET :: type_IL :: type_IJ :: type_IS :: type_U :: type_S :: type_J :: type_R :: type_B :: type_N :: Nil = Enum(14)
}