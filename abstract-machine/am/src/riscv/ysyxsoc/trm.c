#include <am.h>
#include <klib-macros.h>
#include <riscv/riscv.h>
#include "npc.h"

extern char _heap_start;
extern char _pmem_start;
int main(const char *args);

#define PMEM_SIZE 0x2000
#define PMEM_END  ((uintptr_t)&_pmem_start + PMEM_SIZE)

Area heap = RANGE(&_heap_start, PMEM_END);
#ifndef MAINARGS
#define MAINARGS ""
#endif
static const char mainargs[] = MAINARGS;

void putch(char ch) {
  outb(SERIAL_PORT, ch);
}

void halt(int code) {
  asm volatile("mv a0, %0; ebreak" : :"r"(code));
  while (1);
}

extern char _etext;
extern char _sdata;
extern char _edata;
extern char _bss_start;
extern char _bss_end;

void bootloader() {
  // zero out the bss section
  volatile char* bss_start = &_bss_start;
  volatile char* bss_end = &_bss_end;

  while (bss_start < bss_end) {
    *bss_start = 0;
    bss_start++;
  }

  // set initial value for data section
  char* src = &_etext;
  char* dst = &_sdata;

  while (dst < &_edata) {
    *dst = *src;
    dst++;
    src++;
  }

  
}

void _trm_init() {
  bootloader();
  int ret = main(mainargs);
  halt(ret);
}
