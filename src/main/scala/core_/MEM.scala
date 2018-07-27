package core_

import chisel3._
import chisel3.util._

class MEM extends Module {
  val io = IO(new Bundle {
    val ex  = Flipped(new EX_MEM)
    val mmu = new MEM_MMU
    val csr = new MEM_CSR
   
    val wrRegOp   = new WrRegOp

    val flush = Input(Bool())
  })

  val excep = RegInit(0.U.asTypeOf(new Exception))
  excep := io.ex.excep
  io.csr.excep := excep

  // Lock input
  val ramOp       = RegInit(0.U.asTypeOf(new RAMOp_Output))
  val wregAddr    = RegInit(0.U(32.W))
  val exWrRegData = RegInit(0.U(32.W))

  // Stall
  val stall = ramOp.mode =/= RAMMode.NOP && !io.mmu.ok

  when(!stall) {
    ramOp := io.ex.ramOp
    wregAddr := io.ex.wrRegOp.addr
    exWrRegData := io.ex.wrRegOp.data
  }

  io.mmu.addr  := ramOp.addr
  io.mmu.wdata := ramOp.wdata
  io.mmu.mode  := ramOp.mode

  // Output
  io.wrRegOp.addr := Mux(stall, 0.U, wregAddr)
  io.wrRegOp.rdy  := true.B
  io.wrRegOp.data := Mux(RAMMode.isRead(ramOp.mode), io.mmu.rdata, exWrRegData)

  io.ex.ready := !stall

  //------------------- CSR ----------------------

  val wrCSROp = RegInit(0.U.asTypeOf(new WrCSROp))
  when(!stall) {
    wrCSROp := io.ex.wrCSROp
  }
  io.csr.wrCSROp := wrCSROp

  when(io.flush) {
    io.csr.excep.valid := false.B
    wregAddr := 0.U
    wrCSROp.mode := CSRMODE.NOP
    ramOp.mode := RAMMode.NOP
    //printf("[MEM] ! exception come, flushed (0x%x)\n", excepPc)
  }
  when(excep.valid) {
    io.wrRegOp.addr := 0.U
    io.csr.wrCSROp.mode := CSRMODE.NOP
    io.mmu.mode := RAMMode.NOP
    //printf("[MEM] ! Exception Pc: 0x%x Excep: %d\n", excepPc, excepEn)
  }

  //printf("[MEM] Pc: 0x%x (WrRegAddr) [%d <- %d]\n", excepPc, io.wrRegOp.addr, io.wrRegOp.data)
}
