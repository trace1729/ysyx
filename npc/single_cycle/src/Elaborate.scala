import circt.stage._

import cpu.top

object Elaborate extends App {
  // Argument is appended after --
  val seperator = args.indexWhere(_ == "--")
  assert(seperator != -1, "please pass img for program to executed!")
  def top       = new top(memoryFile = args(seperator + 1))
  val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => top))
  (new ChiselStage).execute(args, generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
}
