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

void init_monitor(int argc, char* argv[]);
void sdb_mainloop();
int is_exit_status_bad();
volatile bool end = false;

std::unique_ptr<VerilatedContext> contextp {};
std::unique_ptr<Vtop> top {};

extern "C" void stop() 
{
  end = true;
}

// called by verilog / not cpp
Decode itrace;
extern "C" void Dpi_itrace(unsigned int pc, unsigned int inst) {
  itrace.pc = pc;
  itrace.isa.inst.val = inst;
}

static uint8_t dmem[CONFIG_MSIZE] PG_ALIGN = {};
uint8_t* dpi_guest_to_host(paddr_t paddr) { return dmem + paddr - CONFIG_MBASE; }
paddr_t dpi_host_to_guest(uint8_t *haddr) { return haddr - dmem + CONFIG_MBASE; }

extern "C" void dpi_pmem_read (unsigned int raddr, unsigned  int rdata) {
  raddr = host_read(dpi_guest_to_host(raddr & ~0x3u), 4);
}
extern "C" void dpi_pmem_write(unsigned int waddr, unsigned int wdata, unsigned char wmask) {
  switch (wmask) {
    case 0:
      host_write(dpi_guest_to_host(waddr & ~0x3u), 1, wdata);
    case 1:
      host_write(dpi_guest_to_host(waddr & ~0x3u), 2, wdata);
    case 2:
      host_write(dpi_guest_to_host(waddr & ~0x3u), 4, wdata);
  }
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

    // top->inst = vaddr_ifetch(top->pc, 4);
    // 只要是打印出来的指令，一定是成功执行的
    s->isa.inst.val = itrace.isa.inst.val;
    s->pc = itrace.pc;
    s->snpc = itrace.pc + 4;
    printf("0x%x", s->pc);
    printf(" 0x%x\n", s->isa.inst.val);

    contextp->timeInc(1);
    top->clock = 0;
    top->eval();

    contextp->timeInc(1);
    top->clock = 1;
    top->eval();
    s->dnpc = itrace.pc;
    unsigned next_inst = itrace.isa.inst.val;
    // ebreak
    if (end && next_inst == 0x00100073) {
        printf("debuging\n");
        NEMUTRAP(s->dnpc, top->io_x10);
    // 没实现的指令
    } else if (end && next_inst != 0x00100073) {
        INV(s->dnpc, next_inst);
    }
}

