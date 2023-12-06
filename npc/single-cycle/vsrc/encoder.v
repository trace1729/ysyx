module encoder(
	input [4:0] in,
	input en,
	output reg [31:0] out
);

always @(*) begin
	if (en) begin
	casez (in)
		8'bzzzzzzz1: out = 3'b000;
		8'bzzzzzz10: out = 3'b001;
		8'bzzzzz100: out = 3'b010;
		8'bzzzz1000: out = 3'b011;
		8'bzzz10000: out = 3'b100;
		8'bzz100000: out = 3'b101;
		8'bz1000000: out = 3'b110;
		8'b10000000: out = 3'b111;
		default: out = 3'b000;
	endcase
	end
	else begin
		out = 3'b000;
	end
end

endmodule
