package core_

import chisel3._
import chisel3.util.Valid

/*
  Interface from Core.MMU to IOManager

  # Conflict
  If both read & write for the same address occurs at the same cycle, write first.

  # Address Map
  Compatible with QEMU virt board device tree:
  https://www.sifive.com/blog/2017/12/20/risc-v-qemu-part-1-privileged-isa-hifive1-virtio/

  * [0x10000000, 0x10000008) 8B : UART
  * [0x10001000, 0x10002000) 1K : CGA  (Not std. Compatible with x86 CGA at 0xB8000.)
  * [0x80000000, 0x80400000) 4M : RAM1
  * [0x80400000, 0x80800000) 4M : RAM2
  * [0x80800000, 0x81000000) 8M : Flash

 */
class Core_IO extends Bundle {
  val if_ = new RAMOp()
  val mem = new RAMOp()
}

// repesents an operation of "writing registers", when data might not be ready
class WrRegOp extends Bundle {
  val addr = Output(UInt(5.W)) // if 0 then don't write
  val data = Output(UInt(32.W))
  val rdy  = Output(Bool())     // data might be ready in different stages e.g. EX and MEM
}

class WrCSROp extends Bundle {
  val addr = Output(UInt(12.W))
  val oldVal = Output(UInt(32.W))
  val rsVal = Output(UInt(32.W))
  val mode = Output(UInt(2.W)) // 0 if not write
  val newVal = Output(UInt(32.W))
}

// represents an operation of "ram access"
class RAMOp_Output extends Bundle {
  val addr  = Output(UInt(32.W))
  val mode  = Output(UInt(4.W))   // Consts.scalaRAMMode.XX
  val wdata = Output(UInt(32.W))
}

// Full IO interface
class RAMOp extends RAMOp_Output {
  val rdata = Input(UInt(32.W))
  val ok    = Input(Bool())
}

class IF_MMU extends RAMOp {
  val instPageFault = Input(Bool())
}

class MEM_MMU extends RAMOp {
  val loadPageFault = Input(Bool())
  val storePageFault = Input(Bool())
//  val invalidate_addr = Output(Valid(UInt(32.W)))
}

class CSR_MMU extends Bundle {
  val satp  = Output(UInt(32.W))
  val flush = Output(Valid(UInt(32.W)))
  val priv  = Output(UInt(2.W)) // current privilege mode
  val mxr   = Output(Bool())    // Make eXecutable Readable
  val sum   = Output(Bool())    // Supervisor User Memory access
}

// represents an operation of "reading registers"
class RdRegOp extends Bundle {
  val addr = Output(UInt(5.W))
  val data = Input(UInt(32.W))
}

class IF_ID extends Bundle {
  val pc     = Output(UInt(32.W))
  val inst   = Output(UInt(32.W))
  val branch = Input(Valid(UInt(32.W)))
  val excep  = new Exception
  val ready  = Input(Bool())
}

class ID_Reg extends Bundle {
  val read1 = new RdRegOp()
  val read2 = new RdRegOp()
}

class ID_EX_Output extends Bundle {
  val oprd1      = Output(UInt(32.W))
  val oprd2      = Output(UInt(32.W))
  val opt        = Output(UInt(5.W))
  val wrRegOp    = Output(new WrRegOp)
  val wrCSROp    = Output(new WrCSROp)
  var store_data = Output(UInt(32.W)) // for Store Inst only
  val excep      = new Exception
}

class ID_EX extends ID_EX_Output {
  var ready = Input(Bool())
}

class EX_MEM extends Bundle {
  val ramOp   = new RAMOp_Output
  val wrRegOp = Output(new WrRegOp)
  val wrCSROp = Output(new WrCSROp)
  var ready   = Input(Bool())
  val excep      = new Exception
}

class MEM_CSR extends Bundle {
  val wrCSROp = new WrCSROp
  val excep   = new Exception
}

class ID_CSR extends Bundle {
  val addr  = Output(UInt(12.W))
  val rdata = Input(UInt(32.W))
}

class Exception extends Bundle {
  val valid = Output(Bool())
  val code  = Output(UInt(32.W))
  val pc    = Output(UInt(32.W))
}
