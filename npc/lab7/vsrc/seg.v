module seg(
	input rst,
	input [3:0] in,
	output reg [7:0] seg0
);

reg [7:0] tmp;
reg [7:0] tmp1;

assign tmp1 = tmp & {8{~rst}};
assign seg0 = ~tmp1;


always @(*) begin
	casez (in)
		4'b0000: tmp = 8'b11111101;
		4'b0001: tmp = 8'b01100000;
		4'b0010: tmp = 8'b11011010;
		4'b0011: tmp = 8'b11110010;
		4'b0100: tmp = 8'b01100110;
		4'b0101: tmp = 8'b10110110;
		4'b0110: tmp = 8'b10111110;
		4'b0111: tmp = 8'b11100000;
		4'b1000: tmp = 8'b11111110;
		4'b1001: tmp = 8'b11100110;
		4'b1010: tmp = 8'b11101111;
		4'b1011: tmp = 8'b00111110;
		4'b1100: tmp = 8'b10011100;
		4'b1101: tmp = 8'b01111010;
		4'b1110: tmp = 8'b10011110;
		4'b1111: tmp = 8'b10001110;
		default: tmp = 8'b00000000;
	endcase
end

endmodule
