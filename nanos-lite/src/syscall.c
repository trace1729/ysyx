#include <common.h>
#include <stdint.h>
#include <stdio.h>
#include "syscall.h"
#include "klib-macros.h"
#include <fs.h>

// #define strace

char syscalls[][100] = {
  [SYS_brk] = "SYS_brk",
  [SYS_kill] = "SYS_kill",
  [SYS_exit] = "SYS_exit",
  [SYS_yield] = "SYS_yield",
  [SYS_open] =  "SYS_open",
  [SYS_read] = "SYS_read", 
  [SYS_write] = "SYS_write",
  [SYS_close] = "SYS_close",
  [SYS_lseek] = "SYS_lseek",
  [SYS_gettimeofday] = "SYS_gettimeofday"
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
  int len = fs_write(fd, (const void *)buf, count);
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

int rtc(uintptr_t tv, uintptr_t tz) {
  struct timeval* tval = (struct timeval*) tv;
  unsigned long usec = io_read(AM_TIMER_UPTIME).us;
  tval->tv_sec = usec / 1000000;
  tval->tv_usec = usec;
  return 0;
}

void do_syscall(Context *c) {
  uintptr_t a[4];
  a[0] = c->GPR1;
  a[1] = c->GPR2;
  a[2] = c->GPR3;
  a[3] = c->GPR4;

  int result_code = 0;
#ifdef strace
  bool has_fd = false;
  // determine whether we need to parse fd.
  switch (a[0]) {
    case SYS_read: case SYS_write: case SYS_close: has_fd = true;
  }
#endif



  switch (a[0]) {
    case SYS_yield: result_code = sys_yield(); break;
    case SYS_exit:  sys_exit(a[1]); break;
    case SYS_brk: result_code = sys_brk(a[1]); break;

    case SYS_read: result_code = sys_read(a[1], a[2], a[3]); break;
    case SYS_write: result_code = sys_write(a[1], a[2], a[3]); break;

    case SYS_open: result_code = fs_open((const char*)a[1], a[2], a[3]); break;
    case SYS_close: result_code = fs_close(a[1]); break;

    case SYS_lseek: result_code = fs_lseek(a[1], a[2], a[3]); break;
    case SYS_gettimeofday: result_code = rtc(a[1], a[2]); break;
    default: panic("Unhandled syscall ID = %d", a[0]);
  }

  c->GPRx = result_code;
#ifdef strace
    printf("%s(%d, %p, %d) = %d\n", syscalls[a[0]], a[1], (char*)a[2], a[3], result_code);
    if (has_fd) {
      printf("operaing on file [%s]\n", get_filename_by_fd(a[1]));
    }
#endif
}


