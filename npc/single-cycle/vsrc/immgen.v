module immgen(/*AUTOARG*/
   // Outputs
   sextimm,
   // Inputs
   immediate
   );
   input [11:0] immediate;
   output [31:0] sextimm;

   assign sextimm = { {20{immediate[11]}}, immediate };
   
   endmodule
