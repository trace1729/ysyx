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

seg u_bcd7seg(
	.in  	( q[3:0] ),
	.seg0 	( seg0  )
);

seg u_bcd7seg2(
	.in  	( q[7:4] ),
	.seg0 	( seg1  )
);

endmodule

module shift_reg(
	input clk,
	input reset,
	output reg [7:0] q
);
  reg [31:0] count;

	// 延时函数
	// 
  always @(posedge clk) begin
    if (reset) begin q <= 8'b1; count <= 0; end
    else begin
      if (count == 0) q <= {q[4]^q[3]^q[2]^q[0], q[7:1]};
      count <= (count >= 500000 ? 32'b0 : count + 1);
    end
	// count 延时
  end

endmodule

