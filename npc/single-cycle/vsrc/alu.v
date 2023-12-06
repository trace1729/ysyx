module alu(/*AUTOARG*/
   // Outputs
   res,
   // Inputs
   rs1, rs2
   );
   input [31:0] rs1;
   input [31:0] rs2;
   output [31:0] res;

   assign res = rs1 + rs2;

   endmodule
   
