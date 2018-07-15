import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}


class RegFileTest(rf: RegFile) extends PeekPokeTester(rf) {
  poke(rf.io._MEM.addr, 0)
  poke(rf.io._MEM.data, 9)
  step(1)
  poke(rf.io._ID.read1.addr, 0)
  poke(rf.io._ID.read2.addr, 0)
  expect(rf.io._ID.read1.data, 0)
  expect(rf.io._ID.read2.data, 0)

  poke(rf.io._MEM.addr, 1)
  poke(rf.io._MEM.data, 11111)
  step(1)
  step(1)
  poke(rf.io._ID.read1.addr, 1)
  expect(rf.io._ID.read1.data, 11111)

  poke(rf.io._MEM.addr, 2)
  poke(rf.io._MEM.data, 22222)
  step(1)
  step(1)
  poke(rf.io._ID.read2.addr, 2)
  expect(rf.io._ID.read2.data, 22222)

  poke(rf.io._MEM.addr, 3)
  poke(rf.io._MEM.data, 33333)
  step(1)
  poke(rf.io._ID.read1.addr, 3)
  poke(rf.io._ID.read2.addr, 1)
  expect(rf.io._ID.read1.data, 33333)
  expect(rf.io._ID.read2.data, 11111)

  poke(rf.io._MEM.addr, 31)
  poke(rf.io._MEM.data, 12345)
  step(1)
  poke(rf.io._ID.read1.addr, 31)
  expect(rf.io._ID.read1.data, 12345)
}


class RegFileTester extends ChiselFlatSpec {
    val args = Array[String]()
    "RegFile module" should "pass test" in {
      iotesters.Driver.execute(args, () => new RegFile()) {
        c => new RegFileTest(c)
      } should be (true)
    }
}
