#include <memory/host.h>
#include <memory/paddr.h>
#include <dpi.h>
#include <utils.h>
#include <cpu/difftest.h>

extern Decode itrace; // define in top
extern Ftrace ftrace_block; // define in top

extern "C" void stop() 
{
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
  // printf("paddr_read: Accessing memory at location %02x\n", raddr);
#endif
  if (raddr == CONFIG_RTC_MMIO) {
    uint32_t us = (get_time() & 0xffffffff);
    // difftest_skip_next_ref();
    return us;
  }
  if (raddr == CONFIG_RTC_MMIO + 4) {
    uint32_t us = ((get_time() >> 32) & 0xffffffff);
    // difftest_skip_next_ref();
    return us;
  }
  unsigned rdata = host_read(guest_to_host(raddr & ~0x3u), 4);
  // printf("read addr %x, rdata %x\n", raddr, rdata);
  return rdata;
}

extern "C" void dpi_pmem_write(unsigned int waddr, unsigned int wdata, unsigned char wmask) {
  // 偷个懒，这里应该使用位操作写入数据，比如
  /* wmask: 0110
  // 根据 wmask 生成
          00000000 11111111 11111111 00000000
     MEM: 00000001 11001100 11001001 10021002
     做或 | 运算
          00000001 11111111 11111111 10021002
     数据：11111111 10101010 10101010 11111111
     做与 & 运算
     MEM: 00000001 10101010 10101010 10021002
  */
  // 不过使用这种方法的效果和下面的 switch 语句是等效的。
  // printf("write waddr %x, wdata %x, wmask %x\n", waddr, wdata, wmask);
#if CONFIG_MTRACE
  printf("paddr_write: Accessing memory at location %02x, data %x\n", waddr, wdata);
#endif
  if (waddr == CONFIG_SERIAL_MMIO) {
    putc(wdata, stderr);
    // difftest_skip_next_ref();
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
