
package gpu
import chisel3._

class LSU extends Module {
    val io = IO(new Bundle {
        // if current block has less threads than block size, then some LSU inactive
        val enable = Input(Bool())

        // state
        val coreState = Input(UInt(3.W))

        // memory control signal
        val decodedMemReadEnable = Input(Bool())
        val decodedMemWriteEnable = Input(Bool())

        // Registers
        val rs = Input(UInt(8.W)) 
        val rt = Input(UInt(8.W))

        // Data Memory
        val memReadValid = Output(Bool())
        val memReadAddress = Output(UInt(8.W))
        val memReadReady = Input(Bool())
        val memReadData = Input(UInt(8.W))

        val memWriteValid = Output(Bool())
        val memWriteAddress = Output(UInt(8.W))
        val memWriteData = Output(UInt(8.W))
        val memWriteReady = Input(Bool())

        // LSU Outputs
        val LSUState = Output(UInt(2.W))
        val LSUOut = Output(UInt(8.W)) // 8-bit output
    })

    // default values
    io.memReadValid := false.B
    io.memWriteValid := false.B
    io.memReadAddress := 0.U
    io.memWriteAddress := 0.U
    io.memWriteData := 0.U
    io.LSUOut := 0.U

    // status
    val IDLE = 0.U(2.W)         // 0b00
    val REQUESTING = 1.U(2.W)   // 0b01
    val WAITING = 2.U(2.W)      // 0b10
    val DONE = 3.U(2.W)         // 0b11

    val lsuStateReg = RegInit(IDLE);

    when (reset.asBool) {
        lsuStateReg := IDLE
        io.LSUOut := 0.U
        io.memReadValid := false.B
        io.memReadAddress := 0.U
        io.memWriteValid := false.B
        io.memWriteAddress := 0.U
        io.memWriteData := 0.U
    } .elsewhen (io.enable) {
        // if memory read enable is triggered (LDR) instruction
        when (io.decodedMemReadEnable) {
            when (lsuStateReg === IDLE) {
                // only read when coreState is REQUEST
                when (io.coreState === "b101".U) {
                    lsuStateReg := REQUESTING
                } 
            } .elsewhen (lsuStateReg === REQUESTING) {
                io.memReadValid := true.B
                io.memReadAddress := io.rs
                lsuStateReg := WAITING
            } .elsewhen (lsuStateReg === WAITING) {
                when (io.memReadReady) {
                    io.memReadValid := false.B
                    io.LSUOut := io.memReadData
                    lsuStateReg := DONE
                }
            } .elsewhen (lsuStateReg === DONE) {
                when (io.coreState === "b110".U){
                    // reset when coreState is UPDATE
                    lsuStateReg := IDLE
                }
            }
        }
    }

    // if memory write enable is triggered (STR instruction)
    when (io.decodedMemWriteEnable) {
        when (lsuStateReg === IDLE) {
            // only write when coreState is REQUEST
            when (io.coreState === "b101".U) {
                lsuStateReg := REQUESTING
            }
        } .elsewhen (lsuStateReg === REQUESTING) {
            io.memWriteValid := true.B
            io.memWriteAddress := io.rs
            io.memWriteData := io.rt
            lsuStateReg := WAITING
        } .elsewhen (lsuStateReg === WAITING) {
            when (io.memWriteReady) {
                io.memWriteValid := false.B
                lsuStateReg := DONE
            }
        } .elsewhen (lsuStateReg === DONE) {
            when (io.coreState === "b110".U){
                // reset when coreState is UPDATE
                lsuStateReg := IDLE
            }
        }
    }


    io.LSUState := lsuStateReg

}

object LSUMain extends App {
  println("Generating LSU hardware")
  emitVerilog(new LSU(), Array("--target-dir", "generated"))
}

