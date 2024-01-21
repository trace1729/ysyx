
module Regs_display(
    input [31:0] regs_0 ,
    input [31:0] regs_1 ,
    input [31:0] regs_2 ,
    input [31:0] regs_3 ,
    input [31:0] regs_4 ,
    input [31:0] regs_5 ,
    input [31:0] regs_6 ,
    input [31:0] regs_7 ,
    input [31:0] regs_8 ,
    input [31:0] regs_9 ,
    input [31:0] regs_10,
    input [31:0] regs_11,
    input [31:0] regs_12,
    input [31:0] regs_13,
    input [31:0] regs_14,
    input [31:0] regs_15,
    input [31:0] regs_16,
    input [31:0] regs_17,
    input [31:0] regs_18,
    input [31:0] regs_19,
    input [31:0] regs_20,
    input [31:0] regs_21,
    input [31:0] regs_22,
    input [31:0] regs_23,
    input [31:0] regs_24,
    input [31:0] regs_25,
    input [31:0] regs_26,
    input [31:0] regs_27,
    input [31:0] regs_28,
    input [31:0] regs_29,
    input [31:0] regs_30,
    input [31:0] regs_31
);
    import "DPI-C" function void Regs_display(input [31:0] regs [31:0]);
    wire [31:0] regs [31:0];
    assign regs[0] = regs_0 ;   
    assign regs[1] = regs_1 ;
    assign regs[2] = regs_2 ;
    assign regs[3] = regs_3 ;
    assign regs[4] = regs_4 ;
    assign regs[5] = regs_5 ;
    assign regs[6] = regs_6 ;
    assign regs[7] = regs_7 ;
    assign regs[8] = regs_8 ;
    assign regs[9] = regs_9 ;
    assign regs[10] = regs_10;
    assign regs[11] = regs_11;
    assign regs[12] = regs_12;
    assign regs[13] = regs_13;
    assign regs[14] = regs_14;
    assign regs[15] = regs_15;
    assign regs[16] = regs_16;
    assign regs[17] = regs_17;
    assign regs[18] = regs_18;
    assign regs[19] = regs_19;
    assign regs[20] = regs_20;
    assign regs[21] = regs_21;
    assign regs[22] = regs_22;
    assign regs[23] = regs_23;
    assign regs[24] = regs_24;
    assign regs[25] = regs_25;
    assign regs[26] = regs_26;
    assign regs[27] = regs_27;
    assign regs[28] = regs_28;
    assign regs[29] = regs_29;
    assign regs[30] = regs_30;
    assign regs[31] = regs_31;

    always @(*) begin
        Regs_display(regs);
    end
endmodule //
