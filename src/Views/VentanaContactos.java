package Views;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

public class VentanaContactos extends JFrame {

    private JList<String> listaContactos;
    private DefaultListModel<String> modeloLista;
    private JList<String> listaGrupos;
    private DefaultListModel<String> modeloListaGrupos;

    // NUEVO: nombres de grupos que son privados (para pintar el candado)
    private Set<String> gruposPrivados = new HashSet<>();

    public interface ContactoListener {
        void onContactoSeleccionado(String nombreContacto);
    }

    public interface GrupoListener {
        // NUEVO: contrasena puede ser null si el grupo es público
        void onCrearGrupo(String nombreGrupo, String contrasena);
        void onUnirseGrupo(String nombreGrupo, String contrasena);
    }

    private ContactoListener contactoListener;
    private GrupoListener grupoListener;

    public VentanaContactos(String miUsuario) {
        super("Dashboard - " + miUsuario);
        this.setLayout(new BorderLayout());
        this.getContentPane().setBackground(new Color(248, 250, 252));

        // --- ENCABEZADO SUPERIOR PERFIL ---
        JPanel panelPerfil = new JPanel(new BorderLayout());
        panelPerfil.setBackground(new Color(15, 23, 42));
        panelPerfil.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel lblUser = new JLabel(miUsuario);
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblUser.setForeground(Color.WHITE);

        JLabel lblEstado = new JLabel("● En línea (Red Local)");
        lblEstado.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblEstado.setForeground(new Color(34, 197, 94));

        panelPerfil.add(lblUser, BorderLayout.NORTH);
        panelPerfil.add(lblEstado, BorderLayout.SOUTH);
        this.add(panelPerfil, BorderLayout.NORTH);

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

        // NUEVO: cell renderer que pinta el candado para grupos privados
        listaGrupos.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    String nombre = value.toString();
                    String display = gruposPrivados.contains(nombre) ? "🔒 " + nombre : nombre;
                    ((JLabel) c).setText(display);
                }
                return c;
            }
        });

        // ACTUALIZADO: doble click pide contraseña si el grupo es privado
        listaGrupos.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String grupo = listaGrupos.getSelectedValue();
                    if (grupo != null && grupoListener != null) {
                        String pwd = null;
                        if (gruposPrivados.contains(grupo)) {
                            pwd = solicitarContrasenaGrupo(grupo);
                            if (pwd == null) return; // Cancelado
                        }
                        grupoListener.onUnirseGrupo(grupo, pwd);
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
            // ACTUALIZADO: diálogo personalizado con checkbox de privacidad
            String[] resultado = mostrarDialogoCrearGrupo();
            if (resultado != null && grupoListener != null) {
                grupoListener.onCrearGrupo(resultado[0], resultado[1]);
            }
        });

        panelInferior.add(btnCrear, BorderLayout.CENTER);
        this.add(panelInferior, BorderLayout.SOUTH);

        this.setSize(320, 560);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
    }

    /**
     * Muestra un JDialog personalizado para crear un grupo.
     * Si el usuario marca el checkbox, aparece un campo de contraseña.
     * @return [nombreGrupo, contrasena] o null si se canceló.
     *         contrasena es null cuando el grupo se crea público.
     */
    private String[] mostrarDialogoCrearGrupo() {
        JDialog dialog = new JDialog(this, "Crear Nuevo Grupo", true);
        dialog.setLayout(new BorderLayout());

        JPanel panelContenido = new JPanel();
        panelContenido.setLayout(new BoxLayout(panelContenido, BoxLayout.Y_AXIS));
        panelContenido.setBorder(new EmptyBorder(15, 20, 15, 20));
        panelContenido.setBackground(new Color(248, 250, 252));

        JLabel lblNombre = new JLabel("Nombre del grupo:");
        lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblNombre.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelContenido.add(lblNombre);
        panelContenido.add(Box.createVerticalStrut(5));

        JTextField txtNombre = new JTextField(20);
        txtNombre.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtNombre.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        txtNombre.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelContenido.add(txtNombre);
        panelContenido.add(Box.createVerticalStrut(15));

        JCheckBox checkPrivado = new JCheckBox("🔒 Hacer chat privado");
        checkPrivado.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        checkPrivado.setBackground(new Color(248, 250, 252));
        checkPrivado.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelContenido.add(checkPrivado);
        panelContenido.add(Box.createVerticalStrut(10));

        JLabel lblPassword = new JLabel("Contraseña:");
        lblPassword.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPassword.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPasswordField txtPassword = new JPasswordField(20);
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel panelPassword = new JPanel();
        panelPassword.setLayout(new BoxLayout(panelPassword, BoxLayout.Y_AXIS));
        panelPassword.setOpaque(false);
        panelPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelPassword.add(lblPassword);
        panelPassword.add(Box.createVerticalStrut(5));
        panelPassword.add(txtPassword);
        panelPassword.setVisible(false);

        panelContenido.add(panelPassword);

        // Al activar/desactivar el checkbox mostramos u ocultamos el password
        checkPrivado.addActionListener(e -> {
            panelPassword.setVisible(checkPrivado.isSelected());
            dialog.pack();
        });

        dialog.add(panelContenido, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panelBotones.setBackground(new Color(248, 250, 252));
        JButton btnCrearAceptar = new JButton("Crear");
        JButton btnCancelar = new JButton("Cancelar");
        panelBotones.add(btnCrearAceptar);
        panelBotones.add(btnCancelar);
        dialog.add(panelBotones, BorderLayout.SOUTH);

        final String[] resultado = new String[2];

        btnCrearAceptar.addActionListener(e -> {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "El nombre del grupo no puede estar vacío.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (checkPrivado.isSelected()) {
                String pwd = new String(txtPassword.getPassword()).trim();
                if (pwd.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog,
                            "La contraseña no puede estar vacía para un grupo privado.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                resultado[0] = nombre;
                resultado[1] = pwd;
            } else {
                resultado[0] = nombre;
                resultado[1] = null;
            }
            dialog.dispose();
        });

        btnCancelar.addActionListener(e -> {
            resultado[0] = null;
            dialog.dispose();
        });

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);

        if (resultado[0] == null) return null;
        return resultado;
    }

    /**
     * Pide al usuario la contraseña para unirse a un grupo privado.
     * @return la contraseña introducida, o null si el usuario canceló.
     */
    private String solicitarContrasenaGrupo(String nombreGrupo) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel lblInfo = new JLabel("🔒 \"" + nombreGrupo + "\" es un grupo privado.");
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lblInfo);
        panel.add(Box.createVerticalStrut(5));

        JLabel lblInfo2 = new JLabel("Ingresa la contraseña para unirte:");
        lblInfo2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblInfo2.setForeground(new Color(100, 116, 139));
        lblInfo2.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lblInfo2);
        panel.add(Box.createVerticalStrut(10));

        JPasswordField txtPwd = new JPasswordField(20);
        txtPwd.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        txtPwd.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(txtPwd);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Grupo Privado", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String pwd = new String(txtPwd.getPassword()).trim();
            return pwd.isEmpty() ? null : pwd;
        }
        return null;
    }

    private void configurarEstiloLista(JList<String> lista) {
        lista.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lista.setFixedCellHeight(35);
        lista.setSelectionBackground(new Color(239, 246, 255));
        lista.setSelectionForeground(new Color(29, 78, 216));
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

    /**
     * NUEVO: actualiza qué grupos son privados para mostrar el candado.
     */
    public void actualizarGruposPrivados(String[] gruposPrivados) {
        this.gruposPrivados.clear();
        for (String g : gruposPrivados) {
            if (g != null && !g.trim().isEmpty()) {
                this.gruposPrivados.add(g);
            }
        }
        listaGrupos.repaint();
    }

    public void setContactoListener(ContactoListener listener) {
        this.contactoListener = listener;
    }

    public void setGrupoListener(GrupoListener listener) {
        this.grupoListener = listener;
    }
}
