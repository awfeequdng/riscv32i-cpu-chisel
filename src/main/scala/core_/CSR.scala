package core_

import chisel3._
import chisel3.util._

class CSR extends Module {

  val io = IO(new Bundle {
    val id  = Flipped(new ID_CSR)
    val mem = Flipped(new MEM_CSR)
    val mmu = new CSR_MMU

    val flush = Output(Bool())  // Tell modules to clear registers at NEXT cycle
    val csrNewPc = Output(UInt(32.W))

    val external_inter = Input(Valid(UInt(32.W)))
  })

  val nextPrv = Wire(UInt(2.W))
  val prv     = RegNext(nextPrv, init=Priv.M)
  nextPrv := prv

  object ADDR {
    // M Info
    val mvendorid = "hF11".U
    val marchid   = "hF12".U
    val mimpid    = "hF13".U
    val mhartid   = "hF14".U
    //M Trap Setup
    val mstatus   = "h300".U
    val misa      = "h301".U
    val medeleg   = "h302".U
    val mideleg   = "h303".U
    val mie       = "h304".U
    val mtvec     = "h305".U
    val mcounteren= "h306".U
    //M Trap Hangding
    val mscratch  = "h340".U
    val mepc      = "h341".U
    val mcause    = "h342".U
    val mtval     = "h343".U
    val mip       = "h344".U
    //S Trap Setup
    val sstatus   = "h100".U
    val sedeleg   = "h102".U
    val sideleg   = "h103".U
    val sie       = "h104".U
    val stvec     = "h105".U
    val scounteren= "h106".U
    //S Trap Hangding
    val sscratch  = "h140".U
    val sepc      = "h141".U
    val scause    = "h142".U
    val stval     = "h143".U
    val sip       = "h144".U
    // S Protection and Translation
    val satp      = "h180".U
    // U Trap Setup
    val utvec     = "h005".U
    // U Trap Hangding
    val uscratch  = "h040".U
    val uepc      = "h041".U
    val ucause    = "h042".U
    val utval     = "h043".U
    val uip       = "h044".U
    // emmmm..
    val mtimecmp  = "h321".U
    val mtimecmph = "h322".U
  }

  val csr = Mem(0x400, UInt(32.W))

  // Don't use `for(i <- 0 until 0x400)` to iterate all CSRs.
  // It may generate many unused D-triggers, which makes compiling time unacceptable.
  // Just reflect all fields in ADDR here.
  val csr_ids = ADDR.getClass.getDeclaredFields.map(f => {
    f.setAccessible(true)
    f.get(ADDR).asInstanceOf[UInt]
  })

  when(reset.toBool) {
    for(i <- csr_ids) {
      csr(i) := 0.U
    }
  }

  class MStatus extends Bundle {
    val SD = Bool()
    val zero1 = UInt(8.W)
    val TSR = Bool()
    val TW = Bool()
    val TVM = Bool()
    val MXR = Bool()
    val SUM = Bool()
    val MPriv = Bool()
    val XS = UInt(2.W)
    val FS = UInt(2.W)
    val MPP = UInt(2.W)
    val old_HPP = UInt(2.W)
    val SPP = UInt(1.W)
    val MPIE = Bool()
    val old_HPIE = Bool()
    val SPIE = Bool()
    val UPIE = Bool()
    val MIE = Bool()
    val old_HIE = Bool()
    val SIE = Bool()
    val UIE = Bool()
  }

  val mstatus = RegInit(0.U.asTypeOf(new MStatus))

  // Read CSR from ID
  io.id.rdata := MuxLookup(io.id.addr, csr(io.id.addr), Seq(
    ADDR.mvendorid -> 2333.U(32.W),
    ADDR.marchid -> "h8fffffff".U(32.W),
    ADDR.mimpid -> 2333.U(32.W),
    ADDR.mhartid -> 0.U(32.W),
    ADDR.misa -> (1 << 30 | 1 << ('I' - 'A')).U(32.W),
    ADDR.mstatus -> mstatus.asUInt,
    ADDR.sstatus -> mstatus.asUInt,
    ADDR.sie -> csr(ADDR.mie),
    ADDR.sip -> csr(ADDR.mip)
  ))
  io.id.prv := prv

