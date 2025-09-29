
package com.mycompany.minipc;

import java.io.File;

class Proceso {
    int pid;
    Programa programa;
    BCP bcp;
    File archivo;
    Proceso siguiente; // apunta al siguiente proceso en la lista

    public Proceso(int pid, Programa programa, BCP bcp, File archivo) {
        this.pid = pid;
        this.programa = programa;
        this.bcp = bcp;
        this.archivo = archivo;
        this.siguiente = null;
    }
}
