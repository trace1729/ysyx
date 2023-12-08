module regfile(/*AUTOARG*/
   // Outputs
   rs1, rs2, x1, x2, x3, x4, x5, writeEnables,
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
   output [31:0] x3;
   output [31:0] x4;
   output [31:0] x5;
   output [31:0] writeEnables;

   // 需要重复下面3行 32 次
   // 有没有更好的办法？
   // 使用 verilog 的 generate 函数
   parameter	 REGNUM = 32;
   parameter	 WIDTH = 32;
   parameter	 KEY_LEN = 5;
   
   // 寄存器选择
   wire [WIDTH - 1:0] reg_outs [REGNUM - 1:0];
   // 设置使能信号

	assign x1 = reg_outs[1];
	assign x2 = reg_outs[2];
	assign x3 = reg_outs[3];
	assign x4 = reg_outs[4];
	assign x5 = reg_outs[5];

	Reg #(WIDTH, 0) reg0 (clk, rst, 0, reg_outs[0], writeEn & writeEnables[0]);
	Reg #(WIDTH, 0) reg1 (clk, rst, data, reg_outs[1], writeEn & writeEnables[1]);
	Reg #(WIDTH, 0) reg2 (clk, rst, data, reg_outs[2], writeEn & writeEnables[2]);
	Reg #(WIDTH, 0) reg3 (clk, rst, data, reg_outs[3], writeEn & writeEnables[3]);
	Reg #(WIDTH, 0) reg4 (clk, rst, data, reg_outs[4], writeEn & writeEnables[4]);
	Reg #(WIDTH, 0) reg5 (clk, rst, data, reg_outs[5], writeEn & writeEnables[5]);
	Reg #(WIDTH, 0) reg6 (clk, rst, data, reg_outs[6], writeEn & writeEnables[6]);
	Reg #(WIDTH, 0) reg7 (clk, rst, data, reg_outs[7], writeEn & writeEnables[7]);
	Reg #(WIDTH, 0) reg8 (clk, rst, data, reg_outs[8], writeEn & writeEnables[8]);
	Reg #(WIDTH, 0) reg9 (clk, rst, data, reg_outs[9], writeEn & writeEnables[9]);
	Reg #(WIDTH, 0) reg10 (clk, rst, data, reg_outs[10], writeEn & writeEnables[10]);
	Reg #(WIDTH, 0) reg11 (clk, rst, data, reg_outs[11], writeEn & writeEnables[11]);
	Reg #(WIDTH, 0) reg12 (clk, rst, data, reg_outs[12], writeEn & writeEnables[12]);
	Reg #(WIDTH, 0) reg13 (clk, rst, data, reg_outs[13], writeEn & writeEnables[13]);
	Reg #(WIDTH, 0) reg14 (clk, rst, data, reg_outs[14], writeEn & writeEnables[14]);
	Reg #(WIDTH, 0) reg15 (clk, rst, data, reg_outs[15], writeEn & writeEnables[15]);
	Reg #(WIDTH, 0) reg16 (clk, rst, data, reg_outs[16], writeEn & writeEnables[16]);
	Reg #(WIDTH, 0) reg17 (clk, rst, data, reg_outs[17], writeEn & writeEnables[17]);
	Reg #(WIDTH, 0) reg18 (clk, rst, data, reg_outs[18], writeEn & writeEnables[18]);
	Reg #(WIDTH, 0) reg19 (clk, rst, data, reg_outs[19], writeEn & writeEnables[19]);
	Reg #(WIDTH, 0) reg20 (clk, rst, data, reg_outs[20], writeEn & writeEnables[20]);
	Reg #(WIDTH, 0) reg21 (clk, rst, data, reg_outs[21], writeEn & writeEnables[21]);
	Reg #(WIDTH, 0) reg22 (clk, rst, data, reg_outs[22], writeEn & writeEnables[22]);
	Reg #(WIDTH, 0) reg23 (clk, rst, data, reg_outs[23], writeEn & writeEnables[23]);
	Reg #(WIDTH, 0) reg24 (clk, rst, data, reg_outs[24], writeEn & writeEnables[24]);
	Reg #(WIDTH, 0) reg25 (clk, rst, data, reg_outs[25], writeEn & writeEnables[25]);
	Reg #(WIDTH, 0) reg26 (clk, rst, data, reg_outs[26], writeEn & writeEnables[26]);
	Reg #(WIDTH, 0) reg27 (clk, rst, data, reg_outs[27], writeEn & writeEnables[27]);
	Reg #(WIDTH, 0) reg28 (clk, rst, data, reg_outs[28], writeEn & writeEnables[28]);
	Reg #(WIDTH, 0) reg29 (clk, rst, data, reg_outs[29], writeEn & writeEnables[29]);
	Reg #(WIDTH, 0) reg30 (clk, rst, data, reg_outs[30], writeEn & writeEnables[30]);
	Reg #(WIDTH, 0) reg31 (clk, rst, data, reg_outs[31], writeEn & writeEnables[31]);



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
