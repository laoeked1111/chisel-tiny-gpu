
package gpu

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class LSUTest extends AnyFlatSpec with ChiselScalatestTester {

  "LSU" should "reset correctly" in {
    test(new LSU) { dut =>
      dut.reset.poke(true.B)
      dut.clock.step(1)
      dut.reset.poke(false.B)
      dut.io.LSUState.expect(LSUStateEnum.IDLE)
      dut.io.LSUOut.expect(0.U)
      dut.io.memReadValid.expect(false.B)
      dut.io.memReadAddress.expect(0.U)
      dut.io.memWriteValid.expect(false.B)
      dut.io.memWriteAddress.expect(0.U)
      dut.io.memWriteData.expect(0.U)
    }
  }

  "LSU" should "load correctly" in {
    test(new LSU) { dut =>
      dut.reset.poke(false.B)
      dut.io.enable.poke(true.B)
      dut.io.decodedMemReadEnable.poke(true.B)
      dut.io.rs.poke(0x11.U) // source address
      dut.io.coreState.poke("b101".U) // REQUEST

      dut.clock.step()
      dut.io.LSUState.expect(LSUStateEnum.REQUESTING)
      dut.clock.step()
      dut.io.memReadReady.poke(true.B)
      dut.io.memReadData.poke(0x12.U)
      dut.clock.step()
      dut.io.LSUOut.expect(0x12.U)
      dut.io.LSUState.expect(LSUStateEnum.DONE)
    }
  }

  "LSU" should "store correctly" in {
    test(new LSU) { dut =>
      dut.reset.poke(false.B)
      dut.io.enable.poke(true.B)
      dut.io.decodedMemWriteEnable.poke(true.B)
      dut.io.rs.poke(0x34.U)
      dut.io.rt.poke(0xCD.U)
      dut.io.coreState.poke("b101".U) // REQUEST

      dut.clock.step()
      dut.io.LSUState.expect(LSUStateEnum.REQUESTING)
      dut.clock.step()
      dut.io.memWriteReady.poke(true.B)
      dut.clock.step()
      dut.io.LSUState.expect(LSUStateEnum.DONE)
      dut.io.memWriteValid.expect(false.B)
    }
  }

  "LSU" should "reset after DONE" in {
    test(new LSU) { dut =>
      // Complete an operation to reach DONE...
      dut.io.enable.poke(true.B)
      dut.io.decodedMemReadEnable.poke(true.B)
      dut.io.rs.poke(0x22.U)
      dut.io.coreState.poke("b101".U)
      dut.clock.step(1) // IDLE->REQUESTING
      dut.clock.step(1) // REQUESTING->WAITING
      dut.io.memReadReady.poke(true.B)
      dut.io.memReadData.poke(0x55.U)
      dut.clock.step(1) // WAITING->DONE
      dut.io.LSUState.expect(LSUStateEnum.DONE)

      // Provide UPDATE
      dut.io.coreState.poke("b110".U)
      dut.clock.step(1)
      dut.io.LSUState.expect(LSUStateEnum.IDLE)
    } 
  }

  "LSU" should "be inactive when enable low" in {
    test(new LSU) { dut =>
      dut.io.enable.poke(false.B)
      dut.io.decodedMemReadEnable.poke(true.B)
      dut.io.coreState.poke("b101".U)
      dut.clock.step(2)
      dut.io.LSUState.expect(LSUStateEnum.IDLE) // No transitions
      dut.io.memReadValid.expect(false.B)
      dut.io.memWriteValid.expect(false.B)
    }
  }

  "LSU" should "do nothing when everything is low" in {
    test(new LSU) { dut =>
      dut.io.enable.poke(false.B)
      dut.io.decodedMemReadEnable.poke(false.B)
      dut.io.decodedMemWriteEnable.poke(false.B)
      dut.clock.step(2)
      dut.io.LSUState.expect(LSUStateEnum.IDLE)
    }
  }

}