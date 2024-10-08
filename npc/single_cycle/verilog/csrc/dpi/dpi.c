#include "common.h"
#include <cassert>
#include <cstdio>
#include <memory/host.h>
#include <memory/paddr.h>
#include <dpi.h>
#include <utils.h>
#include <cpu/difftest.h>

const char* uart= "/home/trace/trace/learning/ysyx/ysyx-workbench/npc/uart.bin";

static char mrom[MROM_SIZE] = {};

/* ff810113 addi sp,sp,-8 */
/* 00112223 sw ra,4(sp) */
/* 00812023 sw s0,0(sp) */
/* 00810413 addi s0,sp,8 */
/* 100007b7 lui a5,0x10000 */
/* 04100713 li a4,65 */
/* 00e78023 sb a4,0(a5) */
/* 100007b7 lui a5,0x10000 */
/* 00a00713 li a4,10 */
/* 00e78023 sb a4,0(a5) */
/* 0000006f j 10100 */
/*  */
static uint32_t default_img[] = {
  0x100007b7,
  0x04100713,
  0x00e78023,
  0x0000006f
};


char* copy_to_mrom(int32_t addr) {
  return mrom + addr - MROM_BASE;
};

long mrom_init() {
  memcpy(mrom, default_img, sizeof(default_img));
  return sizeof (default_img);
  /* char* img_file = (char* )uart; */
  /* printf(ANSI_FMT("Using img", ANSI_BG_GREEN)" %s\n", img_file); */
  /* FILE *fp = fopen(img_file, "rb"); */
  /* Assert(fp, "Can not open '%s'", img_file); */
  /*  */
  /* fseek(fp, 0, SEEK_END); */
  /* long size = ftell(fp); */
  /*  */
  /* Log("The image is %s, size = %ld", img_file, size); */
  /*  */
  /* fseek(fp, 0, SEEK_SET); */
  /* int ret = fread(mrom, size, 1, fp); */
  /* assert(ret == 1); */
  /*  */
  /* fclose(fp); */
  /* return size; */
}

extern Decode itrace; // define in top
extern Ftrace ftrace_block; // define in top
extern bool next_inst;

extern "C" void flash_read(int32_t addr, int32_t *data) { assert(0); }
extern "C" void mrom_read(int32_t addr, int32_t *data) { 
  *data = *((uint32_t*)copy_to_mrom(addr & ~(0x3u)));
#ifdef CONFIG_MTRACE
  if (addr >= MROM_BASE)
    printf("mrom trace: 0x%x, 0x%x\n", addr, *data);
#endif
}

extern "C" void Next_inst() 
{
  // Log("ebreak encounterd, execution ended");
  // printf("%x %x\n", itrace.pc, itrace.isa.inst.val);
  next_inst = true;
}
extern "C" void stop() 
{
  // Log("ebreak encounterd, execution ended");
  // printf("%x %x\n", itrace.pc, itrace.isa.inst.val);
  nemu_state.state = NEMU_END;
}

// called by verilog / not cpp

extern "C" void Dpi_itrace(unsigned int pc, unsigned int inst, unsigned int nextpc) {
  // printf("itrace\n");
  itrace.pc = pc;
  itrace.isa.inst.val = inst;
  itrace.dnpc = nextpc;
}


extern "C" void Dpi_ftrace(unsigned char optype, unsigned char rd, unsigned int src1) {
  // printf("ftrace\n");
  ftrace_block.optype = optype;
  ftrace_block.rd = rd;
  ftrace_block.src1 = src1;
  ftrace_block.is_next_ins_j = true;
}

extern "C" unsigned dpi_pmem_read (unsigned int raddr) {
#if CONFIG_MTRACE
  // if (raddr >= 0x80021000) {
    printf("paddr_read: Accessing memory at location %02x\n", raddr);
  // }
  if (raddr == 0) {
    return 0;
  }
#endif
  if (raddr == CONFIG_RTC_MMIO) {
    uint32_t us = (get_time() & 0xffffffff);
#if CONFIG_DIFFTEST
    difftest_skip_ref();
    // difftest_skip_next_ref();
#endif
    return us;
  }
  if (raddr == CONFIG_RTC_MMIO + 4) {
    uint32_t us = ((get_time() >> 32) & 0xffffffff);
#if CONFIG_DIFFTEST
    difftest_skip_ref();
    // difftest_skip_next_ref();
#endif
    return us;
  }
  if (raddr < 0x8000000) {
    return 0;
  }
  unsigned rdata = host_read(guest_to_host(raddr & ~0x3u), 4);
  // printf("read addr %x, rdata %x\n", raddr, rdata);
  return rdata;
}

extern "C" void dpi_pmem_write(unsigned int waddr, unsigned int wdata, unsigned char wmask) {
#if CONFIG_MTRACE
  printf("paddr_write: Accessing memory at location %02x, data %x\n", waddr, wdata);
#endif
  if (waddr == CONFIG_SERIAL_MMIO) {
    putc(wdata, stderr);
#if CONFIG_DIFFTEST
    difftest_skip_ref();
    // difftest_skip_next_ref();
#endif
    return; 
  }
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
  // printf("setting regs\n");
  for (int i = 0; i < 32; i++) {
    cpu.gpr[i] = regs[i].aval;
  }
}

void reset_to_default() {
  // just setting every register to zero
  // 如果 reset 的话，那两边都需要清空
  for (int i = 0; i < 32; i++) {
    cpu.gpr[i] = 0;
  }
}

extern "C" void Csrs_display(const svLogicVecVal* regs) 
{
  cpu.csr[MSTATUS] = regs[0].aval;
  cpu.csr[MEPC] = regs[1].aval;
  cpu.csr[MTVEC] = regs[2].aval;
  cpu.csr[MCAUSE] = regs[3].aval;
}
#ifdef CONFIG_FTRACE
#define JAL 1
#define RA 1
#define ZERO 0
#define JALR 0

int depth = 0;
void get_function_symbol_by_address(uint32_t addr, char *buf);
void ftrace(int rd, int type, Decode* s, word_t src1) {
  char function[128];
  //      jalr          x0              x1
  if (type == JALR && rd == ZERO && src1 == cpu.gpr[RA]) {
    // if register is x0, and instructio type is jalr
    // then it is function return
    // return from instead of return to
    get_function_symbol_by_address(s->pc, function);
    printf("0x%x: ", s->pc);
    for (int i = 0; i < depth; i++) printf(" ");
    printf("ret[%s]\n", function);
    depth--;

  //              jal(r)          ra   ;       jalr            x0      x*(!=x1)  
  } else if ((type == JAL && rd == RA) || (type == JALR && rd == ZERO && src1 != cpu.gpr[RA]) \
          || (type == JALR && rd == RA)) {
    // if register is ra, and instruction type is jal(r)
    // then it is function call
    get_function_symbol_by_address(s->dnpc, function);
    printf("0x%x: ", s->pc);
    for (int i = 0; i < depth; i++) printf(" ");
    printf("call[%s@0x%x]\n", function, s->dnpc);
    depth++;
  }
}
#else
void ftrace(int rd, int type, Decode* s, word_t src1) {}
#endif
