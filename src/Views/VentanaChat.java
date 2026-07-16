package Views;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
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

        JPanel panelBotones = new JPanel(new BorderLayout(10, 0));
        panelBotones.setBackground(new Color(241, 245, 249));

        JPanel panelHerramientas = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panelHerramientas.setBackground(Color.WHITE);
        panelHerramientas.add(btnAbrirEmojis);
        panelHerramientas.add(btnAdjuntarImagen);
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
}
