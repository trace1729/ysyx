module top(/*AUTOARG*/
   // Outputs
   inst,
   //input
   clk, rst
   );
   input clk;
   input rst;
   output [31:0] inst;
 
   wire [31:0]	 a_in1;
   wire [31:0]	 a_in2;
   wire [31:0]	 a_res;
  
   assign a_in2 = 32'h00000004;

   adder adder1(/*AUTOINST*/
		// Outputs
		.a_res			(a_res[31:0]),
		// Inputs
		.a_in1			(a_in1[31:0]),
		.a_in2			(a_in2[31:0]));
   
   Reg		 #(32, 32'h80000000) pc (clk, rst, a_res, a_in1, 1'b1);
   
   assign inst = a_res;
   
   
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
