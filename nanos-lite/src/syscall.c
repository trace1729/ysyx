#include <common.h>
#include <stdint.h>
#include <stdio.h>
#include "am.h"
#include "syscall.h"
#include <fs.h>

#define strace

char syscalls[][100] = {
  [SYS_brk] = "SYS_brk",
  [SYS_kill] = "SYS_kill",
  [SYS_exit] = "SYS_exit",
  [SYS_yield] = "SYS_yield",
  [SYS_open] =  "SYS_open",
  [SYS_read] = "SYS_read", 
  [SYS_write] = "SYS_write",
  [SYS_close] = "SYS_close",
  [SYS_lseek] = "SYS_lseek" 
};


int sys_yield()
{
  yield();
  return 0;
}

int sys_exit(int status) 
{
#ifdef strace
    printf("%s(%d)\n", syscalls[SYS_exit], status);
#endif
  halt(status);
  while(1);
  return -1;
}

int sys_write(uintptr_t fd, uintptr_t buf, uintptr_t count) {
  int len = count;
  if (fd < 3)  {
    for (int i = 0; i < count; i++) {
      putch(((uint8_t*)buf)[i]);
    }
    return len;
  }
  len = fs_write(fd, (const void *)buf, count);
  return len;
}

int sys_read(uintptr_t fd, uintptr_t buf, uintptr_t count) {
  if (fd < 3) {
    assert(0);
  }
  return fs_read(fd, (char *)buf, count);
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
    case SYS_exit:  sys_exit(a[1]); break;
    case SYS_brk: result_code = sys_brk(a[1]); break;

    case SYS_read: result_code = sys_read(a[1], a[2], a[3]); break;
    case SYS_write: result_code = sys_write(a[1], a[2], a[3]); break;

    case SYS_open: result_code = fs_open((const char*)a[1], a[2], a[3]); break;
    case SYS_close: result_code = fs_close(a[1]); break;

    case SYS_lseek: result_code = fs_lseek(a[1], a[2], a[3]); break;
    default: panic("Unhandled syscall ID = %d", a[0]);
  }

  c->GPRx = result_code;
#ifdef strace
    printf("%s(%d, %p, %d) = %d\n", syscalls[a[0]], a[1], (char*)a[2], a[3], result_code);
#endif
}


