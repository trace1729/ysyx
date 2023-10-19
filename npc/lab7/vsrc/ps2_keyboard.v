module ps2_keyboard(clk,resetn,ps2_clk,ps2_data, seg0, seg1, seg2, seg3);
    input clk,resetn,ps2_clk,ps2_data;
    output [7:0] seg0, seg1, seg2, seg3;

    reg [9:0] buffer;        // ps2_data bits
    reg [7:0] counter;        // count the number of keystrokes
    reg [3:0] count;  // count ps2_data bits
    reg [2:0] ps2_clk_sync;

    initial begin
      counter = 8'b0;
    end

    always @(posedge clk) begin
        ps2_clk_sync <=  {ps2_clk_sync[1:0],ps2_clk};
    end

    wire sampling = ps2_clk_sync[2] & ~ps2_clk_sync[1]; // 检测由高电平向低电平的下降沿 (1 & ~(0) = 1)

    seg u_seg0(buffer[4:1], seg0);
    seg u_seg1(buffer[8:5], seg1);
    seg u_seg2(counter[3:0], seg2);
    seg u_seg3(counter[7:4], seg3);

    always @(posedge clk) begin
        if (resetn == 0) begin // reset
            count <= 0;
        end
        else begin
            if (sampling) begin
              if (count == 4'd10) begin
                if ((buffer[0] == 0) &&  // start bit
                    (ps2_data)       &&  // stop bit
                    (^buffer[9:1])) begin      // odd  parity
                    $display("receive %x", buffer[8:1]);
                    counter <= counter + 8'b00000001;
                end
                count <= 0;     // for next
              end else begin
                buffer[count] <= ps2_data;  // store ps2_data
                count <= count + 3'b1;
              end
            end
        end
    end

endmodule

