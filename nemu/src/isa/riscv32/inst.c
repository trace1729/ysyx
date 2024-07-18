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

#include "common.h"
#include "isa.h"
#include "local-include/reg.h"
#include "macro.h"
#include <cpu/cpu.h>
#include <cpu/ifetch.h>
#include <cpu/decode.h>
#include <stdint.h>
#include <stdio.h>

#define R(i) gpr(i)
#define Mr vaddr_read
#define Mw vaddr_write

enum {
  TYPE_I, TYPE_IS, TYPE_U, TYPE_S, TYPE_J, TYPE_R, TYPE_B, 
  TYPE_N // none
};

#define src1R() do { *src1 = R(rs1); } while (0)
#define src2R() do { *src2 = R(rs2); } while (0)
#define immI() do { *imm = SEXT(BITS(i, 31, 20), 12); } while(0)
#define immIS()do { *imm = BITS(i, 25, 20); } while(0)
#define immU() do { *imm = SEXT(BITS(i, 31, 12), 20) << 12; } while(0)
#define immS() do { *imm = (SEXT(BITS(i, 31, 25), 7) << 5) | BITS(i, 11, 7); } while(0)
#define immJ() do { *imm = SEXT((BITS(i, 31, 31) << 19 | BITS(i, 19, 12) << 11 | BITS(i, 20, 20) << 10 | BITS(i, 30, 21)), 20) << 1;} while(0)
#define immB() do {*imm = SEXT((BITS(i, 31, 31) << 11 | BITS(i, 7, 7) << 10 | BITS(i, 30, 25) << 4 | BITS(i, 11, 8)), 12) << 1;} while(0)

#ifdef CONFIG_FTRACE
#define FTRACE(r, t, s, i) ,ftrace(r, t, s, i)
#else
#define FTRACE(r, t, s, i)
#endif

#if CONFIG_FTRACE
#define JAL 1
#define RA 1
#define ZERO 0
#define JALR 0

int depth = 0;
void get_function_symbol_by_address(uint32_t addr, char *buf);
void ftrace(int rd, int type, Decode* s, word_t src1) {
  char function[128];

  //      jalr          x0              x1
  if (type == JALR && rd == ZERO && src1 == R(RA)) {
    // if register is x0, and instructio type is jalr
    // then it is function return
    // return from instead of return to
    get_function_symbol_by_address(s->pc, function);
  /* if (memcmp(function, "printf", strlen(function)) == 0 */
  /*     || memcmp(function, "putch", strlen(function)) == 0  */
  /*     || memcmp(function, "sprintf", strlen(function)) == 0  */
  /*     || memcmp(function, "memcpy", strlen(function)) == 0  */
  /*     || memcmp(function, "vprintf", strlen(function)) == 0  */
  /*     || memcmp(function, "vsprintf", strlen(function)) == 0  */
  /*   ) { */
  /*   return; */
  /* } */
    printf("0x%x: ", s->pc);
    for (int i = 0; i < depth; i++) printf(" ");
    printf("ret[%s]\n", function);
    depth--;

  //              jal(r)          ra   ;       jalr            x0      x*(!=x1)  
  } else if ((type == JAL && rd == RA) || (type == JALR && rd == RA)) {
    // if register is ra, and instruction type is jal(r)
    // then it is function call
    get_function_symbol_by_address(s->dnpc, function);
  /* if (memcmp(function, "printf", strlen(function)) == 0 */
  /*     || memcmp(function, "putch", strlen(function)) == 0  */
  /*     || memcmp(function, "sprintf", strlen(function)) == 0  */
  /*     || memcmp(function, "memcpy", strlen(function)) == 0  */
  /*     || memcmp(function, "vprintf", strlen(function)) == 0  */
  /*     || memcmp(function, "vsprintf", strlen(function)) == 0  */
  /*   ) { */
  /*   return; */
  /* } */
    printf("0x%x: ", s->pc);
    for (int i = 0; i < depth; i++) printf(" ");
    printf("call[%s@0x%x]\n", function, s->dnpc);
    depth++;
  }
}
#endif


static void decode_operand(Decode *s, int *rd, word_t *src1, word_t *src2, word_t *imm, int type) {
  uint32_t i = s->isa.inst.val;
  int rs1 = BITS(i, 19, 15);
  int rs2 = BITS(i, 24, 20);
  *rd     = BITS(i, 11, 7);
  switch (type) {
    case TYPE_I: src1R();          immI(); break;
    case TYPE_IS: src1R();          immIS(); break;
    case TYPE_U:                   immU(); break;
    case TYPE_S: src1R(); src2R(); immS(); break;
    case TYPE_J:                   immJ(); break; // not tested
    case TYPE_R: src1R(); src2R();         break;
    case TYPE_B: src1R(); src2R(); immB(); break;
  }
}

