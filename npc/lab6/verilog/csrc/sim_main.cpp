// DESCRIPTION: Verilator: Verilog example module
//
// This file ONLY is placed under the Creative Commons Public Domain, for
// any use, without warranty, 2017 by Wilson Snyder.
// SPDX-License-Identifier: CC0-1.0
//======================================================================

// Include common routines
#include <nvboard.h>

// Include model header, generated from Verilating "top.v"
#include "Vtop.h"

static TOP_NAME dut;
void nvboard_bind_all_pins(Vtop* top);

void single_cycle()
{
	dut.clock = 0; dut.eval();
	dut.clock = 1; dut.eval();
}

void reset(int n)
{
  dut.reset = 1;
  while(n -- > 0) single_cycle();
  dut.reset = 0;
}

int main() {
  nvboard_bind_all_pins(&dut);
  nvboard_init();

  reset(10);

  while(1) {
    nvboard_update();
	single_cycle();
  }

  nvboard_quit();
}
