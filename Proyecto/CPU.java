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

public class CPU {
    public enum Estado { LISTO, EJECUTANDO, TERMINADO, ERROR }

    public final Map<String,Integer> registros = new LinkedHashMap<>();
    public int PC = 0;
    public int AC = 0;
    public boolean ZF = false;
    public boolean CF = false;
    public Estado estado = Estado.LISTO;
    
    public final Stack<Integer> pila = new Stack<>();

    public CPU() {
        registros.put("AX", 0);
        registros.put("BX", 0);
        registros.put("CX", 0);
        registros.put("DX", 0);
    }

    public void reiniciar() {
        registros.replaceAll((k,v)->0);
        PC = 0;
        AC = 0;
        ZF = false;
        CF = false;
        estado = Estado.LISTO;
    }

    public int obtenerRegistro(String r) {
        if (r.equals("AC")) return AC;
        return registros.getOrDefault(r, 0);
    }

    public void asignarRegistro(String r, int valor) {
        if (registros.containsKey(r)) registros.put(r, valor);
        else if (r.equals("AC")) AC = valor;
        else throw new RuntimeException("Registro desconocido: " + r);
    }
}