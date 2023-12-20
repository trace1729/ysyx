import chisel3._
import chiseltest._
import chisel3.experimental.BundleLiterals._

import utest._

object topSpec extends ChiselUtestTester {
  val tests = Tests {
    test("add-sub") {
      testCircuit(new top) {
        dut => 
          dut.io.sw.poke("b00000010001".asUInt)
          dut.io.res.expect(2.S)
          dut.io.overflow.expect(0.U)
          dut.io.zero.expect(0.U)
          dut.io.carry.expect(0.U)
          dut.io.sw.poke("b00010001000".asUInt)
          dut.io.res.expect(0.S)
          dut.io.overflow.expect(1.U)
          dut.io.zero.expect(1.U)
          dut.io.carry.expect(1.U)
          dut.io.sw.poke("b00000000000".asUInt)
          dut.io.res.expect(0.S)
          dut.io.zero.expect(1.U)
          dut.io.sw.poke("b00100000000".asUInt)
          dut.io.res.expect(0.S)
          dut.io.zero.expect(1.U)
      }
    }
    test("logical") {
      testCircuit(new top) {
        dut =>
          dut.io.sw.poke("b010 0001 0000".replace(" ", "").asUInt)
          dut.io.res.expect(-2.S)
          dut.io.sw.poke("b011 1001 0001".replace(" ", "").asUInt)
          dut.io.res.expect(-7.S)
          dut.io.sw.poke("b100 1001 0001".replace(" ", "").asUInt)
          dut.io.res.expect(1.S)
          dut.io.sw.poke("b101 1001 0001".replace(" ", "").asUInt)
          dut.io.res.expect(-8.S)
      }
    } 
    test("cmp") {
      testCircuit(new top) {
        dut =>
          dut.io.sw.poke("b110 0001 1000".replace(" ", "").asUInt)
          dut.io.res.expect(0.S)
          dut.io.sw.poke("b110 0001 0111".replace(" ", "").asUInt)
          dut.io.overflow.expect(0.U)
          dut.io.res.expect(1.S)
          dut.io.sw.poke("b11100000000".asUInt)
          dut.io.zero.expect(1.U)
          dut.io.res.expect(1.S)
          dut.io.sw.poke("b111 0000 1000".replace(" ", "").asUInt)
          dut.io.res.expect(0.S)
      }
    }
  }
}

