import circt.stage._

import cpu.ysyx
import cpu.Datapath

object Elaborate extends App {
  def cpu       = new Datapath()

  val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => cpu))
  val chiselStageOptions = Seq(
    chisel3.stage.ChiselGeneratorAnnotation(() => cpu), 
    CIRCTTargetAnnotation(CIRCTTarget.SystemVerilog)
  )

  val firtool0ptions = Seq(
    FirtoolOption(
      "--lowering-options=disallowLocalVariables,disallowPackedArrays,locationInfoStyle=wrapInAtSquareBracket"
    ),
    // FirtoolOption("--split-verilog"), 
    // FirtoolOption("-o=build/sv-gen"),
    FirtoolOption("--disable-all-randomization")
  )

  val executeOptions = chiselStageOptions ++ firtool0ptions 
  val executeArgs = Array("-td", "build")
  (new ChiselStage).execute(executeArgs, executeOptions)
}
