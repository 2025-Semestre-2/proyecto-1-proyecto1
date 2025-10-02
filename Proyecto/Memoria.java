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

public class Memoria {
    public int tamano;
    public int tamanoSO;
    private final List<String> celdas;
    private String mensajeEstado = "";

    public Memoria(int tamano, int tamanoSO) {
        this.tamano = tamano;
        this.tamanoSO = tamanoSO;
        this.celdas = new ArrayList<>();
        for (int i = 0; i < tamano; i++) celdas.add("");
        marcarSO();
    }

    public void redimensionar(int nuevoTamano, int nuevoSO) {
        if (nuevoSO >= nuevoTamano) {
            throw new RuntimeException("El tamaño del SO no puede ser mayor o igual al de la Memoria.");
        }
        this.tamano = nuevoTamano;
        this.tamanoSO = nuevoSO;
        celdas.clear();
        for (int i = 0; i < tamano; i++) celdas.add("");
        marcarSO();
    }

    public void limpiarUsuario() {
        for (int i = tamanoSO; i < tamano; i++) celdas.set(i, "");
    }

    public void marcarSO() {
        for (int i = 0; i < tamanoSO && i < tamano; i++) celdas.set(i, "<SO>");
    }

    public String obtenerCelda(int idx) {
        if (idx < 0 || idx >= tamano) return "0";
        String v = celdas.get(idx);
        if (v == null || v.trim().isEmpty()) return "0";
        if ("<SO>".equals(v)) return "0";
        return v;
    }

    public void asignarCelda(int idx, String valor) {
        if (idx < 0 || idx >= tamano) throw new RuntimeException("Dirección fuera de rango: " + idx);
        celdas.set(idx, valor);
    }

    public String obtenerRaw(int idx) { return celdas.get(idx); }

    public void asignarMensaje(String msg) { this.mensajeEstado = msg; }
    public String obtenerMensaje() { return mensajeEstado; }
}

