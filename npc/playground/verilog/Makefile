TOPNAME = top
INC_PATH ?=

# source files
VSRCS = $(shell find $(abspath ./vsrc) -name "*.v")
CSRCS += $(shell find $(abspath ./csrc) -name "*.c" -or -name "*.cpp")

# verilator
VERILATOR = verilator
VERILATOR_CFLAGS += -MMD --build -cc  \
				-O3 --x-assign fast --x-initial fast --noassert --trace

# build
WORK_DIR = $(shell pwd)
BUILD_DIR = $(WORK_DIR)/build
OBJ_DIR = $(BUILD_DIR)/obj_dir
BIN = $(OBJ_DIR)/$(TOPNAME)
$(shell mkdir -p $(BUILD_DIR))


# # for debugging
CXXFLAGS += -ggdb -DVL_DEBUG=1 -D_GLIBCXX_DEBUG
LDFLAGS += -ggdb

# default: run
$(BIN): $(VSRCS) $(CSRCS)
	@rm -rf $(OBJ_DIR)
	$(VERILATOR) $(VERILATOR_CFLAGS) \
		--top-module $(TOPNAME) $^ \
		$(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) --exe -o $(abspath $(BIN))

build: $(BIN)

run: build
	$(BIN)

sim: build
	$(BIN)
	-gtkwave wave.vcd

clean:
	-@rm -fr $(BUILD_DIR)
	-@rm -fr wave.vcd

.PHONY: clean run