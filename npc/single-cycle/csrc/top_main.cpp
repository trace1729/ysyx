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
  0x00100293,
  0x7ff00413,
  0x0ff00313,
  0x01030393,
  0x00000000,
};

// uint32_t paddr_read(uint32_t pc) {
//   return npc_mem[(pc - BASE) / 4];
// } 

int main(int argc, char** argv, char** env) {
 
  Verilated::commandArgs(argc, argv);
  const auto contextp = std::make_unique<VerilatedContext>();
  const auto top = std::make_unique<Vtop>(contextp.get());
  // const auto tfp = std::make_unique<VerilatedVcdC>();
  Verilated::traceEverOn(true);
  // top->trace(tfp.get(), 99);
  // tfp->open("wave.vcd");

  top->clk = 0;
  top->rst = 1;
  top->eval();
  top->rst = 0;

  for (int i = 0; i < 10; i++) {
    contextp->timeInc(1);

    if (!top->clk) {
      printf("0x%x", top->pc);
      top->inst = npc_mem[(top->pc-BASE) / 4];
      printf(" 0x%x\n", top->inst);
    }

    top->eval();
    top->clk = !top->clk;

    if (top->clk) {
      printf("ra, sp, t0, t1, t2, s0, s1, a0,\
      rs1, rs2, \
      readreg1, readreg2, writereg, writeEn, data\n \
      %x %x %x %x %x %x %x %x\
      %x %x \
      %x %x %x %x %x\n", 
      top->x1, top->x2, top->x5, top->x6, top->x7, top->x8, top->x9, top->x10,
      top->rs1, top->rs2, 
      top->readreg1, top->readreg2, top->writereg, top->writeEn, top->data);
    }

    // tfp->dump(contextp->time());
  }
  top->final();
  return 0;
}
