module Mem (
       input [31:0] addr , 
       output reg [31:0] rdata, 
       input [31:0] wdata, 
       input [7:0] wmask, 
       input memEnable,
       input memRW 
);
import "DPI-C" function int dpi_pmem_read(
  input int raddr);
import "DPI-C" function void dpi_pmem_write(
  input int waddr, input int wdata, input byte wmask);

always @(*) begin
    rdata = 0;
    if (memEnable) begin
      if (memRW) begin
        dpi_pmem_write(addr, wdata, wmask);
      end
      else begin
          rdata = dpi_pmem_read(addr);
      end
    end
    else begin
      rdata = 0;
    end
end
    
endmodule //mem
