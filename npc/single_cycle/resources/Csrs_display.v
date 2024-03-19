
module Csrs_display(
    input [31:0] regs_0 ,
    input [31:0] regs_1 ,
    input [31:0] regs_2 ,
    input [31:0] regs_3 
);
    import "DPI-C" function void Csrs_display(input [31:0] regs [31:0]);
    wire [31:0] regs [31:0];
    assign regs[0] = regs_0 ;   
    assign regs[1] = regs_1 ;
    assign regs[2] = regs_2 ;
    assign regs[3] = regs_3 ;
    always @(*) begin
        Csrs_display(regs);
    end
endmodule //
