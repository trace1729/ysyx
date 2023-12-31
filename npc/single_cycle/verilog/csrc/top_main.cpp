#include <cstdio>
#include <cstdlib>
#include <iostream>
#include <memory>

#include "Vtop.h"
#include "memory/vaddr.h"
#include "verilated.h"
#include "verilated_vcd_c.h" //可选，如果要导出vcd则需要加上


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

int main(int argc, char** argv, char** env) {
 
  init_monitor(argc, argv);
  sdb_mainloop();
  Verilated::commandArgs(argc, argv);
  const auto contextp = std::make_unique<VerilatedContext>();
  const auto top = std::make_unique<Vtop>(contextp.get());
  // const auto tfp = std::make_unique<VerilatedVcdC>();
  Verilated::traceEverOn(true);
  // top->trace(tfp.get(), 99);
  // tfp->open("wave.vcd");

  reset(top.get());
  while (end == false) {
    contextp->timeInc(1);

    if (!top->clock) {
      printf("0x%x", top->pc);
      top->inst = vaddr_ifetch(top->pc, 4);
      printf(" 0x%x\n", top->inst);
    }

    top->eval();
    top->clock = !top->clock;

    if (top->clock) {
      printf("ra, sp, t0, t1, t2, s0, s1, a0,\
      rs1, rs2, \
      readreg1, readreg2, writereg, writeEn, data\n \
      %x %x %x %x %x %x %x %x\n", 
      top->x1, top->x2, top->x5, top->x6, top->x7, top->x8, top->x9, top->x10);
      // top->rs1, top->rs2, 
      // top->readreg1, top->readreg2, top->writereg, top->writeEn, top->data 
    }
    //
    // tfp->dump(contextp->time());
  }
  if (top->x10 == 0) {
    printf("Hit Good Trap!\n");
  } else {
    printf("Hit bad Trap!\n");
  }
  top->final();
  
  return 0;
}
