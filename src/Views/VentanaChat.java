package Views;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class VentanaChat extends JFrame {

    private JTextArea taInputMensaje;
    private JButton jbtnEnviar;
    private JPanel panelContenedorMensajes;
    private JScrollPane scrollLectura;
    private JLabel lblEscribiendo;

    private Timer timerEscribiendo;
    private boolean isTyping = false;
    private java.util.function.Consumer<Boolean> accionEscribiendo;

    private Set<String> usuariosEscribiendo = new HashSet<>();

    private java.util.function.Consumer<File> accionEnviarImagen;
    private java.util.function.Consumer<File> accionEnviarArchivo;
    private java.util.function.Consumer<String> accionEnviarSticker;

    public VentanaChat(String contactoDestino) {
        super("Chat: " + contactoDestino);
        this.setLayout(new BorderLayout());
        this.getContentPane().setBackground(new Color(241, 245, 249));

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

        this.add(panelLectura, BorderLayout.CENTER);

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

        JPanel panelInputYEstado = new JPanel(new BorderLayout());
        panelInputYEstado.setOpaque(false);
        panelInputYEstado.add(lblEscribiendo, BorderLayout.NORTH);
        panelInputYEstado.add(scrollInput, BorderLayout.CENTER);

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

        JPanel panelFilaMensaje = new JPanel(new BorderLayout(10, 0));
        panelFilaMensaje.setBackground(new Color(241, 245, 249));
        panelFilaMensaje.setBorder(new EmptyBorder(0, 15, 15, 15));

        panelFilaMensaje.add(panelInputYEstado, BorderLayout.CENTER);
        panelFilaMensaje.add(panelBotones, BorderLayout.EAST);

        this.add(panelFilaMensaje, BorderLayout.SOUTH);
        this.setSize(480, 520);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(null);
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
