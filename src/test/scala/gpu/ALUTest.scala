
package gpu

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ALUTest extends AnyFlatSpec with ChiselScalatestTester {
  
  // 10 + 2 = 12
  "ALU: 10 + 2" should "equal 12" in {
    test(new ALU) { dut =>
      dut.io.enable.poke(true.B)
      dut.io.coreState.poke("b101".U)
      dut.io.decodedALUArithmeticMux.poke(ALUOp.ADD)
      dut.io.decodedALUOutputMux.poke(false.B)
      dut.io.rs.poke(10.U)
      dut.io.rt.poke(2.U)
      dut.clock.step()

      dut.io.ALUOut.expect(12.U) 
    }
  }

  // 10 - 2 = 8
  "ALU: 10 - 2" should "equal 8" in {
    test(new ALU) { dut =>
      dut.io.enable.poke(true.B)
      dut.io.coreState.poke("b101".U)
      dut.io.decodedALUArithmeticMux.poke(ALUOp.SUB)
      dut.io.decodedALUOutputMux.poke(false.B)
      dut.io.rs.poke(10.U)
      dut.io.rt.poke(2.U)
      dut.clock.step()

      dut.io.ALUOut.expect(8.U)
    }
  }

  // 10 * 2 = 20
  "ALU: 10 * 2" should "equal 20" in {
    test(new ALU) { dut =>
      dut.io.enable.poke(true.B)
      dut.io.coreState.poke("b101".U)
      dut.io.decodedALUArithmeticMux.poke(ALUOp.MUL)
      dut.io.decodedALUOutputMux.poke(false.B)
      dut.io.rs.poke(10.U)
      dut.io.rt.poke(2.U)
      dut.clock.step()

      dut.io.ALUOut.expect(20.U)
    }
  }

  // 10 / 2 = 5 
  "ALU: 10 / 2" should "equal 5" in {
    test(new ALU) { dut =>
      dut.io.enable.poke(true.B)
      dut.io.coreState.poke("b101".U)
      dut.io.decodedALUArithmeticMux.poke(ALUOp.DIV)
      dut.io.decodedALUOutputMux.poke(false.B)
      dut.io.rs.poke(10.U)
      dut.io.rt.poke(2.U)
      dut.clock.step()

      dut.io.ALUOut.expect(5.U)
    }
  }

  // 10 > 2 => b00000100
  "ALU: 10 compared to 2" should "output b00000100" in {
    test(new ALU) { dut =>
      dut.io.enable.poke(true.B)
      dut.io.coreState.poke("b101".U)
      dut.io.decodedALUArithmeticMux.poke(ALUOp.ADD)
      dut.io.decodedALUOutputMux.poke(true.B)
      dut.io.rs.poke(10.U)
      dut.io.rt.poke(2.U)
      dut.clock.step()

      dut.io.ALUOut.expect("b00000100".U)
    }
  }

  // ALU not enabled
  "ALU not enabled" should "output 0" in {
    test(new ALU) { dut =>
      dut.io.enable.poke(false.B)
      dut.io.coreState.poke("b101".U)
      dut.io.decodedALUArithmeticMux.poke(ALUOp.ADD)
      dut.io.decodedALUOutputMux.poke(true.B)
      dut.io.rs.poke(10.U)
      dut.io.rt.poke(2.U)
      dut.clock.step()

      dut.io.ALUOut.expect(0.U)
    }
  }

  // core state is not EXECUTE
  "ALU core state not EXECUTE" should "output 0" in {
    test(new ALU) { dut =>
      dut.io.enable.poke(true.B)
      dut.io.coreState.poke("b100".U)
      dut.io.decodedALUArithmeticMux.poke(ALUOp.ADD)
      dut.io.decodedALUOutputMux.poke(true.B)
      dut.io.rs.poke(10.U)
      dut.io.rt.poke(2.U)
      dut.clock.step()

      dut.io.ALUOut.expect(0.U)
    }
  }

}
