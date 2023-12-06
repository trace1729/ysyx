module regfile(/*AUTOARG*/
   // Outputs
   rs1, rs2, x1, x2, x3, x4, x5,
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
   output [31:0] x1;
   output [31:0] x2;
   output [31:0] x3;
   output [31:0] x4;
   output [31:0] x5;

   // 需要重复下面3行 32 次
   // 有没有更好的办法？
   // 使用 verilog 的 generate 函数
   parameter	 REGNUM = 32;
   parameter	 WIDTH = 32;
   parameter	 KEY_LEN = 5;
   
   // 寄存器选择
   wire [WIDTH - 1:0] reg_ins  [REGNUM - 1:0];
   wire [WIDTH - 1:0] reg_outs [REGNUM - 1:0];
   // 设置使能信号
   wire [REGNUM - 1:0] writeEnables; 

   assign x1 = reg_outs[1];
   assign x2 = reg_outs[2];
   assign x3 = reg_outs[3];
   assign x4 = reg_outs[4];
   assign x5 = reg_outs[5];
   
 
   generate
	genvar i;
    for (i = 0; i < REGNUM; i = i + 1) begin
		if (i == 0) 
			Reg #(WIDTH, 0) reg_i (clk, rst, 32'h00000000, reg_outs[i], writeEn & writeEnables[i]);
		else
			Reg #(WIDTH, 0) reg_i (clk, rst, data, reg_outs[i], writeEn & writeEnables[i]);
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

   
	   MuxKey #(REGNUM, KEY_LEN, WIDTH) write (writeEnables, writereg, {
			5'b00000, 32'h00000001,
			5'b00001, 32'h00000002,
			5'b00010, 32'h00000004,
			5'b00011, 32'h00000008,
			5'b00100, 32'h00000010,
			5'b00101, 32'h00000020,
			5'b00110, 32'h00000040,
			5'b00111, 32'h00000080,
			5'b01000, 32'h00000100,
			5'b01001, 32'h00000200,
			5'b01010, 32'h00000400,
			5'b01011, 32'h00000800,
			5'b01100, 32'h00001000,
			5'b01101, 32'h00002000,
			5'b01110, 32'h00004000,
			5'b01111, 32'h00008000,
			5'b10000, 32'h00010000,
			5'b10001, 32'h00020000,
			5'b10010, 32'h00040000,
			5'b10011, 32'h00080000,
			5'b10100, 32'h00100000,
			5'b10101, 32'h00200000,
			5'b10110, 32'h00400000,
			5'b10111, 32'h00800000,
			5'b11000, 32'h01000000,
			5'b11001, 32'h02000000,
			5'b11010, 32'h04000000,
			5'b11011, 32'h08000000,
			5'b11100, 32'h10000000,
			5'b11101, 32'h20000000,
			5'b11110, 32'h40000000,
			5'b11111, 32'h80000000
		   });
   
   endmodule
