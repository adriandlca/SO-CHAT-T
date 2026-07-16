package Views;

import Controllers.HistorialChat;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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
        this.getContentPane().setBackground(new Color(241, 245, 249));

        // --- CABECERA DE CHAT ---
        JPanel panelCabecera = new JPanel(new BorderLayout());
        panelCabecera.setBackground(Color.WHITE);
        panelCabecera.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel lblTituloChat = new JLabel(contactoDestino);
        lblTituloChat.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTituloChat.setForeground(new Color(15, 23, 42));

        JButton btnActivarBusqueda = new JButton("🔍");
        btnActivarBusqueda.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        btnActivarBusqueda.setFocusable(false);
        btnActivarBusqueda.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnActivarBusqueda.setBackground(Color.WHITE);
        btnActivarBusqueda.setBorder(new EmptyBorder(5, 10, 5, 10));

        panelCabecera.add(lblTituloChat, BorderLayout.WEST);
        panelCabecera.add(btnActivarBusqueda, BorderLayout.EAST);

        panelContenedorMensajes = new JPanel();
        panelContenedorMensajes.setLayout(new BoxLayout(panelContenedorMensajes, BoxLayout.Y_AXIS));
        panelContenedorMensajes.setBackground(new Color(241, 245, 249));

        JPanel panelLectura = new JPanel(new BorderLayout());
        panelLectura.setBorder(new EmptyBorder(15, 15, 10, 15));
        panelLectura.setBackground(new Color(241, 245, 249));

        scrollLectura = new JScrollPane(panelContenedorMensajes);
        scrollLectura.setBorder(new LineBorder(new Color(226, 232, 240), 1, true));
        scrollLectura.getVerticalScrollBar().setUnitIncrement(16);
        panelLectura.add(scrollLectura, BorderLayout.CENTER);

        JPanel panelChatCentral = new JPanel(new BorderLayout());
        panelChatCentral.add(panelCabecera, BorderLayout.NORTH);
        panelChatCentral.add(panelLectura, BorderLayout.CENTER);

        // --- PANEL DE BÚSQUEDA LATERAL ---
        panelBusqueda = crearPanelBusqueda();
        panelBusqueda.setVisible(false);

        btnActivarBusqueda.addActionListener(e -> {
            boolean estaVisible = panelBusqueda.isVisible();
            panelBusqueda.setVisible(!estaVisible);
            if (!estaVisible) {
                this.setSize(800, 560);
                btnActivarBusqueda.setForeground(new Color(37, 99, 235));
            } else {
                this.setSize(480, 560);
                btnActivarBusqueda.setForeground(new Color(15, 23, 42));
            }
            this.revalidate();
        });

        this.add(panelChatCentral, BorderLayout.CENTER);
        this.add(panelBusqueda, BorderLayout.EAST);

        taInputMensaje = new JTextArea(2, 20);
        taInputMensaje.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        taInputMensaje.setLineWrap(true);
        taInputMensaje.setWrapStyleWord(true);
        taInputMensaje.setBorder(new EmptyBorder(6, 8, 6, 8));

        JScrollPane scrollInput = new JScrollPane(taInputMensaje);
        scrollInput.setBorder(new LineBorder(new Color(203, 213, 225), 1, true));

        lblEscribiendo = new JLabel(" ");
        lblEscribiendo.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblEscribiendo.setForeground(new Color(100, 116, 139));
        lblEscribiendo.setBorder(new EmptyBorder(0, 5, 2, 0));

        configurarDetectorEscritura();

        JButton btnAbrirEmojis = new JButton("😀");
        btnAbrirEmojis.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        btnAbrirEmojis.setBackground(Color.WHITE);
        btnAbrirEmojis.setBorder(new EmptyBorder(4, 8, 4, 4));
        btnAbrirEmojis.setFocusable(false);
        btnAbrirEmojis.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton btnAdjuntarImagen = new JButton(new FotoIcon());
        btnAdjuntarImagen.setToolTipText("Adjuntar Imagen");
        btnAdjuntarImagen.setBackground(Color.WHITE);
        btnAdjuntarImagen.setBorder(new EmptyBorder(4, 6, 4, 6));
        btnAdjuntarImagen.setFocusable(false);
        btnAdjuntarImagen.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton btnAdjuntarArchivo = new JButton(new ClipIcon());
        btnAdjuntarArchivo.setToolTipText("Adjuntar Documento");
        btnAdjuntarArchivo.setBackground(Color.WHITE);
        btnAdjuntarArchivo.setBorder(new EmptyBorder(4, 4, 4, 8));
        btnAdjuntarArchivo.setFocusable(false);
        btnAdjuntarArchivo.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton btnStickers = new JButton(new StickerIcon());
        btnStickers.setToolTipText("Enviar sticker");
        btnStickers.setBackground(Color.WHITE);
        btnStickers.setBorder(new EmptyBorder(4, 4, 4, 4));
        btnStickers.setFocusable(false);
        btnStickers.setCursor(new Cursor(Cursor.HAND_CURSOR));

        jbtnEnviar = new JButton("Enviar");
        jbtnEnviar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        jbtnEnviar.setForeground(Color.WHITE);
        jbtnEnviar.setBackground(new Color(37, 99, 235));
        jbtnEnviar.setBorder(new EmptyBorder(0, 18, 0, 18));
        jbtnEnviar.setFocusable(false);
        jbtnEnviar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        jbtnEnviar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { jbtnEnviar.setBackground(new Color(29, 78, 216)); }
            public void mouseExited(java.awt.event.MouseEvent evt) { jbtnEnviar.setBackground(new Color(37, 99, 235)); }
        });

        taInputMensaje.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "enviar");
        taInputMensaje.getActionMap().put("enviar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { jbtnEnviar.doClick(); }
        });
        taInputMensaje.getInputMap().put(KeyStroke.getKeyStroke("shift ENTER"), "saltoLinea");
        taInputMensaje.getActionMap().put("saltoLinea", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { taInputMensaje.append("\n"); }
        });

        btnAbrirEmojis.addActionListener(e -> {
            JPopupMenu menu = crearPanelEmojis();
            menu.show(btnAbrirEmojis, 0, -menu.getPreferredSize().height - 5);
        });

        btnAdjuntarImagen.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Selecciona una Imagen para enviar");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Imágenes (JPG, PNG)", "jpg", "jpeg", "png"
            ));
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                if (accionEnviarImagen != null) accionEnviarImagen.accept(fileChooser.getSelectedFile());
            }
        });

        btnAdjuntarArchivo.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Selecciona un Documento para enviar");
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                if (accionEnviarArchivo != null) accionEnviarArchivo.accept(fileChooser.getSelectedFile());
            }
        });

        btnStickers.addActionListener(e -> {
            JPopupMenu menu = crearPanelStickers();
            menu.show(btnStickers, 0, -menu.getPreferredSize().height - 5);
        });

        JPanel panelBotones = new JPanel(new BorderLayout(10, 0));
        panelBotones.setBackground(new Color(241, 245, 249));

        JPanel panelHerramientas = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panelHerramientas.setBackground(Color.WHITE);
        panelHerramientas.add(btnAbrirEmojis);
        panelHerramientas.add(btnAdjuntarImagen);
        panelHerramientas.add(btnStickers);
        panelHerramientas.add(btnAdjuntarArchivo);

        JPanel wrapperAuxiliares = new JPanel(new BorderLayout());
        wrapperAuxiliares.setBackground(Color.WHITE);
        wrapperAuxiliares.setBorder(new LineBorder(new Color(203, 213, 225), 1, true));
        wrapperAuxiliares.add(panelHerramientas, BorderLayout.CENTER);

        panelBotones.add(wrapperAuxiliares, BorderLayout.WEST);
        panelBotones.add(jbtnEnviar, BorderLayout.CENTER);

        JPanel filaInput = new JPanel(new BorderLayout(10, 0));
        filaInput.setOpaque(false);
        filaInput.add(scrollInput, BorderLayout.CENTER);
        filaInput.add(panelBotones, BorderLayout.EAST);

        JPanel panelFilaMensaje = new JPanel(new BorderLayout(0, 0));
        panelFilaMensaje.setBackground(new Color(241, 245, 249));
        panelFilaMensaje.setBorder(new EmptyBorder(0, 15, 15, 15));

        panelFilaMensaje.add(lblEscribiendo, BorderLayout.NORTH);
        panelFilaMensaje.add(filaInput, BorderLayout.CENTER);

        this.add(panelFilaMensaje, BorderLayout.SOUTH);
        this.setSize(480, 560);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(null);
    }

    private JPanel crearPanelBusqueda() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(300, 0));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new LineBorder(new Color(203, 213, 225), 1, false));

        JPanel headerBusqueda = new JPanel(new BorderLayout());
        headerBusqueda.setBackground(Color.WHITE);
        headerBusqueda.setBorder(new EmptyBorder(12, 15, 12, 15));
        JLabel lblTitulo = new JLabel("Buscar mensajes");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        headerBusqueda.add(lblTitulo, BorderLayout.CENTER);

        JTextField txtBuscar = new JTextField();
        txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtBuscar.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));

        JPanel panelInput = new JPanel(new BorderLayout());
        panelInput.setBackground(Color.WHITE);
        panelInput.setBorder(new EmptyBorder(0, 15, 15, 15));
        panelInput.add(txtBuscar, BorderLayout.CENTER);

        JPanel panelTopBusqueda = new JPanel(new BorderLayout());
        panelTopBusqueda.add(headerBusqueda, BorderLayout.NORTH);
        panelTopBusqueda.add(panelInput, BorderLayout.SOUTH);

        JPanel panelResultados = new JPanel();
        panelResultados.setLayout(new BoxLayout(panelResultados, BoxLayout.Y_AXIS));
        panelResultados.setBackground(new Color(248, 250, 252));

        JScrollPane scrollBusqueda = new JScrollPane(panelResultados);
        scrollBusqueda.setBorder(null);
        scrollBusqueda.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(panelTopBusqueda, BorderLayout.NORTH);
        panel.add(scrollBusqueda, BorderLayout.CENTER);

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { ejecutarBusqueda(); }
            public void removeUpdate(DocumentEvent e) { ejecutarBusqueda(); }
            public void changedUpdate(DocumentEvent e) { ejecutarBusqueda(); }

            private void ejecutarBusqueda() {
                String filtro = txtBuscar.getText();
                panelResultados.removeAll();

                if (!filtro.trim().isEmpty()) {
                    List<String> encontrados = HistorialChat.buscarMensajes(miUsuario, contactoDestino, filtro, esGrupo);
                    for (String linea : encontrados) {
                        panelResultados.add(crearItemResultado(linea, filtro));
                        panelResultados.add(Box.createVerticalStrut(4));
                    }
                }
                panelResultados.revalidate();
                panelResultados.repaint();
            }
        });

        return panel;
    }

    private JPanel crearItemResultado(String linea, String filtro) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(Color.WHITE);
        item.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(10, 15, 10, 15)
        ));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        try {
            int idxCierre = linea.indexOf("] ");
            String fecha = linea.substring(1, 11);
            String resto = linea.substring(idxCierre + 2);
            int idxDosPuntos = resto.indexOf(": ");

            if (idxCierre == -1 || idxDosPuntos == -1) throw new Exception();

            String remitente = resto.substring(0, idxDosPuntos);
            String mensaje = resto.substring(idxDosPuntos + 2);

            JLabel lblFecha = new JLabel(fecha);
            lblFecha.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblFecha.setForeground(new Color(100, 116, 139));
            lblFecha.setBorder(new EmptyBorder(0, 0, 4, 0));

            String regex = "(?i)(" + Pattern.quote(filtro) + ")";
            String mensajeResaltado = mensaje.replaceAll(regex, "<b style='color: #16a34a;'>$1</b>");

            JLabel lblMensaje = new JLabel("<html><div style='width: 200px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;'>"
                    + "<b>" + remitente + ":</b> " + mensajeResaltado + "</div></html>");
            lblMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lblMensaje.setForeground(new Color(15, 23, 42));

            item.add(lblFecha, BorderLayout.NORTH);
            item.add(lblMensaje, BorderLayout.CENTER);

            final String textoBurbuja = mensaje;
            item.setCursor(new Cursor(Cursor.HAND_CURSOR));
            item.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    realizarScrollHaciaMensaje(textoBurbuja);
                }
                @Override
                public void mouseEntered(java.awt.event.MouseEvent evt) { item.setBackground(new Color(239, 246, 255)); }
                @Override
                public void mouseExited(java.awt.event.MouseEvent evt) { item.setBackground(Color.WHITE); }
            });

        } catch (Exception e) {
            JLabel lblFallback = new JLabel(linea);
            lblFallback.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            item.add(lblFallback, BorderLayout.CENTER);
        }

        return item;
    }

    private void realizarScrollHaciaMensaje(String textoBurbuja) {
        if (textoBurbuja == null || textoBurbuja.isEmpty()) return;
        Component[] componentes = panelContenedorMensajes.getComponents();
        for (int i = componentes.length - 1; i >= 0; i--) {
            BurbujaMensaje burbuja = encontrarBurbuja(componentes[i]);
            if (burbuja != null) {
                String textoReal = obtenerTextoBurbuja(burbuja);
                if (textoReal != null && textoReal.contains(textoBurbuja)) {
                    scrollHaciaBurbuja(burbuja);
                    return;
                }
            }
        }
    }

    private BurbujaMensaje encontrarBurbuja(Component comp) {
        if (comp instanceof BurbujaMensaje) return (BurbujaMensaje) comp;
        if (comp instanceof Container) {
            for (Component c : ((Container) comp).getComponents()) {
                BurbujaMensaje b = encontrarBurbuja(c);
                if (b != null) return b;
            }
        }
        return null;
    }

    private String obtenerTextoBurbuja(Component comp) {
        if (comp instanceof JEditorPane) {
            try {
                javax.swing.text.Document doc = ((JEditorPane) comp).getDocument();
                return doc.getText(0, doc.getLength()).replace("\u200B", "");
            } catch (Exception ex) {
                return null;
            }
        }
        if (comp instanceof Container) {
            for (Component c : ((Container) comp).getComponents()) {
                String t = obtenerTextoBurbuja(c);
                if (t != null && !t.trim().isEmpty()) return t;
            }
        }
        return null;
    }

    private void scrollHaciaBurbuja(BurbujaMensaje burbuja) {
        SwingUtilities.invokeLater(() -> {
            Container padre = burbuja.getParent();
            if (padre == null) return;
            Point pt = SwingUtilities.convertPoint(padre, burbuja.getLocation(), panelContenedorMensajes);
            JScrollBar vertical = scrollLectura.getVerticalScrollBar();
            int destino = Math.max(0, pt.y - 20);
            vertical.setValue(destino);
        });
    }

    private void configurarDetectorEscritura() {
        timerEscribiendo = new Timer(1500, e -> {
            if (isTyping) {
                isTyping = false;
                if (accionEscribiendo != null) accionEscribiendo.accept(false);
            }
        });
        timerEscribiendo.setRepeats(false);

        taInputMensaje.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { avisarEscribiendo(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { avisarEscribiendo(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { avisarEscribiendo(); }
        });
    }

    private void avisarEscribiendo() {
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

    // Para Chat de 2 personas
    public void mostrarEscribiendo(boolean escribiendo) {
        SwingUtilities.invokeLater(() -> {
            lblEscribiendo.setText(escribiendo ? "escribiendo..." : " ");
        });
    }

    // Para Chat Grupal: rastrea múltiples personas escribiendo simultáneamente
    public void mostrarEscribiendoGrupal(String remitente, boolean escribiendo) {
        SwingUtilities.invokeLater(() -> {
            if (escribiendo) {
                usuariosEscribiendo.add(remitente);
            } else {
                usuariosEscribiendo.remove(remitente);
            }

            if (usuariosEscribiendo.isEmpty()) {
                lblEscribiendo.setText(" ");
            } else if (usuariosEscribiendo.size() == 1) {
                lblEscribiendo.setText(usuariosEscribiendo.iterator().next() + " está escribiendo...");
            } else {
                lblEscribiendo.setText("Varios usuarios están escribiendo...");
            }
        });
    }

    private JPopupMenu crearPanelEmojis() {
        JPopupMenu panelEmojis = new JPopupMenu();
        panelEmojis.setLayout(new GridLayout(3, 4, 2, 2));
        panelEmojis.setBackground(Color.WHITE);
        panelEmojis.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(203, 213, 225), 1),
                new EmptyBorder(5, 5, 5, 5)
        ));

        String[] emojis = {"😀", "😂", "😊", "😍", "🤔", "😥", "😡", "👍", "🙌", "🔥", "🎉", "❤️"};
        for (String emoji : emojis) {
            JButton btnEmoji = new JButton(emoji);
            btnEmoji.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
            btnEmoji.setBackground(Color.WHITE);
            btnEmoji.setBorder(new EmptyBorder(2, 2, 2, 2));
            btnEmoji.setFocusable(false);
            btnEmoji.setCursor(new Cursor(Cursor.HAND_CURSOR));

            btnEmoji.addActionListener(e -> {
                taInputMensaje.append(emoji);
                taInputMensaje.requestFocus();
                panelEmojis.setVisible(false);
            });
            panelEmojis.add(btnEmoji);
        }
        panelEmojis.setPreferredSize(new Dimension(260, 150));
        return panelEmojis;
    }

    private JPopupMenu crearPanelStickers() {
        JPopupMenu panelStickers = new JPopupMenu();
        panelStickers.setBackground(Color.WHITE);

        File carpetaStickers = new File("stickers");
        if (!carpetaStickers.exists()) {
            carpetaStickers.mkdir();
        }

        File[] archivos = carpetaStickers.listFiles((dir, name) -> {
            String nomLower = name.toLowerCase();
            return nomLower.endsWith(".png") || nomLower.endsWith(".jpg")
                    || nomLower.endsWith(".jpeg") || nomLower.endsWith(".gif");
        });

        if (archivos == null || archivos.length == 0) {
            JLabel lblInfo = new JLabel(
                    "<html><center>Carpeta <b>stickers/</b> vacía.<br>Guarda imágenes PNG o JPG allí.</center></html>",
                    SwingConstants.CENTER);
            lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblInfo.setForeground(new Color(100, 116, 139));
            lblInfo.setBorder(new EmptyBorder(15, 15, 15, 15));
            lblInfo.setBackground(Color.WHITE);
            lblInfo.setOpaque(true);
            panelStickers.add(lblInfo);
            panelStickers.setPreferredSize(new Dimension(240, 90));
            return panelStickers;
        }

        int columnas = 3;
        int filas = (int) Math.ceil((double) archivos.length / columnas);
        JPanel panelCuadricula = new JPanel(new GridLayout(filas, columnas, 6, 6));
        panelCuadricula.setBackground(Color.WHITE);
        panelCuadricula.setBorder(new EmptyBorder(6, 6, 6, 6));

        for (File archivo : archivos) {
            try {
                BufferedImage original = ImageIO.read(archivo);
                if (original == null) continue;

                BufferedImage miniatura = redimensionarBuffered(original, 55, 55);
                ImageIcon iconoSticker = new ImageIcon(miniatura);

                JButton btnSticker = new JButton(iconoSticker);
                btnSticker.setBackground(new Color(248, 250, 252));
                btnSticker.setBorder(new LineBorder(new Color(226, 232, 240), 1, true));
                btnSticker.setFocusable(false);
                btnSticker.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btnSticker.setToolTipText(archivo.getName());

                btnSticker.addActionListener(e -> {
                    BufferedImage stickerEstandar = redimensionarBuffered(original, 120, 120);
                    String base64 = bufferedToBase64(stickerEstandar);
                    if (base64 != null && accionEnviarSticker != null) {
                        accionEnviarSticker.accept(base64);
                    }
                    panelStickers.setVisible(false);
                });

                panelCuadricula.add(btnSticker);
            } catch (Exception ex) {
                System.err.println("Error al cargar sticker: " + archivo.getName());
            }
        }

        JPanel panelAlineador = new JPanel(new BorderLayout());
        panelAlineador.setBackground(Color.WHITE);
        panelAlineador.add(panelCuadricula, BorderLayout.NORTH);

        JScrollPane scrollStickers = new JScrollPane(panelAlineador);
        scrollStickers.setBorder(null);
        scrollStickers.getVerticalScrollBar().setUnitIncrement(14);
        scrollStickers.setPreferredSize(new Dimension(240, 220));

        panelStickers.add(scrollStickers);
        panelStickers.setPreferredSize(new Dimension(250, 230));
        return panelStickers;
    }

    private BufferedImage redimensionarBuffered(BufferedImage original, int anchoMax, int altoMax) {
        int anchoOriginal = original.getWidth();
        int altoOriginal = original.getHeight();
        double ratio = Math.min((double) anchoMax / anchoOriginal, (double) altoMax / altoOriginal);
        int nuevoAncho = (int) (anchoOriginal * ratio);
        int nuevoAlto = (int) (altoOriginal * ratio);

        BufferedImage redimensionada = new BufferedImage(nuevoAncho, nuevoAlto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = redimensionada.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(original, 0, 0, nuevoAncho, nuevoAlto, null);
        g.dispose();
        return redimensionada;
    }

    private String bufferedToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    public void mostrarMensajeConColor(String remitente, String mensaje, Color colorRemitente) {
        String nombreLimpio = remitente.replace("[", "").replace("]: ", "").replace("Tú: ", "Tú");
        boolean esMio = nombreLimpio.equals("Tú");

        BurbujaMensaje burbuja = new BurbujaMensaje(nombreLimpio, mensaje.trim(), colorRemitente, esMio);
        panelContenedorMensajes.add(burbuja);
        panelContenedorMensajes.revalidate();
        panelContenedorMensajes.repaint();
        desplazarScrollAlFinal();
    }

    public void mostrarImagenConColor(String remitente, ImageIcon imagen, Color colorRemitente) {
        String nombreLimpio = remitente.replace("[", "").replace("]: ", "").replace("Tú: ", "Tú");
        boolean esMio = nombreLimpio.equals("Tú");

        BurbujaMensaje burbuja = new BurbujaMensaje(nombreLimpio, imagen, colorRemitente, esMio);
        panelContenedorMensajes.add(burbuja);
        panelContenedorMensajes.revalidate();
        panelContenedorMensajes.repaint();
        desplazarScrollAlFinal();
    }

    public void mostrarStickerConColor(String remitente, ImageIcon sticker, Color colorRemitente) {
        String nombreLimpio = remitente.replace("[", "").replace("]: ", "").replace("Tú: ", "Tú");
        boolean esMio = nombreLimpio.equals("Tú");

        JPanel panelAlineador = new JPanel(new FlowLayout(esMio ? FlowLayout.RIGHT : FlowLayout.LEFT));
        panelAlineador.setOpaque(false);

        JPanel panelStickerCompleto = new JPanel();
        panelStickerCompleto.setLayout(new BoxLayout(panelStickerCompleto, BoxLayout.Y_AXIS));
        panelStickerCompleto.setOpaque(false);
        panelStickerCompleto.setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel lblNombre = new JLabel(nombreLimpio);
        lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblNombre.setForeground(colorRemitente);
        lblNombre.setAlignmentX(esMio ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        panelStickerCompleto.add(lblNombre);

        panelStickerCompleto.add(Box.createVerticalStrut(4));

        JLabel lblStickerGrafico = new JLabel(sticker);
        lblStickerGrafico.setAlignmentX(esMio ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        panelStickerCompleto.add(lblStickerGrafico);

        panelAlineador.add(panelStickerCompleto);
        panelContenedorMensajes.add(panelAlineador);
        panelContenedorMensajes.revalidate();
        panelContenedorMensajes.repaint();
        desplazarScrollAlFinal();
    }

    public void mostrarBotonDescargaArchivo(String remitente, String nombreArchivo, String base64, Color colorRemitente) {
        String nombreLimpio = remitente.replace("[", "").replace("]: ", "").replace("Tú: ", "Tú");
        boolean esMio = nombreLimpio.equals("Tú");

        JPanel panelAlineador = new JPanel(new FlowLayout(esMio ? FlowLayout.RIGHT : FlowLayout.LEFT));
        panelAlineador.setOpaque(false);

        JPanel panelCaja = new JPanel(new BorderLayout(5, 5));
        panelCaja.setBackground(Color.WHITE);
        panelCaja.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel lblNombre = new JLabel(nombreLimpio);
        lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblNombre.setForeground(colorRemitente);

        JButton btnDescargar = new JButton("⬇️ " + nombreArchivo);
        btnDescargar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnDescargar.setBackground(new Color(241, 245, 249));
        btnDescargar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDescargar.setFocusable(false);

        btnDescargar.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Guardar archivo como...");
            fileChooser.setSelectedFile(new File(nombreArchivo));

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File destino = fileChooser.getSelectedFile();
                if (ManejadorArchivos.guardarArchivoManual(base64, destino)) {
                    JOptionPane.showMessageDialog(this,
                            "Archivo guardado exitosamente en:\n" + destino.getAbsolutePath(),
                            "Descarga Completada",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        panelCaja.add(lblNombre, BorderLayout.NORTH);
        panelCaja.add(btnDescargar, BorderLayout.CENTER);
        panelAlineador.add(panelCaja);

        panelContenedorMensajes.add(panelAlineador);
        panelContenedorMensajes.revalidate();
        panelContenedorMensajes.repaint();
        desplazarScrollAlFinal();
    }

    public void mostrarMensajePlano(String textoAnterior) {
        JTextArea textoHistorial = new JTextArea(textoAnterior);
        textoHistorial.setEditable(false);
        textoHistorial.setOpaque(false);
        textoHistorial.setForeground(new Color(71, 85, 105));
        textoHistorial.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        JPanel panelAlineador = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelAlineador.setOpaque(false);
        panelAlineador.add(textoHistorial);
        panelContenedorMensajes.add(panelAlineador);
    }

    private void desplazarScrollAlFinal() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollLectura.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    public void setAccionEscribiendo(java.util.function.Consumer<Boolean> accion) { this.accionEscribiendo = accion; }
    public void setOnImagenSeleccionada(java.util.function.Consumer<File> accion) { this.accionEnviarImagen = accion; }
    public void setOnArchivoSeleccionado(java.util.function.Consumer<File> accion) { this.accionEnviarArchivo = accion; }
    public void setOnStickerSeleccionado(java.util.function.Consumer<String> accion) { this.accionEnviarSticker = accion; }
    public String getMensajeEscrito() { return taInputMensaje.getText(); }

    public void limpiarInput() {
        taInputMensaje.setText("");
        taInputMensaje.requestFocus();
        if (isTyping) {
            isTyping = false;
            timerEscribiendo.stop();
            if (accionEscribiendo != null) accionEscribiendo.accept(false);
        }
    }

    public void setAccionEnviar(ActionListener accion) { jbtnEnviar.addActionListener(accion); }

    // --- ICONOS VECTORIALES (GARANTIZAN COMPATIBILIDAD) ---
    private static class ClipIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(71, 85, 105));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.translate(x, y);
            g2.drawLine(12, 5, 12, 13);
            g2.drawArc(4, 9, 8, 8, 180, 180);
            g2.drawLine(4, 13, 4, 5);
            g2.drawArc(4, 2, 6, 6, 0, 180);
            g2.drawLine(10, 5, 10, 11);
            g2.drawArc(6, 9, 4, 4, 180, 180);
            g2.drawLine(6, 11, 6, 7);
            g2.dispose();
        }
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }
    }

    private static class FotoIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(71, 85, 105));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.translate(x, y);
            g2.drawRoundRect(2, 3, 14, 12, 2, 2);
            g2.drawOval(5, 6, 3, 3);
            g2.drawLine(2, 12, 7, 7);
            g2.drawLine(7, 7, 11, 11);
            g2.drawLine(10, 10, 12, 8);
            g2.drawLine(12, 8, 16, 12);
            g2.dispose();
        }
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }
    }

    private static class StickerIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(71, 85, 105));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.translate(x, y);
            g2.drawLine(9, 1, 9, 7);
            g2.drawLine(9, 11, 9, 17);
            g2.drawLine(1, 9, 7, 9);
            g2.drawLine(11, 9, 17, 9);
            g2.drawLine(3, 3, 6, 6);
            g2.drawLine(12, 12, 15, 15);
            g2.drawLine(15, 3, 12, 6);
            g2.drawLine(6, 12, 3, 15);
            g2.dispose();
        }
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }
    }
}
