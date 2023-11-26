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

typedef struct diff_context_t {
  word_t gpr[MUXDEF(CONFIG_RVE, 16, 32)];
  word_t pc;
} diff_ctx;

bool isa_difftest_checkregs(CPU_state *ref_r, vaddr_t pc) {
  diff_ctx ctx;
  int i;
  ref_difftest_regcpy(&ctx, DIFFTEST_TO_DUT);
  for (i = 0; i < 32; i++) {
    if (ctx.gpr[i] != ref_r->gpr[i]) {
      break;
    }
  }
  if (i != 32) {
    difftest_check_reg("", pc, ctx.gpr[i], ref_r->gpr[i]);
  }
  if (ctx.pc != ref_r->pc) {
    difftest_check_reg("pc", pc, ctx.pc, ref_r->pc);
  }
  return true;
}

void isa_difftest_attach() {
}
