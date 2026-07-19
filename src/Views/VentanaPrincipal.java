package Views;

import Views.theme.Avatar;
import Views.theme.GhostButton;
import Views.theme.MaterialGlyph;
import Views.theme.PrimaryButton;
import Views.theme.RoundBorder;
import Views.theme.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * Rediseño de la pantalla de login. Layout 380x520, modal centrada,
 * look Material 3 derivado del mock Stitch (tarjeta elevada + sombra suave).
 *
 * API pública intacta: {@link #setConexionListener(ConexionListener)} y la
 * interfaz {@link ConexionListener} con su método {@code onConectar}.
 */
public class VentanaPrincipal extends JFrame {

    private JTextField txtIp;
    private JTextField txtPuerto;
    private JTextField txtUsuario;
    private JButton btnConectar;
    private JLabel lblError;

    public interface ConexionListener {
        void onConectar(String ipDestino, int puerto, String usuario);
    }
    private ConexionListener listener;

    public VentanaPrincipal() {
        super("JAM-Chat - Iniciar sesión");
        this.setLayout(new BorderLayout());
        this.getContentPane().setBackground(Theme.background());
        this.add(crearPanelConexion(), BorderLayout.CENTER);

        this.setSize(380, 520);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setResizable(false);
    }

    private JPanel crearPanelConexion() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.background());

        // Tarjeta elevada (jpanel blanco con borde redondeado + sombra)
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override public boolean isOpaque() { return false; }
        };
        card.setOpaque(false);
        card.setBorder(new Views.theme.SoftShadowBorder(Theme.RADIUS_LG));

        // Wrap en un panel con borde-radius (BasicPanel blanco)
        JPanel cardHolder = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.surfaceLowest());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_LG, Theme.RADIUS_LG);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        cardHolder.setOpaque(false);
        cardHolder.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 24, 8, 24);

        // --- LOGO CIRCULAR FLOTANTE (Inter blue) ---
        Avatar logoAvatar = new Avatar("JAM", 56, Theme.primary(), Theme.tertiary());
        JPanel logoWrap = new JPanel(new BorderLayout());
        logoWrap.setOpaque(false);
        logoWrap.setBorder(BorderFactory.createEmptyBorder(24, 0, 12, 0));
        logoWrap.add(logoAvatar, BorderLayout.CENTER);
        gbc.gridy = 0;
        gbc.insets = new Insets(28, 24, 0, 24);
        cardHolder.add(logoWrap, gbc);

        // --- TÍTULO + SUBTÍTULO ---
        JLabel lblTitulo = new JLabel("JAM-Chat", SwingConstants.CENTER);
        lblTitulo.setFont(Theme.fontTitle());
        lblTitulo.setForeground(Theme.onSurface());
        gbc.gridy = 1;
        gbc.insets = new Insets(8, 24, 4, 24);
        cardHolder.add(lblTitulo, gbc);

        JLabel lblSub = new JLabel("Conéctate al servidor de chat", SwingConstants.CENTER);
        lblSub.setFont(Theme.font(13, java.awt.Font.PLAIN));
        lblSub.setForeground(Theme.onSurfaceVariant());
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 24, 18, 24);
        cardHolder.add(lblSub, gbc);

        // --- INPUTS ---
        txtUsuario = crearCampo("");
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 24, 6, 24);
        cardHolder.add(crearFilaCampo("Tu usuario", txtUsuario), gbc);

        // IP + Puerto en grid 2 columnas
        JPanel gridIpPuerto = new JPanel(new java.awt.GridLayout(1, 2, 12, 0));
        gridIpPuerto.setOpaque(false);
        txtIp = crearCampo("127.0.0.1");
        txtPuerto = crearCampo("5050");
        gridIpPuerto.add(crearFilaCampo("IP del servidor", txtIp));
        gridIpPuerto.add(crearFilaCampo("Puerto", txtPuerto));
        gbc.gridy = 4;
        gbc.insets = new Insets(8, 24, 6, 24);
        cardHolder.add(gridIpPuerto, gbc);

        // --- MENSAJE DE ERROR INLINE ---
        lblError = new JLabel(" ");
        lblError.setFont(Theme.font(11, java.awt.Font.PLAIN));
        lblError.setForeground(Theme.ERROR);
        Icon errorIcon = MaterialGlyph.close(14, Theme.ERROR);
        lblError.setIcon(errorIcon);
        lblError.setIconTextGap(6);
        lblError.setVisible(false);
        gbc.gridy = 5;
        gbc.insets = new Insets(4, 24, 0, 24);
        cardHolder.add(lblError, gbc);

        // --- BOTÓN PRIMARIO ---
        btnConectar = new PrimaryButton("Ingresar al chat  ➜", Theme.RADIUS_MD);
        btnConectar.setFont(Theme.font(14, java.awt.Font.BOLD));
        btnConectar.setPreferredSize(new Dimension(0, 44));
        btnConectar.addActionListener(e -> intentarConexion());
        gbc.gridy = 6;
        gbc.insets = new Insets(12, 24, 8, 24);
        cardHolder.add(btnConectar, gbc);

        // --- TIP INFERIOR ---
        JPanel tip = new JPanel(new BorderLayout(6, 0));
        tip.setOpaque(false);
        JLabel infoIcon = new JLabel(MaterialGlyph.info(14, Theme.onSurfaceVariant()));
        infoIcon.setVerticalAlignment(SwingConstants.TOP);
        JLabel tipLbl = new JLabel("<html><div style='text-align:left'>Tip: en la misma máquina puedes abrir varias ventanas para probar chats privados y grupales.</div></html>");
        tipLbl.setFont(Theme.font(11, java.awt.Font.PLAIN));
        tipLbl.setForeground(Theme.onSurfaceVariant());
        tip.add(infoIcon, BorderLayout.WEST);
        tip.add(tipLbl, BorderLayout.CENTER);

        gbc.gridy = 7;
        gbc.insets = new Insets(8, 24, 28, 24);
        cardHolder.add(tip, gbc);

        // Posicionamos la cardHolder centrada dentro del panel
        GridBagConstraints outerG = new GridBagConstraints();
        outerG.gridx = 0;
        outerG.gridy = 0;
        outerG.weightx = 1.0;
        outerG.weighty = 1.0;
        outerG.fill = GridBagConstraints.HORIZONTAL;
        outerG.anchor = GridBagConstraints.CENTER;
        outerG.insets = new Insets(16, 16, 16, 16);
        panel.add(cardHolder, outerG);

        card.setVisible(false);
        return panel;
    }

    private void intentarConexion() {
        String usuario = txtUsuario.getText().trim();
        String ip = txtIp.getText().trim();
        String puertoStr = txtPuerto.getText().trim();

        if (usuario.isEmpty()) {
            mostrarError("El usuario es obligatorio.");
            txtUsuario.requestFocus();
            return;
        }
        if (ip.isEmpty()) {
            mostrarError("La IP del servidor es obligatoria.");
            txtIp.requestFocus();
            return;
        }
        int puerto;
        try {
            puerto = Integer.parseInt(puertoStr);
        } catch (NumberFormatException ex) {
            mostrarError("El puerto debe ser un número válido.");
            txtPuerto.requestFocus();
            return;
        }
        if (listener != null) {
            listener.onConectar(ip, puerto, usuario);
        }
    }

    private void mostrarError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
    }

    private JTextField crearCampo(String def) {
        JTextField c = new JTextField(def) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.surfaceLow());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_MD, Theme.RADIUS_MD);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        c.setFont(Theme.fontBase());
        c.setForeground(Theme.onSurface());
        c.setCaretColor(Theme.primary());
        c.setSelectionColor(Theme.primaryContainer());
        c.setSelectedTextColor(Theme.onPrimary());
        c.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(Theme.RADIUS_MD, Theme.outlineVariant()),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        c.setBackground(Theme.surfaceLow());
        c.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                c.setBorder(BorderFactory.createCompoundBorder(
                        new RoundBorder(Theme.RADIUS_MD, Theme.primary()),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                c.setBorder(BorderFactory.createCompoundBorder(
                        new RoundBorder(Theme.RADIUS_MD, Theme.outlineVariant()),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
        });
        return c;
    }

    private JPanel crearFilaCampo(String etiqueta, Component campo) {
        JPanel fila = new JPanel(new BorderLayout(0, 5));
        fila.setOpaque(false);
        JLabel lbl = new JLabel(etiqueta.toUpperCase());
        lbl.setFont(Theme.fontLabel());
        lbl.setForeground(Theme.onSurfaceVariant());
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        fila.add(lbl, BorderLayout.NORTH);
        fila.add(campo, BorderLayout.CENTER);
        return fila;
    }

    public void setConexionListener(ConexionListener listener) {
        this.listener = listener;
    }
}
