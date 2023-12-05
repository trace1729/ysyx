#include <cstdio>
#include <cstdlib>
#include <iostream>
#include <memory>

#include "Vtop.h"
#include "verilated.h"
 
#include "verilated_vcd_c.h" //可选，如果要导出vcd则需要加上
 
int main(int argc, char** argv, char** env) {
 
  Verilated::commandArgs(argc, argv);
  const auto contextp = std::make_unique<VerilatedContext>();
  const auto top = std::make_unique<Vtop>(contextp.get());
 
  // VerilatedVcdC* tfp = new VerilatedVcdC; //初始化VCD对象指针
  // contextp->traceEverOn(true); //打开追踪功能
  // top->trace(tfp, 0); //
  // tfp->open("wave.vcd"); //设置输出的文件wave.vcd

  top->rst = 1;
  for (int i = 0; i < 10; i++) {
    unsigned int pc = top->inst;
    printf("PC = 0x%x\n", pc);
    top->eval();
    top->clk = !top->clk;
  }
  
  return 0;
}
