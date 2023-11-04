module ps2_keyboard(clk,resetn,ps2_clk,ps2_data, seg0, seg1, seg2, seg3, seg4, seg5);
    input clk,resetn,ps2_clk,ps2_data;
    output [7:0] seg0, seg1, seg2, seg3, seg4, seg5;

    reg [9:0] buffer;        // ps2_data bits
    reg [7:0] prev_data;        // ps2_data bits
    reg [7:0] counter;        // count the number of keystrokes
    reg [7:0] strokes;  // count number of keystorkes
    reg [3:0] count;  // count ps2_data bits
    reg [2:0] ps2_clk_sync;
    reg ready;
    reg key_received;  // new register to indicate if a key is received
    wire [7:0] ascii;  // ASCII code of the key

    initial begin
      counter = 8'b0;
      prev_data = 8'b0;
      key_received = 1'b0;  // initialize to 0
    end

    rom rom1(buffer[8:1], ascii);

    assign strokes = counter / 3;

    always @(posedge clk) begin
        ps2_clk_sync <=  {ps2_clk_sync[1:0],ps2_clk};
    end

    wire sampling = ps2_clk_sync[2] & ~ps2_clk_sync[1]; // 检测由高电平向低电平的下降沿 (1 & ~(0) = 1)

    seg u_seg0(key_received ? buffer[4:1] : 4'b0, seg0);
    seg u_seg1(key_received ? buffer[8:5] : 4'b0, seg1);
    seg u_seg2(key_received ? ascii[3:0] : 4'b0, seg2);  // Display ASCII code
    seg u_seg3(key_received ? ascii[7:4] : 4'b0, seg3);  // Display ASCII code
    seg u_seg4(strokes[3:0], seg4);
    seg u_seg5(strokes[7:4], seg5);

    always @(posedge clk) begin
        if (resetn == 0) begin // reset
            count <= 0;
            key_received <= 0;  // reset key_received
        end
        else begin
            if (sampling) begin
              if (count == 4'd10) begin
                if ((buffer[0] == 0) &&  // start bit
                    (ps2_data)       &&  // stop bit
                    (^buffer[9:1])) begin      // odd  parity
                    $display("receive %x", buffer[8:1]);
                    if (prev_data != buffer[8:1])
                      counter <= counter + 8'b00000001;
                    else 
                      counter <= counter;
                    key_received <= 1;  // set key_received to 1
                    prev_data <= buffer[8:1];
                end
                count <= 0;     // for next
              end else begin
                buffer[count] <= ps2_data;  // store ps2_data
                count <= count + 3'b1;
                key_received <= 0;  // reset key_received
              end
            end
        end
    end

endmodule

module rom(input [7:0] addr, output reg [7:0] data);
    always @(*) begin
        case(addr)
            8'h1C: data = 8'h41;  // 'A'
            8'h32: data = 8'h42;  // 'B'
            8'h21: data = 8'h43;
            8'h23: data = 8'h44;
            8'h24: data = 8'h45;
            8'h2B: data = 8'h46;
            8'h34: data = 8'h47;
            8'h33: data = 8'h48;
            8'h43: data = 8'h49;
            8'h3B: data = 8'h4a;
            8'h42: data = 8'h4b;
            8'h4B: data = 8'h4c;
            8'h3A: data = 8'h4d;
            8'h31: data = 8'h4e;
            8'h44: data = 8'h4f;
            8'h4D: data = 8'h50;
            8'h15: data = 8'h51;
            8'h2D: data = 8'h52;
            8'h1B: data = 8'h53;
            8'h2C: data = 8'h54;
            8'h3C: data = 8'h55;
            8'h2A: data = 8'h56;
            8'h1D: data = 8'h57;
            8'h22: data = 8'h58;
            8'h35: data = 8'h59;
            8'h1A: data = 8'h5a;
            8'h45: data = 8'h30;
            8'h16: data = 8'h31;
            8'h1E: data = 8'h32;
            8'h26: data = 8'h33;
            8'h25: data = 8'h34;
            8'h2E: data = 8'h35;
            8'h36: data = 8'h36;
            8'h3D: data = 8'h37;
            8'h3E: data = 8'h38;
            8'h46: data = 8'h39;
            default: data = 8'h00;
        endcase
    end
endmodule