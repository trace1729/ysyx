#include <cstdio>
#include <cstdlib>
#include <iostream>
#include <memory>

#include <Vtop.h>
#include <verilated.h>
#include <verilated_vcd_c.h> //可选，如果要导出vcd则需要加上
#include <isa.h>
#include <memory/vaddr.h>
#include <cpu/cpu.h>

void init_monitor(int argc, char* argv[]);
void sdb_mainloop();
int is_exit_status_bad();
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

void verilator_exec_once(Decode* s) {
    end = false;
    // printf("Executing instruction not implemented\n");
    contextp->timeInc(1);

    top->clock = 0;
    printf("0x%x", top->pc);
    top->inst = vaddr_ifetch(top->pc, 4);
    s->isa.inst.val = top->inst;
    s->snpc = top->pc + 4;
    printf(" 0x%x\n", top->inst);
    top->eval();

    contextp->timeInc(1);
    top->clock = 1;
    top->eval();
    s->dnpc = top->pc;
    // ebreak
    if (end && top->inst == 0x00100073) {
        NEMUTRAP(s->pc, top->x10);
    // 没实现的指令
    } else if (end && top->inst != 0x00100073) {
        INV(top->pc);
    }
}

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

  
  top->final();
  
  return is_exit_status_bad();
}
