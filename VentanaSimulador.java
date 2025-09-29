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
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;

public class VentanaSimulador extends JFrame {

    private final Memoria memoria = new Memoria(32, 8);
    private final CPU cpu = new CPU();
    public static BCP bcp = new BCP();
    private int proximaDireccionLibre = memoria.tamanoSO;  
    private int contProgramas = 0;
    private final Queue<Proceso> colaEspera = new LinkedList<>();
    
    private Proceso cabeza = null;        // primer proceso cargado
    private Proceso cola = null;          // último proceso (para ir encadenando)
    private Proceso procesoActual = null; // el que se está ejecutando
    private int contadorProcesos = 1;     // para dar IDs únicos

    private File ultimoArchivoCargado = null;

    private final ModeloTablaMemoria modeloMemoria = new ModeloTablaMemoria(memoria, () -> obtenerPCAbsoluto());
    private final JTable tablaMemoria = new JTable(modeloMemoria);

    private final DefaultTableModel modeloInstrucciones = new DefaultTableModel(new Object[]{"Instrucción", "Binario"}, 0);
    private final JTable tablaInstrucciones = new JTable(modeloInstrucciones);

    private final JLabel lblEstado = new JLabel("Sin programa");
    private final JLabel lblPC = new JLabel("0");
    private final JLabel lblAC = new JLabel("0");
    private final JLabel lblAX = new JLabel("0");
    private final JLabel lblBX = new JLabel("0");
    private final JLabel lblCX = new JLabel("0");
    private final JLabel lblDX = new JLabel("0");
    private final JLabel lblZF = new JLabel("false");
    private JLabel lblUltimoResultado = new JLabel("-");

    private final JLabel lblIdProceso = new JLabel("-");
    private final JLabel lblEstadoBCP = new JLabel("-");
    private final JLabel lblBaseCodigo = new JLabel("-");
    private final JLabel lblLimiteCodigo = new JLabel("-");
    private final JLabel lblBaseDatos = new JLabel("-");
    private final JLabel lblIR = new JLabel("-");

    private final JSpinner spTamMemoria = new JSpinner(new SpinnerNumberModel(32, 16, 4096, 1));
    private final JSpinner spTamSO = new JSpinner(new SpinnerNumberModel(8, 1, 2048, 1));

    private final JButton btnAsignarMemoria = new JButton("Asignar Memoria");
    private final JButton btnCargar = new JButton("Cargar .asm");
    private final JButton btnRecargar = new JButton("Recargar");
    private final JButton btnPaso = new JButton("Paso a paso");
    private final JButton btnEjecutar = new JButton("Ejecutar");
    private final JButton btnDetener = new JButton("Detener");
    private final JButton btnLimpiar = new JButton("Limpiar");
    JButton btnEstados = new JButton("Ver estados");

    private javax.swing.Timer temporizador;