static void csrrs(word_t no, word_t src1, word_t rd) {
  word_t t = cpu.csr[no];
  cpu.csr[no] = t | src1;
  R(rd) = t;
}

static void csrrw(word_t no, word_t src1, word_t rd) {
  word_t t = cpu.csr[no];
  cpu.csr[no] = src1;
  R(rd) = t;
}

static int decode_exec(Decode *s) {
  int rd = 0;
  word_t src1 = 0, src2 = 0, imm = 0;
  s->dnpc = s->snpc;

#define INSTPAT_INST(s) ((s)->isa.inst.val)
  //                     name agrumnet serves for annotation only
#define INSTPAT_MATCH(s, name, type, ... /* execute body */ ) { \
  decode_operand(s, &rd, &src1, &src2, &imm, concat(TYPE_, type)); \
  __VA_ARGS__ ; \
}

  INSTPAT_START();
  // R type
  // 不知道如何处理这个 signed 和 unsigned 问题
  INSTPAT("0000000 ????? ????? 000 ????? 01100 11", add    , R, R(rd) = (int32_t)src1 + (int32_t)src2);
  INSTPAT("0000000 ????? ????? 001 ????? 01100 11", sll    , R, R(rd) = src1 << (src2 & BITMASK(5)));
  INSTPAT("0000000 ????? ????? 010 ????? 01100 11", slt    , R, R(rd) = (int32_t)src1 < (int32_t)src2); // set less than unsigned
  INSTPAT("0000000 ????? ????? 011 ????? 01100 11", sltu    ,R, R(rd) = src1 < src2); // set less than unsigned
  INSTPAT("0000000 ????? ????? 100 ????? 01100 11", xor     ,R, R(rd) = src1 ^ src2);
  INSTPAT("0000000 ????? ????? 101 ????? 01100 11", srl     ,R, R(rd) = src1 >> (src2 & BITMASK(5)));
  INSTPAT("0000000 ????? ????? 110 ????? 01100 11", or      ,R, R(rd) = src1 | src2);
  INSTPAT("0000000 ????? ????? 111 ????? 01100 11", and     ,R, R(rd) = src1 & src2);
  INSTPAT("0000001 ????? ????? 000 ????? 01100 11", mul     ,R, R(rd) = ((int64_t)(SEXT(src1, 32) * SEXT(src2, 32)) & BITMASK(32)));
  INSTPAT("0000001 ????? ????? 001 ????? 01100 11", mulh    ,R, R(rd) = ((int64_t)(SEXT(src1, 32) * SEXT(src2, 32)) >> 32)) ;
  INSTPAT("0000001 ????? ????? 011 ????? 01100 11", mulhu    ,R, R(rd) = ((uint64_t)src1 * src2 >> 32)) ;
  INSTPAT("0000001 ????? ????? 100 ????? 01100 11", div     ,R, R(rd) = (int32_t)src1 / (int32_t)src2);
  INSTPAT("0000001 ????? ????? 101 ????? 01100 11", divu    ,R, R(rd) = src1 / src2);
  INSTPAT("0000001 ????? ????? 110 ????? 01100 11", rem     ,R, R(rd) = (int32_t)src1 % (int32_t)src2);
  INSTPAT("0000001 ????? ????? 111 ????? 01100 11", remu    ,R, R(rd) = src1 % src2);
  INSTPAT("0100000 ????? ????? 000 ????? 01100 11", sub     ,R, R(rd) = (int32_t)src1 - (int32_t)src2);
  INSTPAT("0100000 ????? ????? 101 ????? 01100 11", sra     ,R, R(rd) = (int32_t)src1 >> (src2 & BITMASK(5)));

  // U type
  INSTPAT("??????? ????? ????? ??? ????? 00101 11", auipc  , U, R(rd) = s->pc + imm);
  INSTPAT("??????? ????? ????? ??? ????? 01101 11", lui    , U, R(rd) = imm);

  // I type
  INSTPAT("??????? ????? ????? 000 ????? 00000 11", lb    , I, R(rd) = SEXT(Mr(src1 + imm, 1), 8));
  INSTPAT("??????? ????? ????? 001 ????? 00000 11", lh    , I, R(rd) = SEXT(Mr(src1 + imm, 2), 16));
  // intent to crash
  // INSTPAT("??????? ????? ????? 010 ????? 00000 11", lw     , I, R(rd) = SEXT(Mr(0, 4), 32));
  INSTPAT("??????? ????? ????? 010 ????? 00000 11", lw     , I, R(rd) = SEXT(Mr(src1 + imm, 4), 32));
  INSTPAT("??????? ????? ????? 100 ????? 00000 11", lbu    , I, R(rd) = Mr(src1 + imm, 1));
  INSTPAT("??????? ????? ????? 101 ????? 00000 11", lhu    , I, R(rd) = Mr(src1 + imm, 2));
  INSTPAT("??????? ????? ????? 000 ????? 00100 11", addi   , I, R(rd) = src1 + imm);
  INSTPAT("??????? ????? ????? 010 ????? 00100 11", slti  , I, R(rd) = (int32_t)src1 < (int32_t)imm); // set less than immdiate
  INSTPAT("??????? ????? ????? 011 ????? 00100 11", sltiu  , I, R(rd) = src1 < imm); // set less than immdiate
  INSTPAT("??????? ????? ????? 100 ????? 00100 11", xori   , I, R(rd) = src1 ^ imm);
  INSTPAT("??????? ????? ????? 110 ????? 00100 11", ori    , I, R(rd) = src1 | imm);
  INSTPAT("??????? ????? ????? 111 ????? 00100 11", andi   , I, R(rd) = src1 & imm);

  INSTPAT("??????? ????? ????? 000 ????? 11001 11", jalr   , I, R(rd) = s->pc + 4, s->dnpc = src1 + imm FTRACE(rd, JALR, s,src1));

  // control and status registers 
  INSTPAT("??????? ????? ????? 010 ????? 11100 11", csrrs   , I, csrrs(BITS(imm, 3, 0), src1, rd));
  INSTPAT("??????? ????? ????? 001 ????? 11100 11", csrrw   , I, csrrw(BITS(imm, 3, 0), src1, rd));
  

  // IS type
  INSTPAT("0000000 ????? ????? 001 ????? 00100 11", slli   , IS, R(rd) = src1 << imm); // 
  INSTPAT("0000000 ????? ????? 101 ????? 00100 11", srli   , IS, R(rd) = src1 >> imm); // 
  INSTPAT("0100000 ????? ????? 101 ????? 00100 11", srai   , IS, R(rd) = (int32_t)src1 >> imm); // 

  // S type
  INSTPAT("??????? ????? ????? 000 ????? 01000 11", sb     , S, Mw(src1 + imm, 1, src2));
  INSTPAT("??????? ????? ????? 001 ????? 01000 11", sh     , S, Mw(src1 + imm, 2, src2));
  INSTPAT("??????? ????? ????? 010 ????? 01000 11", sw     , S, Mw(src1 + imm, 4, src2));
  
  // B type
  INSTPAT("??????? ????? ????? 000 ????? 11000 11", beq     , B, if (src1 == src2) {s->dnpc = s->pc + imm;});
  INSTPAT("??????? ????? ????? 001 ????? 11000 11", bne     , B, if (src1 != src2) {s->dnpc = s->pc + imm;});
  INSTPAT("??????? ????? ????? 101 ????? 11000 11", bge     , B, if ((int32_t)src1 >= (int32_t)src2) {s->dnpc = s->pc + imm;});
  INSTPAT("??????? ????? ????? 100 ????? 11000 11", blt     , B, if ((int32_t)src1 < (int32_t)src2) {s->dnpc = s->pc + imm;});
  INSTPAT("??????? ????? ????? 110 ????? 11000 11", bltu     , B, if (src1 < src2) {s->dnpc = s->pc + imm;});
  INSTPAT("??????? ????? ????? 111 ????? 11000 11", bgeu     , B, if (src1 >= src2) {s->dnpc = s->pc + imm;});

  // J type
  INSTPAT("??????? ????? ????? ??? ????? 11011 11", jal    , J, R(rd) = s->pc + 4, s->dnpc = s->pc + imm FTRACE(rd, JAL, s, 0));

  // N type 
  INSTPAT("0011000 00010 00000 000 00000 11100 11", mret   ,  N, s->dnpc = cpu.csr[MEPC]); 
#ifdef __riscv_e
  // 这个宏定义没啥用，因为在构建NEMU 的时候是没有 __riscv_e 的
  // 之后注意下
  INSTPAT("0000000 00000 00000 000 00000 11100 11", ecall  ,  N, s->dnpc = isa_raise_intr(R(15), s->pc));
#else
  INSTPAT("0000000 00000 00000 000 00000 11100 11", ecall  ,  N, s->dnpc = isa_raise_intr(R(17), s->pc)); 
#endif
  INSTPAT("0000000 00001 00000 000 00000 11100 11", ebreak ,  N, NEMUTRAP(s->pc, R(10))); // R(10) is $a0
  INSTPAT("??????? ????? ????? ??? ????? ????? ??", inv    ,  N, INV(s->pc));

  INSTPAT_END();

  R(0) = 0; // reset $zero to 0

  return 0;
}

int isa_exec_once(Decode *s) {
  s->isa.inst.val = inst_fetch(&s->snpc, 4);
  return decode_exec(s);
}