  // Write CSR from MEM
  when(io.mem.wrCSROp.valid) {
    for(i <- csr_ids) {
      when(i === io.mem.wrCSROp.addr) {
        csr(i) := io.mem.wrCSROp.data
      }
    }
    when(io.mem.wrCSROp.addr === ADDR.mstatus) {
      mstatus := io.mem.wrCSROp.data.asTypeOf(new MStatus)
    }
    when(io.mem.wrCSROp.addr === ADDR.sstatus) {
      mstatus := io.mem.wrCSROp.data.asTypeOf(new MStatus)
    }
    when(io.mem.wrCSROp.addr === ADDR.sie) {
      csr(ADDR.mie) := io.mem.wrCSROp.data
    }
    when(io.mem.wrCSROp.addr === ADDR.sip) {
      csr(ADDR.mip) := io.mem.wrCSROp.data
    }
  }

  // Alias
  val mepc = csr(ADDR.mepc)
  val sepc = csr(ADDR.sepc)
  val uepc = csr(ADDR.uepc)
  val mcause = csr(ADDR.mcause)
  val scause = csr(ADDR.scause)
  val ucause = csr(ADDR.ucause)
  val mtvec = csr(ADDR.mtvec)
  val stvec = csr(ADDR.stvec)
  val utvec = csr(ADDR.utvec)
  val mtval = csr(ADDR.mtval)
  val stval = csr(ADDR.stval)
  val utval = csr(ADDR.utval)
  val medeleg = csr(ADDR.medeleg)
  val mideleg = csr(ADDR.mideleg)
  val sedeleg = csr(ADDR.sedeleg)
  val sideleg = csr(ADDR.sideleg)
  val mie   = csr(ADDR.mie)
  val mip   = csr(ADDR.mip)
  val mtimecmp = Cat(csr(ADDR.mtimecmph), csr(ADDR.mtimecmp))

  val ie = MuxLookup(prv, false.B, Seq(
    Priv.M  -> mstatus.MIE,
    Priv.S  -> mstatus.SIE,
    Priv.U  -> mstatus.UIE
  ))

  // interrupt
  // io.external_inter.bits is the detail of external interrupt
  val ei = io.external_inter.valid

  // time_inter
  val mtime = RegInit(0.U(64.W))
  mtime := mtime + 1.U

  val time_inter = (mtime >= mtimecmp)
  /*
  when(time_inter && !io.external_inter.valid) {
    inter_code := (Cause.Interrupt << 31) | Cause.UTI | prv
    }
  */
  //val icl5 = inter_code(4,0)
  //val inter = (time_inter || io.external_inter.valid) && mie(icl5) // sie is a restricted views of mie


  csr(ADDR.mip):= Cat(
    0.U(20.W),
    (prv === Priv.M) && ei,
    0.U(1.W),
    (prv === Priv.S) && ei,
    (prv === Priv.U) && ei,

    (prv === Priv.M) && time_inter,
    0.U(1.W),
    (prv === Priv.S) && time_inter,
    (prv === Priv.U) && time_inter,
    mip(3,0)
  )

  val ipie = mip & mie
  val ipie_m = ipie & ~mideleg
  val ipie_s = ipie & mideleg
  // interrupt code
  val ic = PriorityMux(Seq(
    (ipie_m(11), 11.U),
    (ipie_m( 9),  9.U),
    (ipie_m( 8),  8.U),

    (ipie_m( 7),  7.U),
    (ipie_m( 5),  5.U),
    (ipie_m( 4),  4.U),

    (ipie_m( 3),  3.U),
    (ipie_m( 1),  1.U),
    (ipie_m( 0),  0.U),
    
    (ipie_s(11), 11.U),
    (ipie_s( 9),  9.U),
    (ipie_s( 8),  8.U),

    (ipie_s( 7),  7.U),
    (ipie_s( 5),  5.U),
    (ipie_s( 4),  4.U),

    (ipie_s( 3),  3.U),
    (ipie_s( 1),  1.U),
    (ipie_s( 0),  0.U),

    (true.B  ,  0.U)
  ))

  val inter_new_mode = Mux( mideleg(ic), Priv.S, Priv.M)
  val inter_enable = ((inter_new_mode > prv) || ((inter_new_mode === prv) && ie))

//  printf("mode: %d, prv: %d, ie: %d \n", inter_new_mode, prv, ie);
  printf(" ~ 0x%x >=  0x%x : %d\n", mtime, mtimecmp, time_inter)
  printf(" ~ mip : 0x%x \n", mip);
  printf(" ~ inter:%d , code:%d\n", inter_enable, ic);
  io.mem.inter.valid := inter_enable && ipie.orR
  io.mem.inter.bits  := (Cause.Interrupt << 31)|  ic

