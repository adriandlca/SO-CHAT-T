package Views;

import Views.theme.Avatar;
import Views.theme.GhostButton;
import Views.theme.MaterialGlyph;
import Views.theme.PrimaryButton;
import Views.theme.RoundBorder;
import Views.theme.Theme;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/**
 * Rediseño de la pantalla de Contactos / Grupos.
 *
 * Estructura:
 *   NORTH: header oscuro con avatar y acciones
 *   CENTER: segmented control (tabs) + buscador + lista (CardLayout)
 *   SOUTH: FAB "+ Crear grupo"
 *
 * API pública intacta: setContactoListener, setGrupoListener, actualizarLista,
 * actualizarListaGrupos, actualizarGruposPrivados y las dos interfaces.
 */
public class VentanaContactos extends JFrame {

    /* ---------- Modelos (compatibilidad: el controlador sigue usando actualizarLista, etc.) ---------- */
    private final DefaultListModel<String> modeloUsuarios = new DefaultListModel<>();
    private final DefaultListModel<String> modeloGrupos   = new DefaultListModel<>();
    private final JList<String> listaUsuarios = new JList<>(modeloUsuarios);
    private final JList<String> listaGrupos   = new JList<>(modeloGrupos);

    private final Set<String> gruposPrivados = new HashSet<>();

    /* ---------- Referencias UI explícitas ---------- */
    private JToggleButton btnTabUsuarios;
    private JToggleButton btnTabGrupos;
    private JPanel        cardContenedor;       // el que tiene CardLayout
    private CardLayout    cardLayout;
    private static final String CARD_USUARIOS = "USUARIOS";
    private static final String CARD_GRUPOS   = "GRUPOS";

    private JPanel panelCardUsuarios;           // contenedor vertical de contact cards
    private JPanel panelCardGrupos;             // contenedor vertical de group cards
    private JTextField txtBuscarUsuarios;
    private JTextField txtBuscarGrupos;

    private JLabel lblEstado;                   // "En línea · Red local" (en header)

    /* ---------- Listeners (API pública) ---------- */
    public interface ContactoListener {
        void onContactoSeleccionado(String nombreContacto);
    }

    public interface GrupoListener {
        void onCrearGrupo(String nombreGrupo, String contrasena);
        void onUnirseGrupo(String nombreGrupo, String contrasena);
    }

    private ContactoListener contactoListener;
    private GrupoListener     grupoListener;

    public VentanaContactos(String miUsuario) {
        super("Dashboard - " + miUsuario);
        this.setLayout(new BorderLayout());
        this.getContentPane().setBackground(Theme.background());

        add(crearHeader(miUsuario), BorderLayout.NORTH);
        add(crearCuerpo(),          BorderLayout.CENTER);
        add(crearFAB(),             BorderLayout.SOUTH);

        setSize(360, 640);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Estado inicial: tab Usuarios activo
        SwingUtilities.invokeOnEDT(() -> {
            seleccionarTab(CARD_USUARIOS);
        });
    }

    /* ============================ HEADER ============================ */

    private JPanel crearHeader(String usuario) {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(Theme.PROFILE_HEADER);
        header.setBorder(new EmptyBorder(14, 14, 14, 14));

        Avatar av = new Avatar(usuario, 40, new Color(0x33, 0x66, 0xDD), new Color(0x66, 0x44, 0xCC));
        header.add(av, BorderLayout.WEST);

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        JLabel lblUser = new JLabel(usuario);
        lblUser.setFont(Theme.font(16, Font.BOLD));
        lblUser.setForeground(Color.WHITE);
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        col.add(lblUser);

        JPanel estado = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        estado.setOpaque(false);
        estado.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel dot = new DotStatus(Theme.SUCCESS);
        estado.add(dot);

        lblEstado = new JLabel("En línea · Red local");
        lblEstado.setFont(Theme.font(11, Font.PLAIN));
        lblEstado.setForeground(new Color(255, 255, 255, 200));
        estado.add(lblEstado);
        col.add(estado);

        header.add(col, BorderLayout.CENTER);

        // Acciones derecha: sólo lo que tiene utilidad.
        // El header no tenía listeners conectados para esos botones, así que
        // los quitamos en lugar de mostrar iconos muertos.
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(Box.createHorizontalStrut(0));   // mantiene alineación
        header.add(actions, BorderLayout.EAST);

        return header;
    }

    /* ============================ CUERPO ============================ */

    private JPanel crearCuerpo() {
        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(12, 12, 90, 12));

