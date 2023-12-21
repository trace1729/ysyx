import chisel3._
import chiseltest._
import chisel3.experimental.BundleLiterals._

import utest._

object topSpec extends ChiselUtestTester {
  val tests = Tests {
    test("top") {
      testCircuit(new top) {
        dut =>
      }
    }
  }
}

