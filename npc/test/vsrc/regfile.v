module regfile(/*AUTOARG*/
   // Outputs
   rs1, rs2, x1, x2, x5, x6, x7, x8, x9, x10,
   // Inputs
   clk, rst, readreg1, readreg2, writereg, data, writeEn
   );
   input clk;
   input rst;
   input [4:0] readreg1;
   input [4:0] readreg2;
   input [4:0] writereg;
   input [31:0]	data;
   input       writeEn;
   output [31:0] rs1;
   output [31:0] rs2;
   // for testing purpose
   output [31:0] x1;
   output [31:0] x2;
   output [31:0] x5;
   output [31:0] x6;
   output [31:0] x7;
   output [31:0] x8;
   output [31:0] x9;
   output [31:0] x10;

   // 需要重复下面3行 32 次
   // 有没有更好的办法？
   // 使用 verilog 的 generate 函数
   parameter	 REGNUM = 32;
   parameter	 WIDTH = 32;
   parameter	 KEY_LEN = 5;
   
   // 寄存器选择
   wire [WIDTH - 1:0] reg_outs [REGNUM - 1:0];
   wire [WIDTH - 1:0] signal [REGNUM - 1:0];
   // 设置使能信号
   wire  [31:0] writeEnables;

	assign x1 = reg_outs[1];
	assign x2 = reg_outs[2];
	assign x5 = reg_outs[5];
	assign x6 = reg_outs[6];
	assign x7 = reg_outs[7];
	assign x8 = reg_outs[8];
	assign x9 = reg_outs[9];
	assign x10 = reg_outs[10];

	Reg #(WIDTH , 0) reg0 (clk  , rst , 0    , reg_outs[0]  , writeEn & writeEnables[0] );
	genvar i;
	generate
		for (i = 1; i < 32; i = i + 1) begin
			Reg #(WIDTH , 0) reg1 (clk  , rst , data , reg_outs[i]  , writeEn & writeEnables[i] );
		end
	endgenerate
   
	generate
		for (i = 0; i < 32; i = i + 1) begin
			assign signal[i] = 32'h00000001 << i;
		end
	endgenerate

   MuxKey #(REGNUM, KEY_LEN, WIDTH) read1 (rs1, readreg1, {
	    5'b00000, reg_outs[0],
		5'b00001, reg_outs[1],
		5'b00010, reg_outs[2],
		5'b00011, reg_outs[3],
		5'b00100, reg_outs[4],
		5'b00101, reg_outs[5],
		5'b00110, reg_outs[6],
		5'b00111, reg_outs[7],
		5'b01000, reg_outs[8],
		5'b01001, reg_outs[9],
		5'b01010, reg_outs[10],
		5'b01011, reg_outs[11],
		5'b01100, reg_outs[12],
		5'b01101, reg_outs[13],
		5'b01110, reg_outs[14],
		5'b01111, reg_outs[15],
		5'b10000, reg_outs[16],
		5'b10001, reg_outs[17],
		5'b10010, reg_outs[18],
		5'b10011, reg_outs[19],
		5'b10100, reg_outs[20],
		5'b10101, reg_outs[21],
		5'b10110, reg_outs[22],
		5'b10111, reg_outs[23],
		5'b11000, reg_outs[24],
		5'b11001, reg_outs[25],
		5'b11010, reg_outs[26],
		5'b11011, reg_outs[27],
		5'b11100, reg_outs[28],
		5'b11101, reg_outs[29],
		5'b11110, reg_outs[30],
		5'b11111, reg_outs[31]
	   });

   MuxKey #(REGNUM, KEY_LEN, WIDTH) read2 (rs2, readreg2, {
	    5'b00000, reg_outs[0],
		5'b00001, reg_outs[1],
		5'b00010, reg_outs[2],
		5'b00011, reg_outs[3],
		5'b00100, reg_outs[4],
		5'b00101, reg_outs[5],
		5'b00110, reg_outs[6],
		5'b00111, reg_outs[7],
		5'b01000, reg_outs[8],
		5'b01001, reg_outs[9],
		5'b01010, reg_outs[10],
		5'b01011, reg_outs[11],
		5'b01100, reg_outs[12],
		5'b01101, reg_outs[13],
		5'b01110, reg_outs[14],
		5'b01111, reg_outs[15],
		5'b10000, reg_outs[16],
		5'b10001, reg_outs[17],
		5'b10010, reg_outs[18],
		5'b10011, reg_outs[19],
		5'b10100, reg_outs[20],
		5'b10101, reg_outs[21],
		5'b10110, reg_outs[22],
		5'b10111, reg_outs[23],
		5'b11000, reg_outs[24],
		5'b11001, reg_outs[25],
		5'b11010, reg_outs[26],
		5'b11011, reg_outs[27],
		5'b11100, reg_outs[28],
		5'b11101, reg_outs[29],
		5'b11110, reg_outs[30],
		5'b11111, reg_outs[31]
	   });


   MuxKey #(REGNUM, KEY_LEN, WIDTH) write1 (writeEnables, writereg, {
		5'b00000, signal[0],
		5'b00001, signal[1],
		5'b00010, signal[2],
		5'b00011, signal[3],
		5'b00100, signal[4],
		5'b00101, signal[5],
		5'b00110, signal[6],
		5'b00111, signal[7],
		5'b01000, signal[8],
		5'b01001, signal[9],
		5'b01010, signal[10],
		5'b01011, signal[11],
		5'b01100, signal[12],
		5'b01101, signal[13],
		5'b01110, signal[14],
		5'b01111, signal[15],
		5'b10000, signal[16],
		5'b10001, signal[17],
		5'b10010, signal[18],
		5'b10011, signal[19],
		5'b10100, signal[20],
		5'b10101, signal[21],
		5'b10110, signal[22],
		5'b10111, signal[23],
		5'b11000, signal[24],
		5'b11001, signal[25],
		5'b11010, signal[26],
		5'b11011, signal[27],
		5'b11100, signal[28],
		5'b11101, signal[29],
		5'b11110, signal[30],
		5'b11111, signal[31]
   });
   
   
   endmodule
