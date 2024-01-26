module Dpi_ftrace(
  input [3:0] optype,
	input [3:0] ref_jal,
  input [3:0] ref_jalr,
  input [4:0] rd,
  input [31:0] src1
);
  import "DPI-C" function void Dpi_ftrace(input byte optype, input byte rd, input int src1);
  `define JAL 1
  `define JALR 0
  always @(*) begin
    if (optype == ref_jal) begin
      Dpi_ftrace(8'h1, {{3{1'b0}}, rd}, src1);
    end
    if (optype == ref_jalr) begin
      Dpi_ftrace(8'h0, {{3{1'b0}}, rd}, src1);
    end
    
  end
endmodule

