import chisel3._
import chiseltest._
import chisel3.experimental.BundleLiterals._

import utest._
import scala.util.Random

object topSpec extends ChiselUtestTester {

  def int2uint(signed: Int, base:Int = 32) = if (signed < 0) (math.pow(2, base) + signed).toLong else signed
  val regfile_test_data = Seq.fill(32)(math.abs(Random.nextInt()))
  println(regfile_test_data)
  val tests = Tests {
    test("regfile") {
      testCircuit(new Regfile) {
        dut => 
          // 不可写
          dut.io.writeEn.poke(0.U)
          regfile_test_data.zipWithIndex.foreach {
            case (data, idx) => 
              dut.io.readreg1.poke(idx.U) 
              dut.io.readreg2.poke(idx.U) 
              dut.io.data.poke(data.U)
              dut.io.writereg.poke(idx.U)
              dut.clock.step(1)
              dut.io.rs1.expect(0.U)
              dut.io.rs2.expect(0.U)
          }
          // 可写
          dut.io.writeEn.poke(1.U)
          regfile_test_data.zipWithIndex.foreach {
            case (data, idx) => 
              dut.io.readreg1.poke(idx.U) 
              dut.io.readreg2.poke(idx.U) 
              dut.io.data.poke(data)
              dut.io.writereg.poke(idx.U)
              dut.clock.step(1)
              // x0 永远为 0
              if (idx == 0) {
                dut.io.rs1.expect(0.U)
                dut.io.rs2.expect(0.U)
              } else {
                dut.io.rs1.expect(data.U)
                dut.io.rs2.expect(data.U)
              }
          }
      }
      test("alu-add/sub") {
        testCircuit(new ALU(32)) {
          dut =>
            // 加法
            dut.io.alusel.poke(0.U)
            dut.io.A.poke(1.U)
            dut.io.B.poke(2.U)
            dut.io.res.expect(3.U)
            // 减法 
            dut.io.alusel.poke("b1100".asUInt)
            dut.io.A.poke(1.U)
            dut.io.B.poke(2.U)
            dut.io.res.expect(int2uint(-1).U)
            dut.io.A.poke(int2uint(-1))
            dut.io.B.poke(1)
            dut.io.res.expect(int2uint(-2))
            
        }
      }
      test("alu-shifter") {
        testCircuit(new ALU(32)) {
          dut =>
            // 左移
            dut.io.alusel.poke("b0001".asUInt)
            dut.io.A.poke(1.U)
            dut.io.B.poke(20.U)
            dut.io.res.expect((1 << 20).U)
            // 逻辑右移 
            dut.io.alusel.poke("b0101".asUInt)
            dut.io.A.poke(12121.U)
            dut.io.B.poke(5.U)
            dut.io.res.expect((12121 >>> 5).asUInt)
            // 逻辑右移 
            dut.io.alusel.poke("b0101".asUInt)
            dut.io.A.poke(int2uint(-1).U)
            dut.io.B.poke(5.U)
            dut.io.res.expect(int2uint(-1 >>> 5).asUInt)
            // 算术右移 
            dut.io.alusel.poke("b1101".asUInt)
            dut.io.A.poke(12121.U)
            dut.io.B.poke(5.U)
            dut.io.res.expect(int2uint(12121 >> 5).asUInt)
            // 算术右移 
            dut.io.alusel.poke("b1101".asUInt)
            dut.io.A.poke(int2uint(-1).U)
            dut.io.B.poke(5.U)
            dut.io.res.expect(int2uint(-1 >> 5).asUInt)
        }
      }
      test("cmp") {
        testCircuit(new ALU(32)) {
          dut =>
            // signed cmp A < B
            dut.io.alusel.poke("b0010".asUInt)
            dut.io.A.poke(int2uint(-12).asUInt)
            dut.io.B.poke(1)
            dut.io.res.expect(1.U)
            // signed cmp A > B
            dut.io.alusel.poke("b0010".asUInt)
            dut.io.A.poke(15)
            dut.io.B.poke(1)
            dut.io.res.expect(0.U)
            // 正数 >  0
            dut.io.alusel.poke("b0010".asUInt)
            dut.io.A.poke(65536)
            dut.io.B.poke(0)
            dut.io.res.expect(0.U)
            // 负数 <  0
            dut.io.alusel.poke("b0010".asUInt)
            dut.io.A.poke(int2uint(-1212))
            dut.io.B.poke(0)
            dut.io.res.expect(1.U)
            //  unsigned cmp A > B
            dut.io.alusel.poke("b0011".asUInt)
            dut.io.A.poke(int2uint(-1))
            dut.io.B.poke(123)
            dut.io.res.expect(0.U)
            //  unsigned cmp A < B
            dut.io.alusel.poke("b0011".asUInt)
            dut.io.A.poke(12)
            dut.io.B.poke(12212)
            dut.io.res.expect(1.U)
            //  unsigned cmp 所有正数都大于0
            dut.io.alusel.poke("b0011".asUInt)
            dut.io.A.poke(12121)
            dut.io.B.poke(0)
            dut.io.res.expect(0.U)
        }
      }
      test("immGen") {
        testCircuit(new ImmGen(32)) {
          dut =>
            dut.io.immsel.poke(0)
            dut.io.inst.poke("b00000000000100000000001010010011".asUInt)
            dut.io.imm.expect(1)
            dut.io.inst.poke("b00000010101000000000001010010011".asUInt)
            dut.io.imm.expect(42)
            dut.io.inst.poke("b00010000000000000000001010010011".asUInt)
            dut.io.imm.expect(256)
            dut.io.inst.poke("b01111111111100000000001010010011".asUInt)
            dut.io.imm.expect(2047)
            dut.io.inst.poke("b11111111111100000000001010010011".asUInt)
            dut.io.imm.expect(int2uint(-1))
        }
      }
    }
  }
}

