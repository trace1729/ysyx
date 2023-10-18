module top(
	input clk,
	input rst,
	input [8:0] sw,
	output [2:0] led,
	output reg [7:0] seg0
);

enc38 enc381(
	.in(sw[7:0]),
	.en(sw[8]),
	.out(led)
);

bcd7seg bcd7seg1(
	.in(led),
	.seg(seg0)
);

endmodule

module enc38(
	input [7:0] in,
	input en,
	output reg [2:0] out
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

module bcd7seg(
	input [2:0] in,
	output reg [7:0] seg
);

reg [7:0] tmp;

assign seg = ~tmp;

always @(*) begin
	casez (in)
		3'b000: tmp = 8'b11111101;
		3'b001: tmp = 8'b01100000;
		3'b010: tmp = 8'b11011010;
		3'b011: tmp = 8'b11110010;
		3'b100: tmp = 8'b01100110;
		3'b101: tmp = 8'b10110110;
		3'b110: tmp = 8'b10111110;
		3'b111: tmp = 8'b11100000;
		default:tmp = 8'b11111111;
	endcase
end

endmodule

