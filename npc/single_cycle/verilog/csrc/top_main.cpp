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

std::unique_ptr<VerilatedContext> contextp {};
std::unique_ptr<Vtop> top {};

// called by verilog / not cpp
Decode itrace;
extern "C" void Dpi_itrace(unsigned int pc, unsigned int inst) {
  itrace.pc = pc;
  itrace.isa.inst.val = inst;
}

void sim_init(char argc, char* argv[]) {
  Verilated::commandArgs(argc, argv);
  contextp = std::make_unique<VerilatedContext>();
  top = std::make_unique<Vtop>(contextp.get());
  // const auto tfp = std::make_unique<VerilatedVcdC>();
  Verilated::traceEverOn(true);
  // top->trace(tfp.get(), 99);
  // tfp->open("wave.vcd");
}

void sim_reset(Vtop* top) {
  top->reset = 1;
  top->clock = 0;
  top->eval();
  top->clock = 1;
  top->eval();
  // 上升沿触发，将初始值赋值给 pc
  top->reset = 0;
  top->clock = 0;

}

void sim_end() {
  top->final();
}

int main(int argc, char** argv, char** env) {
 
  sim_init(argc, argv);
  sim_reset(top.get());

  init_monitor(argc, argv);
  sdb_mainloop();

  sim_end();

  return is_exit_status_bad();
}

void verilator_exec_once(Decode* s) {
    end = false;
    // printf("Executing instruction not implemented\n");
    contextp->timeInc(1);

    top->clock = 0;
    printf("0x%x", itrace.pc);
    // top->inst = vaddr_ifetch(top->pc, 4);
    s->isa.inst.val = itrace.isa.inst.val;
    s->snpc = itrace.pc + 4;
    printf(" 0x%x\n", top->io_inst);
    top->eval();

    contextp->timeInc(1);
    top->clock = 1;
    top->eval();
    s->dnpc = itrace.pc;
    // ebreak
    if (end && itrace.isa.inst.val == 0x00100073) {
        NEMUTRAP(s->pc, top->io_x10);
    // 没实现的指令
    } else if (end && itrace.isa.inst.val != 0x00100073) {
        INV(s->pc);
    }
}

extern "C" void stop() 
{
  end = true;
}
