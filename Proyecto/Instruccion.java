/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.minipc;

/**
 *
 * @author jhonn
 */

import java.util.*;

public class Instruccion {
    public final String opcode;
    public final List<String> operandos;

    public Instruccion(String opcode, List<String> operandos) {
        this.opcode = opcode.toUpperCase();
        this.operandos = operandos;
    }

    public String aBinario() {
        StringBuilder bin = new StringBuilder();

        switch (opcode) {
            case "MOV":   bin.append("0011 "); break;
            case "LOAD":  bin.append("0100 "); break;
            case "STORE": bin.append("0101 "); break;
            case "ADD":   bin.append("0110 "); break;
            case "SUB":   bin.append("0111 "); break;
            case "JMP":   bin.append("1000 "); break;
            case "JZ":    bin.append("1001 "); break;
            case "JNZ":   bin.append("1010 "); break;
            case "INC":   bin.append("1011 "); break;
            case "DEC":   bin.append("1100 "); break;
            case "SWAP":  bin.append("1101 "); break;
            case "CMP":   bin.append("1110 "); break;
            case "JE":    bin.append("1111 "); break;
            case "JNE":   bin.append("0001 "); break;
            case "PUSH":  bin.append("0010 "); break;
            case "POP":   bin.append("0011 "); break;
            case "PARAM": bin.append("0100 "); break;
            case "INT":   bin.append("0101 "); break;
            case "HALT":  return "1111 0000 00000000";
            case "NOP":   return "0000 0000 00000000";
            default:      bin.append("0000 "); break;
        }

        for (String op : operandos) {
            if (op.matches("[-+]?[0-9]+")) {
                int num = Integer.parseInt(op);
                String imm = String.format("%8s", Integer.toBinaryString(num & 0xFF)).replace(' ', '0');
                bin.append(imm).append(" ");
            } else {
                bin.append(regABin(op)).append(" ");
            }
        }

        return bin.toString().trim();
    }

    private String regABin(String r) {
        switch (r) {
            case "AX": return "0001";
            case "BX": return "0010";
            case "CX": return "0011";
            case "DX": return "0100";
            case "AC": return "1111";
            default:   return "0000";
        }
    }

    @Override
    public String toString() {
        if (operandos.isEmpty()) return opcode;
        return opcode + " " + String.join(", ", operandos);
    }
}
