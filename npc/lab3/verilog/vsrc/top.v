// Generated by CIRCT firtool-1.44.0
module adder(	// @[<stdin>:3:10]
  input  [3:0] io_A,	// @[lab3/src/top.scala:49:16]
               io_B,	// @[lab3/src/top.scala:49:16]
               io_Cin,	// @[lab3/src/top.scala:49:16]
  output [3:0] result,	// @[lab3/src/top.scala:49:16]
  output       overflow,	// @[lab3/src/top.scala:49:16]
               carry	// @[lab3/src/top.scala:49:16]
);

  wire [3:0] complement_of_B = io_B ^ {4{io_Cin[0]}};	// @[lab3/src/top.scala:61:{29,34,53}]
  wire [4:0] carry_with_result =
    {io_A[3], io_A} + {complement_of_B[3], complement_of_B} + {io_Cin[3], io_Cin};	// @[lab3/src/top.scala:61:29, :62:77]
  assign result = carry_with_result[3:0];	// @[<stdin>:3:10, lab3/src/top.scala:62:77, :64:35]
  assign overflow = io_A[3] == complement_of_B[3] & carry_with_result[3] != io_A[3];	// @[<stdin>:3:10, lab3/src/top.scala:61:29, :62:77, :67:{25,29,48,53,66,70}]
  assign carry = carry_with_result[4];	// @[<stdin>:3:10, lab3/src/top.scala:62:77, :65:34]
endmodule

module top(	// @[<stdin>:39:10]
  input         clock,	// @[<stdin>:40:11]
                reset,	// @[<stdin>:41:11]
  input  [10:0] sw,	// @[lab3/src/top.scala:6:16]
  output [3:0]  res,	// @[lab3/src/top.scala:6:16]
  output        overflow,	// @[lab3/src/top.scala:6:16]
                carry,	// @[lab3/src/top.scala:6:16]
                zero	// @[lab3/src/top.scala:6:16]
);

  wire [3:0] _add_result;	// @[lab3/src/top.scala:22:21]
  wire       _add_overflow;	// @[lab3/src/top.scala:22:21]
  wire       _zero_output = _add_result == 4'h0;	// @[lab3/src/top.scala:22:21, :32:53]
  adder add (	// @[lab3/src/top.scala:22:21]
    .io_A        (sw[7:4]),	// @[lab3/src/top.scala:18:23]
    .io_B        (sw[3:0]),	// @[lab3/src/top.scala:19:23]
    .io_Cin      ({3'h0, sw[8] | sw[10]}),	// @[lab3/src/top.scala:27:{16,23,27,32}]
    .result   (_add_result),
    .overflow (_add_overflow),
    .carry    (carry)
  );
  assign res =
    sw[10:8] == 3'h0 | sw[10:8] == 3'h1
      ? _add_result
      : sw[10:8] == 3'h2
          ? ~(sw[7:4])
          : sw[10:8] == 3'h3
              ? sw[7:4] | sw[3:0]
              : sw[10:8] == 3'h4
                  ? sw[7:4] & sw[3:0]
                  : sw[10:8] == 3'h5
                      ? sw[7:4] ^ sw[3:0]
                      : {3'h0,
                         sw[10:8] == 3'h6
                           ? _add_result[3] ^ _add_overflow
                           : (&(sw[10:8])) & _zero_output};	// @[<stdin>:39:10, lab3/src/top.scala:17:17, :18:23, :19:23, :22:21, :27:16, :32:{35,53}, :36:14, :37:14, :38:{14,26}, :39:{14,37}, :40:{14,37}, :41:{14,37}, :42:{14,45}, :43:{14,30}, src/main/scala/chisel3/util/Mux.scala:141:16]
  assign overflow = _add_overflow;	// @[<stdin>:39:10, lab3/src/top.scala:22:21]
  assign zero = _zero_output;	// @[<stdin>:39:10, lab3/src/top.scala:32:53]
endmodule

