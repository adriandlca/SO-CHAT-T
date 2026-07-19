package Views;

import Controllers.HistorialChat;
import Views.theme.GhostButton;
import Views.theme.MaterialGlyph;
import Views.theme.RoundBorder;
import Views.theme.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.image.BufferedImage;

/**
 * Panel reutilizable con toda la UI de un chat individual. No contiene
 * JFrame: puede embebirse en cualquier contenedor (VentanaChatUnificado,
 * VentanaChat legacy, tests, etc.).
 *
 * Mantiene la API pública del antiguo VentanaChat.
 */
public class ChatPanel extends JPanel {

    private final String miUsuario;
    private final String contactoDestino;
    private final boolean esGrupo;

    private JTextArea taInputMensaje;
    private JButton jbtnEnviar;
    private JPanel panelContenedorMensajes;
    private JScrollPane scrollLectura;
    private JPanel panelBusqueda;
    private JLabel lblEscribiendo;

    private Timer timerEscribiendo;
    private boolean isTyping = false;
    private java.util.function.Consumer<Boolean> accionEscribiendo;

    private Set<String> usuariosEscribiendo = new HashSet<>();

    private java.util.function.Consumer<File> accionEnviarImagen;
    private java.util.function.Consumer<File> accionEnviarArchivo;
    private java.util.function.Consumer<String> accionEnviarSticker;

    @Deprecated
    private Runnable onVolver;

    public ChatPanel(String miUsuario, String contactoDestino, boolean esGrupo) {
        this.miUsuario = miUsuario;
        this.contactoDestino = contactoDestino;
        this.esGrupo = esGrupo;

        setLayout(new BorderLayout());
        setBackground(Theme.surface());
        setOpaque(true);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.surface());
        root.add(crearHeader(), BorderLayout.NORTH);

        JPanel lectura = new JPanel(new BorderLayout());
        lectura.setBackground(Theme.surface());
        lectura.setBorder(new EmptyBorder(0, 0, 0, 0));

        panelContenedorMensajes = new JPanel();
        panelContenedorMensajes.setLayout(new BoxLayout(panelContenedorMensajes, BoxLayout.Y_AXIS));
        panelContenedorMensajes.setBackground(Theme.surface());
        panelContenedorMensajes.setBorder(new EmptyBorder(16, 0, 16, 0));
        // Cualquier click en el área de mensajes (no sobre un botón/burbuja interactiva)
        // devuelve el foco al textarea para que el siguiente carácter tecleado vaya allí.
        instalarFocoClickEnArea(panelContenedorMensajes);

        scrollLectura = new JScrollPane(panelContenedorMensajes) {
            @Override public boolean isOpaque() { return false; }
        };
        scrollLectura.setOpaque(false);
        scrollLectura.getViewport().setOpaque(false);
        scrollLectura.setBorder(null);
        scrollLectura.getVerticalScrollBar().setUnitIncrement(16);
        scrollLectura.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        lectura.add(scrollLectura, BorderLayout.CENTER);

        root.add(lectura, BorderLayout.CENTER);
        root.add(crearSouthZone(), BorderLayout.SOUTH);

        add(root, BorderLayout.CENTER);

