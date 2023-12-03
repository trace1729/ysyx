#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
 
#include "Vtop.h"
#include "verilated.h"
 
#include "verilated_vcd_c.h" //可选，如果要导出vcd则需要加上
 
int main(int argc, char** argv, char** env) {
 
  VerilatedContext* contextp = new VerilatedContext;
  contextp->commandArgs(argc, argv);
  Vtop* top = new Vtop{contextp};
  
 
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
  
  delete top;
  // tfp->close();
  delete contextp;
  return 0;
}
