module top(
	input clk,
	input rst,
	input sw,
	output [7:0] seg0,
	output [7:0] seg1
);

// outports wire
wire [7:0] 	q;

shift_reg u_shift_reg(
	.clk   	( clk ),
	.reset 	( sw  ),
	.q     	( q )
);

bcd7seg u_bcd7seg(
	.in  	( q[3:0] ),
	.seg 	( seg0  )
);

bcd7seg u_bcd7seg2(
	.in  	( q[7:4] ),
	.seg 	( seg1  )
);

endmodule

module shift_reg(
	input clk,
	input reset,
	output reg [7:0] q
);
  reg [31:0] count;

  always @(posedge clk) begin
    if (reset) begin q <= 8'b1; count <= 0; end
    else begin
      if (count == 0) q <= {q[4]^q[3]^q[2]^q[0], q[7:1]};
      count <= (count >= 500000 ? 32'b0 : count + 1);
    end
  end

endmodule

module bcd7seg(
	input [3:0] in,
	output reg [7:0] seg
);

reg [7:0] tmp;

assign seg = ~tmp;

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
		4'b1101: tmp = 8'b01111000;
		4'b1110: tmp = 8'b10011110;
		4'b1111: tmp = 8'b10001110;
		default:tmp = 8'b11111111;
	endcase
end

endmodule

