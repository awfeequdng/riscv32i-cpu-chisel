import chisel3._
import bundles._

class RegFile extends Module {
  val io = IO(new Bundle {
    val _ID  = Flipped(new ID_Reg())
    val _MEM = Flipped(new MEM_Reg())

    val log = Output(UInt(32.W))
  })

  val regs = Mem(32, UInt(32.W))

  regs(0.U) := 0.U

  // reads are not clocked
  io._ID.read1.data := regs(io._ID.read1.addr)
  io._ID.read2.data := regs(io._ID.read2.addr)

  val addr = Wire(UInt())
  addr := io._MEM.addr
  val data = Wire(UInt())
  data := io._MEM.data
  when (io._MEM.addr.orR) { // write gate happens here
    regs(addr) := data
  }

  printf("[RF] got waddr=%d\n", io._MEM.addr)
  printf("[RF] got wdata=%d\n", io._MEM.data)
  printf("[RF] reg(1)=%d\n", regs(1.U))

  io.log := regs(1.U)
}