    public VentanaSimulador() {
        super("MiniPC - Tarea 1");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 650));
        setLocationRelativeTo(null);

        tablaMemoria.setFillsViewportHeight(true);
        tablaMemoria.setDefaultRenderer(Object.class, new RenderizadorMemoria(memoria, () -> obtenerPCAbsoluto()));

        JPanel barraSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barraSuperior.add(new JLabel("Memoria:"));
        barraSuperior.add(spTamMemoria);
        barraSuperior.add(new JLabel("SO:"));
        barraSuperior.add(spTamSO);
        barraSuperior.add(btnAsignarMemoria);
        barraSuperior.add(btnCargar);
        barraSuperior.add(btnRecargar);
        barraSuperior.add(btnPaso);
        barraSuperior.add(btnEjecutar);
        barraSuperior.add(btnDetener);
        barraSuperior.add(btnLimpiar);

        JPanel panelCPU = construirPanelCPU();
        JPanel panelBCP = construirPanelBCP();

        JPanel panelDerecho = new JPanel(new GridLayout(2, 1));
        panelDerecho.add(panelCPU);
        panelDerecho.add(panelBCP);

        JScrollPane scrollInstr = new JScrollPane(tablaInstrucciones);
        scrollInstr.setBorder(new TitledBorder("Instrucciones"));

        JSplitPane centro = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollInstr, new JScrollPane(tablaMemoria));
        centro.setResizeWeight(0.4);

        JSplitPane divisionPrincipal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centro, panelDerecho);
        divisionPrincipal.setResizeWeight(0.7);

        setLayout(new BorderLayout());
        add(barraSuperior, BorderLayout.NORTH);
        add(divisionPrincipal, BorderLayout.CENTER);
        add(lblEstado, BorderLayout.SOUTH);

        btnAsignarMemoria.addActionListener(e -> {
            int nuevoTam = (int) spTamMemoria.getValue();
            int nuevoSO = (int) spTamSO.getValue();

            if (nuevoSO >= nuevoTam) {
                JOptionPane.showMessageDialog(this,
                        "El tamaño del SO no puede ser mayor o igual al de la Memoria.",
                        "Error de configuración",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            memoria.redimensionar(nuevoTam, nuevoSO);
            modeloMemoria.fireTableDataChanged();
            lblEstado.setText("Memoria asignada: " + nuevoTam + " celdas (SO=" + nuevoSO + ")");
        });

        btnCargar.addActionListener(e -> cargarDesdeChooser());
        btnRecargar.addActionListener(e -> recargarUltimoArchivo());
        btnPaso.addActionListener(e -> ejecutarPaso());
        btnEjecutar.addActionListener(e -> temporizador.start());
        btnDetener.addActionListener(e -> temporizador.stop());
        btnLimpiar.addActionListener(e -> limpiarTodo());
        btnEstados.addActionListener(e -> mostrarEstadosBCP());
        barraSuperior.add(btnEstados);

        temporizador = new javax.swing.Timer(400, e -> ejecutarPaso());
    }
    
    private JPanel construirPanelCPU() {
        JPanel p = new JPanel(new GridLayout(0, 2));
        p.setBorder(new TitledBorder("CPU"));

        p.add(new JLabel("PC:")); p.add(lblPC);
        p.add(new JLabel("AC:")); p.add(lblAC);
        p.add(new JLabel("AX:")); p.add(lblAX);
        p.add(new JLabel("BX:")); p.add(lblBX);
        p.add(new JLabel("CX:")); p.add(lblCX);
        p.add(new JLabel("DX:")); p.add(lblDX);
        p.add(new JLabel("ZF:")); p.add(lblZF);

        return p;
    }

    private JPanel construirPanelBCP() {
        JPanel p = new JPanel(new GridLayout(0, 2));
        p.setBorder(new TitledBorder("BCP actual CPU1"));

        p.add(new JLabel("ID Proceso:")); p.add(lblIdProceso);
        p.add(new JLabel("Estado:")); p.add(lblEstadoBCP);
        p.add(new JLabel("Base Código:")); p.add(lblBaseCodigo);
        p.add(new JLabel("Límite Código:")); p.add(lblLimiteCodigo);
        p.add(new JLabel("Base Datos:")); p.add(lblBaseDatos);
        p.add(new JLabel("IR:")); p.add(lblIR);
         p.add(new JLabel("Último Resultado:")); 
        lblUltimoResultado = new JLabel("-");
        p.add(lblUltimoResultado);
        return p;
    }

    private void limpiarTodo() {
        cpu.reiniciar();
        
        if (bcp != null) {
            bcp.cambiarEstado(EstadoProceso.TERMINADO);
        }
        
        memoria.limpiarUsuario();
        contProgramas = 0;
        
        proximaDireccionLibre = memoria.tamanoSO;
        modeloInstrucciones.setRowCount(0);
        modeloMemoria.fireTableDataChanged();
        actualizarVistas();
        lblEstado.setText("CPU lista para el siguiente proceso.");
    }

    private void recargarUltimoArchivo() {
        if (ultimoArchivoCargado != null && ultimoArchivoCargado.exists()) {
            limpiarTodo();
            //cargarArchivoAsm(ultimoArchivoCargado);
            lblEstado.setText("Programa recargado desde " + ultimoArchivoCargado.getName());
        } else {
            JOptionPane.showMessageDialog(this, "No hay un archivo cargado para recargar.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void cargarEnMemoria(Proceso p) {
        Programa cargado = p.programa;

        // asignar base en la proximaDireccionLibre
        int baseCodigo = proximaDireccionLibre;
        int baseDatos = baseCodigo + cargado.longitud();
        if (baseDatos >= memoria.tamano) {
            // No hay espacio contiguo en memoria (caso raro si compactación no fue suficiente)
            JOptionPane.showMessageDialog(this, "No hay espacio en memoria para cargar el proceso en memoria.", "Memoria llena", JOptionPane.ERROR_MESSAGE);
            // poner en espera
            p.bcp.cambiarEstado(EstadoProceso.ESPERA);
            colaEspera.add(p);
            return;
        }

        // Escribir programa en memoria
        for (int i = 0; i < cargado.longitud(); i++) {
            memoria.asignarCelda(baseCodigo + i, cargado.lineaOriginal(i));
        }

        // completar BCP con direcciones reales
        p.bcp.baseCodigo = baseCodigo;
        p.bcp.limiteCodigo = baseCodigo + cargado.longitud() - 1;
        p.bcp.baseDatos = baseDatos;
        p.bcp.cambiarEstado(EstadoProceso.LISTO);

        // Encadenar en la lista enlazada (al final)
        if (cabeza == null) {
            cabeza = p;
            cola = p;
        } else {
            cola.siguiente = p;
            cola = p;
        }

        proximaDireccionLibre = baseDatos; // avanzar el puntero
        contProgramas++;

        modeloMemoria.fireTableDataChanged();
    }

    private void cargarDesdeChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos ASM", "asm"));

        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File archivo = chooser.getSelectedFile();

            if (!archivo.getName().toLowerCase().endsWith(".asm")) {
                JOptionPane.showMessageDialog(this,
                        "Solo se permiten archivos con extensión .asm",
                        "Archivo inválido", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Parseamos primero para tener Programa (esto evita repetir parse más tarde)
            try {
                List<String> lineas = Files.readAllLines(archivo.toPath(), StandardCharsets.UTF_8);
                Programa cargado = Cargador.parsear(lineas);

                BCP nuevoBCP = new BCP();
                nuevoBCP.idProceso = contadorProcesos;
                nuevoBCP.cambiarEstado(EstadoProceso.NUEVO); // aún no está en memoria

                Proceso nuevo = new Proceso(contadorProcesos, cargado, nuevoBCP, archivo);
                contadorProcesos++;

                // Si hay menos de 5 programas en memoria -> cargar en memoria
                if (contProgramas < 5) {
                    cargarEnMemoria(nuevo);
                    JOptionPane.showMessageDialog(this,
                            "Archivo #" + nuevo.bcp.idProceso + " cargado en memoria.",
                            "Archivo Cargado", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    // poner en cola de espera
                    nuevo.bcp.cambiarEstado(EstadoProceso.ESPERA);
                    colaEspera.add(nuevo);
                    JOptionPane.showMessageDialog(this,
                            "Archivo #" + nuevo.bcp.idProceso + " puesto en cola de espera.",
                            "En espera", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Formato .asm inválido", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void cargarArchivoAsm(File archivo, int idProceso) {
        try {
            List<String> lineas = Files.readAllLines(archivo.toPath(), StandardCharsets.UTF_8);
            Programa cargado = Cargador.parsear(lineas);

            int baseCodigo = proximaDireccionLibre;
            int baseDatos = baseCodigo + cargado.longitud();
            if (baseDatos >= memoria.tamano) throw new ExcepcionAsm("Memoria insuficiente.");

            // Escribir programa en memoria
            for (int i = 0; i < cargado.longitud(); i++) {
                memoria.asignarCelda(baseCodigo + i, cargado.lineaOriginal(i));
            }

            // Crear BCP
            BCP nuevoBCP = new BCP();
            nuevoBCP.idProceso = idProceso;
            nuevoBCP.cambiarEstado(EstadoProceso.LISTO);
            nuevoBCP.baseCodigo = baseCodigo;
            nuevoBCP.limiteCodigo = baseCodigo + cargado.longitud() - 1;
            nuevoBCP.baseDatos = baseDatos;

            // Crear Proceso
            Proceso nuevo = new Proceso(idProceso, cargado, nuevoBCP, archivo);

            // Encadenar en la lista
            if (cabeza == null) {
                cabeza = nuevo;
                cola = nuevo;
            } else {
                cola.siguiente = nuevo;
                cola = nuevo;
            }

            // Avanzar puntero de memoria libre
            proximaDireccionLibre = baseDatos;
            contProgramas++;
            

            JOptionPane.showMessageDialog(this,
                    "Archivo #"+ idProceso+ " cargado correctamente",
                    "Archivo Cargado", JOptionPane.INFORMATION_MESSAGE);
            


        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Formato .asm inválido", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void compactarMemoria() {
        // Empieza a partir del SO
        int base = memoria.tamanoSO;

        Proceso cursor = cabeza;
        while (cursor != null) {
            Programa prog = cursor.programa;
            int longitud = prog.longitud();

            // Copiar las líneas del programa a la nueva base
            for (int i = 0; i < longitud; i++) {
                memoria.asignarCelda(base + i, prog.lineaOriginal(i));
            }

            // Actualizar BCP con las nuevas direcciones
            cursor.bcp.baseCodigo = base;
            cursor.bcp.limiteCodigo = base + longitud - 1;
            cursor.bcp.baseDatos = base + longitud;

            base += longitud;
            cursor = cursor.siguiente;
        }

        // Limpiar el resto de memoria de usuario (opcional, para evitar restos)
        for (int i = base; i < memoria.tamano; i++) {
            memoria.asignarCelda(i, "");
        }

        proximaDireccionLibre = base;
        modeloMemoria.fireTableDataChanged();
    }

    private void ejecutarPaso() {
        // Si no hay proceso actual, arrancamos desde la cabeza
        if (procesoActual == null) {
            procesoActual = cabeza;
            if (procesoActual == null) {
                lblEstado.setText("No hay programas cargados.");
                return;
            }
            cpu.reiniciar();
            bcp = procesoActual.bcp;
            lblEstado.setText("Ejecutando: " + procesoActual.archivo.getName());

            // Mostrar instrucciones del proceso actual
            System.out.println(procesoActual.programa.instrucciones);
            modeloInstrucciones.setRowCount(0);
            for (Instruccion inst : procesoActual.programa.instrucciones) {
                modeloInstrucciones.addRow(new Object[]{
                        inst.toString(),
                        inst.aBinario()
                });
            }
        }
        procesoActual.bcp.cambiarEstado(EstadoProceso.EJECUTANDO);

        // Si terminó este proceso → pasar al siguiente
        if (cpu.estado == CPU.Estado.TERMINADO || cpu.PC >= procesoActual.programa.longitud()) {
            // marcaremos el proceso actual como terminado
            procesoActual.bcp.cambiarEstado(EstadoProceso.TERMINADO);
            actualizarVistas();

            // Si el proceso actual es la cabeza, la removemos de la lista enlazada
            if (procesoActual == cabeza) {
                cabeza = cabeza.siguiente;
                // si removimos la cola también, actualizar cola
                if (cabeza == null) cola = null;
            } else {
                // Si en algún caso procesoActual no fuera la cabeza (por seguridad), 
                // recorremos y eliminamos el nodo actual de la lista enlazada.
                Proceso p = cabeza;
                while (p != null && p.siguiente != null) {
                    if (p.siguiente == procesoActual) {
                        p.siguiente = procesoActual.siguiente;
                        if (p.siguiente == null) cola = p;
                        break;
                    }
                    p = p.siguiente;
                }
            }

            // reducir contador de procesos en memoria
            contProgramas = Math.max(0, contProgramas - 1);

            // compactar memoria para eliminar huecos y actualizar direcciones
            //compactarMemoria();

            // si hay procesos en cola de espera -> traer el primero a memoria
            if (!colaEspera.isEmpty()) {
                Proceso siguienteEnEspera = colaEspera.poll();
                // cargarEnMemoria colocará el proceso al final (en proximaDireccionLibre)
                cargarEnMemoria(siguienteEnEspera);
            }

            // cambiar el procesoActual al nuevo cabeza (el siguiente a ejecutar)
            procesoActual = cabeza;
            if (procesoActual == null) {
                lblEstado.setText("Todos los programas han sido ejecutados.");
                modeloInstrucciones.setRowCount(0);
                actualizarVistas();
                return;
            } else {
                // Reiniciar CPU para nuevo proceso y mostrar sus instrucciones
                cpu.reiniciar();
                bcp = procesoActual.bcp;
                bcp.cambiarEstado(EstadoProceso.EJECUTANDO);
                lblEstado.setText("Ejecutando: " + procesoActual.archivo.getName());

                modeloInstrucciones.setRowCount(0);
                for (Instruccion inst : procesoActual.programa.instrucciones) {
                    modeloInstrucciones.addRow(new Object[]{
                            inst.toString(),
                            inst.aBinario()
                    });
                }
                return;
            }
        }

        // Ejecutar instrucción actual
        Instruccion inst = procesoActual.programa.obtener(cpu.PC);
        ejecutarInstruccion(inst);
        cpu.PC++;

        actualizarVistas();
        modeloMemoria.fireTableDataChanged();
    }



    private void ejecutarInstruccion(Instruccion inst) {
        String op = inst.opcode;
        List<String> args = inst.operandos;
        try {
            switch (op) {
                case "MOV":
                    int val = obtenerValorOperando(args.get(1));
                    cpu.asignarRegistro(args.get(0), val);
                    break;
                case "LOAD":
                    cpu.AC = cpu.obtenerRegistro(args.get(0));
                    break;
                case "STORE":
                    int direccion = procesoActual.bcp.baseDatos + cpu.obtenerRegistro(args.get(0));
                    String valor = String.valueOf(cpu.AC);
                    memoria.asignarCelda(direccion, valor);

                    // Guardamos en el BCP
                    procesoActual.bcp.ultimoResultado = "Dir " + direccion + " = " + valor;
                    break;
                case "ADD":
                    cpu.AC += cpu.obtenerRegistro(args.get(0));
                    cpu.ZF = (cpu.AC == 0);
                    break;
                case "SUB":
                    cpu.AC -= cpu.obtenerRegistro(args.get(0));
                    cpu.ZF = (cpu.AC == 0);
                    break;
                case "JMP":
                    cpu.PC = procesoActual.programa.etiquetas.getOrDefault(args.get(0), cpu.PC);
                    break;
                case "JZ":
                    if (cpu.ZF) cpu.PC = procesoActual.programa.etiquetas.getOrDefault(args.get(0), cpu.PC);
                    break;
                case "JNZ":
                    if (!cpu.ZF) cpu.PC = procesoActual.programa.etiquetas.getOrDefault(args.get(0), cpu.PC);
                    break;
                case "HALT":
                    cpu.estado = CPU.Estado.TERMINADO;
                    break;
                case "NOP":
                    break;
                default:
                    throw new RuntimeException("Instrucción no implementada: " + op);
            }
        } catch (Exception e) {
            cpu.estado = CPU.Estado.ERROR;
            procesoActual.bcp.cambiarEstado(EstadoProceso.ERROR);
            lblEstado.setText("Error en instrucción: " + inst.opcode + " -> " + e.getMessage());
            temporizador.stop();
        }
    }


    private int obtenerValorOperando(String token) {
        if (cpu.registros.containsKey(token)) return cpu.obtenerRegistro(token);
        if (token.equals("AC")) return cpu.AC;
        if (token.matches("[-+]?[0-9]+")) return Integer.parseInt(token);
        throw new RuntimeException("Operando inválido: " + token);
    }

    private void actualizarVistas() {
        lblPC.setText(String.valueOf(cpu.PC));
        lblAC.setText(String.valueOf(cpu.AC));
        lblAX.setText(String.valueOf(cpu.obtenerRegistro("AX")));
        lblBX.setText(String.valueOf(cpu.obtenerRegistro("BX")));
        lblCX.setText(String.valueOf(cpu.obtenerRegistro("CX")));
        lblDX.setText(String.valueOf(cpu.obtenerRegistro("DX")));
        lblZF.setText(String.valueOf(cpu.ZF));

        if (procesoActual != null) {
            BCP bcp = procesoActual.bcp;
            lblIdProceso.setText(String.valueOf(bcp.idProceso));
            lblEstadoBCP.setText(bcp.estado.toString());
            lblBaseCodigo.setText(String.valueOf(bcp.baseCodigo));
            lblLimiteCodigo.setText(String.valueOf(bcp.limiteCodigo));
            lblBaseDatos.setText(String.valueOf(bcp.baseDatos));
            lblUltimoResultado.setText(bcp.ultimoResultado);

            Programa prog = procesoActual.programa;
            if (prog != null && cpu.PC < prog.longitud()) {
                lblIR.setText(prog.obtener(cpu.PC).aBinario());
            }
        }
    }

    public BCP getBcp() {
        return bcp;
    }
    

    private int obtenerPCAbsoluto() {
        if (procesoActual == null || procesoActual.programa == null) return -1;
        return procesoActual.bcp.baseCodigo + cpu.PC;
    }
    
    private List<BCP> obtenerTodosLosBCPs() {
        List<BCP> lista = new ArrayList<>();

        // Procesos en memoria
        Proceso cursor = cabeza;
        while (cursor != null) {
            lista.add(cursor.bcp);
            cursor = cursor.siguiente;
        }

        // Procesos en espera
        for (Proceso p : colaEspera) {
            lista.add(p.bcp);
        }

        return lista;
    }
    
    private void mostrarEstadosBCP() {
        // Crear tabla con los datos de todos los procesos
        String[] columnas = {"PID", "Estado", "Base Código", "Límite Código", "Base Datos"};
        List<BCP> todos = obtenerTodosLosBCPs(); // función que recorre tu lista de procesos y retorna todos los BCP

        Object[][] datos = new Object[todos.size()][columnas.length];
        for (int i = 0; i < todos.size(); i++) {
            BCP bcp = todos.get(i);
            datos[i][0] = bcp.idProceso;
            datos[i][1] = bcp.estado;
            datos[i][2] = bcp.baseCodigo;
            datos[i][3] = bcp.limiteCodigo;
            datos[i][4] = bcp.baseDatos;
        }

        JTable tabla = new JTable(datos, columnas);
        JScrollPane scroll = new JScrollPane(tabla);

        JDialog dialogo = new JDialog(this, "Estados de procesos", true);
        dialogo.add(scroll);
        dialogo.setSize(500, 300);
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }
}
