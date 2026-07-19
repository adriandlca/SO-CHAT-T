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
import java.awt.GridLayout;
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
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
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
 * Rediseño de la ventana de chat con el sistema Stitch:
 * - Header 64px con back, título y subtítulo dinámico.
 * - Stream central con day separators ("Hoy", "Ayer", fecha).
 * - Composer con iconos emoji / imagen / archivo / sticker + textarea + send.
 * - Panel lateral de búsqueda overlay (anchura 320) con resaltado.
 *
 * API pública intacta: mismas firmas que la versión anterior.
 */
public class VentanaChat extends JFrame {

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

    public VentanaChat(String miUsuario, String contactoDestino, boolean esGrupo) {
        super("Chat: " + contactoDestino);
        this.miUsuario = miUsuario;
        this.contactoDestino = contactoDestino;
        this.esGrupo = esGrupo;
        this.setLayout(new BorderLayout());
        this.getContentPane().setBackground(Theme.surfaceLow());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.surfaceLow());
        root.add(crearHeader(), BorderLayout.NORTH);

        JPanel lectura = new JPanel(new BorderLayout());
        lectura.setBackground(Theme.surfaceLow());
        lectura.setBorder(new EmptyBorder(8, 8, 8, 8));

        panelContenedorMensajes = new JPanel();
        panelContenedorMensajes.setLayout(new BoxLayout(panelContenedorMensajes, BoxLayout.Y_AXIS));
        panelContenedorMensajes.setBackground(Theme.surfaceLow());

