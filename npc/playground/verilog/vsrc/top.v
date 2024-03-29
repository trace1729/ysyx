// Generated by CIRCT firtool-1.42.0
module top(	// @[<stdin>:3:10]
  input         clock,	// @[<stdin>:4:11]
                reset,	// @[<stdin>:5:11]
  input  [11:0] io_csr_no,	// @[playground/src/CSR.scala:27:16]
  input  [31:0] io_data,	// @[playground/src/CSR.scala:27:16]
  output [31:0] io_csr_value	// @[playground/src/CSR.scala:27:16]
);

  reg [31:0] mstatus;	// @[playground/src/CSR.scala:29:26]
  reg [31:0] mepc;	// @[playground/src/CSR.scala:30:25]
  reg [31:0] mtvec;	// @[playground/src/CSR.scala:31:25]
  reg [31:0] mcause;	// @[playground/src/CSR.scala:32:25]
  always @(posedge clock) begin	// @[<stdin>:4:11]
    if (reset) begin	// @[<stdin>:4:11]
      mstatus <= 32'h1800;	// @[playground/src/CSR.scala:29:26]
      mepc <= 32'h0;	// @[playground/src/CSR.scala:30:25]
      mtvec <= 32'h0;	// @[playground/src/CSR.scala:30:25, :31:25]
      mcause <= 32'h0;	// @[playground/src/CSR.scala:30:25, :32:25]
    end
    else begin	// @[<stdin>:4:11]
      if (io_csr_no == 12'h300)	// @[playground/src/CSR.scala:54:20]
        mstatus <= io_data;	// @[playground/src/CSR.scala:29:26]
      if (io_csr_no == 12'h341)	// @[playground/src/CSR.scala:48:20]
        mepc <= io_data;	// @[playground/src/CSR.scala:30:25]
      if (io_csr_no == 12'h301)	// @[playground/src/CSR.scala:45:20]
        mtvec <= io_data;	// @[playground/src/CSR.scala:31:25]
      if (io_csr_no == 12'h342)	// @[playground/src/CSR.scala:51:20]
        mcause <= io_data;	// @[playground/src/CSR.scala:32:25]
    end
  end // always @(posedge)
  assign io_csr_value =
    io_csr_no == 12'h342
      ? mcause
      : io_csr_no == 12'h301
          ? mtvec
          : io_csr_no == 12'h341 ? mepc : io_csr_no == 12'h300 ? mstatus : 32'h0;	// @[<stdin>:3:10, playground/src/CSR.scala:29:26, :30:25, :31:25, :32:25, :45:20, :48:20, :51:20, :54:20, src/main/scala/chisel3/util/Lookup.scala:31:38, :34:39]
endmodule

