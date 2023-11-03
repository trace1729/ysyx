V_SRC = top.v 
CXX_SRC = top_main.cpp 

all: sim

sim: $(V_SRC) $(CXX_SRC)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	verilator -Wall $(V_SRC) $(CXX_SRC) --cc --trace --exe --build
	@./obj_dir/Vtop   

include ../../Makefile
