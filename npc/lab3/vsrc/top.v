module top(
	input clk,
	input rst,
	input [10:0] sw,
	output [6:0] led
);

alu alu1(
	// 选择信号
	.sel(sw[10:8]),
	// 两输入端口
	.a(sw[7:4]),
	.b(sw[3:0]),
	// zero flag
	.zf(led[6]),
	// carry flag
	.cf(led[5]),
	// overflow flag
	.of(led[4]),
	// res
	.res(led[3:0])
);

endmodule

module alu(
	input [2:0] sel,
	input [3:0] a,
	input [3:0] b,
	output reg zf,
	output reg cf,
	output reg of,
	output reg [3:0] res
);
	
	wire [3:0] results [7:0];
	wire zfs [7:0];
	wire cfs [7:0];
	wire ofs [7:0];

	my_add_sub add_sub1(.a(a), .b(b), .sub(0), .res(results[0]), .zf(zfs[0]), .cf(cfs[0]), .of(ofs[0]));
	my_add_sub add_sub2(.a(a), .b(b), .sub(1), .res(results[1]), .zf(zfs[1]), .cf(cfs[1]), .of(ofs[1]));
	my_revert revert   (.a(a), .b(b), .res(results[2]), .zf(zfs[2]), .cf(cfs[2]), .of(ofs[2]));
	my_and		and1   (.a(a), .b(b), .res(results[3]), .zf(zfs[3]), .cf(cfs[3]), .of(ofs[3]));
	my_or		or1    (.a(a), .b(b), .res(results[4]), .zf(zfs[4]), .cf(cfs[4]), .of(ofs[4]));
	my_xor		xor1   (.a(a), .b(b), .res(results[5]), .zf(zfs[5]), .cf(cfs[5]), .of(ofs[5]));
	my_cmp		cmp    (.a(a), .b(b), .res(results[6]), .zf(zfs[6]), .cf(cfs[6]), .of(ofs[6]));
	my_eq		eq     (.a(a), .b(b), .res(results[7]), .zf(zfs[7]), .cf(cfs[7]), .of(ofs[7]));

	always @(*) begin
		case (sel)
			3'b000: begin res = results[0]; zf = zfs[0]; cf = cfs[0]; of = ofs[0];  end
			3'b001: begin res = results[1]; zf = zfs[1]; cf = cfs[1]; of = ofs[1];  end
			3'b010: begin res = results[2]; zf = zfs[2]; cf = cfs[2]; of = ofs[2];  end
			3'b011: begin res = results[3]; zf = zfs[3]; cf = cfs[3]; of = ofs[3];  end
			3'b100: begin res = results[4]; zf = zfs[4]; cf = cfs[4]; of = ofs[4];  end
			3'b101: begin res = results[5]; zf = zfs[5]; cf = cfs[5]; of = ofs[5];  end
			3'b110: begin res = results[6]; zf = zfs[6]; cf = cfs[6]; of = ofs[6];  end
			3'b111: begin res = results[7]; zf = zfs[7]; cf = cfs[7]; of = ofs[7];  end
			default: begin res = 4'b0;  zf = 0; cf = 0; of = 0; end
		endcase
	end

endmodule

module my_add_sub(
	input [3:0] a,
	input [3:0] b,
	input sub, // whether sub or add
	output [3:0] res,
	output zf, 
	output cf, 
	output of
);
	wire [3:0] b_xor;
	assign b_xor = {4{ sub }}^b;
	assign {cf,res} = a + b_xor + {{3{1'b0}}, sub};
	assign of = (a[3] == b[3]) && (res [3] != a[3]);
	assign zf = ~(| res);
endmodule


module my_revert(
	input [3:0] a,
	input [3:0] b,
	output [3:0] res,
	output zf, 
	output cf, 
	output of
);
	assign res = ~a;
	assign cf = 1;
	assign zf = ~(| res);

endmodule

module my_and(
	input [3:0] a,
	input [3:0] b,
	output [3:0] res,
	output zf, 
	output cf, 
	output of 
);
	assign res = a & b;
	assign cf = 0;
	assign of = (a[3] == b[3]) && (res [3] != a[3]);
	assign zf = ~(| res);
endmodule

module my_or(
	input [3:0] a,
	input [3:0] b,
	output [3:0] res,
	output zf, 
	output cf, 
	output of 
);
	assign res = a | b;
	assign cf = 0;
	assign of = (a[3] == b[3]) && (res [3] != a[3]);
	assign zf = ~(| res);

endmodule

module my_xor(
	input [3:0] a,
	input [3:0] b,
	output [3:0] res,
	output zf, 
	output cf, 
	output of 
);
	assign res = a ^ b;
	assign cf = 0;
	assign of = (a[3] == b[3]) && (res [3] != a[3]);
	assign zf = ~(| res);

endmodule

module my_cmp(
	input [3:0] a,
	input [3:0] b,
	output [3:0] res,
	output zf, 
	output cf, 
	output of
);
	wire [3:0] tmp;
	my_add_sub add_sub3(.a(a), .b(b), .sub(1), .res(tmp), .zf(zf), .cf(cf), .of(of));
	assign res = {3'b000, tmp[3] ^ of};
	
endmodule

module my_eq(
	input [3:0] a,
	input [3:0] b,
	output [3:0] res,
	output zf, 
	output cf, 
	output of
);

	wire [3:0] tmp;
	my_add_sub add_sub4(.a(a), .b(b), .sub(1), .res(tmp), .zf(zf), .cf(cf), .of(of));
	assign res = {3'b000, zf};
	
endmodule


