#include "svdpi.h"
#include "utils.h"
#include <cstdio>
#include <cstdlib>
#include <iostream>
#include <memory>

#include <Vtop.h>
#include <verilated.h>
#include <verilated_vcd_c.h> //可选，如果要导出vcd则需要加上
#include <isa.h>
#include <memory/vaddr.h>
#include <memory/host.h>
#include <memory/paddr.h>
#include <cpu/cpu.h>
#include <dpi.h>

void init_monitor(int argc, char* argv[]);
void sdb_mainloop();
int is_exit_status_bad();

std::unique_ptr<VerilatedContext> contextp {};
std::unique_ptr<Vtop> top {};
VerilatedVcdC* tfp = new VerilatedVcdC;

Decode itrace;
Ftrace ftrace_block;
extern CPU_state cpu;

void sim_init(char argc, char* argv[]) {
  contextp = std::make_unique<VerilatedContext>();
  top = std::make_unique<Vtop>(contextp.get());
  Verilated::commandArgs(argc, argv);
  /* generate wave */
  contextp->traceEverOn(true);
  top->trace(tfp, 3);
  tfp->open("wave.vcd");
}

void sim_reset(Vtop* top) {
  top->reset = 1;
  top->clock = 0;
  top->eval();
    contextp->timeInc(1);
    tfp->dump(contextp->time());
  top->clock = 1;
  top->eval();
    contextp->timeInc(1);
    tfp->dump(contextp->time());
  // 上升沿触发，将初始值赋值给 pc
  top->reset = 0;
  // nemu_state.state = NEMU_RUNNING;
}

void sim_end() {
  top->final();
  tfp->close();
}

static void dummy() {
  while(1){
    top->clock = 0;
    top->eval();
    contextp->timeInc(1);
    tfp->dump(contextp->time());
    top->clock = 1;
    top->eval();
    contextp->timeInc(1);
    tfp->dump(contextp->time());

    if (nemu_state.state == NEMU_END) {
      Log("execution ended");
      break;
    // 没实现的指令
    } 
  }
}
int main(int argc, char** argv, char** env) {
 
  sim_init(argc, argv);
  sim_reset(top.get());

  init_monitor(argc, argv);
  // sdb_mainloop();
  dummy();
  sim_end();

  return is_exit_status_bad();
}
void verilator_exec_once(Decode* s) {
    ftrace_block.is_next_ins_j = false;
    /* ======================================================================  */
    // ===============   current instruction state =========================
    /* ======================================================================  */
    top->clock = 0;
    top->eval();
    contextp->timeInc(1);
    // printf("================= current state =====================\n");
    tfp->dump(contextp->time());
    s->isa.inst.val = itrace.isa.inst.val;
    s->pc = itrace.pc;
    s->snpc = s->pc + 4;
    // for ftrace
    s->dnpc = itrace.dnpc;
#if CONFIG_FTRACE
    if (ftrace_block.ftrace_flag) {
      ftrace(ftrace_block.rd, ftrace_block.optype, s, ftrace_block.src1);
      ftrace_block.ftrace_flag = false;
    }
#endif
    /* ======================================================================  */
    // ********************  next instruction state ********************
    /* ======================================================================  */
    // printf("============ next state ===================\n");
    top->clock = 1;
    top->eval();
    contextp->timeInc(1);

    tfp->dump(contextp->time());
    // s->dnpc = itrace.pc;
    unsigned next_inst = itrace.isa.inst.val;
#if CONFIG_FTRACE
    // ftrace
    if (ftrace_block.is_next_ins_j) {
      ftrace_block.ftrace_flag = true;
    }
#endif
    if (nemu_state.state == NEMU_END && next_inst == 0x00100073) {
        NEMUTRAP(s->dnpc, cpu.gpr[10]);
    // 没实现的指令
    } else if (nemu_state.state == NEMU_END && next_inst != 0x00100073) {
        INV(s->dnpc, next_inst);
    }
}

