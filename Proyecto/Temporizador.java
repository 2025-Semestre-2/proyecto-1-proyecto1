package com.mycompany.minipc;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author jhonn
 */

import javax.swing.*;

public class Temporizador {

    private final Timer timer;
    private final Runnable listener;

    public Temporizador(int milisegundos, Runnable listener) {
        this.listener = listener;
        this.timer = new Timer(milisegundos, e -> this.listener.run());
    }

    public void iniciar() {
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    public void detener() {
        if (timer.isRunning()) {
            timer.stop();
        }
    }

    public boolean estaCorriendo() {
        return timer.isRunning();
    }

    public void setIntervalo(int milisegundos) {
        timer.setDelay(milisegundos);
    }
}
