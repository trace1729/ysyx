#include <am.h>
#include <riscv/riscv.h>
#include <klib.h>

static Context* (*user_handler)(Event, Context*) = NULL;

#if __riscv_xlen == 32
#define XLEN 4
#else 
#define XLEN 8
#endif

#define CONTEXT_SIZE  ((NR_REGS + 3 + 1) * XLEN)

Context* __am_irq_handle(Context *c) {
  if (user_handler) {
    Event ev = {0};
    switch (c->mcause) {
      case 0xb: ev.event = EVENT_YIELD; break;
      case 0x8: ev.event = EVENT_SYSCALL; break;
      default: ev.event = EVENT_ERROR; break;
    }
    c->mepc += XLEN; 
    c = user_handler(ev, c);
    assert(c != NULL);
  }

  return c;
}

extern void __am_asm_trap(void);

bool cte_init(Context*(*handler)(Event, Context*)) {
  // initialize exception entry
  asm volatile("csrw mtvec, %0" : : "r"(__am_asm_trap));

  printf("NR_REGS %d; xlen %d\n", NR_REGS, XLEN);
  // register event handler
  user_handler = handler;

  return true;
}

Context *kcontext(Area kstack, void (*entry)(void *), void *arg) {
  if (entry == NULL) {
    printf("entry function is NULL!\n"); assert(0);
  }
  printf("NR_REGS %d\n", NR_REGS);
  Context* ctx = (Context*)(kstack.end - CONTEXT_SIZE);
  for (int i = 0; i < NR_REGS; i++) {
      ctx->gpr[i] = 0;
  }
  ctx->mcause = 0;
  ctx->mstatus = 0x1800;
  // set mepc to f
  ctx->mepc = (uintptr_t)entry;
  // x10 -> a0 储存参数
  ctx->gpr[10] = (uintptr_t)arg;
  return ctx;
}

void yield() {
 #ifdef __riscv_e
  asm volatile("li a5, -1; ecall");
 #else
  asm volatile("li a7, -1; ecall");
 #endif
}

bool ienabled() {
  return false;
}

void iset(bool enable) {
}
