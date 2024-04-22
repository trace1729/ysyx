#include <common.h>
#include "am.h"
#include "syscall.h"

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
    default: panic("Unhandled syscall ID = %d", a[0]);
  }

  c->GPRx = result_code;
}


