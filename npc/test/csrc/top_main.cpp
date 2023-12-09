#include <cstdio>
#include <cstdlib>
#include <iostream>
#include <memory>

#include "Vtop.h"
#include "verilated.h"

uint16_t test_inst[] = {
  0x84a2, 0x88a1, 0x9422, 0x9845,
  0x9ca6, 0xa0c7, 0xa4e8, 0xa909};
uint32_t test_data[] = {
  0xffffbeef, 0x00000dad,
  0xfffff00d, 0x00005678,
  0x0afdfadf, 0xffffffff,
  0xa786adc2, 0x98a09723};

int main(int argc, char** argv, char** env) {
 
  Verilated::commandArgs(argc, argv);
  const auto contextp = std::make_unique<VerilatedContext>();
  const auto top = std::make_unique<Vtop>(contextp.get());

  top->clk = 0;
  top->eval();

  for (int i = 0; i < 16; i++) {
    contextp->timeInc(1);

    if (!top->clk) {
      printf("%d\n", top->cnt);
      top->inst = test_inst[top->cnt];
      top->data = test_data[top->cnt];
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

  }

  top->final();
  return 0;
}
