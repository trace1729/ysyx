BUILD_DIR = ./build
TARGET = lab6

export PATH := $(PATH):$(abspath ./utils)

test:
	mill -i __.test

verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	mill -i $(TARGET).test.runMain Elaborate -td $(BUILD_DIR)

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


sim: verilog
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@sed -E 's/io_([a-z0-9])/\1/g' $(BUILD_DIR)/top.v > $(NPC_HOME)/$(TARGET)/verilog/vsrc/top.v
	make -C $(NPC_HOME)/$(TARGET)/verilog run

.PHONY: test verilog help compile bsp reformat checkformat clean sim

-include ../Makefile
