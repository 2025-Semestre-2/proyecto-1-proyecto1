/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.minipc;

/**
 *
 * @author jhonn
 */

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class RenderizadorMemoria extends DefaultTableCellRenderer {
    private final Memoria memoria;
    private final ModeloTablaMemoria.ProveedorPC proveedorPC;

    public RenderizadorMemoria(Memoria memoria, ModeloTablaMemoria.ProveedorPC proveedorPC) {
        this.memoria = memoria;
        this.proveedorPC = proveedorPC;
    }

    @Override
    public Component getTableCellRendererComponent(JTable tabla, Object valor,
                                                   boolean seleccionado, boolean foco,
                                                   int fila, int columna) {
        Component c = super.getTableCellRendererComponent(tabla, valor, seleccionado, foco, fila, columna);
        c.setBackground(Color.WHITE);

        if (fila < memoria.tamanoSO) {
            c.setBackground(new Color(230,230,230));
        } else {
            String v = memoria.obtenerRaw(fila);
            if (v != null && !v.isEmpty()) {
                if (fila == proveedorPC.obtener()) {
                    c.setBackground(new Color(255,255,150));
                } else {
                    c.setBackground(new Color(200,220,255));
                }
            }
        }
        return c;
    }
}
