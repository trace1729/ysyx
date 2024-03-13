import circt.stage._

object Elaborate extends App {
  def top       = new AsyncBus() 
  val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => top))

  val chiselStageOptions = Seq(
    chisel3.stage.ChiselGeneratorAnnotation(() => top), 
    CIRCTTargetAnnotation(CIRCTTarget.SystemVerilog)
  )

  val firtool0ptions = Seq(
    FirtoolOption(
      "--lowering-options=disallowLocalVariables,disallowPackedArrays,locationInfoStyle=wrapInAtSquareBracket"
    ),
//     FirtoolOption("--split-verilog"), 
    FirtoolOption("-o=build/sv-gen"),
    FirtoolOption("--disable-all-randomization")
  )

  val executeOptions = chiselStageOptions ++ firtool0ptions 
  val executeArgs = Array("-td", "build")
  (new ChiselStage).execute(executeArgs, executeOptions)

  // (new ChiselStage).execute(args, generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
}
