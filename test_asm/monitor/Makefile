ARCH_PREF := riscv64-unknown-elf-
GCC := $(ARCH_PREF)gcc
LD := $(ARCH_PREF)ld
OBJDUMP := $(ARCH_PREF)objdump
OBJCOPY := $(ARCH_PREF)objcopy
MARCH := rv32i
CFLAGS := -mabi=ilp32 -DWITH_CSR -DWITH_INTERRUPT -DWITH_ECALL -O2
START_ADR := 0x80000000
VA_BASE := 0x80000000

all: disas.S monitor.bin

disas.S: out.o
	$(OBJDUMP) -S out.o > disas.S

monitor.bin: out.o
	$(OBJCOPY) -O binary out.o monitor.bin
	./align.sh monitor.bin

out.o: entry.o monitor.o
	$(LD) -melf32lriscv -nostdlib -N -e _start -Ttext $(START_ADR) entry.o monitor.o -o out.o

entry.o: entry.S
	$(GCC) -c -march=$(MARCH) $(CFLAGS) -DVA_BASE=$(VA_BASE) -fno-builtin -o entry.o entry.S

monitor.o: monitor.c inst.h arch.h
	$(GCC) -c -march=$(MARCH) $(CFLAGS) -DVA_BASE=$(VA_BASE) -fno-builtin -o monitor.o monitor.c

bootable.bin: ../bootloader/bootedcat.bin out.o
	cat ../bootloader/bootedcat.bin out.o > bootable.bin

clean:
	rm -rf *.o
	rm -rf monitor.bin
	rm -rf disas.S

.PHONY: all clean