        body.add(crearSegmentedControl(), BorderLayout.NORTH);
        body.add(crearCardLayout(),        BorderLayout.CENTER);
        return body;
    }

    /* ----- Tabs ----- */

    private JPanel crearSegmentedControl() {
        JPanel pill = new JPanel(new GridLayout(1, 2, 4, 0));
        pill.setBackground(Theme.surfaceHigh());
        pill.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(Theme.RADIUS_MD, Theme.surfaceHigh()),
                new EmptyBorder(3, 3, 3, 3)));

        btnTabUsuarios = crearToggleTab("USUARIOS", CARD_USUARIOS);
        btnTabGrupos   = crearToggleTab("GRUPOS",   CARD_GRUPOS);
        ButtonGroup grp = new ButtonGroup();
        grp.add(btnTabUsuarios);
        grp.add(btnTabGrupos);

        pill.add(btnTabUsuarios);
        pill.add(btnTabGrupos);
        return pill;
    }

    /**
     * Crea un botón tipo "tab" con estilo Stitch: cuando está seleccionado
     * muestra fondo surfaceLowest + texto primary; si no, fondo surfaceHigh
     * y texto muted. Usa JToggleButton + ButtonGroup para que Swing
     * gestione automáticamente la exclusión.
     */
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
                    g2.setColor(Theme.surfaceHigh());
                    g2.fillRoundRect(0, 0, w, h, Theme.RADIUS_MD, Theme.RADIUS_MD);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(Theme.font(12, Font.BOLD));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 32));
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setForeground(Theme.onSurfaceVariant());
        btn.addChangeListener(e -> {
            btn.setForeground(btn.isSelected() ? Theme.primary() : Theme.onSurfaceVariant());
        });
        btn.addActionListener(e -> seleccionarTab(cardName));
        return btn;
    }

    private void seleccionarTab(String cardName) {
        if (cardLayout == null || cardContenedor == null) return;
        cardLayout.show(cardContenedor, cardName);
        JToggleButton toSelect = CARD_USUARIOS.equals(cardName) ? btnTabUsuarios : btnTabGrupos;
        if (toSelect != null && !toSelect.isSelected()) toSelect.setSelected(true);
    }

    /* ----- CardLayout + buscardor + listas ----- */

    private JPanel crearCardLayout() {
        cardLayout    = new CardLayout();
        cardContenedor = new JPanel(cardLayout);
        cardContenedor.setOpaque(false);

        cardContenedor.add(crearPanelListaUsuarios(), CARD_USUARIOS);
        cardContenedor.add(crearPanelListaGrupos(),   CARD_GRUPOS);

        return cardContenedor;
    }

    /* ----- Lista de usuarios ----- */

    private JPanel crearPanelListaUsuarios() {
        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);

        txtBuscarUsuarios = crearBuscador("Buscar usuario…");
        txtBuscarUsuarios.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { rebuildCards(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { rebuildCards(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { rebuildCards(); }
        });
        wrap.add(crearBuscadorWrap(txtBuscarUsuarios), BorderLayout.NORTH);

        panelCardUsuarios = new JPanel();
        panelCardUsuarios.setLayout(new BoxLayout(panelCardUsuarios, BoxLayout.Y_AXIS));
        panelCardUsuarios.setOpaque(false);

        JScrollPane scroll = new JScrollPane(panelCardUsuarios);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        wrap.add(scroll, BorderLayout.CENTER);

        // Hint inicial cuando no hay usuarios
        rebuildCards();
        return wrap;
    }

    /* ----- Lista de grupos ----- */

    private JPanel crearPanelListaGrupos() {
        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);

        txtBuscarGrupos = crearBuscador("Buscar grupo…");
        txtBuscarGrupos.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { rebuildCards(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { rebuildCards(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { rebuildCards(); }
        });
        wrap.add(crearBuscadorWrap(txtBuscarGrupos), BorderLayout.NORTH);

        panelCardGrupos = new JPanel();
        panelCardGrupos.setLayout(new BoxLayout(panelCardGrupos, BoxLayout.Y_AXIS));
        panelCardGrupos.setOpaque(false);

        JScrollPane scroll = new JScrollPane(panelCardGrupos);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        wrap.add(scroll, BorderLayout.CENTER);

        rebuildCards();
        return wrap;
    }

    /* ----- Buscador reutilizable ----- */

    private JTextField crearBuscador(String placeholder) {
        JTextField txt = new JTextField() {
            @Override public boolean isOpaque() { return false; }
            @Override public Color getBackground() { return new Color(0, 0, 0, 0); }
        };
        txt.setOpaque(false);
        txt.setBorder(null);
        txt.setFont(Theme.fontBase());
        txt.setForeground(Theme.onSurface());
        txt.setCaretColor(Theme.primary());
        return txt;
    }

    private JPanel crearBuscadorWrap(JTextField txt) {
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

    /* ----- Reconstruir cards a partir de los modelos (filtrado por buscador) ----- */

    private void rebuildCards() {
        if (panelCardUsuarios != null) {
            panelCardUsuarios.removeAll();
            String filtro = txtBuscarUsuarios == null ? "" : txtBuscarUsuarios.getText().trim().toLowerCase();
            for (int i = 0; i < modeloUsuarios.getSize(); i++) {
                String u = modeloUsuarios.get(i);
                if (u == null) continue;
                if (!filtro.isEmpty() && !u.toLowerCase().contains(filtro)) continue;
                panelCardUsuarios.add(crearContactoCard(u));
            }
            panelCardUsuarios.add(Box.createVerticalGlue());
            panelCardUsuarios.revalidate();
            panelCardUsuarios.repaint();
        }

        if (panelCardGrupos != null) {
            panelCardGrupos.removeAll();
            String filtro = txtBuscarGrupos == null ? "" : txtBuscarGrupos.getText().trim().toLowerCase();
            for (int i = 0; i < modeloGrupos.getSize(); i++) {
                String g = modeloGrupos.get(i);
                if (g == null || g.trim().isEmpty()) continue;
                if (!filtro.isEmpty() && !g.toLowerCase().contains(filtro)) continue;
                panelCardGrupos.add(crearGrupoCard(g));
            }
            panelCardGrupos.add(Box.createVerticalGlue());
            panelCardGrupos.revalidate();
            panelCardGrupos.repaint();
        }
    }

    /* ----- Card de contacto ----- */

    private JPanel crearContactoCard(String contacto) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setOpaque(false);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        Avatar av = new Avatar(contacto, 38, Theme.primary(), Theme.tertiary());
        card.add(av, BorderLayout.WEST);

        JLabel lblNombre = new JLabel(contacto);
        lblNombre.setFont(Theme.font(14, Font.BOLD));
        lblNombre.setForeground(Theme.onSurface());
        lblNombre.setVerticalAlignment(SwingConstants.CENTER);

        JLabel lblEstado = new JLabel("● en línea");
        lblEstado.setFont(Theme.font(11, Font.PLAIN));
        lblEstado.setForeground(Theme.onSurfaceVariant());

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        lblNombre.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblEstado.setAlignmentX(Component.LEFT_ALIGNMENT);
        centro.add(lblNombre);
        centro.add(Box.createVerticalStrut(2));
        centro.add(lblEstado);
        card.add(centro, BorderLayout.CENTER);

        // Doble-click → abrir chat privado
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && contactoListener != null) {
                    contactoListener.onContactoSeleccionado(contacto);
                }
            }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { card.setBackground(Theme.surfaceHigh()); }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  { card.setBackground(null); }
        });
        return card;
    }

    /* ----- Card de grupo ----- */

    private JPanel crearGrupoCard(String nombre) {
        boolean privado = gruposPrivados.contains(nombre);

        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setOpaque(false);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        // Avatar grupo: circular con iniciales / hash
        Avatar avGrupo = new Avatar("#" + nombre, 38,
                privado ? new Color(0x94, 0x37, 0x00) : new Color(0x33, 0x66, 0xDD),
                privado ? new Color(0xBC, 0x48, 0x00) : new Color(0x66, 0x44, 0xCC));
        card.add(avGrupo, BorderLayout.WEST);

        JLabel lblNombre = new JLabel((privado ? "🔒 " : "# ") + nombre);
        lblNombre.setFont(Theme.font(14, Font.BOLD));
        lblNombre.setForeground(Theme.onSurface());
        lblNombre.setVerticalAlignment(SwingConstants.CENTER);

        JLabel lblHint = new JLabel("Doble click para unirse");
        lblHint.setFont(Theme.font(11, Font.PLAIN));
        lblHint.setForeground(Theme.onSurfaceVariant());

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        lblNombre.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        centro.add(lblNombre);
        centro.add(Box.createVerticalStrut(2));
        centro.add(lblHint);
        card.add(centro, BorderLayout.CENTER);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && grupoListener != null) {
                    String pwd = null;
                    if (privado) {
                        pwd = solicitarContrasenaGrupo(nombre);
                        if (pwd == null) return;
                    }
                    grupoListener.onUnirseGrupo(nombre, pwd);
                }
            }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { card.setBackground(Theme.surfaceHigh()); }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  { card.setBackground(null); }
        });
        return card;
    }

    /* ============================ FAB ============================ */

    private JPanel crearFAB() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(0, 16, 16, 16));

        JButton btn = new PrimaryButton("+ Crear nuevo grupo", 999);
        btn.setFont(Theme.font(13, Font.BOLD));
        btn.setPreferredSize(new Dimension(0, 48));
        btn.addActionListener(e -> mostrarDialogoCrearGrupo());
        wrap.add(btn, BorderLayout.CENTER);
        return wrap;
    }

    /* ============================ Modal "Crear grupo" ============================ */

    private void mostrarDialogoCrearGrupo() {
        final String[] resultado = new String[2];

        JDialog dialog = new JDialog(this, "Crear nuevo grupo", true);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));
        // El dialog ocupa exactamente el tamaño de la ventana padre: nada de offset raro.
        dialog.setSize(getSize());
        dialog.setLocation(getLocationOnScreen());

        // Capa overlay (oscurece + click fuera cierra)
        JPanel overlay = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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

        // Bottom-sheet: BorderLayout.SOUTH, redondeado sólo arriba.
        JPanel sheet = new JPanel() {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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

        // Drag handle (línea pequeña en el centro, opcional)
        JPanel handleWrap = new JPanel();
        handleWrap.setOpaque(false);
        handleWrap.setLayout(new BoxLayout(handleWrap, BoxLayout.X_AXIS));
        handleWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        handleWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        JPanel handle = new JPanel() {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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

        // Title row + close
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JLabel lblTitle = new JLabel("Crear nuevo grupo");
        lblTitle.setFont(Theme.font(20, Font.BOLD));
        lblTitle.setForeground(Theme.onSurface());
        titleRow.add(lblTitle, BorderLayout.WEST);

        // Botón cerrar con icono MaterialGlyph.close
        JButton btnClose = new JButton(MaterialGlyph.close(20, Theme.onSurfaceVariant())) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                if (getModel().isRollover()) {
                    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
        btnClose.setMargin(new Insets(0, 0, 0, 0));
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

        JTextField txtNombre = new Views.theme.TextInput();
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

        JPasswordField pwd = new Views.theme.PasswordInput();
        pwd.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        pwd.setAlignmentX(Component.LEFT_ALIGNMENT);
        pwdPanel.add(pwd);
        pwdPanel.setVisible(false);

        sheet.add(pwdPanel);
        sheet.add(Box.createVerticalStrut(20));
        checkPrivado.addActionListener(e -> pwdPanel.setVisible(checkPrivado.isSelected()));

        JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        GhostButton btnCancelar = new GhostButton("Cancelar", null, Theme.RADIUS_MD);
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
                resultado[0] = n;
                resultado[1] = p;
            } else {
                resultado[0] = n;
                resultado[1] = null;
            }
            dialog.dispose();
        });
        buttons.add(btnCrear);

        sheet.add(buttons);

        overlay.add(sheet, BorderLayout.SOUTH);
        // Wrapper transparente encima del overlay: bloquea clicks sobre el sheet
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
        JPasswordField txt = new Views.theme.PasswordInput();
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

    /* ============================ Helpers ============================ */

    private JButton iconButton(javax.swing.Icon icon, String tooltip) {
        JButton b = new JButton(icon);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setPreferredSize(new Dimension(36, 36));
        if (tooltip != null) b.setToolTipText(tooltip);
        return b;
    }

    private static class DotStatus extends JPanel {
        DotStatus(Color c) {
            setOpaque(false);
            setPreferredSize(new Dimension(8, 8));
            color = c;
        }
        private Color color;
        @Override protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    private static class SwingUtilities {
        static void invokeOnEDT(Runnable r) {
            if (java.awt.EventQueue.isDispatchThread()) r.run();
            else java.awt.EventQueue.invokeLater(r);
        }
    }

    /* ============================ API pública ============================ */

    public void actualizarLista(String[] usuarios) {
        modeloUsuarios.clear();
        if (usuarios != null) {
            for (String u : usuarios) {
                if (u != null && !u.trim().isEmpty()) modeloUsuarios.addElement(u);
            }
        }
        rebuildCards();
    }

    public void actualizarListaGrupos(String[] grupos) {
        modeloGrupos.clear();
        if (grupos != null) {
            for (String g : grupos) {
                if (g != null && !g.trim().isEmpty()) modeloGrupos.addElement(g);
            }
        }
        rebuildCards();
    }

    public void actualizarGruposPrivados(String[] gruposPrivadosArr) {
        this.gruposPrivados.clear();
        if (gruposPrivadosArr != null) {
            for (String g : gruposPrivadosArr) {
                if (g != null && !g.trim().isEmpty()) this.gruposPrivados.add(g);
            }
        }
        rebuildCards();
    }

    public void setContactoListener(ContactoListener listener) { this.contactoListener = listener; }
    public void setGrupoListener(GrupoListener listener)       { this.grupoListener = listener; }
}
