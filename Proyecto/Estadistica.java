/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.minipc;

/**
 *
 * @author jhonn
 */

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Estadistica {
    private final String nombreArchivo;
    private final LocalDateTime inicio;
    private LocalDateTime fin;

    public Estadistica(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
        this.inicio = LocalDateTime.now();
    }

    public void marcarFin() {
        this.fin = LocalDateTime.now();
    }

    public String getInicioStr() {
        return inicio.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public String getFinStr() {
        if (fin == null) return "-";
        return fin.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public long getDuracionSegundos() {
        if (fin == null) return 0;
        return Duration.between(inicio, fin).getSeconds();
    }

    @Override
    public String toString() {
        return String.format("Archivo: %s | Inicio: %s | Fin: %s | Duración: %ds",
                nombreArchivo, getInicioStr(), getFinStr(), getDuracionSegundos());
    }
}
