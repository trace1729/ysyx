package cpu

import chisel3._
import chisel3.util._
import cpu.config._
import cpu.utils._

// val lsfr = Module(new LSFR(4))
// 如何拿到 lsfr 的数据，让他在 sram 读取期间保存这个延迟不改变呢

// By using Value, you're telling Scala to automatically assign ordinal values to these members.
// By default, aIDLE will have the value 0, aWRITE will have the value 1,
//   aREAD will have the value 2, and aACK will have the value 3.

object SRAMState extends ChiselEnum {
  val aIDLE, awriteDataAddr, awriteData, awriteAddr, aREAD, aWriteACK, aReadACK, aUART, aRTC, aUARTACK, aRTCACK = Value
}

class SRAM extends Module {
  val ifuEnable = IO(Input(Bool()))
  val ifuIn     = IO(Flipped(AxiLiteMaster(width, width)))
  val lsuIn     = IO(Flipped(AxiLiteMaster(width, width)))
  val dmem      = Module(new Dmem(width))

  // ready follows valid

  when(ifuEnable) {
    ifuIn.writeAddr.ready := ifuIn.writeAddr.valid
    ifuIn.writeData.ready := ifuIn.writeData.valid
    ifuIn.readAddr.ready  := ifuIn.readAddr.valid
    lsuIn                 := DontCare
  }.otherwise {
    lsuIn.writeAddr.ready := lsuIn.writeAddr.valid
    lsuIn.writeData.ready := lsuIn.writeData.valid
    lsuIn.readAddr.ready  := lsuIn.readAddr.valid
    ifuIn                 := DontCare
  }

  import SRAMState._

  // the data and data address are indepentdent of each other
  //   the axi controller pass the data to SRAM when valid and ready are both asserted
  //   the sram tries to write data into the rom
  //   then set the state to ack state

  val state = RegInit(aIDLE)
  val mtime = RegInit(UInt(64.W), 0.U)
  mtime := mtime + 1.U
  val rtcEnable = state === aRTC
  val rtcData = MuxCase(
    0.U,
    Seq(
      (lsuIn.readAddr.bits.addr === config.RTC_MNIO.U) -> mtime(31, 0),
      (lsuIn.readAddr.bits.addr === (config.RTC_MNIO + 4).U) -> mtime(63, 32)
    )
  )

  // 直接设置一个计数器，来模拟延迟，每一条指令的执行周期都不一样，这样也算模拟了随机延迟了。
  // TODO 为 valid / ready 信号设置一个随机延迟
  // val timer = RegInit(0.U(32.W))
  // timer := Mux(timer === 10.U, 0.U, timer + 1.U)

  dmem.io.raddr := Mux(ifuEnable, ifuIn.readAddr.bits.addr, lsuIn.readAddr.bits.addr)
  dmem.io.waddr := Mux(ifuEnable, ifuIn.writeAddr.bits.addr, lsuIn.writeAddr.bits.addr)
  dmem.io.wdata := Mux(ifuEnable, ifuIn.writeData.bits.data, lsuIn.writeData.bits.data)
  dmem.io.wmask := Mux(ifuEnable, ifuIn.writeData.bits.strb, lsuIn.writeData.bits.strb)

  dmem.io.memRW     := 0.U
  dmem.io.memEnable := false.B

  ifuIn.readData.bits.data := dmem.io.rdata
  lsuIn.readData.bits.data := dmem.io.rdata

  when(ifuEnable) {
    ifuIn.writeResp.valid    := false.B
    ifuIn.readData.valid     := false.B
    ifuIn.readData.bits.resp := 1.U
    ifuIn.writeResp.bits     := 1.U
    lsuIn                    := DontCare
  }.otherwise {
    lsuIn.writeResp.valid    := false.B
    lsuIn.readData.valid     := false.B
    lsuIn.readData.bits.resp := 1.U
    lsuIn.writeResp.bits     := 1.U
    ifuIn                    := DontCare
  }

