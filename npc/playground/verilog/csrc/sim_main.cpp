// DESCRIPTION: Verilator: Verilog example module
//
// This file ONLY is placed under the Creative Commons Public Domain, for
// any use, without warranty, 2017 by Wilson Snyder.
// SPDX-License-Identifier: CC0-1.0

//======================================================================
#include <Vtop.h>
#include <cstdio>
#include <verilated.h>
#include <verilated_vcd_c.h> //可选，如果要导出vcd则需要加上


std::unique_ptr<VerilatedContext> contextp {};
std::unique_ptr<Vtop> top {};

void sim_init(char argc, char* argv[]) {
  contextp = std::make_unique<VerilatedContext>();
  top = std::make_unique<Vtop>(contextp.get());
  contextp->traceEverOn(true);
  Verilated::commandArgs(argc, argv);
}


int main(int argc, char** argv, char** env) {
 
  sim_init(argc, argv);

  // have to put tfp in main function, otherwise it will be destroyed after the function ends
  auto tfp = std::make_unique<VerilatedVcdC>();
  top->trace(tfp.get(), 99);
  tfp->open("wave.vcd");

  for (int i = 0; i < 10; i++) {
    top->clock = 0;
    top->reset = 1;
    top->eval();
    contextp->timeInc(1);
    tfp->dump(contextp->time());

    top->clock = 1;
    top->eval();
    contextp->timeInc(1);
    tfp->dump(contextp->time());
  }

  printf("Simulation done\n");

  tfp->close();
  top->final();

  return 0;
}