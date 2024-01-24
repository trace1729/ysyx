#include "svdpi.h"
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
std::unique_ptr<VerilatedVcdC> tfp{};

extern "C" void stop() 
{
  end = true;
}

// called by verilog / not cpp
Decode itrace;
extern CPU_state cpu;
extern "C" void Dpi_itrace(unsigned int pc, unsigned int inst, unsigned int nextpc) {
  itrace.pc = pc;
  itrace.isa.inst.val = inst;
  itrace.dnpc = nextpc;
}

// static uint8_t dmem[CONFIG_MSIZE] PG_ALIGN = {};
// uint8_t* dpi_guest_to_host(paddr_t paddr) { return dmem + paddr - CONFIG_MBASE; }
// paddr_t dpi_host_to_guest(uint8_t *haddr) { return haddr - dmem + CONFIG_MBASE; }

extern "C" unsigned dpi_pmem_read (unsigned int raddr) {
  unsigned rdata = host_read(guest_to_host(raddr & ~0x3u), 4);
  printf("read addr %x, rdata %x\n", raddr, rdata);
  return rdata;
}

extern "C" void dpi_pmem_write(unsigned int waddr, unsigned int wdata, unsigned char wmask) {
  // 偷个懒，这里应该使用位操作写入数据，比如
  /* wmask: 0110
  // 根据 wmask 生成
          0000 0000 1111 1111 1111 1111 0000 0000
     MEM: 0000 0001 1100 1100 1100 1001 1002 1002
     做或运算
          0000 0001 1111 1111 1111 1111 1002 1002
     数据：1111 1111 1010 1010 1010 1010 1111 1111
     做与运算
     MEM: 0000 0001 1010 1010 1010 1010 1002 1002
  */
  // 不过使用这种方法的效果和下面的 switch 语句是等效的。
  printf("write waddr %x, wdata %x\n", waddr, wdata);
  switch (wmask) {
    case 1:
      host_write(guest_to_host((waddr & ~0x3u) ), 1, wdata);
      break;
    case 2:
      host_write(guest_to_host((waddr & ~0x3u) + 1 ), 1, wdata);
      break;
    case 4:
      host_write(guest_to_host((waddr & ~0x3u) + 2 ), 1, wdata);
      break;
    case 8:
      host_write(guest_to_host((waddr & ~0x3u) + 3), 1, wdata);
      break;
    case 3:
      host_write(guest_to_host((waddr & ~0x3u) ), 2, wdata);
      break;
    case 6:
      host_write(guest_to_host((waddr & ~0x3u) + 1), 2, wdata);
      break;
    case 12:
      host_write(guest_to_host((waddr & ~0x3u) + 2), 2, wdata);
      break;
    case 15:
      host_write(guest_to_host((waddr & ~0x3u) ), 4, wdata);
      break;
    default:break;
  }
}


extern "C" void Regs_display(const svLogicVecVal* regs) 
{
  for (int i = 0; i < 32; i++) {
    cpu.gpr[i] = regs[i].aval;
  }
}

void sim_init(char argc, char* argv[]) {
  contextp = std::make_unique<VerilatedContext>();
  top = std::make_unique<Vtop>(contextp.get());
  contextp->traceEverOn(true);
  Verilated::commandArgs(argc, argv);
  tfp = std::make_unique<VerilatedVcdC>();
  // top->trace(tfp.get(), 3);
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

}

void sim_end() {
  top->final();
  tfp->close();
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
    top->clock = 0;
    top->eval();
    contextp->timeInc(1);
    // tfp->dump(contextp->time());
    s->isa.inst.val = itrace.isa.inst.val;
    s->pc = itrace.pc;
    s->snpc = s->pc + 4;
    // printf("Before exec inst\n");
    // printf("0x%x", s->pc);
    // printf(" 0x%x\n", s->isa.inst.val);
    top->clock = 1;
    // printf("After exec instruction.\n");
    top->eval();
    contextp->timeInc(1);
    // tfp->dump(contextp->time());
    // printf("ra = 0x%x\n", top->io_x1);
    // printf("sp = 0x%x\n", top->io_x2);
    // printf("t0 = 0x%x\n", top->io_x5);
    // printf("t1 = 0x%x\n", top->io_x6);
    // printf("t2 = 0x%x\n", top->io_x7);
    // printf("fp = 0x%x\n", top->io_x8);
    // printf("s1 = 0x%x\n", top->io_x9);
    // printf("a0 = 0x%x\n", top->io_x10);

    // tfp->dump(contextp->time());
    s->dnpc = itrace.pc;
    unsigned next_inst = itrace.isa.inst.val;
    // printf("nextpc 0x%x", s->dnpc);
    // printf(" next inst 0x%x\n", next_inst);

    // ebreak 因为 end 的值是由组合逻辑确定的，所以可以提前判断
    if (end && next_inst == 0x00100073) {
        NEMUTRAP(s->dnpc, top->io_x10);
    // 没实现的指令
    } else if (end && next_inst != 0x00100073) {
        INV(s->dnpc, next_inst);
    }
}