  // using a state machine would elegantly represent
  // the whole axi interface communicating process
  switch(state) {
    is(aIDLE) {
      // received write data and address concurrently
      when(ifuEnable) {
        when(ifuIn.readAddr.ready && ifuIn.readAddr.valid) {
          state := aREAD
        }
      }.otherwise {
        when(lsuIn.writeAddr.ready && lsuIn.writeAddr.valid && lsuIn.writeData.valid && lsuIn.writeData.ready) {
          state := Mux(lsuIn.writeAddr.bits.addr === config.SERAL_MNIO.U, aUART, awriteDataAddr)
        }.elsewhen(lsuIn.writeData.ready && lsuIn.writeData.valid) {
          state := awriteData
        }.elsewhen(lsuIn.writeAddr.ready && lsuIn.writeAddr.valid) {
          state := awriteAddr
        }
        when(lsuIn.readAddr.ready && lsuIn.readAddr.valid) {
          state := Mux(
            (lsuIn.readAddr.bits.addr === config.RTC_MNIO.U) || (lsuIn.readAddr.bits.addr === (config.RTC_MNIO + 4).U),
            aRTC,
            aREAD
          )
        }
      }
    }
    // only received write addr
    is(awriteData) {
      when(lsuIn.writeAddr.ready && lsuIn.writeAddr.valid) {
        state := awriteDataAddr
      }
    }
    // only received write data
    is(awriteAddr) {
      when(lsuIn.writeData.ready && lsuIn.writeData.valid) {
        state := awriteDataAddr
      }
    }
    // 假设在 一周期内完成读写，然后在 ack 阶段阻塞数据
    // ready to write
    is(awriteDataAddr) {
      state := aWriteACK
    }
    // ready to read
    is(aREAD) {
      state := aReadACK
    }
    is(aUART) {
      printf("%c", lsuIn.writeData.bits.data(7, 0))
      state := aUARTACK
    }
    is(aRTC) {
      dmem.io.memRW := 0.U
      state         := aRTCACK
    }
    is(aUARTACK) {
      lsuIn.writeResp.valid := true.B
      lsuIn.writeResp.bits  := 0.U
      when(lsuIn.writeResp.ready && lsuIn.writeResp.valid) {
        state := aIDLE
      }
    }
    is(aRTCACK) {
      lsuIn.readData.valid     := 1.U
      lsuIn.readData.bits.resp := 0.U
      when(lsuIn.readData.ready && lsuIn.readData.valid) {
        state := aIDLE
      }
    }
    // finished write/read transaction
    is(aWriteACK) {
      // when (timer === 0.U) {
      dmem.io.memEnable     := true.B
      dmem.io.memRW         := 1.U
      lsuIn.writeResp.valid := true.B
      lsuIn.writeResp.bits  := 0.U
      when(lsuIn.writeResp.ready && lsuIn.writeResp.valid) {
        state := aIDLE
      }
      // }
    }
    is(aReadACK) {
      // when (timer === 0.U) {
      dmem.io.memRW     := 0.U
      dmem.io.memEnable := true.B
      when(ifuEnable) {
        ifuIn.readData.valid     := 1.U
        ifuIn.readData.bits.resp := 0.U
        when(ifuIn.readData.ready && ifuIn.readData.valid) {
          state := aIDLE
        }
      }.otherwise {
        lsuIn.readData.valid     := 1.U
        lsuIn.readData.bits.resp := 0.U
        when(lsuIn.readData.ready && lsuIn.readData.valid) {
          state := aIDLE
        }
      }
      // }
    }
  }
}

class MemIO(width: Int) extends Bundle {
  val raddr     = Input(UInt(width.W))
  val rdata     = Output(UInt(width.W))
  val wdata     = Input(UInt(width.W))
  val wmask     = Input(UInt(8.W))
  val memEnable = Input(Bool())
  val memRW     = Input(Bool())
  val waddr     = Input(UInt(width.W))
}

class Dmem(val width: Int) extends BlackBox with HasBlackBoxResource {
  val io = IO(new MemIO(width))
  addResource("/Dmem.sv")
}
