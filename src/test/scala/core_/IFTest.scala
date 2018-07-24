package core_

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class IFTest(t: IF) extends PeekPokeTester(t) {
  def reset_poke_init(): Unit = {
    reset(10) // press rst for more than a while pls
    poke(t.io.id.branch.valid, 0)
    poke(t.io.id.id_stall, 0)
    poke(t.io.ram.ok, 1)
  }

  // sequential if
  reset_poke_init()
  for (i <- 0 until 7) {
    val some_inst = i*0x11110000 + (i+1)*0x00001111
    poke(t.io.ram.rdata, some_inst)
    expect(t.io.id.pc, i*4)
    expect(t.io.ram.addr, i*4)
    expect(t.io.ram.mode, RAMMode.LW)
    expect(t.io.id.inst, some_inst)
    step(1)
  }

  // Stall
  reset_poke_init()
  poke(t.io.ram.ok, 0)
  step(1)
  expect(t.io.id.pc, 0)
  expect(t.io.ram.addr, 0)
  expect(t.io.ram.mode, RAMMode.LW)
  step(1)
  expect(t.io.id.pc, 0)
  poke(t.io.id.id_stall, 1)
  step(1)
  expect(t.io.id.pc, 0)
  poke(t.io.ram.ok, 1)
  step(1)
  expect(t.io.id.pc, 0)
  poke(t.io.id.id_stall, 0)
  step(1)
  expect(t.io.id.pc, 4)

  // Branch (inst is ignored)
  reset_poke_init()
  //   Only branch
  poke(t.io.id.branch.valid, 1)
  poke(t.io.id.branch.bits, 40)
  step(1)
  expect(t.io.id.pc, 40)
  //   Branch with stall: Do branch
  poke(t.io.id.branch.valid, 1)
  poke(t.io.id.branch.bits, 80)
  poke(t.io.ram.ok, 0)
  step(1)
  expect(t.io.id.pc, 80)
}


class IFTester extends ChiselFlatSpec {
    val args = Array[String]()
    "IF module" should "not tested now " in {
      iotesters.Driver.execute(args, () => new IF()) {
        c => new IFTest(c)
      } should be (true)
    }
}
