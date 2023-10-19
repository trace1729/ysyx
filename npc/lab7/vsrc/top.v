module top
(
	input clk,
	input rst,
	input ps2_clk,
	input ps2_data,
	output [7:0] seg0,
	output [7:0] seg1,
	output [7:0] seg2,
	output [7:0] seg3,
	output [7:0] seg4,
	output [7:0] seg5,
	output [7:0] seg6
);

ps2_keyboard keyboard(
	.clk(clk),
	.resetn(~rst),
	.ps2_clk(ps2_clk),
	.ps2_data(ps2_data),
	.seg0(seg0),
	.seg1(seg1),
	.seg2(seg2),
	.seg3(seg3)
);

endmodule


