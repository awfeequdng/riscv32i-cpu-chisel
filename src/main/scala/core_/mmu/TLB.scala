package core_.mmu

import chisel3._
import chisel3.util._

class TLBQuery extends Bundle {
  val req = Input(Valid(new PN))
  val rsp = Output(Valid(new PN))
}

class TLBModify extends Bundle {
  val mode = Input(UInt(2.W))
  val vpn = Input(new PN)
  val ppn = Input(new PN)    // Only used when insert
}

object TLBOp {
  val None   = 0.U(2.W)
  val Insert = 1.U(2.W)   // Insert (vpn, ppn)
  val Remove = 2.U(2.W)   // Remove vpn
  val Clear  = 3.U(2.W)   // Remove all
}

class TLB(val SIZE_LOG2: Int) extends Module {
  val io = IO(new Bundle {
    val query = new TLBQuery    // | Response at the same cycle
    val query2 = new TLBQuery   // | If reset = 1, rsp = req
    val modify = new TLBModify  // Do at rising edge
  })
  val SIZE = 1 << SIZE_LOG2

  class TLBEntry extends Bundle {
    val valid = Bool()
    val vpn = new PN
    val ppn = new PN
  }
  val entries = Mem(new TLBEntry, SIZE)

  // Debug
//  for(i <- 0 until SIZE)
//    printf("%x: %x 0x%x->0x%x\n", i.U, entries(i).valid, entries(i).vpn.asUInt, entries(i).ppn.asUInt)

  // Reset
  when(reset.toBool || io.modify.mode === TLBOp.Clear) {
    for(i <- 0 until SIZE) {
      entries(i).valid := false.B
    }
  }

  // Handle query
  def handleQuery(q: TLBQuery): Unit = {
    when(reset.toBool) {
      q.rsp := q.req
    }.otherwise {
      val id = q.req.bits.asUInt()(SIZE_LOG2-1, 0)
      val entry = entries(id)
      q.rsp.valid := q.req.valid && entry.valid && entry.vpn.asUInt === q.req.bits.asUInt
      q.rsp.bits := entry.ppn
    }
  }
  handleQuery(io.query)
  handleQuery(io.query2)

  // Handle modify, at rising edge

  // + insert
  val id = io.modify.vpn.asUInt()(SIZE_LOG2-1, 0)
  when(io.modify.mode === TLBOp.Insert) {
    // WARNING: Don't define `entry = entries(id)`
    //          then use `entry` as a l-value.
    //          It makes writing delay a cycle.
    entries(id).valid := true.B
    entries(id).vpn := io.modify.vpn
    entries(id).ppn := io.modify.ppn
  }

  // - remove
  when(io.modify.mode === TLBOp.Remove) {
    when(entries(id).vpn.asUInt === io.modify.vpn.asUInt) {
      entries(id).valid := false.B
    }
  }
}
