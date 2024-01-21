import "DPI-C" function void Dpi_itrace(input int pc, input int inst, input int nextpc);

module Dpi_itrace(
  input [31: 0] pc,
	input [31: 0] inst,
  input [31: 0] nextpc
);
  always @(*) begin
    Dpi_itrace(pc, inst, nextpc);
  end
endmodule

