import "DPI-C" function void stop();

module BlackBoxRealAdd(
	input [3:0] inst_ref,
	input [3:0] inst_type
);
  always @(*) begin
    if (inst_ref == inst_type) begin
      stop();
    end

  end
endmodule
