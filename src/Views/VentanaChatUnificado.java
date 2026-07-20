package Views;

import Controllers.HistorialChat;
import Views.theme.Avatar;
import Views.theme.GhostButton;
import Views.theme.MaterialGlyph;
import Views.theme.PlaceholderTextField;
import Views.theme.PrimaryButton;
import Views.theme.RoundBorder;
import Views.theme.SoftShadowBorder;
import Views.theme.Theme;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class VentanaChatUnificado extends JFrame {

    private static final int TOPBAR_HEIGHT     = 64;
    private static final int SIDEBAR_WIDTH     = 320;
    private static final int WINDOW_DEFAULT_W  = 1180;
    private static final int WINDOW_DEFAULT_H  = 760;

    private final String miUsuario;

    /* ------------------- Estado de conversaciones ------------------- */
    private final Map<String, ChatPanel> chats = new HashMap<>(); // key = "priv_<name>" | "grupo_<name>"
    private String conversacionActivaClave;

    /* ------------------- Listas sidebar ------------------- */
    private final DefaultListModel<String> modeloUsuarios = new DefaultListModel<>();
    private final DefaultListModel<String> modeloGrupos   = new DefaultListModel<>();
    private final Set<String> gruposPrivados = new HashSet<>();

    /* ------------------- Listeners hacia el controlador ------------------- */
    private ContactoListener contactoListener;
    private GrupoListener grupoListener;
    private LogoutListener logoutListener;
    /** Hook que se dispara cada vez que se crea un ChatPanel nuevo. El controlador
     *  lo usa para wirear sus listeners de envío/imagen/archivo/sticker. */
    private java.util.function.Consumer<ChatPanel> onChatCreated;

    /* ------------------- Componentes ------------------- */
    private JToggleButton btnTabChats;
    private JToggleButton btnTabGrupos;
    private CardLayout cardTabs;
    private JPanel cardContenedorTabs;

    private JPanel panelListaUsuarios;
    private JPanel panelListaGrupos;
    private JTextField txtBuscarUsuarios;
    private JTextField txtBuscarGrupos;

    private JPanel chatContainer;  // CardLayout con Welcome + chats
    private CardLayout cardChats;
    private final String WELCOME = "WELCOME";
    private static final String CARD_CHATS  = "CHATS";
    private static final String CARD_GRUPOS = "GRUPOS";

    /* ------------------- API ------------------- */
    public interface ContactoListener {
        void onContactoSeleccionado(String nombre);
    }
    public interface GrupoListener {
        void onCrearGrupo(String nombre, String contrasena);
        void onUnirseGrupo(String nombre, String contrasena);
    }
    public interface LogoutListener {
        void onLogout();
    }

    public VentanaChatUnificado(String miUsuario) {
        super("JAM-Chat");
        this.miUsuario = miUsuario;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(WINDOW_DEFAULT_W, WINDOW_DEFAULT_H);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(960, 600));

        getContentPane().setBackground(Theme.background());
        getContentPane().setLayout(new BorderLayout());

        add(crearTopBar(), BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout());
        middle.setOpaque(true);
        middle.setBackground(Theme.background());
        middle.add(crearSidebar(), BorderLayout.WEST);
        middle.add(crearChatArea(), BorderLayout.CENTER);
        add(middle, BorderLayout.CENTER);

        // Card inicial: welcome
        cardChats.show(chatContainer, WELCOME);
        SwingUtilities.invokeLater(() -> seleccionarTab(CARD_CHATS));
    }

    /* ============================ TOP BAR ============================ */

    private JPanel crearTopBar() {
        JPanel top = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setColor(Theme.surfaceLowest());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.outlineVariant());
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        top.setOpaque(false);
        top.setPreferredSize(new Dimension(0, TOPBAR_HEIGHT));
        top.setBorder(new EmptyBorder(10, 24, 10, 24));

        // Logo
        JLabel lbl = new JLabel("JAM-Chat");
        lbl.setFont(Theme.fontTitle());
        lbl.setForeground(Theme.primary());
        top.add(lbl, BorderLayout.WEST);

        // Acciones (avatar + logout). El icono de settings se eliminó: no estaba implementado.
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.setOpaque(false);

        // Avatar del usuario logueado
        Avatar av = new Avatar(miUsuario, 32, Theme.primary(), Theme.tertiary());
        actions.add(av);

        JButton btnLogout = topIconButton(MaterialGlyph.logout(20, Theme.onSurfaceVariant()));
        btnLogout.setToolTipText("Cerrar sesión");
        btnLogout.addActionListener(e -> {
            if (logoutListener != null) logoutListener.onLogout();
            else dispose();
        });
        actions.add(btnLogout);

        top.add(actions, BorderLayout.EAST);
        return top;
    }

    private JButton topIconButton(Icon icon) {
        JButton b = new JButton(icon);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setBorder(null);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setPreferredSize(new Dimension(36, 36));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    /* ============================ SIDEBAR ============================ */

    private JPanel crearSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setColor(Theme.surface());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.outlineVariant());
                g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));

        // Header (avatar "JC" + título "Conversations")
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(20, 20, 12, 20));

        Avatar logoAvatar = new Avatar("JC", 40,
                new Color(0x33, 0x66, 0xDD), new Color(0x66, 0x44, 0xCC));
        header.add(logoAvatar, BorderLayout.WEST);

        JPanel titulos = new JPanel();
        titulos.setOpaque(false);
        titulos.setLayout(new BoxLayout(titulos, BoxLayout.Y_AXIS));
        JLabel lblTitulo = new JLabel("Conversations");
        lblTitulo.setFont(Theme.fontTitle());
        lblTitulo.setForeground(Theme.onSurface());
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblSub = new JLabel("Recent messages");
        lblSub.setFont(Theme.font(12, Font.PLAIN));
        lblSub.setForeground(Theme.onSurfaceVariant());
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        titulos.add(lblTitulo);
        titulos.add(Box.createVerticalStrut(2));
        titulos.add(lblSub);
        header.add(titulos, BorderLayout.CENTER);

        sidebar.add(header, BorderLayout.NORTH);

        // Body (botón "Crear grupo" + tabs + lista)
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 16, 16, 16));

        // Botón Crear Grupo full-width azul (acción principal visible)
        JPanel newMsg = new JPanel(new BorderLayout());
        newMsg.setOpaque(false);
        JButton btnCrearGrupo = new PrimaryButton("+  Crear Grupo", Theme.RADIUS_LG);
        btnCrearGrupo.setFont(Theme.font(13, Font.BOLD));
        btnCrearGrupo.setPreferredSize(new Dimension(0, 48));
        btnCrearGrupo.addActionListener(e -> mostrarDialogoCrearGrupo());
        newMsg.add(btnCrearGrupo, BorderLayout.CENTER);
        body.add(newMsg, BorderLayout.NORTH);

        // Center = tabs + lista
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(crearTabs(), BorderLayout.NORTH);
        center.add(crearListasCardLayout(), BorderLayout.CENTER);
        body.add(center, BorderLayout.CENTER);

        sidebar.add(body, BorderLayout.CENTER);

        // Footer (sección para futuros controles reales; "Settings" se eliminó por no estar implementado)
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(new EmptyBorder(0, 8, 8, 8));

        JSeparatorSide sepFooter = new JSeparatorSide();
        sepFooter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        footer.add(sepFooter);

        sidebar.add(footer, BorderLayout.SOUTH);

        return sidebar;
    }

    private JPanel crearTabs() {
        JPanel pill = new JPanel(new GridBagLayout());
        pill.setOpaque(false);

        btnTabChats  = crearToggleTab("Chats",  CARD_CHATS);
        btnTabGrupos = crearToggleTab("Groups", CARD_GRUPOS);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0; pill.add(btnTabChats, gbc);
        gbc.gridx = 1; pill.add(btnTabGrupos, gbc);
        return pill;
    }

    private JToggleButton crearToggleTab(String label, String cardName) {
        JToggleButton btn = new JToggleButton(label) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                if (isSelected()) {
                    g2.setColor(Theme.surfaceLowest());
                    g2.fillRoundRect(0, 0, w, h, Theme.RADIUS_MD, Theme.RADIUS_MD);
                } else if (getModel().isRollover()) {
                    g2.setColor(Theme.surfaceLow());
                    g2.fillRoundRect(0, 0, w, h, Theme.RADIUS_MD, Theme.RADIUS_MD);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(Theme.fontLabel());
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 36));
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setForeground(Theme.onSurfaceVariant());
        btn.addChangeListener(e -> btn.setForeground(btn.isSelected() ? Theme.onSurface() : Theme.onSurfaceVariant()));
        btn.addActionListener(e -> seleccionarTab(cardName));
        return btn;
    }

    private void seleccionarTab(String cardName) {
        if (cardTabs == null || cardContenedorTabs == null) return;
        cardTabs.show(cardContenedorTabs, cardName);
        JToggleButton to = CARD_CHATS.equals(cardName) ? btnTabChats : btnTabGrupos;
        if (to != null && !to.isSelected()) to.setSelected(true);
    }

    private JPanel crearListasCardLayout() {
        cardTabs = new CardLayout();
        cardContenedorTabs = new JPanel(cardTabs);
        cardContenedorTabs.setOpaque(false);

        cardContenedorTabs.add(crearPanelListaUsuarios(), CARD_CHATS);
        cardContenedorTabs.add(crearPanelListaGrupos(),   CARD_GRUPOS);
        return cardContenedorTabs;
    }

    private JPanel crearPanelListaUsuarios() {
        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);

        txtBuscarUsuarios = crearBuscador("Buscar usuario…");
        txtBuscarUsuarios.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { rebuildCards(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { rebuildCards(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { rebuildCards(); }
        });
        wrap.add(envolverBuscador(txtBuscarUsuarios), BorderLayout.NORTH);

        // Encabezado "RECENT"
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(new EmptyBorder(12, 8, 4, 8));
        JLabel lblRecent = new JLabel("RECENT");
        lblRecent.setFont(Theme.font(10, Font.BOLD));
        lblRecent.setForeground(Theme.outline());
        head.add(lblRecent, BorderLayout.WEST);
        wrap.add(head, BorderLayout.CENTER);

        // Lista
        panelListaUsuarios = new JPanel();
        panelListaUsuarios.setLayout(new BoxLayout(panelListaUsuarios, BoxLayout.Y_AXIS));
        panelListaUsuarios.setOpaque(false);

        JPanel cont = new JPanel(new BorderLayout());
        cont.setOpaque(false);
        cont.add(panelListaUsuarios, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(cont);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        wrap.add(scroll, BorderLayout.CENTER);

        rebuildCards();
        return wrap;
    }

    private JPanel crearPanelListaGrupos() {
        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);

        txtBuscarGrupos = crearBuscador("Buscar grupo…");
        txtBuscarGrupos.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { rebuildCards(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { rebuildCards(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { rebuildCards(); }
        });
        wrap.add(envolverBuscador(txtBuscarGrupos), BorderLayout.NORTH);

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(new EmptyBorder(12, 8, 4, 8));
        JLabel lblRecent = new JLabel("AVAILABLE GROUPS");
        lblRecent.setFont(Theme.font(10, Font.BOLD));
        lblRecent.setForeground(Theme.outline());
        head.add(lblRecent, BorderLayout.WEST);
        wrap.add(head, BorderLayout.CENTER);

        panelListaGrupos = new JPanel();
        panelListaGrupos.setLayout(new BoxLayout(panelListaGrupos, BoxLayout.Y_AXIS));
        panelListaGrupos.setOpaque(false);

        JPanel cont = new JPanel(new BorderLayout());
        cont.setOpaque(false);
        cont.add(panelListaGrupos, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(cont);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        wrap.add(scroll, BorderLayout.CENTER);

        rebuildCards();
        return wrap;
    }

    private JTextField crearBuscador(String placeholder) {
        PlaceholderTextField txt = new PlaceholderTextField(placeholder) {
            @Override public Color getBackground() { return new Color(0, 0, 0, 0); }
        };
        txt.setOpaque(false);
        txt.setBorder(null);
        txt.setFont(Theme.fontBase());
        txt.setForeground(Theme.onSurface());
        txt.setCaretColor(Theme.primary());
        return txt;
    }

    private JPanel envolverBuscador(JTextField txt) {
        JPanel search = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.surfaceLow());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_MD, Theme.RADIUS_MD);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        search.setOpaque(false);
        search.setBorder(new EmptyBorder(0, 12, 0, 12));
        search.setPreferredSize(new Dimension(0, 38));
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        search.add(new JLabel(MaterialGlyph.search(18, Theme.onSurfaceVariant())), BorderLayout.WEST);
        search.add(txt, BorderLayout.CENTER);
        return search;
    }

    /* ============================ Reconstruir cards ============================ */

    private void rebuildCards() {
        if (panelListaUsuarios != null) {
            panelListaUsuarios.removeAll();
            String filtro = txtBuscarUsuarios == null ? "" : txtBuscarUsuarios.getText().trim().toLowerCase();
            for (int i = 0; i < modeloUsuarios.getSize(); i++) {
                String u = modeloUsuarios.get(i);
                if (u == null) continue;
                if (!filtro.isEmpty() && !u.toLowerCase().contains(filtro)) continue;
                panelListaUsuarios.add(crearContactoItem(u));
            }
            panelListaUsuarios.add(Box.createVerticalGlue());
            panelListaUsuarios.revalidate();
            panelListaUsuarios.repaint();
        }
        if (panelListaGrupos != null) {
            panelListaGrupos.removeAll();
            String filtro = txtBuscarGrupos == null ? "" : txtBuscarGrupos.getText().trim().toLowerCase();
            for (int i = 0; i < modeloGrupos.getSize(); i++) {
                String g = modeloGrupos.get(i);
                if (g == null || g.trim().isEmpty()) continue;
                if (!filtro.isEmpty() && !g.toLowerCase().contains(filtro)) continue;
                panelListaGrupos.add(crearGrupoItem(g));
            }
            panelListaGrupos.add(Box.createVerticalGlue());
            panelListaGrupos.revalidate();
            panelListaGrupos.repaint();
        }
    }

    private JPanel crearContactoItem(String contacto) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setOpaque(false);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setBorder(new EmptyBorder(10, 8, 10, 8));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        Avatar av = new Avatar(contacto, 40, Theme.primary(), Theme.tertiary());
        card.add(av, BorderLayout.WEST);

        // Centro: nombre + último mensaje (preview desde historial)
        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));

        JLabel lblNombre = new JLabel(contacto);
        lblNombre.setFont(Theme.fontBold());
        lblNombre.setForeground(Theme.onSurface());
        lblNombre.setAlignmentX(Component.LEFT_ALIGNMENT);
        centro.add(lblNombre);

        String preview = cargarPreview(false, contacto);
        JLabel lblPreview = new JLabel(preview == null ? "Toca para iniciar una conversación" : preview);
        lblPreview.setFont(Theme.font(12, Font.PLAIN));
        lblPreview.setForeground(Theme.onSurfaceVariant());
        lblPreview.setAlignmentX(Component.LEFT_ALIGNMENT);
        centro.add(Box.createVerticalStrut(2));
        centro.add(lblPreview);

        card.add(centro, BorderLayout.CENTER);

        // Indicador de chat activo + timestamp
        String stamp = cargarUltimaHora(false, contacto);
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JLabel lblStamp = new JLabel(stamp == null ? "" : stamp);
        lblStamp.setFont(Theme.font(10, Font.PLAIN));
        lblStamp.setForeground(Theme.outline());
        lblStamp.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(lblStamp);
        if (esConversacionActiva(false, contacto)) {
            right.add(Box.createVerticalStrut(4));
            JPanel dotA = new JPanel() {
                @Override protected void paintComponent(java.awt.Graphics g) {
                    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Theme.primary());
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
                @Override public boolean isOpaque() { return false; }
            };
            dotA.setPreferredSize(new Dimension(8, 8));
            dotA.setMaximumSize(new Dimension(8, 8));
            dotA.setAlignmentX(Component.RIGHT_ALIGNMENT);
            right.add(dotA);
        }
        card.add(right, BorderLayout.EAST);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                abrirConversacion(contacto, false);
            }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { card.setBackground(Theme.surfaceLow()); }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  { card.setBackground(null); }
        });
        return card;
    }

    private JPanel crearGrupoItem(String nombre) {
        boolean privado = gruposPrivados.contains(nombre);
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setOpaque(false);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setBorder(new EmptyBorder(10, 8, 10, 8));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        Avatar av = new Avatar((privado ? "🔒 " : "#") + nombre, 40,
                privado ? new Color(0x94, 0x37, 0x00) : new Color(0x33, 0x66, 0xDD),
                privado ? new Color(0xBC, 0x48, 0x00) : new Color(0x66, 0x44, 0xCC));
        card.add(av, BorderLayout.WEST);

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        JLabel lblNombre = new JLabel((privado ? "🔒 " : "# ") + nombre);
        lblNombre.setFont(Theme.fontBold());
        lblNombre.setForeground(Theme.onSurface());
        lblNombre.setAlignmentX(Component.LEFT_ALIGNMENT);
        centro.add(lblNombre);

        String preview = cargarPreview(true, nombre);
        JLabel lblPreview = new JLabel(preview == null ? (privado ? "Doble click para unirse" : "Click para abrir") : preview);
        lblPreview.setFont(Theme.font(12, Font.PLAIN));
        lblPreview.setForeground(Theme.onSurfaceVariant());
        lblPreview.setAlignmentX(Component.LEFT_ALIGNMENT);
        centro.add(Box.createVerticalStrut(2));
        centro.add(lblPreview);

        card.add(centro, BorderLayout.CENTER);

        String stamp = cargarUltimaHora(true, nombre);
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JLabel lblStamp = new JLabel(stamp == null ? "" : stamp);
        lblStamp.setFont(Theme.font(10, Font.PLAIN));
        lblStamp.setForeground(Theme.outline());
        lblStamp.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(lblStamp);
        if (esConversacionActiva(true, nombre)) {
            right.add(Box.createVerticalStrut(4));
            JPanel dot = new JPanel() {
                @Override protected void paintComponent(java.awt.Graphics g) {
                    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Theme.primary());
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
                @Override public boolean isOpaque() { return false; }
            };
            dot.setPreferredSize(new Dimension(8, 8));
            dot.setMaximumSize(new Dimension(8, 8));
            dot.setAlignmentX(Component.RIGHT_ALIGNMENT);
            right.add(dot);
        }
        card.add(right, BorderLayout.EAST);

            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 1) {
                        String pwd = null;
                        if (privado) {
                            pwd = solicitarContrasenaGrupo(nombre);
                            if (pwd == null) return;
                        }
                        // SIEMPRE nos unimos al grupo en el servidor al abrirlo
                        // (público o privado), si no, los mensajes en vivo nunca
                        // llegan al cliente que abre el chat sin haberse unido.
                        if (grupoListener != null) grupoListener.onUnirseGrupo(nombre, pwd);
                        abrirConversacion(nombre, true);
                    }
                }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { card.setBackground(Theme.surfaceLow()); }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  { card.setBackground(null); }
        });
        return card;
    }

    /* ============================ CHAT AREA ============================ */

    private JPanel crearChatArea() {
        JPanel wrap = new JPanel(new BorderLayout()) {
            @Override public boolean isOpaque() { return false; }
        };
        wrap.setBackground(Theme.background());

        cardChats = new CardLayout();
        chatContainer = new JPanel(cardChats);
        chatContainer.setBackground(Theme.background());
        chatContainer.setOpaque(true);
        chatContainer.add(crearWelcome(), WELCOME);
        wrap.add(chatContainer, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel crearWelcome() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setBackground(Theme.background());
        wrap.setOpaque(true);

        JPanel card = new JPanel();
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel lbl1 = new JLabel("👋");
        lbl1.setFont(Theme.fontEmoji(56, Font.PLAIN));
        lbl1.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lbl1);
        card.add(Box.createVerticalStrut(16));

        JLabel lbl2 = new JLabel("Bienvenido a JAM-Chat");
        lbl2.setFont(Theme.font(22, Font.BOLD));
        lbl2.setForeground(Theme.onSurface());
        lbl2.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lbl2);
        card.add(Box.createVerticalStrut(6));

        JLabel lbl3 = new JLabel("Selecciona una conversación o inicia una nueva");
        lbl3.setFont(Theme.font(14, Font.PLAIN));
        lbl3.setForeground(Theme.onSurfaceVariant());
        lbl3.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lbl3);

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0;
        g.weightx = 1; g.weighty = 1;
        g.anchor = GridBagConstraints.CENTER;
        wrap.add(card, g);
        return wrap;
    }

    /* ============================ Abrir / cargar conversaciones ============================ */

    private String clave(String nombre, boolean grupo) { return (grupo ? "grupo_" : "priv_") + nombre; }

    private ChatPanel obtenerChat(String nombre, boolean grupo) { return chats.get(clave(nombre, grupo)); }

    private boolean esConversacionActiva(boolean grupo, String nombre) {
        return clave(nombre, grupo).equals(conversacionActivaClave);
    }

    /**
     * Abre o enfoca una conversación. Crea el ChatPanel si no existe, lo monta
     * en el contenedor, dispara el hook `onChatCreated` (para que el controlador
     * pueda wirear listeners) y le carga su historial.
     */
    public ChatPanel abrirConversacion(String nombre, boolean esGrupo) {
        String k = clave(nombre, esGrupo);
        ChatPanel chat = chats.get(k);
        if (chat == null) {
            chat = new ChatPanel(miUsuario, nombre, esGrupo);
            // Disparar hook ANTES de cargar historial: así el controlador puede wirear
            // listeners de envío/imagen ANTES de que llegue un mensaje.
            if (onChatCreated != null) {
                onChatCreated.accept(chat);
            }
            cargarHistorialEn(chat, nombre, esGrupo);
            chats.put(k, chat);
            chatContainer.add(chat, k);
        }
        final ChatPanel chatFinal = chat;
        conversacionActivaClave = k;
        cardChats.show(chatContainer, k);
        SwingUtilities.invokeLater(() -> {
            chatFinal.requestFocusInWindow();
            rebuildCards();
        });
        return chat;
    }

    private void cargarListenersEnChat(ChatPanel chat) {
        // Hook obsoleto. El controller ahora wirea sus listeners vía setOnChatCreated.
        // Mantenido vacío por compatibilidad binaria.
        if (onChatCreated != null) onChatCreated.accept(chat);
    }

    private void cargarHistorialEn(ChatPanel chat, String destino, boolean grupo) {
        List<HistorialChat.Mensaje> historial = HistorialChat.cargarHistorial(miUsuario, destino, grupo);
        String hoy = java.time.LocalDate.now().toString();
        String ayer = java.time.LocalDate.now().minusDays(1).toString();
        java.time.format.DateTimeFormatter fmtCorto = java.time.format.DateTimeFormatter.ofPattern("d MMM");
        String fechaUltima = null;

        for (HistorialChat.Mensaje msg : historial) {
            String rem = msg.getRemitente();
            if (rem == null || rem.isEmpty()) rem = destino;
            String t = msg.getTexto();
            if (t == null || t.isEmpty()) continue;

            String f = msg.getFecha();
            if (!f.isEmpty() && !f.equals(fechaUltima)) {
                String etiqueta;
                if (f.equals(hoy))        etiqueta = "Hoy";
                else if (f.equals(ayer))  etiqueta = "Ayer";
                else                      etiqueta = java.time.LocalDate.parse(f).format(fmtCorto);
                chat.mostrarSeparadorDia(etiqueta);
                fechaUltima = f;
            }

            chat.procesarMensajeDeHistorial(rem, t, new Color(0x80, 0x80, 0x80));
        }
        chat.repaint();
    }

    private void cargarListenersChatRecientes(ChatPanel chat, String destino) {
        // Hook placeholder si se quisiera re-rutar en caliente tras refresco. Hoy se hace
        // una sola vez al abrir.
    }

    /* ============================ Preview / hora último mensaje ============================ */

    private String cargarPreview(boolean grupo, String nombre) {
        List<HistorialChat.Mensaje> h = HistorialChat.cargarHistorial(miUsuario, nombre, grupo);
        if (h.isEmpty()) return null;
        HistorialChat.Mensaje last = h.get(h.size() - 1);
        String t = last.getTexto();
        if (t == null) t = "";
        if (t.length() > 40) t = t.substring(0, 40) + "…";
        String rem = last.getRemitente();
        if (rem == null || rem.isEmpty()) rem = nombre;
        return rem.equals("Tú") ? "Tú: " + t : rem + ": " + t;
    }

    private String cargarUltimaHora(boolean grupo, String nombre) {
        File carpeta = new File("historiales");
        if (!carpeta.exists()) return null;
        String[] files = carpeta.list();
        if (files == null) return null;
        String prefix = "chat_" + (grupo ? "grupo_" : "priv_") + miUsuario.toLowerCase() + "_" + nombre.toLowerCase() + ".txt";
        for (String f : files) if (f.equalsIgnoreCase(prefix)) return leerUltimaHora(new File(carpeta, f));
        return null;
    }

    private String leerUltimaHora(File f) {
        if (f == null || !f.exists()) return null;
        try {
            String last = null;
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) last = line;
            }
            if (last == null) return null;
            int ini = last.indexOf("[");
            int fin = last.indexOf("]");
            if (ini == -1 || fin == -1 || fin <= ini) return null;
            String stamp = last.substring(ini + 1, fin);
            if (stamp.length() >= 16) return stamp.substring(11, 16);
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /* ============================ Diálogos: crear grupo / contraseña ============================ */

    private void mostrarDialogoCrearGrupo() {
        final String[] resultado = new String[2];
        JDialog dialog = new JDialog(this, "Crear nuevo grupo", true);
        dialog.setUndecorated(true);
        dialog.setSize(getSize());
        dialog.setLocation(getLocationOnScreen());

        JPanel overlay = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setColor(new Color(0, 0, 0, 110));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        overlay.setOpaque(false);
        overlay.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { dialog.dispose(); }
        });

        JPanel sheet = new JPanel() {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setColor(Theme.surfaceLowest());
                int r = 24;
                g2.fillRoundRect(0, 0, getWidth(), getHeight() + r, r, r);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        sheet.setOpaque(false);
        sheet.setLayout(new BoxLayout(sheet, BoxLayout.Y_AXIS));
        sheet.setBorder(new EmptyBorder(20, 22, 22, 22));

        JPanel handleWrap = new JPanel();
        handleWrap.setOpaque(false);
        handleWrap.setLayout(new BoxLayout(handleWrap, BoxLayout.X_AXIS));
        handleWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        handleWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        JPanel handle = new JPanel() {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setColor(Theme.outlineVariant());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        handle.setOpaque(false);
        handle.setPreferredSize(new Dimension(40, 4));
        handle.setMaximumSize(new Dimension(40, 4));
        handleWrap.add(Box.createHorizontalGlue());
        handleWrap.add(handle);
        handleWrap.add(Box.createHorizontalGlue());
        sheet.add(handleWrap);
        sheet.add(Box.createVerticalStrut(14));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JLabel lblTitle = new JLabel("Crear nuevo grupo");
        lblTitle.setFont(Theme.font(20, Font.BOLD));
        lblTitle.setForeground(Theme.onSurface());
        titleRow.add(lblTitle, BorderLayout.WEST);

        JButton btnClose = new JButton(MaterialGlyph.close(20, Theme.onSurfaceVariant())) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                if (getModel().isRollover()) {
                    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                    g2.setColor(Theme.surfaceHigh());
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
                super.paintComponent(g);
            }
            @Override public boolean isContentAreaFilled() { return false; }
            @Override public boolean isOpaque() { return false; }
        };
        btnClose.setFocusPainted(false);
        btnClose.setBorderPainted(false);
        btnClose.setContentAreaFilled(false);
        btnClose.setOpaque(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.setPreferredSize(new Dimension(36, 36));
        btnClose.addActionListener(e -> dialog.dispose());
        titleRow.add(btnClose, BorderLayout.EAST);
        sheet.add(titleRow);

        sheet.add(Box.createVerticalStrut(18));

        JLabel lblNombre = new JLabel("NOMBRE DEL GRUPO");
        lblNombre.setFont(Theme.fontLabel());
        lblNombre.setForeground(Theme.onSurfaceVariant());
        lblNombre.setAlignmentX(Component.LEFT_ALIGNMENT);
        sheet.add(lblNombre);
        sheet.add(Box.createVerticalStrut(6));

        JTextField txtNombre = new JTextField();
        txtNombre.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        txtNombre.setAlignmentX(Component.LEFT_ALIGNMENT);
        sheet.add(txtNombre);
        sheet.add(Box.createVerticalStrut(16));

        JCheckBox checkPrivado = new JCheckBox("Grupo privado (con contraseña)");
        checkPrivado.setFont(Theme.font(14, Font.PLAIN));
        checkPrivado.setForeground(Theme.onSurface());
        checkPrivado.setOpaque(false);
        checkPrivado.setAlignmentX(Component.LEFT_ALIGNMENT);
        sheet.add(checkPrivado);
        sheet.add(Box.createVerticalStrut(10));

        JPanel pwdPanel = new JPanel();
        pwdPanel.setOpaque(false);
        pwdPanel.setLayout(new BoxLayout(pwdPanel, BoxLayout.Y_AXIS));
        pwdPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblPwd = new JLabel("CONTRASEÑA");
        lblPwd.setFont(Theme.fontLabel());
        lblPwd.setForeground(Theme.onSurfaceVariant());
        lblPwd.setAlignmentX(Component.LEFT_ALIGNMENT);
        pwdPanel.add(lblPwd);
        pwdPanel.add(Box.createVerticalStrut(6));

        JPasswordField pwd = new JPasswordField();
        pwd.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        pwd.setAlignmentX(Component.LEFT_ALIGNMENT);
        pwdPanel.add(pwd);
        pwdPanel.setVisible(false);

        sheet.add(pwdPanel);
        sheet.add(Box.createVerticalStrut(20));
        checkPrivado.addActionListener(e -> pwdPanel.setVisible(checkPrivado.isSelected()));

        JPanel buttons = new JPanel(new java.awt.GridLayout(1, 2, 10, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        GhostButton btnCancelar = new GhostButton("Cancelar");
        btnCancelar.addActionListener(e -> dialog.dispose());
        buttons.add(btnCancelar);

        PrimaryButton btnCrear = new PrimaryButton("Crear", Theme.RADIUS_MD);
        btnCrear.setFont(Theme.font(14, Font.BOLD));
        btnCrear.addActionListener(e -> {
            String n = txtNombre.getText().trim();
            if (n.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "El nombre del grupo no puede estar vacío.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (checkPrivado.isSelected()) {
                String p = new String(pwd.getPassword()).trim();
                if (p.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "La contraseña no puede estar vacía para un grupo privado.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                resultado[0] = n; resultado[1] = p;
            } else {
                resultado[0] = n; resultado[1] = null;
            }
            dialog.dispose();
        });
        buttons.add(btnCrear);
        sheet.add(buttons);

        overlay.add(sheet, BorderLayout.SOUTH);
        JPanel glass = new JPanel(new BorderLayout());
        glass.setOpaque(false);
        glass.add(overlay, BorderLayout.CENTER);
        sheet.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { e.consume(); }
        });
        dialog.setContentPane(glass);
        dialog.setVisible(true);

        if (resultado[0] != null && grupoListener != null) {
            grupoListener.onCrearGrupo(resultado[0], resultado[1]);
        }
    }

    private String solicitarContrasenaGrupo(String nombreGrupo) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.surfaceLowest());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel info = new JLabel("<html>🔒 \"" + nombreGrupo + "\" es un grupo privado.<br>Ingresa la contraseña para unirte:</html>");
        info.setFont(Theme.font(13, Font.PLAIN));
        info.setForeground(Theme.onSurface());
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(info);
        panel.add(Box.createVerticalStrut(12));
        JPasswordField txt = new JPasswordField();
        txt.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txt.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(txt);

        int r = JOptionPane.showConfirmDialog(this, panel, "Grupo privado",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r == JOptionPane.OK_OPTION) {
            String pwd = new String(txt.getPassword()).trim();
            return pwd.isEmpty() ? null : pwd;
        }
        return null;
    }

    /* ============================ Util ============================ */

    private static class JSeparatorSide extends JPanel {
        JSeparatorSide() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 1));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        }
        @Override protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setColor(Theme.outlineVariant());
            g2.fillRect(0, 0, getWidth(), 1);
            g2.dispose();
        }
    }

    /* ============================ API pública ============================ */

    public void actualizarLista(String[] usuarios) {
        modeloUsuarios.clear();
        if (usuarios != null) for (String u : usuarios) if (u != null && !u.trim().isEmpty()) modeloUsuarios.addElement(u);
        rebuildCards();
    }

    public void actualizarListaGrupos(String[] grupos) {
        modeloGrupos.clear();
        if (grupos != null) for (String g : grupos) if (g != null && !g.trim().isEmpty()) modeloGrupos.addElement(g);
        rebuildCards();
    }

    public void actualizarGruposPrivados(String[] gruposPrivadosArr) {
        gruposPrivados.clear();
        if (gruposPrivadosArr != null) for (String g : gruposPrivadosArr) if (g != null && !g.trim().isEmpty()) gruposPrivados.add(g);
        rebuildCards();
    }

    public void setContactoListener(ContactoListener l) { this.contactoListener = l; }
    public void setGrupoListener(GrupoListener l) { this.grupoListener = l; }
    public void setLogoutListener(LogoutListener l) { this.logoutListener = l; }
    /** Se invoca cada vez que se CREA un ChatPanel. ControladorCliente lo usa para wirear listeners. */
    public void setOnChatCreated(java.util.function.Consumer<ChatPanel> c) { this.onChatCreated = c; }

    /* ----- Acceso al ChatPanel activo (para enrutar mensajes) ----- */
    public ChatPanel getChatActivo() {
        return conversacionActivaClave == null ? null : chats.get(conversacionActivaClave);
    }

    public ChatPanel getChat(String destino, boolean esGrupo) {
        return chats.get(clave(destino, esGrupo));
    }

    /* ============================ Glyphs extra usados aquí ============================ */
    // settings y logout no estaban en MaterialGlyph; los añadimos aquí.
}
