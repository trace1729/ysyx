BUILD_DIR = ./build
TARGET = single_cycle

export PATH := $(PATH):$(abspath ./utils)

test:
	mill -i __.test

verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	mill -i $(TARGET).test.runMain Elaborate -td $(BUILD_DIR) -- $(MEM)

help:
	mill -i __.test.runMain Elaborate --help

compile:
	mill -i __.compile

bsp:
	mill -i mill.bsp.BSP/install

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

clean:
	-rm -rf $(BUILD_DIR)


run: verilog 
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	rm -rf $(NPC_HOME)/$(TARGET)/verilog/vsrc
	mkdir -p $(NPC_HOME)/$(TARGET)/verilog/vsrc
	cp $(BUILD_DIR)/sv-gen/*.sv $(NPC_HOME)/$(TARGET)/verilog/vsrc/
	make -C $(NPC_HOME)/$(TARGET)/verilog clean
	make -C $(NPC_HOME)/$(TARGET)/verilog run IMG=$(IMG)
	-gtkwave $(NPC_HOME)/$(TARGET)/verilog/wave.vcd


gdb: verilog 
	$(call git_commit, "debugger RTL") # DO NOT REMOVE THIS LINE!!!
	rm -rf $(NPC_HOME)/$(TARGET)/verilog/vsrc
	mkdir -p $(NPC_HOME)/$(TARGET)/verilog/vsrc
	cp $(BUILD_DIR)/sv-gen/*.sv $(NPC_HOME)/$(TARGET)/verilog/vsrc/
	make -C $(NPC_HOME)/$(TARGET)/verilog clean
	make -C $(NPC_HOME)/$(TARGET)/verilog gdb IMG=$(IMG)

.PHONY: test verilog help compile bsp reformat checkformat clean sim run gdb

-include ../Makefile
