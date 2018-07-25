package core_
import chisel3._

class MMU extends Module {
  val io = IO(new Bundle {
    val iff = Flipped(new RAMOp())
    val mem = Flipped(new RAMOp())

    val dev = new Core_IO
  })

  io.iff <> io.dev.if_
  io.mem <> io.dev.mem

  // TODO: Translate address
}
