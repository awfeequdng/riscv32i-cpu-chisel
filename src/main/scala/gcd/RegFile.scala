package gcd;

import chisel3._

class RegFileIO extends Bundle {
  val raddr1 = Input(UInt(5.W))
  val raddr2 = Input(UInt(5.W))
  val rdata1 = Output(UInt(32.W))
  val rdata2 = Output(UInt(32.W))
  val waddr  = Input(UInt(5.W))   // set to 0 if no write is needed
  val wdata  = Input(UInt(32.W))
}

class RegFile extends Module {
  val io = IO(new RegFileIO)
  val regs = Mem(32, UInt(32.W))

  io.rdata1 := Mux(io.raddr1.orR, regs(io.raddr1), 0.U)
  io.rdata2 := Mux(io.raddr2.orR, regs(io.raddr2), 0.U)

  when (io.waddr.orR) {
    regs(io.waddr) := io.wdata;
  }
}
