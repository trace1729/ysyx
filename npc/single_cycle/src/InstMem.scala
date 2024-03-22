package cpu
import chisel3._
import chisel3.util._
import cpu.config._
import chisel3.util.experimental.loadMemoryFromFileInline
import firrtl.annotations.MemoryLoadFileType

/* 
    width: 指令宽度
    memoryFile: 读取的数据文件路径
 */

object InstMem {
    val byte_len = 8
}

class InstMemIO (width: Int, ilen: Int) extends Bundle {
    val pc = Input(UInt(width.W))
    val inst = Output(Vec(ilen, UInt(InstMem.byte_len.W)))
}

class InstMem(val memoryFile: String = "") extends Module {
    val memSize = 8192 * 4 * 8
    val ilen = width / InstMem.byte_len

    val io = IO(new InstMemIO(width, ilen))
    val mem = Mem(memSize, UInt(InstMem.byte_len.W))

    // Initialize memory
    if (memoryFile.trim().nonEmpty) {
        loadMemoryFromFileInline(mem, memoryFile, MemoryLoadFileType.Binary)
    }

    io.inst := VecInit(
        mem.read(io.pc - config.startPC.U + 3.U),
        mem.read(io.pc - config.startPC.U + 2.U),
        mem.read(io.pc - config.startPC.U + 1.U),
        mem.read(io.pc - config.startPC.U + 0.U),
    )
}
