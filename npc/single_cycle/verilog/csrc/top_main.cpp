#include <cstdio>
#include <cstdlib>
#include <iostream>
#include <memory>

#include <Vtop.h>
#include <verilated.h>
#include <verilated_vcd_c.h> //可选，如果要导出vcd则需要加上


void init_monitor(int argc, char* argv[]);
void sdb_mainloop();
volatile bool end = false;
extern "C" void stop() 
{
  end = true;
}

void reset(Vtop* top) {
  top->reset = 1;
  top->clock = 0;
  top->eval();
  top->clock = 1;
  top->eval();
  // 上升沿触发，将初始值赋值给 pc
  top->reset = 0;
  top->clock = 0;

}

std::unique_ptr<VerilatedContext> contextp {};
std::unique_ptr<Vtop> top {};


int main(int argc, char** argv, char** env) {
 
  Verilated::commandArgs(argc, argv);
  contextp = std::make_unique<VerilatedContext>();
  top = std::make_unique<Vtop>(contextp.get());
  // const auto tfp = std::make_unique<VerilatedVcdC>();
  Verilated::traceEverOn(true);
  // top->trace(tfp.get(), 99);
  // tfp->open("wave.vcd");
  reset(top.get());

  init_monitor(argc, argv);
  sdb_mainloop();

  if (top->x10 == 0) {
    printf("Hit Good Trap!\n");
  } else {
    printf("Hit bad Trap!\n");
  }
  top->final();
  
  return 0;
}
