import chisel3._
import chisel3.util._
import bundles._

import Const._

class MEM extends Module {
  val io = IO(new Bundle {
    val ex  = Flipped(new EX_MEM()) 
    val mmu = new RAMOp()
    val reg = new MEM_Reg()

    val exWrRegOp = Input(new WrRegOp())
    val wrRegOp = Output(new WrRegOp())
  })

  val opt = RegInit(OptCode.ADD)
  opt := io.ex.opt
  val store_data = RegInit(0.U(32.W))
  store_data := io.ex.store_data
  val alu_out = RegInit(0.U(32.W))
  alu_out := io.ex.alu_out
  val wregAddr = RegInit(0.U(32.W))
  wregAddr := io.exWrRegOp.addr
  val wregData = RegInit(0.U(32.W))
  wregData := Mux(
    (io.ex.opt & OptCode.LW) === OptCode.LW, // must use io.opt here
    io.mmu.rdata,
    io.exWrRegOp.data
  )

  io.mmu.addr  := alu_out
  io.mmu.wdata := store_data
  io.mmu.mode  := Mux(
    opt(4).toBool,
    opt(3,0),
    0.U(4.W)
  )

  io.wrRegOp.addr := wregAddr
  io.wrRegOp.rdy  := true.B
  io.wrRegOp.data := wregData

  io.reg.addr := wregAddr
  io.reg.data := wregData
}