  val epc = io.mem.excep.pc // NOTE: no +4, do by trap handler if necessary
  val have_excep = io.mem.excep.valid && io.mem.excep.valid_inst
  val cause = io.mem.excep.code

  io.flush := have_excep
  io.csrNewPc := 0.U

  // Handle exception from MEM at the same cycle
  when(have_excep) {
    // xRet
    when(Cause.isRet(cause)) {
      val x = Cause.retX(cause)
      // prv <- xPP
      // xIE <- xPIE
      // xPIE <- 1
      // xPP <- U
      nextPrv := MuxLookup(x, 0.U, Seq(
        Priv.M  -> mstatus.MPP,
        Priv.S  -> mstatus.SPP,
        Priv.U  -> Priv.U
      ))
      switch(x) {
        is(Priv.M) {
          mstatus.MIE := mstatus.MPIE
          mstatus.MPIE := 1.U
          mstatus.MPP := Priv.U
        }
        is(Priv.S) {
          mstatus.SIE := mstatus.SPIE
          mstatus.SPIE := 1.U
          mstatus.SPP := 0.U
        }
        is(Priv.U) {
          mstatus.UIE := mstatus.MPIE
          mstatus.UPIE := 1.U
        }
      }
      io.csrNewPc := MuxLookup(x, 0.U , Seq(
        Priv.M -> mepc,
        Priv.S -> sepc,
        Priv.U -> uepc
      ))
    }.elsewhen(cause.asUInt === Cause.SFenceOne || cause.asUInt === Cause.SFenceAll) {
      io.csrNewPc := io.mem.excep.pc + 4.U
    }.otherwise { // Exception or Interrupt
      val epc = io.mem.excep.pc // NOTE: no +4, do by trap handler if necessary
      val tval = io.mem.excep.value
      nextPrv := PriorityMux(Seq(
        (!cause(31) && !medeleg(cause(4,0)), Priv.M),
        ( cause(31) && !mideleg(cause(4,0)), Priv.M),
        (!cause(31) && !sedeleg(cause(4,0)), Priv.S),
        ( cause(31) && !sideleg(cause(4,0)), Priv.S),
        (true.B,                             Priv.U)
      ))
      switch(nextPrv) {
        is(Priv.M) {
          mstatus.MPIE := mstatus.MIE
          mstatus.MIE  := 0.U
          mstatus.MPP := prv
          mepc := epc
          mcause := cause
          mtval := tval
        }
        is(Priv.S) {
          mstatus.SPIE := mstatus.SIE
          mstatus.SIE  := 0.U
          mstatus.SPP := (prv === Priv.S)
          sepc := epc
          scause := cause
          stval := tval
        }
        is(Priv.U) {
          mstatus.UPIE := mstatus.UIE
          mstatus.UIE  := 0.U
          uepc := epc
          ucause := cause
          utval := tval
        }
      }
      val xtvec = MuxLookup(nextPrv, 0.U, Seq(
        Priv.M -> mtvec,
        Priv.S -> stvec,
        Priv.U -> utvec
        ))
      val xcause = MuxLookup(nextPrv, 0.U, Seq(
        Priv.M -> mcause,
        Priv.S -> scause,
        Priv.U -> ucause
        ))

      val pcA4 = Cat(xtvec(31,2), 0.U(2.W))
      io.csrNewPc := Mux(xtvec(1,0) === 0.U,
        pcA4,
        pcA4 + 4.U * cause
      )
    }
  }

  //------------------- MMU ----------------------
  // MMU may lock it at next rising edge
  io.mmu.satp := csr(ADDR.satp)
  io.mmu.sum := mstatus.SUM
  io.mmu.mxr := mstatus.MXR
  io.mmu.flush.one := io.mem.excep.valid && io.mem.excep.code === Cause.SFenceOne
  io.mmu.flush.all := io.mem.excep.valid && io.mem.excep.code === Cause.SFenceAll
  io.mmu.flush.addr := io.mem.excep.value
  io.mmu.priv := nextPrv
}