        scrollLectura = new JScrollPane(panelContenedorMensajes) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.surfaceLowest());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        scrollLectura.setOpaque(false);
        scrollLectura.getViewport().setOpaque(false);
        scrollLectura.setBorder(new RoundBorder(Theme.RADIUS_LG, Theme.outlineVariant()));
        scrollLectura.getVerticalScrollBar().setUnitIncrement(16);
        lectura.add(scrollLectura, BorderLayout.CENTER);

        root.add(lectura, BorderLayout.CENTER);
        root.add(crearSouthZone(), BorderLayout.SOUTH);

        this.add(root, BorderLayout.CENTER);

        // Panel de búsqueda (overlay derecho; inicialmente oculto)
        panelBusqueda = crearPanelBusqueda();
        panelBusqueda.setVisible(false);
        this.add(panelBusqueda, BorderLayout.EAST);

        this.setSize(520, 640);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(null);
    }

    /* ============================ HEADER ============================ */

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
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
        header.setBorder(new EmptyBorder(10, 8, 10, 8));

        // Left side (back + title)
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);

        JButton btnBack = iconButton(MaterialGlyph.arrowBack(22, Theme.onSurfaceVariant()), "Volver");
        btnBack.setPreferredSize(new Dimension(36, 36));
        btnBack.addActionListener(e -> dispose());
        left.add(btnBack);

        JLabel lblTitulo = new JLabel("# " + contactoDestino);
        lblTitulo.setFont(Theme.font(16, Font.BOLD));
        lblTitulo.setForeground(Theme.onSurface());
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel(esGrupo ? "Grupo · múltiples miembros" : "Conversación privada");
        lblSub.setFont(Theme.font(11, Font.PLAIN));
        lblSub.setForeground(Theme.onSurfaceVariant());
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel subWrap = new JPanel();
        subWrap.setOpaque(false);
        subWrap.setLayout(new BoxLayout(subWrap, BoxLayout.Y_AXIS));
        subWrap.add(lblTitulo);
        subWrap.add(Box.createVerticalStrut(1));
        subWrap.add(lblSub);
        left.add(subWrap);

        header.add(left, BorderLayout.WEST);

        // Right actions: solo buscador. El ⋮ que tenía no hacía nada.
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
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
        if (show) {
            this.setSize(820, 640);
        } else {
            this.setSize(520, 640);
        }
        this.revalidate();
    }

    /* ============================ SOUTH (composer + typing) ============================ */

    private JPanel crearSouthZone() {
        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.setBackground(Theme.background());
        south.setBorder(new EmptyBorder(0, 12, 12, 12));

        // Typing indicator
        lblEscribiendo = new JLabel(" ");
        lblEscribiendo.setFont(Theme.font(12, Font.ITALIC));
        lblEscribiendo.setForeground(Theme.onSurfaceVariant());
        lblEscribiendo.setBorder(new EmptyBorder(0, 8, 2, 0));
        south.add(lblEscribiendo, BorderLayout.NORTH);

        // === Composer bar: UNA SOLA row en BoxLayout(X_AXIS) ===
        //   Orden: [textarea (grow) | 4 botones (40×40) | enviar (40×40)]
        JPanel composerBar = new JPanel() {
            @Override public boolean isOpaque() { return false; }
        };
        composerBar.setLayout(new BoxLayout(composerBar, BoxLayout.X_AXIS));
        composerBar.setBackground(Theme.surfaceLowest());
        composerBar.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(Theme.RADIUS_XL, Theme.outlineVariant()),
                new EmptyBorder(4, 12, 4, 6)));

        Dimension btnSize = new Dimension(40, 40);

        // --- 1) Textarea (grow) ---
        taInputMensaje = new JTextArea(1, 20);
        taInputMensaje.setFont(Theme.fontEmoji(15, Font.PLAIN));
        taInputMensaje.setLineWrap(true);
        taInputMensaje.setWrapStyleWord(true);
        taInputMensaje.setForeground(Theme.onSurface());
        taInputMensaje.setBackground(Theme.surfaceLowest());
        taInputMensaje.setCaretColor(Theme.primary());
        taInputMensaje.setBorder(new EmptyBorder(4, 4, 4, 4));
        taInputMensaje.setOpaque(false);
        taInputMensaje.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JScrollPane inputScroll = new JScrollPane(taInputMensaje);
        inputScroll.setOpaque(false);
        inputScroll.getViewport().setOpaque(false);
        inputScroll.setBorder(null);
        inputScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        inputScroll.getVerticalScrollBar().setUnitIncrement(14);

        composerBar.add(inputScroll);
        composerBar.add(Box.createHorizontalGlue()); // Hace que textarea crezca

        // --- 2) Toolbar con 4 botones (entre texto y enviar) ---
        JPanel toolbar = new JPanel() {
            @Override public boolean isOpaque() { return false; }
        };
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setBackground(Theme.surfaceLowest());

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
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && accionEnviarImagen != null) {
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
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && accionEnviarArchivo != null) {
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

        // Glue pequeño entre toolbar y enviar (separador 4px)
        composerBar.add(Box.createHorizontalStrut(4));

        // --- 3) Botón enviar ---
        jbtnEnviar = new JButton(MaterialGlyph.sendUp(20, Color.WHITE)) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean enabled = jbtnEnviar.isEnabled();
                g2.setColor(enabled ? Theme.primary() : Theme.outlineVariant());
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isContentAreaFilled() { return false; }
            @Override public boolean isOpaque() { return false; }
        };
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
        jbtnEnviar.setEnabled(false);
        composerBar.add(jbtnEnviar);

        south.add(composerBar, BorderLayout.CENTER);

        // Habilitar send cuando hay texto
        taInputMensaje.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { jbtnEnviar.setEnabled(taInputMensaje.getText().trim().length() > 0); }
            public void removeUpdate(DocumentEvent e) { jbtnEnviar.setEnabled(taInputMensaje.getText().trim().length() > 0); }
            public void changedUpdate(DocumentEvent e) { jbtnEnviar.setEnabled(taInputMensaje.getText().trim().length() > 0); }
        });

        // ENTER envía, SHIFT+ENTER salto de línea
        taInputMensaje.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enviar");
        taInputMensaje.getActionMap().put("enviar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (jbtnEnviar.isEnabled()) jbtnEnviar.doClick();
            }
        });
        taInputMensaje.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), "salto");
        taInputMensaje.getActionMap().put("salto", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { taInputMensaje.append("\n"); }
        });

        configurarDetectorEscritura();
        return south;
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
        p.setBorder(new EmptyBorder(0, 0, 0, 0));

        // Header
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBackground(Theme.surfaceLowest());
        head.setBorder(new EmptyBorder(16, 16, 8, 16));
        JLabel lbl = new JLabel("Buscar mensajes");
        lbl.setFont(Theme.font(15, Font.BOLD));
        lbl.setForeground(Theme.onSurface());
        head.add(lbl, BorderLayout.WEST);
        JButton btnCerrar = iconButton(MaterialGlyph.close(18, Theme.onSurfaceVariant()), "Cerrar");
        btnCerrar.addActionListener(e -> togglePanelBusqueda());
        head.add(btnCerrar, BorderLayout.EAST);
        p.add(head, BorderLayout.NORTH);

        // Input buscar
        JPanel inputRow = new JPanel(new BorderLayout()) {
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

        // Resultados
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
        JPanel grid = new JPanel(new GridLayout(3, 4, 4, 4));
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
        JPanel grid = new JPanel(new GridLayout(filas, cols, 6, 6));
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

    /* ============================ Mostrar mensajes (API pública) ============================ */

    public void mostrarMensajeConColor(String remitente, String mensaje, Color colorRemitente) {
        String nombreLimpio = remitente.replace("[", "").replace("]: ", "").replace("Tú: ", "Tú");
        boolean esMio = nombreLimpio.equals("Tú");
        BurbujaMensaje b = new BurbujaMensaje(nombreLimpio, mensaje.trim(), colorRemitente, esMio);
        panelContenedorMensajes.add(b);
        panelContenedorMensajes.revalidate();
        panelContenedorMensajes.repaint();
        desplazarScrollAlFinal();
    }

    public void mostrarImagenConColor(String remitente, ImageIcon imagen, Color colorRemitente) {
        String nombreLimpio = remitente.replace("[", "").replace("]: ", "").replace("Tú: ", "Tú");
        boolean esMio = nombreLimpio.equals("Tú");
        BurbujaMensaje b = new BurbujaMensaje(nombreLimpio, imagen, colorRemitente, esMio);
        panelContenedorMensajes.add(b);
        panelContenedorMensajes.revalidate();
        panelContenedorMensajes.repaint();
        desplazarScrollAlFinal();
    }

    public void mostrarStickerConColor(String remitente, ImageIcon sticker, Color colorRemitente) {
        String nombreLimpio = remitente.replace("[", "").replace("]: ", "").replace("Tú: ", "Tú");
        boolean esMio = nombreLimpio.equals("Tú");

        JPanel alin = new JPanel(new FlowLayout(esMio ? FlowLayout.RIGHT : FlowLayout.LEFT));
        alin.setOpaque(false);
        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(4, 10, 4, 10));

        JLabel lblNombre = new JLabel(nombreLimpio);
        lblNombre.setFont(Theme.font(12, Font.BOLD));
        lblNombre.setForeground(colorRemitente);
        lblNombre.setAlignmentX(esMio ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        wrap.add(lblNombre);
        wrap.add(Box.createVerticalStrut(2));

        JLabel lblSticker = new JLabel(sticker);
        lblSticker.setAlignmentX(esMio ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        wrap.add(lblSticker);

        alin.add(wrap);
        panelContenedorMensajes.add(alin);
        panelContenedorMensajes.revalidate();
        panelContenedorMensajes.repaint();
        desplazarScrollAlFinal();
    }

    public void mostrarBotonDescargaArchivo(String remitente, String nombreArchivo, String base64, Color colorRemitente) {
        String nombreLimpio = remitente.replace("[", "").replace("]: ", "").replace("Tú: ", "Tú");
        boolean esMio = nombreLimpio.equals("Tú");

        JPanel alin = new JPanel(new FlowLayout(esMio ? FlowLayout.RIGHT : FlowLayout.LEFT));
        alin.setOpaque(false);

        JPanel caja = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.surfaceLowest());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_XL, Theme.RADIUS_XL);
                g2.setColor(Theme.outlineVariant());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.RADIUS_XL, Theme.RADIUS_XL);
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

        JButton btnDescargar = new GhostButton("Descargar", MaterialGlyph.download(16, Theme.primary()), 999);
        btnDescargar.setFont(Theme.font(12, Font.BOLD));
        btnDescargar.setForeground(Theme.primary());
        btnDescargar.setBackground(Theme.surfaceLow());
        btnDescargar.setOpaque(true);
        btnDescargar.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Guardar archivo como...");
            fc.setSelectedFile(new File(nombreArchivo));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File destino = fc.getSelectedFile();
                if (ManejadorArchivos.guardarArchivoManual(base64, destino)) {
                    JOptionPane.showMessageDialog(this,
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
        panelContenedorMensajes.add(alin);
        panelContenedorMensajes.revalidate();
        panelContenedorMensajes.repaint();
        desplazarScrollAlFinal();
    }

    public void mostrarMensajePlano(String textoAnterior) {
        JLabel lbl = new JLabel(textoAnterior);
        lbl.setFont(Theme.font(12, Font.ITALIC));
        lbl.setForeground(Theme.onSurfaceVariant());
        JPanel alin = new JPanel(new FlowLayout(FlowLayout.CENTER));
        alin.setOpaque(false);
        alin.add(lbl);
        panelContenedorMensajes.add(alin);
    }

    /* ============================ Typing indicator (API pública) ============================ */

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
        return new javax.swing.Icon() {
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
        // Sin un preferredSize, JButton puede colapsar a <24px y comerse el icono.
        b.setPreferredSize(new Dimension(40, 40));
        b.setMinimumSize(new Dimension(32, 32));
        if (tooltip != null) b.setToolTipText(tooltip);
        return b;
    }

    /* ============================ API pública setters/getters ============================ */

    public void setAccionEscribiendo(java.util.function.Consumer<Boolean> a) { this.accionEscribiendo = a; }
    public void setOnImagenSeleccionada(java.util.function.Consumer<File> a) { this.accionEnviarImagen = a; }
    public void setOnArchivoSeleccionado(java.util.function.Consumer<File> a) { this.accionEnviarArchivo = a; }
    public void setOnStickerSeleccionado(java.util.function.Consumer<String> a) { this.accionEnviarSticker = a; }
    public String getMensajeEscrito() { return taInputMensaje.getText(); }

    public void limpiarInput() {
        taInputMensaje.setText("");
        taInputMensaje.requestFocus();
        jbtnEnviar.setEnabled(false);
        if (isTyping) {
            isTyping = false;
            timerEscribiendo.stop();
            if (accionEscribiendo != null) accionEscribiendo.accept(false);
        }
    }

    public void setAccionEnviar(ActionListener a) { jbtnEnviar.addActionListener(a); }
}
