#include <common.h>
#include <isa.h>

void verilator_exec_once(Decode* s);


int isa_exec_once(struct Decode *s) {
    verilator_exec_once(s);

    // if (top->clock) {
    //   printf("ra, sp, t0, t1, t2, s0, s1, a0,\
    //   rs1, rs2, \
    //   readreg1, readreg2, writereg, writeEn, data\n \
    //   %x %x %x %x %x %x %x %x\n", 
    //   top->x1, top->x2, top->x5, top->x6, top->x7, top->x8, top->x9, top->x10);
    //   // top->rs1, top->rs2, 
    //   // top->readreg1, top->readreg2, top->writereg, top->writeEn, top->data 
    // }
    //
    // tfp->dump(contextp->time());
    
    return 0;
}