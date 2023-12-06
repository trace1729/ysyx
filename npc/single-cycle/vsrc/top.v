module top(
   // for testing purposes
   x1, x2, x3, x4, x5,
   // Outputs
   inst, pc, 
   // Inputs
   clk, rst
   );
   input clk;
   input rst;

   /* control logic*/
   wire		writeEn;
   wire		immSel;

   assign writeEn = 1;
   assign immSel = 1;

   /* Instruction Fetch */ 
   output [31:0] inst;
   output [31:0] pc;
 
   wire [31:0]	 a_in1;
   wire [31:0]	 a_in2;
   wire [31:0]	 a_res;
  
   // reg 的输出作为加法器的一个输入
   assign pc = a_in1;

   // rst1 的作用是 将 rst 的变化延缓一个时钟周期
   // 保证在 rst 为 0的时候，读取到的第一个信号是 
   // 0x80000000
   Reg #(4, 4'h0) rst1 (clk, 0, rst ? 0: 4'h4, a_in2[3:0], 1'b1);

   adder adder1(/*AUTOINST*/
		// Outputs
		.a_res			(a_res[31:0]),
		// Inputs
		.a_in1			(a_in1[31:0]),
		.a_in2			(a_in2[31:0]));
   
   Reg		 #(32, 32'h80000000) r_pc (clk, rst, a_res, a_in1, 1'b1);
   
   /* Instruction Fetch Ended */ 
   
   /* Instruction Decode */
   
   // regfile
   // Beginning of automatic wires (for undeclared instantiated-module outputs)
   /*AUTOWIRE*/
   
   // for testing purpose
   output [31:0]		x1;			// From regfile0 of regfile.v
   output [31:0]		x2;			// From regfile0 of regfile.v
   output [31:0]		x3;			// From regfile0 of regfile.v
   output [31:0]		x4;			// From regfile0 of regfile.v
   output [31:0]		x5;			// From regfile0 of regfile.v
   // for testing purpose
   
   wire [31:0]		rs1;			// From regfile0 of regfile.v
   wire [31:0]		rs2;			// From regfile0 of regfile.v
   // End of automatics

   // for inputs
   wire [4:0]		readreg1;
   wire [4:0]		readreg2;
   wire [4:0]		writereg;
   wire [31:0]		data;
   
   assign readreg1 = inst[19:15];
   assign readreg2 = inst[24:20];
   assign writereg = inst[11:7];
   assign data = res;

   regfile regfile0(/*AUTOINST*/
		    // Outputs
		    .rs1		(rs1[31:0]),
		    .rs2		(rs2[31:0]),
		    .x1			(x1[31:0]),
		    .x2			(x2[31:0]),
		    .x3			(x3[31:0]),
		    .x4			(x4[31:0]),
		    .x5			(x5[31:0]),
		    // Inputs
		    .clk		(clk),
		    .rst		(rst),
		    .readreg1		(readreg1[4:0]),
		    .readreg2		(readreg2[4:0]),
		    .writereg		(writereg[4:0]),
		    .data		(data[31:0]),
		    .writeEn		(writeEn));

   
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
	// alu
   wire [31:0]		res;			// From alu0 of alu.v
   alu alu0(/*AUTOINST*/
	    // Outputs
	    .res			(res[31:0]),
	    // Inputs
	    .rs1			(rs1[31:0]),
	    .rs2			(immSel? sextimm: rs2));
   /* Instruction Execute Ended*/
   
endmodule // top

module adder(/*AUTOARG*/
   // Outputs
   a_res,
   // Inputs
   a_in1, a_in2
   );
   input [31:0] a_in1;
   input [31:0]	a_in2;
   output [31:0] a_res;
   
   assign a_res = a_in1 + a_in2;
endmodule // adder
