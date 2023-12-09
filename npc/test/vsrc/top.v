module top(/*AUTOARG*/
	// output
   cnt, rs1, rs2, x1, x2, x5, x6, x7, x8, x9, x10, readreg1, readreg2, writereg, writeEn,
	// intput
	clk, inst, data
   );

   input clk;
   input [15:0] inst;
   input [31:0] data;
   output [7:0] cnt;
   output [31:0] rs1;
   output [31:0] rs2;
   // for testing purpose
   output [31:0] x1;
   output [31:0] x2;
   output [31:0] x5;
   output [31:0] x6;
   output [31:0] x7;
   output [31:0] x8;
   output [31:0] x9;
   output [31:0] x10;

   output [4:0] readreg1;
   output [4:0] readreg2;
   output [4:0] writereg;
   output       writeEn;

   assign readreg1 = inst[4:0];
   assign readreg2 = inst[9:5];
   assign writereg = inst[14:10];
   assign writeEn  = inst[15];

	Reg #(8, 0) i0 (clk, 0, cnt + 8'h01, cnt, 1'b1);

   regfile regfile0(
	// output
	   rs1, rs2, x1, x2, x5, x6, x7, x8, x9, x10,
   // Inputs
	   clk, 0, readreg1, readreg2, writereg, data, writeEn
   );

endmodule
