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
import java.util.List;
import java.util.*;

public class VentanaSimulador extends JFrame {

    private final Memoria memoria = new Memoria(512, 64);
    private final CPU cpu = new CPU();
    private final BCP bcp = new BCP();
    private Programa programa = null;

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

    private Temporizador temporizador;
    
    private Instruccion instruccionActual = null;
    private int ciclosPendientes = 0;
    
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

        return p;
    }

    private void limpiarTodo() {
        cpu.reiniciar();
        bcp.reiniciar();
        programa = null;
        memoria.limpiarUsuario();
        modeloInstrucciones.setRowCount(0);
        modeloMemoria.fireTableDataChanged();
        actualizarVistas();
        lblEstado.setText("Reset completado.");
    }

    private void recargarUltimoArchivo() {
        if (ultimoArchivoCargado != null && ultimoArchivoCargado.exists()) {
            limpiarTodo();
            cargarArchivoAsm(ultimoArchivoCargado);
            lblEstado.setText("Programa recargado desde " + ultimoArchivoCargado.getName());
        } else {
            JOptionPane.showMessageDialog(this, "No hay un archivo cargado para recargar.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void cargarDesdeChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos ASM", "asm"));
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File archivo = chooser.getSelectedFile();
            if (!archivo.getName().toLowerCase().endsWith(".asm")) {
                JOptionPane.showMessageDialog(this, "Solo se permiten archivos con extensión .asm",
                        "Archivo inválido", JOptionPane.ERROR_MESSAGE);
                return;
            }
            ultimoArchivoCargado = archivo;
            cargarArchivoAsm(archivo);
        }
    }

    private void cargarArchivoAsm(File archivo) {
        try {
            List<String> lineas = Files.readAllLines(archivo.toPath(), StandardCharsets.UTF_8);
            Programa cargado = Cargador.parsear(lineas);

            int baseCodigo = memoria.tamanoSO;
            int baseDatos = baseCodigo + cargado.longitud();
            if (baseDatos >= memoria.tamano) throw new ExcepcionAsm("Memoria insuficiente.");

            memoria.limpiarUsuario();
            for (int i = 0; i < cargado.longitud(); i++) {
                memoria.asignarCelda(baseCodigo + i, cargado.lineaOriginal(i));
            }

            programa = cargado;
            cpu.reiniciar();
            bcp.reiniciar();
            bcp.idProceso = 1;
            bcp.estado = "LISTO";
            bcp.baseCodigo = baseCodigo;
            bcp.limiteCodigo = baseCodigo + cargado.longitud() - 1;
            bcp.baseDatos = baseDatos;

            modeloInstrucciones.setRowCount(0);
            for (Instruccion inst : programa.instrucciones) {
                modeloInstrucciones.addRow(new Object[]{
                        inst.toString(),
                        inst.aBinario()
                });
            }

            actualizarVistas();
            modeloMemoria.fireTableDataChanged();
            lblEstado.setText("Programa cargado en memoria.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Formato .asm inválido", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ejecutarPaso() {
        if (programa == null) return;
        if (cpu.estado == CPU.Estado.TERMINADO) {
            temporizador.detener();
            lblEstado.setText("Programa finalizado.");
            return;
        }

        if (instruccionActual == null) {
            if (cpu.PC >= programa.longitud()) {
                cpu.estado = CPU.Estado.TERMINADO;
                lblEstado.setText("Fin del programa.");
                return;
            }
            instruccionActual = programa.obtener(cpu.PC);
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
                case "MOV":
                    int val = obtenerValorOperando(args.get(1));
                    cpu.asignarRegistro(args.get(0), val);
                    break;

                case "LOAD":
                    cpu.AC = cpu.obtenerRegistro(args.get(0));
                    cpu.ZF = (cpu.AC == 0);
                    break;

                case "STORE":
                    memoria.asignarCelda(bcp.baseDatos + cpu.obtenerRegistro(args.get(0)), String.valueOf(cpu.AC));
                    break;

                case "ADD":
                    cpu.AC += cpu.obtenerRegistro(args.get(0));
                    cpu.ZF = (cpu.AC == 0);
                    break;

                case "SUB":
                    cpu.AC -= cpu.obtenerRegistro(args.get(0));
                    cpu.ZF = (cpu.AC == 0);
                    break;

                case "INC":
                    if (args.isEmpty()) {
                        cpu.AC++;
                        cpu.ZF = (cpu.AC == 0);
                    } else {
                        String r = args.get(0);
                        int newVal = cpu.obtenerRegistro(r) + 1;
                        cpu.asignarRegistro(r, newVal);
                        cpu.ZF = (newVal == 0);
                    }
                    break;

                case "DEC":
                    if (args.isEmpty()) {
                        cpu.AC--;
                        cpu.ZF = (cpu.AC == 0);
                    } else {
                        String r = args.get(0);
                        int newVal = cpu.obtenerRegistro(r) - 1;
                        cpu.asignarRegistro(r, newVal);
                        cpu.ZF = (newVal == 0);
                    }
                    break;

                case "SWAP":
                    String r1 = args.get(0), r2 = args.get(1);
                    int v1 = cpu.obtenerRegistro(r1);
                    int v2 = cpu.obtenerRegistro(r2);
                    cpu.asignarRegistro(r1, v2);
                    cpu.asignarRegistro(r2, v1);
                    break;

                case "CMP":
                    int a = cpu.obtenerRegistro(args.get(0));
                    int b = cpu.obtenerRegistro(args.get(1));
                    cpu.ZF = (a == b);
                    cpu.CF = (a < b);
                    break;

                case "JE":
                    if (cpu.ZF) cpu.PC = resolverDestino(args.get(0));
                    break;

                case "JNE":
                    if (!cpu.ZF) cpu.PC = resolverDestino(args.get(0));
                    break;

                case "JMP":
                    cpu.PC = resolverDestino(args.get(0));
                    break;

                case "PUSH":
                    cpu.pila.push(cpu.obtenerRegistro(args.get(0)));
                    break;

                case "POP":
                    if (cpu.pila.isEmpty()) throw new RuntimeException("Pila vacía");
                    cpu.asignarRegistro(args.get(0), cpu.pila.pop());
                    break;

                case "PARAM":
                    if (args.size() > 3) throw new RuntimeException("PARAM admite máximo 3 valores");
                    for (String sval : args) {
                        if (!sval.matches("[-+]?[0-9]+")) throw new RuntimeException("PARAM solo acepta números");
                        cpu.pila.push(Integer.parseInt(sval));
                    }
                    break;

                case "INT":
                    manejarINT(args.get(0));
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
            lblEstado.setText("Error en instrucción: " + inst.opcode + " -> " + e.getMessage());
            temporizador.detener();
        }
    }

    private int resolverDestino(String token) {
        token = token.trim();

        if (token.matches("[+-]\\d+")) {
            return cpu.PC + Integer.parseInt(token);
        }

        Integer pos = programa.etiquetas.get(token.toUpperCase());
        if (pos != null) return pos;

        if (token.matches("\\d+")) return Integer.parseInt(token);
        throw new RuntimeException("Destino inválido: " + token);
    }

    private void manejarINT(String code) {
        code = code.toUpperCase().replace("H", "");

        switch (code) {
            case "20":
                cpu.estado = CPU.Estado.TERMINADO;
                temporizador.detener();
                lblEstado.setText("INT 20H -> Programa finalizado");
                break;

            case "10":
                JOptionPane.showMessageDialog(this,
                        "INT 10H -> Valor en DX = " + cpu.obtenerRegistro("DX"),
                        "Salida de pantalla", JOptionPane.INFORMATION_MESSAGE);
                break;

            case "09":
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
                break;


            default:
                throw new RuntimeException("INT no soportado: " + code);
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

        lblIdProceso.setText(String.valueOf(bcp.idProceso));
        lblEstadoBCP.setText(bcp.estado);
        lblBaseCodigo.setText(String.valueOf(bcp.baseCodigo));
        lblLimiteCodigo.setText(String.valueOf(bcp.limiteCodigo));
        lblBaseDatos.setText(String.valueOf(bcp.baseDatos));

        if (programa != null && cpu.PC < programa.longitud()) {
            lblIR.setText(programa.obtener(cpu.PC).aBinario());
        }
    }

    private int obtenerPCAbsoluto() {
        if (programa == null) return -1;
        return bcp.baseCodigo + cpu.PC;
    }
}
