VERILATOR=verilator 
FLAGS=--cc --exe --build -j 0 -Wall
TRACE=--trace
CPP_SRC = $(wildcard csrc/*.cpp)
V_SRC = $(wildcard vsrc/*.v)

all:
	@echo "Write this Makefile by your self."

sim: $(CPP_SRC) $(V_SRC)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	$(VERILATOR) $(FLAGS) $(TRACE) $^

clean:
	rm -fr obj_dir/
	rm ./wave.vcd

include ../Makefile
