package core_

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import OptCode._

class EXTest(ex: EX) extends PeekPokeTester(ex) {
  val ops = Array(
    (ADD, (a: Int, b: Int) => a + b),
    (SUB, (a: Int, b: Int) => a - b),
    (SLT, (a: Int, b: Int) => if (a < b) 1 else 0),
    (SLTU, (a: Int, b: Int) => if ((a + 0x80000000) < (b + 0x80000000)) 1 else 0),
    (XOR, (a: Int, b: Int) => a ^ b),
    (OR, (a: Int, b: Int) => a | b),
    (AND, (a: Int, b: Int) => a & b),
    (SLL, (a: Int, b: Int) => a << b),
    (SRL, (a: Int, b: Int) => a >>> b),
    (SRA, (a: Int, b: Int) => a >> b)
  )

  private def toUInt(x: Int): UInt =
    if (x >= 0) x.U else (x.toLong + 0x100000000L).U

  poke(ex.io.idExcep.en, false.B)
  poke(ex.io.csrExcepEn, false.B)

  for ((op, func) <- ops) {
    for (_ <- 0 until 10) {
      val rd1 = rnd.nextInt()
      val rd2 = rnd.nextInt()
      val res = func(rd1, rd2)
      poke(ex.io.id.oprd1, toUInt(rd1))
      poke(ex.io.id.oprd2, toUInt(rd2))
      poke(ex.io.id.opt, op)
      step(1)
      expect(ex.io.mem.alu_out, toUInt(res))
    }
  }

  // TODO: Test other IO
}

class EXTester extends ChiselFlatSpec {
  val args = Array[String]()
  "EX module" should "pass test" in {
    iotesters.Driver.execute(args, () => new EX()) {
      c => new EXTest(c)
    } should be(true)
  }
}
