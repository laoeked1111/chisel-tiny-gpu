
package gpu

import chisel3._

// Create enum for ALU operations
object ALUOp extends ChiselEnum {
    val ADD, SUB, MUL, DIV = Value
}

class ALU extends Module {
    val io = IO(new Bundle {
        // if current block has less threads than block size, then some ALUs inactive
        val enable = Input(Bool())

        val coreState = Input(UInt(3.W))

        val decodedALUArithmeticMux = Input(ALUOp()) // if instruction is arithmetic
        val decodedALUOutputMux = Input(Bool()) // if instruction is comparison
        val rs = Input(UInt(8.W)) // 8-bit input operand
        val rt = Input(UInt(8.W)) // 8-bit input operand

        val ALUOut = Output(UInt(8.W)) // 8-bit output
    })

    val ALUOutReg = RegInit(0.U(8.W)) // default value

    when (reset.asBool) {
        ALUOutReg := 0.U
    } .elsewhen (io.enable) {
        // calculate ALUOut when core_state = EXECUTE 
        when (io.coreState === "b101".U){

            // do comparison
            when (io.decodedALUOutputMux === true.B){
                // set values to compare with NZP register in ALUOut(2, 0)
                ALUOutReg := 0.U(5.W) ## (io.rs - io.rt > 0.U).asUInt ## (io.rs - io.rt === 0.U).asUInt ## (io.rs - io.rt < 0.U).asUInt
            } 

            // do arithmetic 
            .otherwise {
                when (io.decodedALUArithmeticMux === ALUOp.ADD) {
                    ALUOutReg := io.rs + io.rt
                } .elsewhen (io.decodedALUArithmeticMux === ALUOp.SUB) {
                    ALUOutReg := io.rs - io.rt
                } .elsewhen (io.decodedALUArithmeticMux === ALUOp.MUL) {
                    ALUOutReg := io.rs * io.rt
                } .elsewhen (io.decodedALUArithmeticMux === ALUOp.DIV) {
                    ALUOutReg := io.rs / io.rt
                }
            }
        }
    }
    
    io.ALUOut := ALUOutReg
}

object ALUMain extends App {
  println("Generating ALU hardware")
  emitVerilog(new ALU(), Array("--target-dir", "generated"))
}