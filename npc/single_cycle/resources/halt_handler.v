import "DPI-C" function void stop();

module BlackBoxRealAdd(
    input  [31:0] inst
);
  always @(*) begin
    if (inst == 32'h00100073) begin
      stop();
    end

  end
endmodule
