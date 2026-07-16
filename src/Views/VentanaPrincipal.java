package Views;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class VentanaPrincipal extends JFrame {

    private JTextField txtIp;
    private JTextField txtPuerto;
    private JTextField txtUsuario;
    private JButton btnConectar;

    public interface ConexionListener {
        void onConectar(String ipDestino, int puerto, String usuario);
    }
    private ConexionListener listener;

    public VentanaPrincipal() {
        super("JAM-Chat - Iniciar Sesión");
        this.setLayout(new BorderLayout());
        this.getContentPane().setBackground(new Color(248, 250, 252)); 
        this.add(crearPanelConexion(), BorderLayout.CENTER);
        
        this.setSize(340, 460); 
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setResizable(false);
    }

    private JPanel crearPanelConexion() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(248, 250, 252));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 30, 20, 30); // Márgenes laterales anchos
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        // --- TÍTULO ---
        JLabel lblTitulo = new JLabel("JAM-Chat", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitulo.setForeground(new Color(15, 23, 42)); // Color "Slate" oscuro
        gbc.gridy = 0;
        gbc.insets = new Insets(20, 30, 5, 30);
        panel.add(lblTitulo, gbc);

        JLabel lblSubtitulo = new JLabel("Ingresa para conectar al servidor", SwingConstants.CENTER);
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitulo.setForeground(new Color(100, 116, 139));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 30, 30, 30);
        panel.add(lblSubtitulo, gbc);

        // --- CAMPOS DE TEXTO ---
        gbc.insets = new Insets(5, 30, 15, 30);

        txtUsuario = crearCampoEstilizado("");
        gbc.gridy = 2;
        panel.add(crearFilaCampo("Tu Usuario:", txtUsuario), gbc);

        txtIp = crearCampoEstilizado("127.0.0.1");
        gbc.gridy = 3;
        panel.add(crearFilaCampo("IP Servidor:", txtIp), gbc);

        txtPuerto = crearCampoEstilizado("5050");
        gbc.gridy = 4;
        panel.add(crearFilaCampo("Puerto:", txtPuerto), gbc);

        btnConectar = new JButton("Ingresar");
        btnConectar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnConectar.setBackground(new Color(37, 99, 235)); // Azul rey
        btnConectar.setForeground(Color.WHITE);
        btnConectar.setFocusPainted(false);
        btnConectar.setBorder(new EmptyBorder(12, 15, 12, 15));
        btnConectar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnConectar.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                btnConectar.setBackground(new Color(29, 78, 216)); // Azul más oscuro
            }
            public void mouseExited(MouseEvent evt) {
                btnConectar.setBackground(new Color(37, 99, 235)); // Vuelve al normal
            }
        });

        btnConectar.addActionListener(e -> {
            String ip = txtIp.getText().trim();
            String usu = txtUsuario.getText().trim();
            if (!ip.isEmpty() && !usu.isEmpty() && listener != null) {
                try {
                    int puerto = Integer.parseInt(txtPuerto.getText().trim());
                    listener.onConectar(ip, puerto, usu);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "El puerto debe ser un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Debe ingresar un usuario y una IP válida.", "Campos vacíos", JOptionPane.WARNING_MESSAGE);
            }
        });

        gbc.gridy = 5;
        gbc.insets = new Insets(15, 30, 20, 30);
        panel.add(btnConectar, gbc);

        return panel;
    }

    // --- MÉTODOS AUXILIARES DE DISEÑO ---
    private JTextField crearCampoEstilizado(String textoPorDefecto) {
        JTextField campo = new JTextField(textoPorDefecto);
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        campo.setForeground(new Color(51, 65, 85));
        campo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
        return campo;
    }

    private JPanel crearFilaCampo(String etiqueta, JTextField campo) {
        JPanel panelFila = new JPanel(new BorderLayout(0, 5)); 
        panelFila.setBackground(new Color(248, 250, 252));
        
        JLabel lbl = new JLabel(etiqueta);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(71, 85, 105)); 
        
        panelFila.add(lbl, BorderLayout.NORTH);
        panelFila.add(campo, BorderLayout.CENTER);
        return panelFila;
    }

    public void setConexionListener(ConexionListener listener) {
        this.listener = listener;
    }
}