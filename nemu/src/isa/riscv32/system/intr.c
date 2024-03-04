/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include "debug.h"
#include <isa.h>
#ifndef CONFIG_TARGET_AM
#include <string.h>
#endif

word_t isa_raise_intr(word_t NO, vaddr_t epc) {
  /* TODO: Trigger an interrupt/exception with ``NO''.
   * Then return the address of the interrupt/exception vector.
   */
  if (NO == -1) {
    cpu.csr[MEPC] = epc;
    cpu.csr[MCAUSE] = 0xb;
  }
#if CONFIG_ETRACE
  char exception[128];
  switch (NO) {
    case -1: strcpy(exception, "YIELD"); break;
    default: break;
  }
  Log("%s exception occurs\n", exception);
#endif
  return cpu.csr[MTVEC];
}

word_t isa_query_intr() {
  return INTR_EMPTY;
}
