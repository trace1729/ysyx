#include <cstdio>
#include <cstdlib>
#include <iostream>
#include <memory>

#include "Vtop.h"
#include "verilated.h"
 
#include "verilated_vcd_c.h" //可选，如果要导出vcd则需要加上
#define MEMSIZE 10
#define BASE 0x80000000
uint32_t npc_mem[MEMSIZE] = {
  0x00000013,
  0x00100093,
  0x00200113,
  0x00300193,
  0x00400213
};

uint32_t paddr_read(uint32_t pc) {
  return npc_mem[(pc - BASE) / 4];
} 

int main(int argc, char** argv, char** env) {
 
  Verilated::commandArgs(argc, argv);
  const auto contextp = std::make_unique<VerilatedContext>();
  const auto top = std::make_unique<Vtop>(contextp.get());
  const auto tfp = std::make_unique<VerilatedVcdC>();
  Verilated::traceEverOn(true);
  top->trace(tfp.get(), 99);
  tfp->open("wave.vcd");

  top->clk = 0;
  top->rst = 1;

  for (int i = 0; i < 20; i++) {
    contextp->timeInc(1);

    top->clk = !top->clk;

    if (top->clk && !top->rst) {
      top->inst = paddr_read(top->pc);
      unsigned int inst = top->inst;
      printf("PC = 0x%x, x1 = 0x%x, x2 = 0x%x, x3 = 0x%x, x4 = 0x%x, x5 = 0x%x\n", inst
          ,top->x1, top->x2, top->x3, top->x4,top->x5);
    }

    // 前3ps 用来重置
    if (contextp->time() > 4) {
      top->rst = !1;
    }

    top->eval();
    tfp->dump(contextp->time());
  }
  top->final();
  return 0;
}
