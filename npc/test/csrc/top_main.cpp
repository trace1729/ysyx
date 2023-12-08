#include <cstdio>
#include <cstdlib>
#include <iostream>
#include <memory>

#include "Vtop.h"
#include "verilated.h"
 
int main(int argc, char** argv, char** env) {
 
  Verilated::commandArgs(argc, argv);
  const auto contextp = std::make_unique<VerilatedContext>();
  const auto top = std::make_unique<Vtop>(contextp.get());


  for (int i = 0; i < 10; i++) {
    top->writereg = i;
    top->eval();
    printf("0x%x\n", top->writeEnables);
  }

  top->final();
  return 0;
}
