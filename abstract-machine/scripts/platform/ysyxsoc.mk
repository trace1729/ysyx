AM_SRCS := riscv/ysyxsoc/start.S \
           riscv/ysyxsoc/trm.c

CFLAGS    += -fdata-sections -ffunction-sections

# TODO need to modify
LDFLAGS   += -T $(AM_HOME)/scripts/ysyxsoclinker.ld

# enabling garbage collectin, 
# defining `_start` as start symbols
LDFLAGS   += --gc-sections -e _start 

# below is for cpp test framework
NPCFLAGS += -b -l $(shell dirname $(IMAGE).elf)/npc-log.txt -e $(IMAGE).elf -d $(NEMU_HOME)/build/riscv32-nemu-interpreter-so 

CFLAGS += -DMAINARGS=\"$(mainargs)\"
.PHONY: $(AM_HOME)/am/src/riscv/ysyxsoc/trm.c

image: $(IMAGE).elf
	@echo $(CFLAGS)
	@$(OBJDUMP) -d $(IMAGE).elf > $(IMAGE).txt
	@echo + OBJCOPY "->" $(IMAGE_REL).bin
	@$(OBJCOPY) -S --set-section-flags .bss=alloc,contents -O binary $(IMAGE).elf $(IMAGE).bin
	#@echo + MEMCOPY "->" $(IMAGE_REL).mem
	#@$(OBJCOPY) -O binary -j .text $(IMAGE).elf $(IMAGE).bin1
	#@xxd -b $(IMAGE).bin1 | cut -d ' ' -f 2-7 > $(IMAGE).mem
	#@rm $(IMAGE).bin1

run: image
	$(MAKE) -C $(NPC_HOME) run ARGS="$(NPCFLAGS)" MEM=$(IMAGE).mem IMG=$(IMAGE).bin
	
gdb: image
	$(MAKE) -C $(NPC_HOME) gdb ARGS="$(NPCFLAGS)" MEM=$(IMAGE).mem IMG=$(IMAGE).bin
	

