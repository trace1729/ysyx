#ifndef __DPI_H__
#define __DPI_H__

#include "svdpi.h"
#include <isa.h>

typedef struct {
  unsigned char rd;
  unsigned char optype;
  unsigned int src1;
  bool is_next_ins_j;
  bool ftrace_flag;
} Ftrace;

extern "C" void stop();
extern "C" void Dpi_itrace(unsigned int pc, unsigned int inst, unsigned int nextpc);
extern "C" void Dpi_ftrace(unsigned char optype, unsigned char rd, unsigned int src1);
extern "C" unsigned dpi_pmem_read (unsigned int raddr);
extern "C" void dpi_pmem_write(unsigned int waddr, unsigned int wdata, unsigned char wmask);
extern "C" void Regs_display(const svLogicVecVal* regs);
extern "C" void Csrs_display(const svLogicVecVal* regs);
void ftrace(int rd, int type, Decode* s, word_t src1);

#endif