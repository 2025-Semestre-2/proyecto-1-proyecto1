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

public class Cargador {
    private static final Set<String> REGISTROS = new HashSet<>(Arrays.asList("AX","BX","CX","DX","AC"));
    private static final Set<String> INSTRUCCIONES = new HashSet<>(Arrays.asList(
            "MOV","LOAD","STORE","ADD","SUB","JMP","JZ","JNZ","HALT","NOP"
    ));

    public static Programa parsear(List<String> lineas) throws ExcepcionAsm {
        List<Instruccion> instrucciones = new ArrayList<>();
        List<String> lineasMemoria = new ArrayList<>();
        Map<String,Integer> etiquetas = new HashMap<>();

        int lineaNo = 0;
        for (String raw : lineas) {
            String linea = raw.trim();
            if (linea.isEmpty() || linea.startsWith(";")) continue;

            if (linea.endsWith(":")) {
                etiquetas.put(linea.substring(0,linea.length()-1).toUpperCase(), instrucciones.size());
                continue;
            }

            String[] partes = linea.split("\\s+", 2);
            String op = partes[0].toUpperCase();
            if (!INSTRUCCIONES.contains(op)) {
                throw new ExcepcionAsm("Instrucción no soportada: " + op);
            }
            List<String> ops = new ArrayList<>();
            if (partes.length == 2) {
                for (String t : partes[1].split(",")) {
                    ops.add(t.trim().toUpperCase());
                }
            }
            for (String o : ops) {
                if (REGISTROS.contains(o) || o.matches("[-+]?[0-9]+") || o.startsWith("#") || o.startsWith("[")) {
                    continue;
                } else {
                    throw new ExcepcionAsm("Operando inválido: " + o + " en línea " + (lineaNo+1));
                }
            }
            instrucciones.add(new Instruccion(op, ops));
            lineasMemoria.add(linea);
            lineaNo++;
        }
        return new Programa(instrucciones, lineasMemoria, etiquetas);
    }
}
