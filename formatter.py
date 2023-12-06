#! /bin/python
from enum import Enum

Instr = Enum('Instr',
             ['R', 'I', 'I_STAR', 'S', 'B', 'U', 'J'])

def formatter(inst, inst_type):
    binary_format = bin(inst)
    binary_format = binary_format[2:] # remove 0b
    binary_format = (32 - len(binary_format)) * '0' + binary_format
    assert(len(binary_format) == 32)
    print(f'Instrcution type is {inst_type}');
    if inst_type == Instr.R:
        print(f'{"funct7":7s} {"rs2":5s} {"rs1":5s} {"func3":3s} {"rd":5s} {"opcode":7s}')
        print(binary_format[0:7], binary_format[7:12], binary_format[12:17], binary_format[17:20], binary_format[20:25], binary_format[25:32])
    elif inst_type == Instr.I:
        print(f'{"imm[11:0]":11s} {"rs1":5s} {"func3":3s} {"rd":5s} {"opcode":7s}')
        print(binary_format[0:12], binary_format[12:17], binary_format[17:20], binary_format[20:25], binary_format[25:32])
    elif inst_type == Instr.I_STAR:
        print(f'{"funct7":7s} {"imm[4:0]":5s} {"rs1":5s} {"func3":3s} {"rd":5s} {"opcode":7s}')
        print(binary_format[0:7], binary_format[7:12], binary_format[12:17], binary_format[17:20], binary_format[20:25], binary_format[25:32])
    elif inst_type == Instr.S:
        print(f'{"imm[11:5]":7s} {"rs2":5s} {"rs1":5s} {"func3":3s} {"imm[4:0]":5s} {"opcode":7s}')
        print(binary_format[0:7], binary_format[7:12], binary_format[12:17], binary_format[17:20], binary_format[20:25], binary_format[25:32])
    elif inst_type == Instr.B:
        print(f'{"imm[12|10:5]":7s} {"rs2":5s} {"rs1":5s} {"func3":3s} {"imm[4:1]|11":5s} {"opcode":7s}')
        print(binary_format[0:7], binary_format[7:12], binary_format[12:17], binary_format[17:20], binary_format[20:25], binary_format[25:32])
        print("immB:", binary_format[0:1], binary_format[24:25], binary_format[1:7], binary_format[20:24])
    elif inst_type == Instr.U:
        print(f'{"imm[31:12]":20s} {"rd":5s} {"opcode":7s}' )
        print(binary_format[0:20], binary_format[20:25], binary_format[25:32])
    elif inst_type == Instr.J:
        print(f'{"imm[20|10:1|11|19:12]":20s} {"rd":5s} {"opcode":7s}' )
        print(binary_format[0:1], binary_format[12:20], binary_format[11:12], binary_format[1:11], "0",
              binary_format[20:25],
              binary_format[25:32])

def gen_I(imm, rs1, rd, name):
    opcode, funct3 = "", ""
    if name == "addi":
        opcode = "0010011"
        funct3 = "000"
    
    rd  = "{:>05s}".format(bin(rd)[2:])
    rs1 = "{:>05s}".format(bin(rs1)[2:])
    imm = "{:>012s}".format(bin(imm)[2:])

    inst = imm + rs1 + funct3 + rd + opcode

    return inst        

def inst_gen(inst_type):
    if inst_type == Instr.I:
        for i in range (5):
            inst = gen_I(imm = i, rs1 = 0, rd = i, name = "addi")
            inst = hex(int(inst, 2))[2:]
            print(f'0x{inst:>08s}')
    return ""


def main():
    inst_gen(Instr.I)
    
if __name__ == "__main__":
    main()

# 524264
