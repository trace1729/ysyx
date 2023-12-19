import chisel3._
import chiseltest._
import chisel3.experimental.BundleLiterals._

import utest._

object topSpec extends ChiselUtestTester {

  val tests = Tests {
    test("83-encoder") {
      testCircuit(new top) {
        dut =>
          dut.io.sw.poke(0.U)
          dut.io.ledr(0).expect(0.U)
          dut.io.ledr(1).expect(0.U)
          dut.io.ledr(2).expect(0.U)
          for (i <- 0 to 7) {
            println(p"i = $i\n")
            dut.io.sw.poke((1 << i).U)
            // 1, 2, 4, 8 ... 
            val expected_ledr = (i).U(3.W)
            dut.io.ledr(0).expect(expected_ledr(0))
            dut.io.ledr(1).expect(expected_ledr(1))
            dut.io.ledr(2).expect(expected_ledr(2))
          }
      }
    }
    test("38-decoder")  {
      testCircuit(new Seg) {
        dut =>
          dut.io.enable.poke(1.U)
          dut.io.seg_in.poke(0.U)
          dut.io.seg_out.expect((~0xfd & 0xff).U)
      }
    }
  }
}

