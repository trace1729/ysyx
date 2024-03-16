import chisel3._
import chiseltest._
import chisel3.experimental.BundleLiterals._

import utest._

object CSRs {
  val mstatus = 0x300.U(12.W)
  val mtvec = 0x301.U(12.W)
  val mepc = 0x341.U(12.W)
  val mcause = 0x342.U(12.W)
}
/**
  * This is a trivial example of how to run this Specification
  * From within sbt use:
  * {{{
  * testOnly gcd.GcdDecoupledTester
  * }}}
  * From a terminal shell use:
  * {{{
  * sbt 'testOnly gcd.GcdDecoupledTester'
  * }}}
  */
object GCDSpec extends ChiselUtestTester {
  val tests = Tests {
    test("GCD") {
      testCircuit(new DecoupledGcd(16)) {
        dut =>
          dut.input.initSource()
          dut.input.setSourceClock(dut.clock)
          dut.output.initSink()
          dut.output.setSinkClock(dut.clock)
          val testValues = for {x <- 0 to 10; y <- 0 to 10} yield (x, y)
          val inputSeq = testValues.map { case (x, y) => (new GcdInputBundle(16)).Lit(_.value1 -> x.U, _.value2 -> y.U) }
          val resultSeq = testValues.map { case (x, y) =>
            (new GcdOutputBundle(16)).Lit(_.value1 -> x.U, _.value2 -> y.U, _.gcd -> BigInt(x).gcd(BigInt(y)).U)
          }
          fork {
            // push inputs into the calculator, stall for 11 cycles one third of the way
            val (seq1, seq2) = inputSeq.splitAt(resultSeq.length / 3)
            dut.input.enqueueSeq(seq1)
            dut.clock.step(11)
            dut.input.enqueueSeq(seq2)
          }.fork {
            // retrieve computations from the calculator, stall for 10 cycles one half of the way
            val (seq1, seq2) = resultSeq.splitAt(resultSeq.length / 2)
            dut.output.expectDequeueSeq(seq1)
            dut.clock.step(10)
            dut.output.expectDequeueSeq(seq2)
          }.join()
      }
    }
    test("bus") {
      testCircuit(new AsyncBus()) {
        dut =>
          // 才可以拿到数据
          dut.clock.step(2)
          dut.io.out.pc.expect(4.U)
          dut.io.out.inst.expect(4.U)
      }
    }
    test("CSR") {
      testCircuit(new CSR(10, 32)) {
        dut =>
          dut.io.csr_no.poke(CSRs.mstatus)
          dut.io.csr_value.expect(0x1800.U)
      }
    }
  }
}
