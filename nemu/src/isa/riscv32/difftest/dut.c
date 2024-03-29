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
#include <cpu/difftest.h>
#include "../local-include/reg.h"
#include "difftest-def.h"

bool isa_difftest_checkregs(CPU_state *ref_r, vaddr_t pc) {
  unsigned i = 0, n = MUXDEF(CONFIG_RVE, 16, 32); 
  for (; i < n; i++) {
    if (gpr(i) != ref_r->gpr[i]) {
      break;
    }
  }
  if (i != n) {
    return difftest_check_reg(reg_name(i), pc, ref_r->gpr[i], gpr(i));
  }
  if (cpu.csr[MEPC] != ref_r->csr[MEPC]) {
    return difftest_check_reg("mepc", pc, ref_r->csr[MEPC], cpu.csr[MEPC]);
  }

  if (cpu.csr[MCAUSE] != ref_r->csr[MCAUSE]) {
    return difftest_check_reg("mcause", pc, ref_r->csr[MCAUSE], cpu.csr[MCAUSE]);
  }
  
  return true;
}

void isa_difftest_attach() {
}
