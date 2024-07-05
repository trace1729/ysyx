#include <common.h>
#include <stdint.h>
#include <stdio.h>
#include "am.h"
#include "syscall.h"

#define strace

char syscalls[][100] = {
  [SYS_brk] = "SYS_brk",
  [SYS_kill] = "SYS_kill",
  [SYS_exit] = "SYS_exit",
  [SYS_write] = "SYS_write",
};

int sys_yield()
{
  yield();
  return 0;
}

int sys_exit(int status) 
{
  halt(status);
  return 0;
}

int sys_write(uintptr_t fd, uintptr_t buf, uintptr_t count) {
  if (fd != 1 && fd != 2)  {
    return -1;
  }
  for (int i = 0; i < count; i++) {
    putch(((uint8_t*)buf)[i]);
  }
  return count;
}

int sys_brk(uintptr_t addr) {
  return 0;
}

void do_syscall(Context *c) {
  uintptr_t a[4];
  a[0] = c->GPR1;
  a[1] = c->GPR2;
  a[2] = c->GPR3;
  a[3] = c->GPR4;

  int result_code = 0;


  switch (a[0]) {
    case SYS_yield: result_code = sys_yield(); break;
    case SYS_exit: result_code = sys_exit(a[1]); break;
    case SYS_write: result_code = sys_write(a[1], a[2], a[3]); break;
    case SYS_brk: result_code = sys_brk(a[1]); break;
    default: panic("Unhandled syscall ID = %d", a[0]);
  }

  c->GPRx = result_code;
#ifdef strace
  printf("%s(%d, %d, %d) = %d\n", syscalls[a[0]], a[1], a[2], a[3], result_code);
#endif
}


