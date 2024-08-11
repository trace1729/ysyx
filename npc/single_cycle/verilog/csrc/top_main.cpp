#include "utils.h"
#include <cstdio>
#include <cstdlib>
#include <memory>

#include <verilated.h>
#include <verilated_vcd_c.h> //可选，如果要导出vcd则需要加上
#include <isa.h>
#include <memory/vaddr.h>
#include <memory/host.h>
#include <memory/paddr.h>
#include <cpu/cpu.h>
#include <dpi.h>

#define STRINGIFY(x) #x
#define HEADER(x) STRINGIFY(x.h)

#include HEADER(TOP_NAME)


void init_monitor(int argc, char* argv[]);
void sdb_mainloop();
int is_exit_status_bad();

std::unique_ptr<VerilatedContext> contextp {};
std::unique_ptr<TOP_NAME> top {};
VerilatedVcdC* tfp = new VerilatedVcdC;

Decode itrace;
Ftrace ftrace_block;
extern CPU_state cpu;
bool next_inst = false;

void sim_init(char argc, char* argv[]) {
  contextp = std::make_unique<VerilatedContext>();
  top = std::make_unique<TOP_NAME>(contextp.get());
  Verilated::commandArgs(argc, argv);
  /* generate wave */
  contextp->traceEverOn(true);
  top->trace(tfp, 3);
  tfp->open("wave.vcd");
}

void soc_sim_rest(TOP_NAME* top) {
  top->reset = 1;
  for (int i = 0; i < 9; i++) {
    top->clock = 0; top->eval();
    contextp->timeInc(1);
    tfp->dump(contextp->time());

    top->clock = 1; top->eval();
    contextp->timeInc(1);
    tfp->dump(contextp->time());
  }
}

#ifdef CONFIG_WAVE
void dump_wave() {
    contextp->timeInc(1);
    tfp->dump(contextp->time());
}
#else
void dump_wave() {}
#endif

void sim_reset(TOP_NAME* top) {
  top->reset = 1;
  top->clock = 0;
  top->eval();
  dump_wave();
  top->clock = 1;
  top->eval();
  dump_wave();
  // 上升沿触发，将初始值赋值给 pc
  top->reset = 0;

  // nemu_state.state = NEMU_RUNNING;
  // delay for ten cycles
  // 这个时候不接入测试框架
  // 还需要把 nemu_state 设置为 running
  for (int i = 0; i < 9; i++) {
    top->clock = 0; top->eval();
    dump_wave();

    top->clock = 1; top->eval();
    dump_wave();
  }

  nemu_state.state = NEMU_STOP;
}

void sim_end() {
  top->final();
  tfp->close();
}

static void dummy() {
  int num_i = 0;
  for (int i = 0; i < 200; i++)
  {
    Log("%d clock cycle", num_i++);
    top->clock = 0;
    top->eval();
    contextp->timeInc(1);
    tfp->dump(contextp->time());
    top->clock = 1;
    top->eval();
    contextp->timeInc(1);
    tfp->dump(contextp->time());

    if (nemu_state.state == NEMU_END) {
      Log("execution ended at cycle %d", num_i);
      break;
    // 没实现的指令
    } 
  }
}
int main(int argc, char** argv, char** env) {
 
  sim_init(argc, argv);
  sim_reset(top.get());

  init_monitor(argc, argv);
#ifdef CONFIG_DEBUG
  dummy();
#else
  sdb_mainloop();
#endif
  sim_end();
  Log("gracefully quit");
  
  return is_exit_status_bad();
}
void verilator_exec_once(Decode* s) {
    ftrace_block.is_next_ins_j = false;
    next_inst = false;
    while (!next_inst) {
      // tick = 0
      top->clock = 0;
      top->eval();
      dump_wave();
      // tick = 1
      top->clock = 1;
      top->eval();
      dump_wave();
    }  
    s->isa.inst.val = itrace.isa.inst.val;
    s->pc = itrace.pc;
    s->snpc = s->pc + 4;
    s->dnpc = itrace.dnpc;
#if CONFIG_FTRACE
    if (ftrace_block.ftrace_flag) {
      ftrace(ftrace_block.rd, ftrace_block.optype, s, ftrace_block.src1);
      ftrace_block.ftrace_flag = false;
    }
#endif
    unsigned next_inst = itrace.isa.inst.val;
#if CONFIG_FTRACE
    // ftrace
    if (ftrace_block.is_next_ins_j) {
      ftrace_block.ftrace_flag = true;
    }
#endif
    // 结束检测
    if (nemu_state.state == NEMU_END) {
        NEMUTRAP(s->dnpc, cpu.gpr[10]);
    // 没实现的指令
    } 
    if (s->pc == 0) {
      void skip_nop();
      skip_nop();
    }
    // else if (nemu_state.state == NEMU_END && next_inst != 0x00100073) {
    //     INV(s->dnpc, next_inst);
    // }
}

