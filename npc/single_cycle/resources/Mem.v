module Mem (
       input [31:0] addr , 
       output reg [31:0] rdata, 
       input [31:0] wdata, 
       input [7:0] wmask, 
       input memRW 
);
import "DPI-C" function void dpi_pmem_read(
  input int raddr, output int rdata);
import "DPI-C" function void dpi_pmem_write(
  input int waddr, input int wdata, input byte wmask);

always @(*) begin
    rdata = 0;
    if (memRW) begin
       dpi_pmem_write(addr, wdata, wmask);
    end
    else
        dpi_pmem_read(addr, rdata);
end
    
endmodule //mem
