/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.minipc;

/**
 *
 * @author jhonn
 */
public class BCP {
    public int idProceso = -1;
    public EstadoProceso estado = EstadoProceso.NUEVO;
    public int baseCodigo = -1;
    public int limiteCodigo = -1;
    public int baseDatos = -1;

    public void reiniciar() {
        idProceso = -1;
        estado = EstadoProceso.NUEVO;
        baseCodigo = limiteCodigo = baseDatos = -1;
    }
    
    public String ultimoResultado = "-";
    
    public void cambiarEstado(EstadoProceso nuevo) {
        this.estado = nuevo;
    }
}
