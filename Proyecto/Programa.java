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

public class Programa {
    public final List<Instruccion> instrucciones;
    public final List<String> lineasOriginales;
    public final Map<String,Integer> etiquetas;

    public Programa(List<Instruccion> instrucciones, List<String> lineasOriginales, Map<String,Integer> etiquetas) {
        this.instrucciones = instrucciones;
        this.lineasOriginales = lineasOriginales;
        this.etiquetas = etiquetas;
    }

    public Instruccion obtener(int pc) { return instrucciones.get(pc); }
    public int longitud() { return instrucciones.size(); }
    public String lineaOriginal(int i) { return lineasOriginales.get(i); }
}
