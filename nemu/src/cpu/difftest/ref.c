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

#include <isa.h>
#include <cpu/cpu.h>
#include <difftest-def.h>
#include <memory/paddr.h>

#define NR_GPR MUXDEF(CONFIG_RVE, 16, 32)
struct diff_context_t {
  word_t csr[NR_CSR];
  word_t gpr[NR_GPR];
  vaddr_t pc;
  uint32_t mode;
};

void diff_get_regs(void* dut) {
  struct diff_context_t* ctx = (struct diff_context_t*)dut;
  for (int i = 0; i < NR_GPR; i++) {
    ctx->gpr[i] = cpu.gpr[i];
  }
  ctx->pc = cpu.pc;
  //ctx->csr[MSTATUS] = cpu.csr[MSTATUS];
  //ctx->csr[MCAUSE] = cpu.csr[MCAUSE];
  //ctx->csr[MEPC] = cpu.csr[MEPC];
  //ctx->csr[MTVEC] = cpu.csr[MTVEC];

}

void diff_set_regs(void* dut) {
  struct diff_context_t* ctx = (struct diff_context_t*)dut;
  for (int i = 0; i < NR_GPR; i++) {
    cpu.gpr[i] = ctx->gpr[i];
  }
  cpu.pc = ctx->pc;
  //cpu.csr[MSTATUS] = ctx->csr[MSTATUS];
  //cpu.csr[MCAUSE] = ctx->csr[MCAUSE];
  //cpu.csr[MEPC] = ctx->csr[MEPC];
  //cpu.csr[MTVEC] = ctx->csr[MTVEC];
}
__EXPORT void difftest_memcpy(paddr_t addr, void *buf, size_t n, bool direction) {
  if (direction == DIFFTEST_TO_REF) {
    memcpy(guest_to_host(RESET_VECTOR), buf, n);
  } else {
    assert(0);
  }
}

__EXPORT void difftest_regcpy(void *dut, bool direction) {
  // 33 = 32 个寄存器 + 1个PC
  if (direction == DIFFTEST_TO_REF) {
    diff_set_regs(dut);
  } else if (direction == DIFFTEST_TO_DUT) {
    diff_get_regs(dut);
  }
}

__EXPORT void difftest_exec(uint64_t n) {
  cpu_exec(n);
}

__EXPORT void difftest_raise_intr(word_t NO) {
  assert(0);
}

__EXPORT void difftest_init(int port) {
  void init_mem();
  init_mem();
  /* Perform ISA dependent initialization. */
  init_isa();
}
