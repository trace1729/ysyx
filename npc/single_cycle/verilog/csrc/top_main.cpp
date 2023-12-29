#include <cstdio>
#include <cstdlib>
#include <iostream>
#include <memory>

#include "Vtop.h"
#include "verilated.h"
#include "verilated_vcd_c.h" //可选，如果要导出vcd则需要加上

#define BASE 0x80000000
#define PG_ALIGN __attribute((aligned(4096)))

uint32_t npc_mem[100] PG_ALIGN = {
  0x00000413,
  0x00009117,
  0xffc10113,
  0x00c000ef,
  0x00000513,
  0x00008067,
  0xff410113,
  0x00000517,
  0x01c50513,
  0x00112423,
  0xfe9ff0ef,
  0x00050513,
  0x00100073,
  0x0000006f,
};

volatile bool end = false;

extern "C" void stop() 
{
  end = true;
}

// uint32_t paddr_read(uint32_t pc) {
//   return npc_mem[(pc - BASE) / 4];
// } 

// const char* img_file = getenv("IMG");

// static long load_img() {
//   if (img_file == NULL) {
//     printf("No image is given. Use the default build-in image.");
//     return 4096; // built-in image size
//   }

//   FILE *fp = fopen(img_file, "rb");
//   // Assert(fp, "Can not open '%s'", img_file);

//   fseek(fp, 0, SEEK_END);
//   long size = ftell(fp);

//   printf("The image is %s, size = %ld\n", img_file, size);

//   fseek(fp, 0, SEEK_SET);
//   int ret = fread(npc_mem, size, 1, fp);
//   assert(ret == 1);

//   fclose(fp);
//   return size;
// }

int main(int argc, char** argv, char** env) {
 
  // load_img();
  // printf("%s\n", img_file);
  Verilated::commandArgs(argc, argv);
  const auto contextp = std::make_unique<VerilatedContext>();
  const auto top = std::make_unique<Vtop>(contextp.get());
  // const auto tfp = std::make_unique<VerilatedVcdC>();
  Verilated::traceEverOn(true);
  // top->trace(tfp.get(), 99);
  // tfp->open("wave.vcd");

  top->reset = 1;
  top->clock = 0;
  top->eval();
  top->clock = 1;
  top->eval();
  // 上升沿触发，将初始值赋值给 pc
  top->reset = 0;
  top->clock = 0;

  while (end == false) {
    contextp->timeInc(1);

    if (!top->clock) {
      printf("0x%x", top->pc);
      top->inst = npc_mem[(top->pc - BASE) / 4];
      printf(" 0x%x\n", top->inst);
    }

    top->eval();
    top->clock = !top->clock;

    if (top->clock) {
      printf("ra, sp, t0, t1, t2, s0, s1, a0,\
      rs1, rs2, \
      readreg1, readreg2, writereg, writeEn, data\n \
      %x %x %x %x %x %x %x %x\n", 
      top->x1, top->x2, top->x5, top->x6, top->x7, top->x8, top->x9, top->x10);
      // top->rs1, top->rs2, 
      // top->readreg1, top->readreg2, top->writereg, top->writeEn, top->data 
    }
    //
    // tfp->dump(contextp->time());
  }
  if (top->x10 == 0) {
    printf("Hit Good Trap!\n");
  } else {
    printf("Hit bad Trap!\n");
  }
  top->final();
  
  return 0;
}
