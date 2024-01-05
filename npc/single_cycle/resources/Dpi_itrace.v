import "DPI-C" function void Dpi_itrace(input int pc, input int inst);

module Dpi_itrace(
  input [31: 0] pc,
	input [31: 0] inst
);
  always @(*) begin
    Dpi_itrace(pc, inst);
  end
endmodule