        // Panel de búsqueda (overlay derecho; inicialmente oculto)
        panelBusqueda = crearPanelBusqueda();
        panelBusqueda.setVisible(false);
        add(panelBusqueda, BorderLayout.EAST);
    }

    /* ============================ HEADER ============================ */

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout(8, 0)) {
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
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(8, 16, 8, 16));
        header.setPreferredSize(new Dimension(0, 64));

        // Left side (avatar + title). El botón "back" se eliminó del rediseño unificado
        // (el sidebar queda siempre visible; mantener back era engañoso).
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.setAlignmentY(Component.CENTER_ALIGNMENT);

        // Avatar del contacto/grupo
        Views.theme.Avatar avatar = new Views.theme.Avatar(contactoDestino, 40,
                Theme.primary(), Theme.tertiary());
        left.add(avatar);

        JLabel lblTitulo = new JLabel(contactoDestino);
        lblTitulo.setFont(Theme.fontTitle());
        lblTitulo.setForeground(Theme.onSurface());
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Estado "Active now" con punto verde (solo privados)
        JPanel subWrap = new JPanel();
        subWrap.setOpaque(false);
        subWrap.setLayout(new BoxLayout(subWrap, BoxLayout.Y_AXIS));
        subWrap.setAlignmentY(Component.CENTER_ALIGNMENT);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        subWrap.add(lblTitulo);

        JPanel estadoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        estadoRow.setOpaque(false);
        estadoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        DotStatus dot = new DotStatus(Theme.SUCCESS);
        estadoRow.add(dot);
        JLabel lblEstado = new JLabel(esGrupo ? "Grupo · múltiples miembros" : "Active now");
        lblEstado.setFont(Theme.font(12, Font.PLAIN));
        lblEstado.setForeground(Theme.onSurfaceVariant());
        estadoRow.add(lblEstado);
        subWrap.add(estadoRow);

        left.add(subWrap);
        header.add(left, BorderLayout.WEST);

        // Right actions (sólo búsqueda, los demás botones sin implementación se eliminan)
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);

        JButton btnSearch = iconButton(MaterialGlyph.search(22, Theme.onSurfaceVariant()), "Buscar en la conversación");
        btnSearch.setPreferredSize(new Dimension(36, 36));
        btnSearch.addActionListener(e -> togglePanelBusqueda());
        right.add(btnSearch);

        header.add(right, BorderLayout.EAST);
        return header;
    }

    private void togglePanelBusqueda() {
        boolean show = !panelBusqueda.isVisible();
        panelBusqueda.setVisible(show);
        revalidate();
        repaint();
    }

    /* ============================ SOUTH (composer + typing) ============================ */

    private JPanel crearSouthZone() {
        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.setBackground(Theme.surface());
        south.setBorder(new EmptyBorder(0, 16, 16, 16));

        // Typing indicator
        lblEscribiendo = new JLabel(" ");
        lblEscribiendo.setFont(Theme.font(12, Font.ITALIC));
        lblEscribiendo.setForeground(Theme.onSurfaceVariant());
        lblEscribiendo.setBorder(new EmptyBorder(0, 8, 2, 0));
        south.add(lblEscribiendo, BorderLayout.NORTH);

        // === Composer bar tipo Stitch (HTML referencia) ===
        JPanel composerBar = new JPanel() {
            @Override public boolean isOpaque() { return false; }
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.surfaceLow());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_LG, Theme.RADIUS_LG);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        composerBar.setLayout(new BoxLayout(composerBar, BoxLayout.X_AXIS));
        composerBar.setOpaque(false);
        composerBar.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(Theme.RADIUS_LG, Theme.outlineVariant()),
                new EmptyBorder(4, 8, 4, 4)));

        Dimension btnSize = new Dimension(40, 40);

        // --- 1) Textarea (grow) ---
        taInputMensaje = new JTextArea(1, 20);
        taInputMensaje.setFont(Theme.fontEmoji(15, Font.PLAIN));
        taInputMensaje.setLineWrap(true);
        taInputMensaje.setWrapStyleWord(true);
        taInputMensaje.setForeground(Theme.onSurface());
        taInputMensaje.setBackground(Theme.surfaceLow());
        taInputMensaje.setCaretColor(Theme.primary());
        taInputMensaje.setBorder(new EmptyBorder(6, 6, 6, 6));
        taInputMensaje.setOpaque(false);
        taInputMensaje.setFocusable(true);
        taInputMensaje.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JScrollPane inputScroll = new JScrollPane(taInputMensaje);
        inputScroll.setOpaque(false);
        inputScroll.getViewport().setOpaque(false);
        inputScroll.setBorder(null);
        inputScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        inputScroll.getVerticalScrollBar().setUnitIncrement(14);

        composerBar.add(inputScroll);
        composerBar.add(Box.createHorizontalGlue());

        // --- 2) Toolbar con botones (emoji/img/file/sticker) ---
        JPanel toolbar = new JPanel() {
            @Override public boolean isOpaque() { return false; }
        };
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setBackground(Theme.surfaceLow());

        JButton btnEmoji = iconButton(MaterialGlyph.emoji(20, Theme.onSurfaceVariant()), "Emojis");
        btnEmoji.setPreferredSize(btnSize);
        btnEmoji.setMaximumSize(btnSize);
        btnEmoji.setMinimumSize(btnSize);
        btnEmoji.addActionListener(e -> {
            JPopupMenu menu = crearPopupEmojis();
            menu.show(btnEmoji, 0, -menu.getPreferredSize().height - 6);
        });
        toolbar.add(btnEmoji);

        JButton btnImage = iconButton(MaterialGlyph.image(20, Theme.onSurfaceVariant()), "Adjuntar imagen");
        btnImage.setPreferredSize(btnSize);
        btnImage.setMaximumSize(btnSize);
        btnImage.setMinimumSize(btnSize);
        btnImage.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Selecciona una imagen para enviar");
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imágenes (JPG, PNG)", "jpg", "jpeg", "png"));
            if (fc.showOpenDialog(SwingUtilities.getWindowAncestor(this)) == JFileChooser.APPROVE_OPTION && accionEnviarImagen != null) {
                accionEnviarImagen.accept(fc.getSelectedFile());
            }
        });
        toolbar.add(btnImage);

        JButton btnFile = iconButton(MaterialGlyph.attachFile(20, Theme.onSurfaceVariant()), "Adjuntar documento");
        btnFile.setPreferredSize(btnSize);
        btnFile.setMaximumSize(btnSize);
        btnFile.setMinimumSize(btnSize);
        btnFile.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Selecciona un documento para enviar");
            java.awt.Window w = SwingUtilities.getWindowAncestor(this);
            if (fc.showOpenDialog(w) == JFileChooser.APPROVE_OPTION && accionEnviarArchivo != null) {
                accionEnviarArchivo.accept(fc.getSelectedFile());
            }
        });
        toolbar.add(btnFile);

        JButton btnSticker = iconButton(MaterialGlyph.sticker(20, Theme.onSurfaceVariant()), "Stickers");
        btnSticker.setPreferredSize(btnSize);
        btnSticker.setMaximumSize(btnSize);
        btnSticker.setMinimumSize(btnSize);
        btnSticker.addActionListener(e -> {
            JPopupMenu menu = crearPopupStickers();
            menu.show(btnSticker, 0, -menu.getPreferredSize().height - 6);
        });
        toolbar.add(btnSticker);

        composerBar.add(toolbar);
        composerBar.add(Box.createHorizontalStrut(4));

        // --- 3) Botón enviar redondo tipo Stitch (patrón PrimaryButton) ---
        // El botón está SIEMPRE habilitado. Si el texto está vacío, el helper `dispararEnvio`
        // ignora la pulsación (no hace nada visualmente). Esto evita depender del estado
        // disabled para detectar el click.
        jbtnEnviar = new JButton() {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                boolean conTexto = taInputMensaje != null && !taInputMensaje.getText().trim().isEmpty();
                boolean rollover = getModel().isRollover();
                boolean pressed  = getModel().isPressed();
                if (!conTexto)             g2.setColor(Theme.surfaceHigh());
                else if (pressed)          g2.setColor(Theme.primaryContainer());
                else if (rollover)         g2.setColor(Theme.primaryContainer());
                else                       g2.setColor(Theme.primary());
                g2.fillRoundRect(0, 0, w, h, Theme.RADIUS_MD, Theme.RADIUS_MD);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        jbtnEnviar.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        jbtnEnviar.setIcon(MaterialGlyph.sendUp(20, Color.WHITE));
        jbtnEnviar.setPreferredSize(btnSize);
        jbtnEnviar.setMaximumSize(btnSize);
        jbtnEnviar.setMinimumSize(btnSize);
        jbtnEnviar.setFocusPainted(false);
        jbtnEnviar.setBorderPainted(false);
        jbtnEnviar.setContentAreaFilled(false);
        jbtnEnviar.setOpaque(false);
        jbtnEnviar.setBorder(null);
        jbtnEnviar.setMargin(new Insets(0, 0, 0, 0));
        jbtnEnviar.setIconTextGap(0);
        jbtnEnviar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        jbtnEnviar.setEnabled(true);
        jbtnEnviar.setToolTipText("Enviar mensaje (Enter)");
        composerBar.add(jbtnEnviar);

        south.add(composerBar, BorderLayout.CENTER);

        // Repintar el botón cuando hay cambios de texto para actualizar su visual enabled/disabled.
        taInputMensaje.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { actualizarEstadoEnvio(); }
            public void removeUpdate(DocumentEvent e) { actualizarEstadoEnvio(); }
            public void changedUpdate(DocumentEvent e) { actualizarEstadoEnvio(); }
        });

        // ENTER envía, SHIFT+ENTER salto de línea.
        taInputMensaje.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enviar");
        taInputMensaje.getActionMap().put("enviar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispararEnvio(); }
        });
        taInputMensaje.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), "salto");
        taInputMensaje.getActionMap().put("salto", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { taInputMensaje.append("\n"); }
        });

        // Click en el botón → siempre dispara el helper.
        jbtnEnviar.addActionListener(e -> dispararEnvio());

        configurarDetectorEscritura();

        // Hint Enter para enviar (Stitch design)
        JLabel lblHint = new JLabel("Press Enter to send, Shift + Enter for new line");
        lblHint.setFont(Theme.font(11, Font.PLAIN));
        lblHint.setForeground(Theme.outline());
        lblHint.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel hintWrap = new JPanel(new BorderLayout());
        hintWrap.setOpaque(false);
        hintWrap.add(lblHint, BorderLayout.CENTER);
        hintWrap.setBorder(new EmptyBorder(2, 0, 0, 0));
        south.add(hintWrap, BorderLayout.SOUTH);

        return south;
    }

    /** Inserta un separador tipo pill (Hoy/Ayer/Fecha) en el stream de mensajes. */
    public void mostrarSeparadorDia(String texto) {
        JPanel wrap = new JPanel() {
            @Override public boolean isOpaque() { return false; }
        };
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.X_AXIS));
        wrap.setBorder(new EmptyBorder(8, 8, 8, 8));
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JPanel pill = new JPanel() {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.surfaceLow());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        pill.setOpaque(false);
        pill.setBorder(new EmptyBorder(3, 12, 3, 12));
        JLabel l = new JLabel(texto);
        l.setFont(Theme.font(11, Font.BOLD));
        l.setForeground(Theme.outline());
        pill.add(l);

        wrap.add(Box.createHorizontalGlue());
        wrap.add(pill);
        wrap.add(Box.createHorizontalGlue());
        panelContenedorMensajes.add(wrap);
        panelContenedorMensajes.revalidate();
        panelContenedorMensajes.repaint();
    }

    /* ============================ Detector de typing ============================ */

    private void configurarDetectorEscritura() {
        timerEscribiendo = new Timer(1500, e -> {
            if (isTyping) {
                isTyping = false;
                if (accionEscribiendo != null) accionEscribiendo.accept(false);
            }
        });
        timerEscribiendo.setRepeats(false);
        taInputMensaje.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { avisar(); }
            public void removeUpdate(DocumentEvent e) { avisar(); }
            public void changedUpdate(DocumentEvent e) { avisar(); }
        });
    }

    private void avisar() {
        if (!isTyping && taInputMensaje.getText().length() > 0) {
            isTyping = true;
            if (accionEscribiendo != null) accionEscribiendo.accept(true);
        }
        if (taInputMensaje.getText().length() == 0 && isTyping) {
            isTyping = false;
            timerEscribiendo.stop();
            if (accionEscribiendo != null) accionEscribiendo.accept(false);
        } else {
            timerEscribiendo.restart();
        }
    }

    /* ============================ Panel de búsqueda lateral ============================ */

    private JPanel crearPanelBusqueda() {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setColor(Theme.surfaceLowest());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.outlineVariant());
                g2.drawLine(0, 0, 0, getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(300, 0));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(new EmptyBorder(16, 16, 8, 16));
        JLabel lbl = new JLabel("Buscar mensajes");
        lbl.setFont(Theme.font(15, Font.BOLD));
        lbl.setForeground(Theme.onSurface());
        head.add(lbl, BorderLayout.WEST);
        JButton btnCerrar = iconButton(MaterialGlyph.close(18, Theme.onSurfaceVariant()), "Cerrar");
        btnCerrar.addActionListener(e -> togglePanelBusqueda());
        head.add(btnCerrar, BorderLayout.EAST);
        p.add(head, BorderLayout.NORTH);

        JPanel inputRow = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.surface());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_MD, Theme.RADIUS_MD);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        inputRow.setOpaque(false);
        inputRow.setBorder(new EmptyBorder(0, 12, 0, 12));
        inputRow.setPreferredSize(new Dimension(0, 38));

        JTextField txtBuscar = new JTextField("Buscar…") {
            @Override public boolean isOpaque() { return false; }
        };
        txtBuscar.setOpaque(false);
        txtBuscar.setBorder(null);
        txtBuscar.setFont(Theme.fontBase());
        txtBuscar.setForeground(Theme.onSurface());

        inputRow.add(new JLabel(MaterialGlyph.search(18, Theme.onSurfaceVariant())), BorderLayout.WEST);
        inputRow.add(txtBuscar, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 16, 12, 16));
        top.add(inputRow, BorderLayout.CENTER);
        p.add(top, BorderLayout.NORTH);

        JPanel panelResultados = new JPanel();
        panelResultados.setLayout(new BoxLayout(panelResultados, BoxLayout.Y_AXIS));
        panelResultados.setBackground(Theme.surfaceLowest());
        panelResultados.setOpaque(true);

        JScrollPane scrollR = new JScrollPane(panelResultados);
        scrollR.setOpaque(false);
        scrollR.getViewport().setOpaque(false);
        scrollR.setBorder(null);
        scrollR.getVerticalScrollBar().setUnitIncrement(14);
        p.add(scrollR, BorderLayout.CENTER);

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { ejecutar(); }
            public void removeUpdate(DocumentEvent e) { ejecutar(); }
            public void changedUpdate(DocumentEvent e) { ejecutar(); }
            private void ejecutar() {
                String filtro = txtBuscar.getText();
                panelResultados.removeAll();
                if (!filtro.trim().isEmpty()) {
                    List<String> matches = HistorialChat.buscarMensajes(miUsuario, contactoDestino, filtro, esGrupo);
                    for (String linea : matches) {
                        panelResultados.add(crearItemResultado(linea, filtro));
                        panelResultados.add(Box.createVerticalStrut(6));
                    }
                }
                panelResultados.revalidate();
                panelResultados.repaint();
            }
        });

        return p;
    }

    private JPanel crearItemResultado(String linea, String filtro) {
        JPanel item = new JPanel(new BorderLayout(0, 4));
        item.setBackground(Theme.surfaceLowest());
        item.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(Theme.RADIUS_SM, Theme.outlineVariant()),
                new EmptyBorder(10, 12, 10, 12)));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        try {
            int idxCierre = linea.indexOf("] ");
            String fecha = linea.substring(1, 11);
            String resto = linea.substring(idxCierre + 2);
            int idxDP = resto.indexOf(": ");
            if (idxCierre == -1 || idxDP == -1) throw new Exception();

            String remitente = resto.substring(0, idxDP);
            String mensaje = resto.substring(idxDP + 2);

            JLabel lblFecha = new JLabel(fecha);
            lblFecha.setFont(Theme.fontMono(10));
            lblFecha.setForeground(Theme.onSurfaceVariant());

            String regex = "(?i)(" + Pattern.quote(filtro) + ")";
            String res = mensaje.replaceAll(regex, "<b style='color: #2563EB; background: #EFF6FF;'>$1</b>");
            JLabel lblMsg = new JLabel("<html><div style='width: 240px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;'>"
                    + "<b>" + remitente + ":</b> " + res + "</div></html>");
            lblMsg.setFont(Theme.font(12, Font.PLAIN));
            lblMsg.setForeground(Theme.onSurface());

            item.add(lblFecha, BorderLayout.NORTH);
            item.add(lblMsg,   BorderLayout.CENTER);

            final String target = mensaje;
            item.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) { scrollHaciaTexto(target); }
            });
        } catch (Exception e) {
            JLabel lbl = new JLabel(linea);
            lbl.setFont(Theme.font(12, Font.PLAIN));
            item.add(lbl, BorderLayout.CENTER);
        }
        return item;
    }

    private void scrollHaciaTexto(String texto) {
        for (int i = 0; i < panelContenedorMensajes.getComponentCount(); i++) {
            BurbujaMensaje b = encontrarBurbuja(panelContenedorMensajes.getComponent(i));
            if (b != null) {
                String t = obtenerTexto(b);
                if (t != null && t.contains(texto)) {
                    SwingUtilities.invokeLater(() -> {
                        Rectangle r = SwingUtilities.convertRectangle(panelContenedorMensajes, b.getBounds(), scrollLectura.getViewport());
                        JScrollBar vertical = scrollLectura.getVerticalScrollBar();
                        vertical.setValue(Math.max(0, r.y - 20));
                    });
                    return;
                }
            }
        }
    }

    private BurbujaMensaje encontrarBurbuja(Component c) {
        if (c instanceof BurbujaMensaje) return (BurbujaMensaje) c;
        if (c instanceof java.awt.Container) {
            for (Component child : ((java.awt.Container) c).getComponents()) {
                BurbujaMensaje b = encontrarBurbuja(child);
                if (b != null) return b;
            }
        }
        return null;
    }

    private String obtenerTexto(Component c) {
        if (c instanceof BurbujaMensaje) {
            return BurbujaExtractor.extraer((BurbujaMensaje) c);
        }
        if (c instanceof java.awt.Container) {
            for (Component child : ((java.awt.Container) c).getComponents()) {
                String t = obtenerTexto(child);
                if (t != null && !t.trim().isEmpty()) return t;
            }
        }
        return null;
    }

    /* ============================ Popups (emojis / stickers) ============================ */

    private JPopupMenu crearPopupEmojis() {
        JPopupMenu panel = new JPopupMenu();
        panel.setBackground(Theme.surfaceLowest());
        panel.setBorder(new RoundBorder(Theme.RADIUS_MD, Theme.outlineVariant()));

        String[] emojis = {"😀","😂","😊","😍","🤔","😥","😡","👍","🙌","🔥","🎉","❤️"};
        JPanel grid = new JPanel(new java.awt.GridLayout(3, 4, 4, 4));
        grid.setBackground(Theme.surfaceLowest());
        grid.setBorder(new EmptyBorder(6, 6, 6, 6));
        for (String e : emojis) {
            JButton b = new JButton(e);
            b.setFont(Theme.fontEmoji(22, Font.PLAIN));
            b.setBackground(Theme.surfaceLowest());
            b.setBorder(new EmptyBorder(2, 2, 2, 2));
            b.setFocusPainted(false);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            b.addActionListener(ev -> {
                taInputMensaje.append(e);
                taInputMensaje.requestFocus();
                panel.setVisible(false);
            });
            grid.add(b);
        }
        panel.add(grid);
        panel.setPreferredSize(new Dimension(260, 150));
        return panel;
    }

    private JPopupMenu crearPopupStickers() {
        JPopupMenu panel = new JPopupMenu();
        panel.setBackground(Theme.surfaceLowest());

        File carpeta = new File("stickers");
        if (!carpeta.exists()) carpeta.mkdir();
        File[] archivos = carpeta.listFiles((dir, name) -> {
            String l = name.toLowerCase();
            return l.endsWith(".png") || l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".gif");
        });
        if (archivos == null || archivos.length == 0) {
            JLabel info = new JLabel("<html><center>Carpeta <b>stickers/</b> vacía.<br>Guarda imágenes PNG o JPG allí.</center></html>", SwingConstants.CENTER);
            info.setFont(Theme.font(12, Font.PLAIN));
            info.setForeground(Theme.onSurfaceVariant());
            info.setBorder(new EmptyBorder(15, 15, 15, 15));
            info.setOpaque(true);
            info.setBackground(Theme.surfaceLowest());
            panel.add(info);
            panel.setPreferredSize(new Dimension(240, 90));
            return panel;
        }
        int cols = 3;
        int filas = (int) Math.ceil((double) archivos.length / cols);
        JPanel grid = new JPanel(new java.awt.GridLayout(filas, cols, 6, 6));
        grid.setBackground(Theme.surfaceLowest());
        grid.setBorder(new EmptyBorder(6, 6, 6, 6));

        for (File f : archivos) {
            try {
                BufferedImage orig = ImageIO.read(f);
                if (orig == null) continue;
                BufferedImage thumb = redimensionarBuffered(orig, 55, 55);
                JButton b = new JButton(new ImageIcon(thumb));
                b.setBackground(Theme.surfaceLow());
                b.setBorder(new RoundBorder(Theme.RADIUS_SM, Theme.outlineVariant()));
                b.setFocusPainted(false);
                b.setCursor(new Cursor(Cursor.HAND_CURSOR));
                b.setToolTipText(f.getName());
                b.addActionListener(ev -> {
                    BufferedImage std = redimensionarBuffered(orig, 120, 120);
                    String b64 = bufferedToBase64(std);
                    if (b64 != null && accionEnviarSticker != null) accionEnviarSticker.accept(b64);
                    panel.setVisible(false);
                });
                grid.add(b);
            } catch (Exception ex) { System.err.println("Sticker load failed: " + f.getName()); }
        }
        JScrollPane sp = new JScrollPane(grid);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(null);
        sp.setPreferredSize(new Dimension(240, 220));
        panel.add(sp);
        panel.setPreferredSize(new Dimension(250, 230));
        return panel;
    }

    private BufferedImage redimensionarBuffered(BufferedImage orig, int w, int h) {
        double r = Math.min((double) w / orig.getWidth(), (double) h / orig.getHeight());
        int nw = (int) (orig.getWidth() * r);
        int nh = (int) (orig.getHeight() * r);
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(orig, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    private String bufferedToBase64(BufferedImage img) {
        try {
            java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
            ImageIO.write(img, "png", b);
            return Base64.getEncoder().encodeToString(b.toByteArray());
        } catch (Exception e) { return null; }
    }

    /* ============================ Mostrar mensajes ============================ */

    private JPanel wrapperBurbujaCentrado(Component contenido) {
        JPanel alineador = new JPanel() {
            @Override public boolean isOpaque() { return false; }
        };
        alineador.setLayout(new BoxLayout(alineador, BoxLayout.X_AXIS));
        alineador.setBorder(new EmptyBorder(0, 8, 0, 8));
        if (contenido instanceof javax.swing.JComponent) {
            ((javax.swing.JComponent) contenido).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        alineador.add(contenido);
        alineador.add(Box.createHorizontalGlue());
        alineador.setMaximumSize(new Dimension(Integer.MAX_VALUE, contenido.getPreferredSize().height + 16));
        return alineador;
    }

    public void mostrarMensajeConColor(String remitente, String mensaje, Color colorRemitente) {
        String nombreLimpio = remitente.replace("[", "").replace("]: ", "").replace("Tú: ", "Tú");
        boolean esMio = nombreLimpio.equals("Tú");
        BurbujaMensaje b = new BurbujaMensaje(nombreLimpio, mensaje.trim(), colorRemitente, esMio);
        JPanel wrap = wrapperBurbujaCentrado(b);
        panelContenedorMensajes.add(wrap);
        panelContenedorMensajes.revalidate();
        panelContenedorMensajes.repaint();
        desplazarScrollAlFinal();
    }

    public void mostrarImagenConColor(String remitente, ImageIcon imagen, Color colorRemitente) {
        String nombreLimpio = remitente.replace("[", "").replace("]: ", "").replace("Tú: ", "Tú");
        boolean esMio = nombreLimpio.equals("Tú");
        BurbujaMensaje b = new BurbujaMensaje(nombreLimpio, imagen, colorRemitente, esMio);
        JPanel wrap = wrapperBurbujaCentrado(b);
        panelContenedorMensajes.add(wrap);
        panelContenedorMensajes.revalidate();
        panelContenedorMensajes.repaint();
        desplazarScrollAlFinal();
    }

    public void mostrarStickerConColor(String remitente, ImageIcon sticker, Color colorRemitente) {
        String nombreLimpio = remitente.replace("[", "").replace("]: ", "").replace("Tú: ", "Tú");

        JPanel alin = new JPanel() {
            @Override public boolean isOpaque() { return false; }
        };
        alin.setLayout(new BoxLayout(alin, BoxLayout.X_AXIS));
        alin.setBorder(new EmptyBorder(4, 8, 4, 8));

        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setOpaque(false);

        JLabel lblNombre = new JLabel(nombreLimpio);
        lblNombre.setFont(Theme.font(12, Font.BOLD));
        lblNombre.setForeground(colorRemitente);
        lblNombre.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(lblNombre);
        wrap.add(Box.createVerticalStrut(2));

        JLabel lblSticker = new JLabel(sticker);
        lblSticker.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(lblSticker);

        alin.add(wrap);
        alin.add(Box.createHorizontalGlue());
        int h = wrap.getPreferredSize().height;
        alin.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.max(80, h + 16)));
        panelContenedorMensajes.add(alin);
        panelContenedorMensajes.revalidate();
        panelContenedorMensajes.repaint();
        desplazarScrollAlFinal();
    }

    public void mostrarBotonDescargaArchivo(String remitente, String nombreArchivo, String base64, Color colorRemitente) {
        String nombreLimpio = remitente.replace("[", "").replace("]: ", "").replace("Tú: ", "Tú");

        JPanel alin = new JPanel() {
            @Override public boolean isOpaque() { return false; }
        };
        alin.setLayout(new BoxLayout(alin, BoxLayout.X_AXIS));
        alin.setBorder(new EmptyBorder(4, 8, 4, 8));

        JPanel caja = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.surfaceLow());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_LG, Theme.RADIUS_LG);
                g2.setColor(Theme.outlineVariant());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.RADIUS_LG, Theme.RADIUS_LG);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        caja.setOpaque(false);
        caja.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel lblNombre = new JLabel(nombreLimpio);
        lblNombre.setFont(Theme.font(12, Font.BOLD));
        lblNombre.setForeground(colorRemitente);

        JPanel filaBoton = new JPanel(new BorderLayout(8, 0));
        filaBoton.setOpaque(false);
        JLabel iconFile = new JLabel(MaterialGlyph.attachFile(18, Theme.primary()));
        JLabel lblFile = new JLabel(nombreArchivo);
        lblFile.setFont(Theme.font(13, Font.PLAIN));
        lblFile.setForeground(Theme.onSurface());
        filaBoton.add(iconFile, BorderLayout.WEST);
        filaBoton.add(lblFile, BorderLayout.CENTER);

        GhostButton btnDescargar = new GhostButton("Descargar", MaterialGlyph.download(16, Theme.primary()), 999);
        btnDescargar.setFont(Theme.font(12, Font.BOLD));
        btnDescargar.setForeground(Theme.primary());
        btnDescargar.setBackground(Theme.surfaceLow());
        btnDescargar.setOpaque(true);
        btnDescargar.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Guardar archivo como...");
            fc.setSelectedFile(new File(nombreArchivo));
            java.awt.Window w = SwingUtilities.getWindowAncestor(this);
            if (fc.showSaveDialog(w) == JFileChooser.APPROVE_OPTION) {
                File destino = fc.getSelectedFile();
                if (ManejadorArchivos.guardarArchivoManual(base64, destino)) {
                    JOptionPane.showMessageDialog(w,
                            "Archivo guardado en:\n" + destino.getAbsolutePath(),
                            "Descarga completada", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        filaBoton.add(btnDescargar, BorderLayout.EAST);

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        lblNombre.setAlignmentX(Component.LEFT_ALIGNMENT);
        filaBoton.setAlignmentX(Component.LEFT_ALIGNMENT);
        inner.add(lblNombre);
        inner.add(Box.createVerticalStrut(4));
        inner.add(filaBoton);

        caja.add(inner, BorderLayout.CENTER);
        alin.add(caja);
        alin.add(Box.createHorizontalGlue());
        int h = caja.getPreferredSize().height;
        alin.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.max(60, h + 16)));

        panelContenedorMensajes.add(alin);
        panelContenedorMensajes.revalidate();
        panelContenedorMensajes.repaint();
        desplazarScrollAlFinal();
    }

    public void mostrarMensajePlano(String textoAnterior) {
        JLabel lbl = new JLabel(textoAnterior);
        lbl.setFont(Theme.font(12, Font.ITALIC));
        lbl.setForeground(Theme.onSurfaceVariant());
        JPanel alin = new JPanel() {
            @Override public boolean isOpaque() { return false; }
        };
        alin.setLayout(new BoxLayout(alin, BoxLayout.X_AXIS));
        alin.setBorder(new EmptyBorder(4, 8, 4, 8));
        alin.add(lbl);
        alin.add(Box.createHorizontalGlue());
        panelContenedorMensajes.add(alin);
    }

    /* ============================ Parser unificado de historial ============================ */

    /**
     * Renderiza un mensaje del historial detectando su tipo por prefijo:
     *   [ARCHIVO]nombre&lt;::&gt;base64 → botón de descarga
     *   [IMAGEN]base64                 → imagen
     *   [STICKER]base64                → sticker
     *   otro                           → texto plano
     *
     * Si el base64 está corrupto o la imagen no se puede decodificar, cae a texto
     * (con un prefijo que indica el fallo para que se note el problema).
     */
    public void procesarMensajeDeHistorial(String remitente, String texto, Color colorRemitente) {
        if (texto == null || texto.isEmpty()) return;

        String nombreLimpio = remitente == null ? "" :
                remitente.replace("[", "").replace("]: ", "").replace("Tú: ", "Tú");

        // 1) ARCHIVO
        if (texto.startsWith("[ARCHIVO]")) {
            String contenido = texto.substring(9);
            int sep = contenido.indexOf("<::>");
            if (sep > 0 && sep < contenido.length() - 4) {
                String nombreArchivo = contenido.substring(0, sep);
                String b64 = contenido.substring(sep + 4);
                if (!nombreArchivo.isEmpty() && !b64.isEmpty()) {
                    mostrarBotonDescargaArchivo(nombreLimpio, nombreArchivo, b64, colorRemitente);
                    return;
                }
            }
            mostrarMensajeConColor(nombreLimpio,
                    "[Archivo no disponible: nombre/base64 inválidos]",
                    colorRemitente);
            return;
        }

        // 2) IMAGEN
        if (texto.startsWith("[IMAGEN]")) {
            String b64 = texto.substring(8);
            if (!b64.isEmpty()) {
                ImageIcon img = ConversorImagen.base64ToImageIcon(b64);
                if (img != null && img.getIconWidth() > 0) {
                    mostrarImagenConColor(nombreLimpio, img, colorRemitente);
                    return;
                }
            }
            mostrarMensajeConColor(nombreLimpio,
                    "[Imagen no disponible: base64 corrupto]",
                    colorRemitente);
            return;
        }

        // 3) STICKER
        if (texto.startsWith("[STICKER]")) {
            String b64 = texto.substring(9);
            if (!b64.isEmpty()) {
                ImageIcon st = ConversorImagen.base64ToImageIcon(b64);
                if (st != null && st.getIconWidth() > 0) {
                    mostrarStickerConColor(nombreLimpio, st, colorRemitente);
                    return;
                }
            }
            mostrarMensajeConColor(nombreLimpio,
                    "[Sticker no disponible]",
                    colorRemitente);
            return;
        }

        // 4) TEXTO PLANO (con tope de 200 chars para no explotar la UI)
        String display = texto.length() > 200 ? texto.substring(0, 200) + "…" : texto;
        mostrarMensajeConColor(nombreLimpio, display, colorRemitente);
    }

    /* ============================ Typing indicator ============================ */

    public void mostrarEscribiendo(boolean escribiendo) {
        SwingUtilities.invokeLater(() -> {
            if (escribiendo) {
                lblEscribiendo.setIcon(dotsIcon());
                lblEscribiendo.setText("escribiendo...");
                lblEscribiendo.setIconTextGap(6);
            } else {
                lblEscribiendo.setIcon(null);
                lblEscribiendo.setText(" ");
            }
        });
    }

    public void mostrarEscribiendoGrupal(String remitente, boolean escribiendo) {
        SwingUtilities.invokeLater(() -> {
            if (escribiendo) usuariosEscribiendo.add(remitente);
            else              usuariosEscribiendo.remove(remitente);
            if (usuariosEscribiendo.isEmpty()) {
                lblEscribiendo.setIcon(null);
                lblEscribiendo.setText(" ");
            } else if (usuariosEscribiendo.size() == 1) {
                lblEscribiendo.setIcon(dotsIcon());
                lblEscribiendo.setText(usuariosEscribiendo.iterator().next() + " está escribiendo...");
                lblEscribiendo.setIconTextGap(6);
            } else {
                lblEscribiendo.setIcon(dotsIcon());
                lblEscribiendo.setText("Varios usuarios están escribiendo...");
                lblEscribiendo.setIconTextGap(6);
            }
        });
    }

    private Icon dotsIcon() {
        return new Icon() {
            public int getIconWidth()  { return 18; }
            public int getIconHeight() { return 10; }
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int r = 2;
                g2.setColor(Theme.primary());
                g2.fillOval(x + 2, y + 4 - r, r * 2, r * 2);
                g2.fillOval(x + 7, y + 4 - r, r * 2, r * 2);
                g2.fillOval(x + 12, y + 4 - r, r * 2, r * 2);
                g2.dispose();
            }
        };
    }

    /* ============================ Util ============================ */

    private void desplazarScrollAlFinal() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollLectura.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private JButton iconButton(Icon icon, String tooltip) {
        JButton b = new JButton(icon);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setBorder(null);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setIconTextGap(0);
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setVerticalTextPosition(SwingConstants.CENTER);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(40, 40));
        b.setMinimumSize(new Dimension(32, 32));
        if (tooltip != null) b.setToolTipText(tooltip);
        return b;
    }

    /** Punto indicador (verde / rojo) estilo Stitch. */
    private static class DotStatus extends JPanel {
        private final Color color;
        DotStatus(Color c) {
            setOpaque(false);
            setPreferredSize(new Dimension(8, 8));
            this.color = c;
        }
        @Override protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    /** Helper: devuelve la ventana Swing que contiene al componente. */
    private static java.awt.Window ancestor(java.awt.Component c) { return SwingUtilities.getWindowAncestor(c); }

    /* ============================ API pública setters/getters ============================ */

    public void setAccionEscribiendo(java.util.function.Consumer<Boolean> a) { this.accionEscribiendo = a; }
    public void setOnImagenSeleccionada(java.util.function.Consumer<File> a) { this.accionEnviarImagen = a; }
    public void setOnArchivoSeleccionado(java.util.function.Consumer<File> a) { this.accionEnviarArchivo = a; }
    public void setOnStickerSeleccionado(java.util.function.Consumer<String> a) { this.accionEnviarSticker = a; }
    public String getMensajeEscrito() { return taInputMensaje.getText(); }

    public void limpiarInput() {
        taInputMensaje.setText("");
        taInputMensaje.requestFocus();
        if (jbtnEnviar != null) jbtnEnviar.repaint();
        if (isTyping) {
            isTyping = false;
            timerEscribiendo.stop();
            if (accionEscribiendo != null) accionEscribiendo.accept(false);
        }
    }

    private boolean accionEnviarAsignada = false;

    /**
     * Instala un MouseAdapter en un panel: cuando se hace click en cualquier zona que
     * no sea un botón o el propio textarea, devuelve el foco al textarea. Esto blinda
     * el caso típico en el que el usuario teclea sin tener foco y el botón enviar no
     * se entera.
     */
    private void instalarFocoClickEnArea(java.awt.Container area) {
        area.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                java.awt.Component src = e.getComponent();
                if (src == null) return;
                java.awt.Component deepest = javax.swing.SwingUtilities.getDeepestComponentAt(area, e.getX(), e.getY());
                if (deepest instanceof javax.swing.JButton)                  return;
                if (deepest instanceof javax.swing.JToggleButton)            return;
                if (deepest instanceof javax.swing.JCheckBox)               return;
                if (deepest instanceof javax.swing.JTextField)               return;
                if (deepest instanceof javax.swing.JPasswordField)           return;
                if (deepest instanceof javax.swing.JComboBox)                return;
                if (deepest == taInputMensaje)                                return;
                SwingUtilities.invokeLater(() -> {
                    if (taInputMensaje != null && taInputMensaje.isShowing()) {
                        taInputMensaje.setCaretPosition(taInputMensaje.getDocument().getLength());
                        taInputMensaje.requestFocusInWindow();
                    }
                });
            }
        });
    }

    /** Lista de listeners de envío. Idempotente: registrar el MISMO listener dos veces no duplica. */
    private final java.util.List<ActionListener> enviarListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    /**
     * Registra el listener que será invocado cuando el usuario haga click en el botón
     * enviar o presione ENTER con texto. Idempotente.
     */
    public void setAccionEnviar(ActionListener a) {
        if (a != null && !enviarListeners.contains(a)) enviarListeners.add(a);
    }

    /** Disparado por click del botón y por ENTER. Si hay texto, invoca los listeners. */
    private void dispararEnvio() {
        if (taInputMensaje == null) return;
        String texto = taInputMensaje.getText();
        if (texto.trim().isEmpty()) return;
        if (enviarListeners.isEmpty()) {
            System.err.println("[CHAT] click detectado pero NO hay listener de envío registrado.");
            return;
        }
        ActionEvent ev = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "enviar");
        for (ActionListener al : enviarListeners) al.actionPerformed(ev);
        System.out.println("[CHAT] enviar disparado, texto='" + texto + "', listeners=" + enviarListeners.size());
    }

    /** Repinta el botón según el contenido del textarea. El botón siempre está enabled;
     *  el color del fondo cambia para dar feedback visual de "hay texto". */
    private void actualizarEstadoEnvio() {
        if (jbtnEnviar != null) jbtnEnviar.repaint();
    }

    /** Cuando el panel se conecta al árbol Swing, agenda focus al textarea. */
    @Override public void addNotify() {
        super.addNotify();
        // Belt-and-suspenders: el foco se intenta en múltiples eventos.
        // Algunos L&F o layouts no procesan requestFocusInWindow durante addNotify.
        SwingUtilities.invokeLater(this::solicitarFocusTextarea);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    taInputMensaje.setCaretPosition(taInputMensaje.getDocument().getLength());
                    taInputMensaje.requestFocusInWindow();
                });
            }
        });
    }

    /** Pide el foco de forma robusta (varios intentos). */
    private void solicitarFocusTextarea() {
        if (taInputMensaje == null || !taInputMensaje.isShowing()) return;
        try {
            taInputMensaje.setCaretPosition(taInputMensaje.getDocument().getLength());
            boolean got = taInputMensaje.requestFocusInWindow();
            if (!got) {
                // Reintento asincrónico adicional
                SwingUtilities.invokeLater(() -> {
                    taInputMensaje.setCaretPosition(taInputMensaje.getDocument().getLength());
                    taInputMensaje.requestFocusInWindow();
                });
            }
        } catch (Exception ignored) {}
    }

    /** Devuelve si el panel está embebido en una ventana activa. */
    public boolean isActive() {
        java.awt.Window w = SwingUtilities.getWindowAncestor(this);
        return w != null && w.isActive();
    }

    public String getContactoDestino() { return contactoDestino; }
    public boolean esGrupo() { return esGrupo; }
}
