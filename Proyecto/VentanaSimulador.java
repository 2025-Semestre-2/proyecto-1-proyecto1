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
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class VentanaSimulador extends JFrame {

    private final Memoria memoria = new Memoria(512, 64);
    private final CPU cpu = new CPU();
    public static BCP bcp = new BCP();
    
    private File ultimoArchivoCargado = null;
    
    private int proximaDireccionLibre = memoria.tamanoSO;  
    private int contProgramas = 0;
    private final Queue<Proceso> colaEspera = new LinkedList<>();
    
    private Proceso cabeza = null;        // primer proceso cargado
    private Proceso cola = null;          // último proceso (para ir encadenando)
    private Proceso procesoActual = null; 
    private int contadorProcesos = 1;     // ID's de los procesos

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

    private final JSpinner spTamMemoria = new JSpinner(new SpinnerNumberModel(512, 16, 4096, 1));
    private final JSpinner spTamSO = new JSpinner(new SpinnerNumberModel(64, 1, 2048, 1));

    private final JButton btnAsignarMemoria = new JButton("Asignar Memoria");
    private final JButton btnCargar = new JButton("Cargar .asm");
    private final JButton btnRecargar = new JButton("Recargar");
    private final JButton btnPaso = new JButton("Paso a paso");
    private final JButton btnEjecutar = new JButton("Ejecutar");
    private final JButton btnDetener = new JButton("Detener");
    private final JButton btnLimpiar = new JButton("Limpiar");
    private final JButton btnEstadisticas = new JButton("Estadísticas");
    JButton btnEstados = new JButton("Ver estados");
    
    private Temporizador temporizador;

    private Instruccion instruccionActual = null;
    private int ciclosPendientes = 0;
    
    private final java.util.List<Estadistica> estadisticas = new ArrayList<>();
    private Estadistica estadisticaActual = null;
    
    private static final Map<String,Integer> DURACIONES = new HashMap<>();
    static {
        DURACIONES.put("LOAD", 2);
        DURACIONES.put("STORE", 2);
        DURACIONES.put("MOV", 5);
        DURACIONES.put("ADD", 3);
        DURACIONES.put("SUB", 3);
        DURACIONES.put("INC", 1);
        DURACIONES.put("DEC", 1);
        DURACIONES.put("SWAP", 1);
        DURACIONES.put("INT", 2);
        DURACIONES.put("JMP", 2);
        DURACIONES.put("CMP", 2);
        DURACIONES.put("JE", 2);
        DURACIONES.put("JNE", 2);
        DURACIONES.put("PARAM", 3);
        DURACIONES.put("PUSH", 1);
        DURACIONES.put("POP", 1);
    }

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
        barraSuperior.add(btnEstadisticas);
        barraSuperior.add(btnEstados);

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
        btnEjecutar.addActionListener(e -> temporizador.iniciar());
        btnDetener.addActionListener(e -> temporizador.detener());
        btnLimpiar.addActionListener(e -> limpiarTodo());
        btnEstados.addActionListener(e -> mostrarEstadosBCP());
        btnEstadisticas.addActionListener(e -> mostrarEstadisticas());
        
        temporizador = new Temporizador(1000, this::ejecutarPaso);
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
            // No hay espacio contiguo en memoria 
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
            } catch (ExcepcionAsm | HeadlessException | IOException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Formato .asm inválido", JOptionPane.ERROR_MESSAGE);
            }
        }
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
            estadisticaActual = new Estadistica(Integer.toString(bcp.idProceso));
            // Mostrar instrucciones del proceso actual
            
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
            // marcamos el proceso actual como terminado
            procesoActual.bcp.cambiarEstado(EstadoProceso.TERMINADO);
            temporizador.detener();
            registrarEstadistica();
            lblEstado.setText("Programa finalizado.");
            actualizarVistas();

            // Si el proceso actual es la cabeza, la removemos de la lista enlazada
            if (procesoActual == cabeza) {
                cabeza = cabeza.siguiente;
                
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

            contProgramas = Math.max(0, contProgramas - 1);

            
            // si hay procesos en cola de espera -> traer el primero a memoria
            if (!colaEspera.isEmpty()) {
                Proceso siguienteEnEspera = colaEspera.poll();
                cargarEnMemoria(siguienteEnEspera);
            }


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
                estadisticaActual = new Estadistica(Integer.toString(bcp.idProceso));
                
                return;
            }
        }
        
    if (instruccionActual == null) {
        if (cpu.PC >= procesoActual.programa.longitud()) {
            cpu.estado = CPU.Estado.TERMINADO;
            registrarEstadistica();
            lblEstado.setText("Fin del programa.");
            return;
        }
        instruccionActual = procesoActual.programa.obtener(cpu.PC);
        ciclosPendientes = DURACIONES.getOrDefault(instruccionActual.opcode, 1);
    }

    ciclosPendientes--;

    if (ciclosPendientes <= 0) {
        ejecutarInstruccion(instruccionActual);
        cpu.PC++;
        instruccionActual = null;
    }

        actualizarVistas();
        modeloMemoria.fireTableDataChanged();
    }



    private void ejecutarInstruccion(Instruccion inst) {
        String op = inst.opcode;
        List<String> args = inst.operandos;
        try {
            switch (op) {
                case "MOV" -> {
                    int val = obtenerValorOperando(args.get(1));
                    cpu.asignarRegistro(args.get(0), val);
                }

                case "LOAD" -> {
                    cpu.AC = cpu.obtenerRegistro(args.get(0));
                    cpu.ZF = (cpu.AC == 0);
                }

                case "STORE" -> {
                    int direccion = procesoActual.bcp.baseDatos + cpu.obtenerRegistro(args.get(0));
                    String valor = String.valueOf(cpu.AC);
                    memoria.asignarCelda(direccion, valor);

                    // Guardamos en el BCP
                    procesoActual.bcp.ultimoResultado = "Dir " + direccion + " = " + valor;
                }

                case "ADD" -> {
                    cpu.AC += cpu.obtenerRegistro(args.get(0));
                    cpu.ZF = (cpu.AC == 0);
                }

                case "SUB" -> {
                    cpu.AC -= cpu.obtenerRegistro(args.get(0));
                    cpu.ZF = (cpu.AC == 0);
                }

                case "INC" -> {
                    if (args.isEmpty()) {
                        cpu.AC++;
                        cpu.ZF = (cpu.AC == 0);
                    } else {
                        String r = args.get(0);
                        int newVal = cpu.obtenerRegistro(r) + 1;
                        cpu.asignarRegistro(r, newVal);
                        cpu.ZF = (newVal == 0);
                    }
                }

                case "DEC" -> {
                    if (args.isEmpty()) {
                        cpu.AC--;
                        cpu.ZF = (cpu.AC == 0);
                    } else {
                        String r = args.get(0);
                        int newVal = cpu.obtenerRegistro(r) - 1;
                        cpu.asignarRegistro(r, newVal);
                        cpu.ZF = (newVal == 0);
                    }
                }

                case "SWAP" -> {
                    String r1 = args.get(0), r2 = args.get(1);
                    int v1 = cpu.obtenerRegistro(r1);
                    int v2 = cpu.obtenerRegistro(r2);
                    cpu.asignarRegistro(r1, v2);
                    cpu.asignarRegistro(r2, v1);
                }

                case "CMP" -> {
                    int a = cpu.obtenerRegistro(args.get(0));
                    int b = cpu.obtenerRegistro(args.get(1));
                    cpu.ZF = (a == b);
                    cpu.CF = (a < b);
                }

                case "JE" -> {
                    if (cpu.ZF) cpu.PC = resolverDestino(args.get(0));
                }

                case "JNE" -> {
                    if (!cpu.ZF) cpu.PC = resolverDestino(args.get(0));
                }

                case "JMP" -> cpu.PC = resolverDestino(args.get(0));

                case "PUSH" -> cpu.pila.push(cpu.obtenerRegistro(args.get(0)));

                case "POP" -> {
                    if (cpu.pila.isEmpty()) throw new RuntimeException("Pila vacía");
                    cpu.asignarRegistro(args.get(0), cpu.pila.pop());
                }

                case "PARAM" -> {
                    if (args.size() > 3) throw new RuntimeException("PARAM admite máximo 3 valores");
                    for (String sval : args) {
                        if (!sval.matches("[-+]?[0-9]+")) throw new RuntimeException("PARAM solo acepta números");
                        cpu.pila.push(Integer.valueOf(sval));
                    }
                }

                case "INT" -> manejarINT(args.get(0));

                case "HALT" -> cpu.estado = CPU.Estado.TERMINADO;

                case "NOP" -> {
                }

                default -> throw new RuntimeException("Instrucción no implementada: " + op);
            }
        } catch (RuntimeException e) {
            cpu.estado = CPU.Estado.ERROR;
            lblEstado.setText("Error en instrucción: " + inst.opcode + " -> " + e.getMessage());
            temporizador.detener();
        }
    }

    private int resolverDestino(String token) {
        token = token.trim();

        if (token.matches("[+-]\\d+")) {
            return cpu.PC + Integer.parseInt(token);
        }

        Integer pos = procesoActual.programa.etiquetas.get(token.toUpperCase());
        if (pos != null) return pos;

        if (token.matches("\\d+")) return Integer.parseInt(token);
        throw new RuntimeException("Destino inválido: " + token);
    }
    
    private void manejarINT(String code) {
        code = code.toUpperCase().replace("H", "");

        switch (code) {
            case "20" -> {
                cpu.estado = CPU.Estado.TERMINADO;
                temporizador.detener();
                lblEstado.setText("INT 20H -> Programa finalizado");
            }

            case "10" -> JOptionPane.showMessageDialog(this,
                        "INT 10H -> Valor en DX = " + cpu.obtenerRegistro("DX"),
                        "Salida de pantalla", JOptionPane.INFORMATION_MESSAGE);

            case "09" -> {
                while (true) {
                    JTextField txtInput = new JTextField();
                    txtInput.setDocument(new javax.swing.text.PlainDocument() {
                        @Override
                        public void insertString(int offs, String str, javax.swing.text.AttributeSet a) throws javax.swing.text.BadLocationException {
                            if (str == null) return;
                            if (getLength() + str.length() > 3) return;
                            if (!str.matches("\\d+")) return;
                            super.insertString(offs, str, a);
                        }
                    });

                    int ok = JOptionPane.showConfirmDialog(this, txtInput,
                            "INT 09H -> Ingrese número (0-255)", JOptionPane.OK_CANCEL_OPTION);

                    if (ok != JOptionPane.OK_OPTION) {
                        continue;
                    }

                    String valStr = txtInput.getText().trim();
                    if (valStr.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Debe ingresar un valor.", "Error", JOptionPane.ERROR_MESSAGE);
                        continue;
                    }

                    try {
                        int val = Integer.parseInt(valStr);
                        if (val < 0 || val > 255) {
                            JOptionPane.showMessageDialog(this, "El valor debe estar entre 0 y 255.", "Error", JOptionPane.ERROR_MESSAGE);
                            continue;
                        }
                        cpu.asignarRegistro("DX", val);
                        lblEstado.setText("INT 09H -> DX = " + val);
                        break;
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Entrada inválida. Debe ser numérica.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }


            default -> throw new RuntimeException("INT no soportado: " + code);
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
            BCP bcpLocal = procesoActual.bcp;
            lblIdProceso.setText(String.valueOf(bcpLocal.idProceso));
            lblEstadoBCP.setText(bcpLocal.estado.toString());
            lblBaseCodigo.setText(String.valueOf(bcpLocal.baseCodigo));
            lblLimiteCodigo.setText(String.valueOf(bcpLocal.limiteCodigo));
            lblBaseDatos.setText(String.valueOf(bcpLocal.baseDatos));
            lblUltimoResultado.setText(bcpLocal.ultimoResultado);

            Programa prog = procesoActual.programa;
            if (prog != null && cpu.PC < prog.longitud()) {
                lblIR.setText(prog.obtener(cpu.PC).aBinario());
            }
        }
    }
    
    private void registrarEstadistica() {
        if (estadisticaActual != null) {
            estadisticaActual.marcarFin();
            estadisticas.add(estadisticaActual);
            JOptionPane.showMessageDialog(this,
                    "Programa finalizado.\n" + estadisticaActual.toString(),
                    "Estadística", JOptionPane.INFORMATION_MESSAGE);
            estadisticaActual = null;
        }
    }

    private void mostrarEstadisticas() {
        if (estadisticas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay estadísticas registradas.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Estadistica est : estadisticas) {
            sb.append(est.toString()).append("\n");
        }
        JTextArea area = new JTextArea(sb.toString(), 10, 50);
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
                "Historial de Estadísticas", JOptionPane.INFORMATION_MESSAGE);
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
            BCP bcpLocal = todos.get(i);
            datos[i][0] = bcpLocal.idProceso;
            datos[i][1] = bcpLocal.estado;
            datos[i][2] = bcpLocal.baseCodigo;
            datos[i][3] = bcpLocal.limiteCodigo;
            datos[i][4] = bcpLocal.baseDatos;
        }

        JTable tabla = new JTable(datos, columnas);
        JScrollPane scroll = new JScrollPane(tabla);

        JDialog dialogo = new JDialog(this, "Estados de procesos", true);
        dialogo.add(scroll);
        dialogo.setSize(500, 300);
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }
    
    public BCP getBcp() {
        return bcp;
    }

}