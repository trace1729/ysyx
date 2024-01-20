import chisel3._
import chisel3.util._
import config.Config._

class ImmGen(width: Int = 32) extends Module {

    def padding(len:Int): UInt = Cat(Seq.fill(len)(0.U(1.W)))

    val io = IO(new Bundle {
        val inst = Input(UInt(width.W))
        val immsel = Input(UInt(6.W))
        val imm = Output(UInt(width.W))
    })
    
    val sign_imm = Wire(SInt(width.W))

    // sign-extending
    sign_imm := MuxCase(0.S, Seq(
        // I
        (io.immsel === type_I) -> io.inst(31, 20).asSInt,
        // U
        (io.immsel === type_U) -> Cat(io.inst(31, 12), padding(12)).asSInt,
        // S
        (io.immsel === type_S) -> Cat(io.inst(31, 25), io.inst(11, 7)).asSInt,
        // J
        (io.immsel === type_J) -> Cat(io.inst(31), io.inst(19, 12), io.inst(20), io.inst(30, 21), padding(1)).asSInt,
    ))

    io.imm := sign_imm.asUInt
    
}