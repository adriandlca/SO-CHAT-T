package Views;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class VentanaContactos extends JFrame {

    private JList<String> listaContactos;
    private DefaultListModel<String> modeloLista;
    private JList<String> listaGrupos;
    private DefaultListModel<String> modeloListaGrupos;

    public interface ContactoListener {
        void onContactoSeleccionado(String nombreContacto);
    }
    
    public interface GrupoListener {
        void onCrearGrupo(String nombreGrupo);
        void onUnirseGrupo(String nombreGrupo);
    }

    private ContactoListener contactoListener;
    private GrupoListener grupoListener;

    public VentanaContactos(String miUsuario) {
        super("Dashboard - " + miUsuario);
        this.setLayout(new BorderLayout());
        this.getContentPane().setBackground(new Color(248, 250, 252));

        // --- ENCABEZADO SUPERIOR PERFIL ---
        JPanel panelPerfil = new JPanel(new BorderLayout());
        panelPerfil.setBackground(new Color(15, 23, 42)); // Azul pizarra muy oscuro
        panelPerfil.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        JLabel lblUser = new JLabel(miUsuario);
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblUser.setForeground(Color.WHITE);
        
        JLabel lblEstado = new JLabel("● En línea (Red Local)");
        lblEstado.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblEstado.setForeground(new Color(34, 197, 94)); // Verde brillante
        
        panelPerfil.add(lblUser, BorderLayout.NORTH);
        panelPerfil.add(lblEstado, BorderLayout.SOUTH);
        this.add(panelPerfil, BorderLayout.NORTH);

        // Panel divisor central
        JPanel panelCentral = new JPanel(new GridLayout(2, 1, 0, 15));
        panelCentral.setBorder(new EmptyBorder(15, 15, 15, 15));
        panelCentral.setBackground(new Color(248, 250, 252));

        // --- LISTA DE CONTACTOS ---
        JPanel panelContactos = new JPanel(new BorderLayout(0, 5));
        panelContactos.setBackground(new Color(248, 250, 252));
        JLabel lblHeader = new JLabel("USUARIOS ACTIVOS");
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblHeader.setForeground(new Color(148, 163, 184));
        panelContactos.add(lblHeader, BorderLayout.NORTH);

        modeloLista = new DefaultListModel<>();
        listaContactos = new JList<>(modeloLista);
        configurarEstiloLista(listaContactos);

        listaContactos.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String seleccionado = listaContactos.getSelectedValue();
                    if (seleccionado != null && contactoListener != null) {
                        contactoListener.onContactoSeleccionado(seleccionado);
                    }
                }
            }
        });

        panelContactos.add(crearScrollEstilizado(listaContactos), BorderLayout.CENTER);
        panelCentral.add(panelContactos);

        // --- LISTA DE GRUPOS ACTIVOS ---
        JPanel panelGrupos = new JPanel(new BorderLayout(0, 5));
        panelGrupos.setBackground(new Color(248, 250, 252));
        JLabel lblGrupos = new JLabel("GRUPOS DISPONIBLES");
        lblGrupos.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblGrupos.setForeground(new Color(148, 163, 184));
        panelGrupos.add(lblGrupos, BorderLayout.NORTH);

        modeloListaGrupos = new DefaultListModel<>();
        listaGrupos = new JList<>(modeloListaGrupos);
        configurarEstiloLista(listaGrupos);

        listaGrupos.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String grupo = listaGrupos.getSelectedValue();
                    if (grupo != null && grupoListener != null) {
                        grupoListener.onUnirseGrupo(grupo);
                    }
                }
            }
        });

        panelGrupos.add(crearScrollEstilizado(listaGrupos), BorderLayout.CENTER);
        panelCentral.add(panelGrupos);

        this.add(panelCentral, BorderLayout.CENTER);

        // --- BOTÓN PARA CREAR NUEVO GRUPO ---
        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.setBorder(new EmptyBorder(0, 15, 15, 15));
        panelInferior.setBackground(new Color(248, 250, 252));
        
        JButton btnCrear = new JButton("+ Crear Nuevo Grupo");
        btnCrear.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnCrear.setForeground(new Color(37, 99, 235));
        btnCrear.setBackground(Color.WHITE);
        btnCrear.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(191, 219, 254), 1, true),
                new EmptyBorder(10, 10, 10, 10)
        ));
        btnCrear.setFocusable(false);
        btnCrear.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnCrear.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { btnCrear.setBackground(new Color(239, 246, 255)); }
            public void mouseExited(MouseEvent evt) { btnCrear.setBackground(Color.WHITE); }
        });

        btnCrear.addActionListener(e -> {
            String nombre = JOptionPane.showInputDialog(this, "Ingresa el nombre del nuevo grupo:");
            if (nombre != null && !nombre.trim().isEmpty() && grupoListener != null) {
                grupoListener.onCrearGrupo(nombre.trim());
            }
        });

        panelInferior.add(btnCrear, BorderLayout.CENTER);
        this.add(panelInferior, BorderLayout.SOUTH);

        this.setSize(320, 560); 
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
    }

    private void configurarEstiloLista(JList<String> lista) {
        lista.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lista.setFixedCellHeight(35); // Más separación y altura por ítem
        lista.setSelectionBackground(new Color(239, 246, 255)); // Azul suave moderno al seleccionar
        lista.setSelectionForeground(new Color(29, 78, 216));   // Texto azul al seleccionar
        lista.setBorder(new EmptyBorder(5, 5, 5, 5));
        lista.setBackground(Color.WHITE);
    }

    private JScrollPane crearScrollEstilizado(JList<String> lista) {
        JScrollPane scroll = new JScrollPane(lista);
        scroll.setBorder(new LineBorder(new Color(226, 232, 240), 1, true));
        return scroll;
    }

    public void actualizarLista(String[] usuarios) {
        modeloLista.clear();
        for (String u : usuarios) {
            modeloLista.addElement(u);
        }
    }

    public void actualizarListaGrupos(String[] grupos) {
        modeloListaGrupos.clear();
        for (String g : grupos) {
            if (!g.trim().isEmpty()) {
                modeloListaGrupos.addElement(g);
            }
        }
    }

    public void setContactoListener(ContactoListener listener) {
        this.contactoListener = listener;
    }
    
    public void setGrupoListener(GrupoListener listener) {
        this.grupoListener = listener;
    }
}