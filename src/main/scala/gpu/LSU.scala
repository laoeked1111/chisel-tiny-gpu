
package gpu
import chisel3._

// Create enum for LSU operations
object LSUStateEnum extends ChiselEnum {
    val IDLE, REQUESTING, WAITING, DONE = Value
}

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
        val LSUState = Output(LSUStateEnum())
        val LSUOut = Output(UInt(8.W)) // 8-bit output
    })

    val LSUStateReg = RegInit(LSUStateEnum.IDLE)
    val LSUOutReg = RegInit(0.U(8.W))
    val memReadValidReg = RegInit(false.B)
    val memReadAddressReg = RegInit(0.U(8.W))
    val memWriteValidReg = RegInit(false.B)
    val memWriteAddressReg = RegInit(0.U(8.W))
    val memWriteDataReg = RegInit(0.U(8.W))

    when (reset.asBool) {
        LSUStateReg := LSUStateEnum.IDLE
        LSUOutReg := 0.U
        memReadValidReg := false.B
        memReadAddressReg := 0.U
        memWriteValidReg := false.B
        memWriteAddressReg := 0.U
        memWriteDataReg := 0.U
    } .elsewhen (io.enable) {
        // if memory read enable is triggered (LDR) instruction
        when (io.decodedMemReadEnable) {
            when (LSUStateReg === LSUStateEnum.IDLE) {
                // only read when coreState is REQUEST
                when (io.coreState === "b101".U) {
                    LSUStateReg := LSUStateEnum.REQUESTING
                } 
            } .elsewhen (LSUStateReg === LSUStateEnum.REQUESTING) {
                memReadValidReg := true.B
                memReadAddressReg := io.rs
                LSUStateReg := LSUStateEnum.WAITING
            } .elsewhen (LSUStateReg === LSUStateEnum.WAITING) {
                when (io.memReadReady) {
                    memReadValidReg := false.B
                    LSUOutReg := io.memReadData
                    LSUStateReg := LSUStateEnum.DONE
                }
            } .elsewhen (LSUStateReg === LSUStateEnum.DONE) {
                when (io.coreState === "b110".U){
                    // reset when coreState is UPDATE
                    LSUStateReg := LSUStateEnum.IDLE
                }
            }
        }
    }

    // if memory write enable is triggered (STR instruction)
    when (io.decodedMemWriteEnable) {
        when (LSUStateReg === LSUStateEnum.IDLE) {
            // only write when coreState is REQUEST
            when (io.coreState === "b101".U) {
                LSUStateReg := LSUStateEnum.REQUESTING
            }
        } .elsewhen (LSUStateReg === LSUStateEnum.REQUESTING) {
            memWriteValidReg := true.B
            memWriteAddressReg := io.rs
            memWriteDataReg := io.rt
            LSUStateReg := LSUStateEnum.WAITING
        } .elsewhen (LSUStateReg === LSUStateEnum.WAITING) {
            when (io.memWriteReady) {
                memWriteValidReg := false.B
                LSUStateReg := LSUStateEnum.DONE
            }
        } .elsewhen (LSUStateReg === LSUStateEnum.DONE) {
            when (io.coreState === "b110".U){
                // reset when coreState is UPDATE
                LSUStateReg := LSUStateEnum.IDLE
            }
        }
    }

    io.LSUState := LSUStateReg
    io.LSUOut := LSUOutReg
    io.memReadValid := memReadValidReg
    io.memReadAddress := memReadAddressReg
    io.memWriteValid := memWriteValidReg
    io.memWriteAddress := memWriteAddressReg
    io.memWriteData := memWriteDataReg
}

object LSUMain extends App {
  println("Generating LSU hardware")
  emitVerilog(new LSU(), Array("--target-dir", "generated"))
}

