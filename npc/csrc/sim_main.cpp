// DESCRIPTION: Verilator: Verilog example module
//
// This file ONLY is placed under the Creative Commons Public Domain, for
// any use, without warranty, 2017 by Wilson Snyder.
// SPDX-License-Identifier: CC0-1.0
//======================================================================

// Include common routines
#include <verilated.h>
#include "verilated_vcd_c.h"
#include <stdio.h>
#include <stdlib.h>
#include <assert.h>



// Include model header, generated from Verilating "top.v"
#include "Vtop.h"

int main(int argc, char** argv) {
    // See a similar example walkthrough in the verilator manpage.

    // This is intended to be a minimal example.  Before copying this to start a
    // real project, it is better to start with a more complete example,
    // e.g. examples/c_tracing.

    // Construct a VerilatedContext to hold simulation time, etc.
    VerilatedContext* contextp = new VerilatedContext;

    // Pass arguments so Verilated code can see them, e.g. $value$plusargs
    // This needs to be called before you create any model
    contextp->commandArgs(argc, argv);

    // Construct the Verilated model, from Vtop.h generated from Verilating "top.v"
    Vtop* top = new Vtop{contextp};

    VerilatedVcdC* tfp = new VerilatedVcdC; //初始化VCD对象指针
    contextp->traceEverOn(true); //打开追踪功能
    top->trace(tfp, 0); //
    tfp->open("wave.vcd"); //设置输出的文件wave.vcd

    // Simulate until $finish
    while (!contextp->gotFinish()) {

        // Evaluate model
		int a = rand() & 1;
		int b = rand() & 1;
		top->a = a;
		top->b = b;
        top->eval();
		printf("a = %d, b = %d, c = %d\n", a, b, top->f);

        tfp->dump(contextp->time()); // dump wave 
        contextp->timeInc(1);


		assert(top->f == (a^b));
    }

    // Final model cleanup
    top->final();

    // Destroy model
    delete top;
    tfp->close();
    delete contextp;
    // Return good completion status
    return 0;
}
