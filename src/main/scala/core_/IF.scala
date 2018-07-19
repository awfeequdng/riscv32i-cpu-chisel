import chisel3._
import bundles._


class IF extends Module {
  val io = IO(new Bundle {
    val ram = new IFRAMOp()
    val id = new IF_ID()
  })

  // pc bookkeeping
  val pc  = RegInit(Const.PC_INIT)
  val npc = pc + 4.U
  pc := Mux(io.id.if_branch, io.id.branch_tar, npc) 
    // happens on ff

  // instruction fetch
  io.ram.addr  := pc; // while feeding current instruction to ID
                      // continue to fetch **NEXT** instruction
  io.ram.mode  := Const.MMU_MODE_LW
  io.ram.wdata := 0.U

  // feed to ID
  io.id.pc   := pc
  io.id.inst := io.ram.rdata 

  // TODO: branching
}
