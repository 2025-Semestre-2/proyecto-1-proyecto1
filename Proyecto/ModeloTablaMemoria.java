/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.minipc;

/**
 *
 * @author jhonn
 */

import javax.swing.table.AbstractTableModel;

public class ModeloTablaMemoria extends AbstractTableModel {
    private final Memoria memoria;
    private final String[] columnas = {"Dir", "Contenido"};
    private final ProveedorPC proveedorPC;

    public ModeloTablaMemoria(Memoria memoria, ProveedorPC pcSupplier) {
        this.memoria = memoria;
        this.proveedorPC = pcSupplier;
    }

    @Override public int getRowCount() { return memoria.tamano; }
    @Override public int getColumnCount() { return columnas.length; }
    @Override public String getColumnName(int c) { return columnas[c]; }
    public Object getValueAt(int fila, int col) {
        switch (col) {
            case 0: return fila;
            case 1: 
                if (VentanaSimulador.bcp != null &&
                    fila >= VentanaSimulador.bcp.baseCodigo &&
                    fila <= VentanaSimulador.bcp.limiteCodigo) {
                    // fila pertenece al proceso activo
                    return memoria.obtenerRaw(fila);
                } else {
                    return ""; // ocultar el resto
                }
            default: return "";
        }
    }

    public interface ProveedorPC { int obtener(); }
}

