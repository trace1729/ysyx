module top(
   // for testing purposes
   rs1, rs2, x1, x2, x5, x6, x7, x8, x9, x10, readreg1, readreg2, writereg, writeEn, data,
   // Outputs
   inst, pc, 
   // Inputs
   clk, rst
   );
   input clk;
   input rst;

   /* control logic*/
   output		writeEn;
   wire		immSel;

   assign writeEn = 1;
   assign immSel = 1;

   /* Instruction Fetch */ 
   input [31:0] inst;
   output [31:0] pc;
   wire [31:0] b_pc;
   assign pc = b_pc + 32'h80000000;
 
   Reg		 #(32, 32'h00000000) r_pc (clk, rst, b_pc + 32'h00000004, b_pc, 1'b1);
   
   /* Instruction Fetch Ended */ 
   
   /* Instruction Decode */
   
   // regfile
   // Beginning of automatic wires (for undeclared instantiated-module outputs)
   /*AUTOWIRE*/
   
   // for testing purpose
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
   output [31:0]		data;
   
   assign readreg1 = inst[19:15];
   assign readreg2 = inst[24:20];
   assign writereg = inst[11:7];

   regfile regfile0(
	// output
	   rs1, rs2, x1, x2, x5, x6, x7, x8, x9, x10,
   // Inputs
	   clk, rst, readreg1, readreg2, writereg, data, writeEn
   );
   
	// imm gen
   wire [31:0]		sextimm;		// From immgen0 of immgen.v
   wire [11:0]		immediate;
   assign immediate = inst[31:20];
   immgen immgen0(/*AUTOINST*/
		  // Outputs
		  .sextimm		(sextimm[31:0]),
		  // Inputs
		  .immediate		(immediate[11:0]));
   
   /* Instruction Decode Ended*/

   /* Instruction Execute */
	
   alu alu0(/*AUTOINST*/
	    // Outputs
	    .res			(data[31:0]),
	    // Inputs
	    .rs1			(rs1[31:0]),
	    .rs2			(immSel? sextimm: rs2));
   /* Instruction Execute Ended*/
   
endmodule // top

