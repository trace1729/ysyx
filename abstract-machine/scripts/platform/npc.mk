AM_SRCS := riscv/npc/start.S \
           riscv/npc/trm.c \
           riscv/npc/ioe.c \
           riscv/npc/timer.c \
           riscv/npc/input.c \
           riscv/npc/cte.c \
           riscv/npc/trap.S \
           platform/dummy/vme.c \
           platform/dummy/mpe.c

CFLAGS    += -fdata-sections -ffunction-sections
LDFLAGS   += -T $(AM_HOME)/scripts/linker.ld \
						 --defsym=_pmem_start=0x80000000 --defsym=_entry_offset=0x0
LDFLAGS   += --gc-sections -e _start
NPCFLAGS += -b -l $(shell dirname $(IMAGE).elf)/npc-log.txt -d $(NEMU_HOME)/build/riscv32-nemu-interpreter-so
CFLAGS += -DMAINARGS=\"$(mainargs)\"
.PHONY: $(AM_HOME)/am/src/riscv/npc/trm.c

image: $(IMAGE).elf
	@$(OBJDUMP) -d $(IMAGE).elf > $(IMAGE).txt
	@echo + OBJCOPY "->" $(IMAGE_REL).bin
	@$(OBJCOPY) -S --set-section-flags .bss=alloc,contents -O binary $(IMAGE).elf $(IMAGE).bin
	@echo + MEMCOPY "->" $(IMAGE_REL).mem
	@xxd -b $(IMAGE).bin | cut -d ' ' -f 2-7 > $(IMAGE).mem

run: image
	$(MAKE) -C $(NPC_HOME) run ARGS="$(NPCFLAGS)" MEM=$(IMAGE).mem IMG=$(IMAGE).bin
	
gdb: image
	$(MAKE) -C $(NPC_HOME) gdb ARGS="$(NPCFLAGS)" MEM=$(IMAGE).mem IMG=$(IMAGE).bin
	

