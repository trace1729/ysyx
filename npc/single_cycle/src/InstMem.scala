import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFileInline
import config._
import firrtl.annotations.MemoryLoadFileType

/* 
    width: 指令宽度
    memoryFile: 读取的数据文件路径
 */
class InstMem(val width:Int = 32, val memoryFile: String = "") extends Module {
    val byte_len = 8
    val ilen = width / byte_len
    val memSize = 1024

    val io = IO(new Bundle{
        val pc = Input(UInt(width.W))
        val inst = Output(Vec(ilen, UInt(byte_len.W)))
    })

    val mem = Mem(memSize, UInt(byte_len.W))

    // Initialize memory
    if (memoryFile.trim().nonEmpty) {
        loadMemoryFromFileInline(mem, memoryFile, MemoryLoadFileType.Binary)
    }

    io.inst := VecInit(
        mem.read(io.pc - Config.base + 3.U),
        mem.read(io.pc - Config.base + 2.U),
        mem.read(io.pc - Config.base + 1.U),
        mem.read(io.pc - Config.base + 0.U),
    )
}
