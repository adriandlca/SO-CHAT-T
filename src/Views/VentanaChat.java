package Views;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class VentanaChat extends JFrame {

    private JTextArea taInputMensaje;
    private JButton jbtnEnviar;
    private JPanel panelContenedorMensajes;
    private JScrollPane scrollLectura;

    private java.util.function.Consumer<ImageIcon> accionEnviarImagen;

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

        JButton btnAbrirEmojis = new JButton("😀");
        btnAbrirEmojis.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        btnAbrirEmojis.setBackground(Color.WHITE);
        btnAbrirEmojis.setBorder(new EmptyBorder(4, 8, 4, 4));
        btnAbrirEmojis.setFocusable(false);
        btnAbrirEmojis.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Botón con el clip vectorial dibujado de forma nativa
        JButton btnAdjuntar = new JButton(new ClipIcon());
        btnAdjuntar.setToolTipText("Adjuntar Imagen");
        btnAdjuntar.setBackground(Color.WHITE);
        btnAdjuntar.setBorder(new EmptyBorder(4, 4, 4, 8));
        btnAdjuntar.setFocusable(false);
        btnAdjuntar.setCursor(new Cursor(Cursor.HAND_CURSOR));

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

        btnAdjuntar.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Selecciona una Imagen para enviar");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Imágenes (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif"
            ));

            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    java.io.File archivo = fileChooser.getSelectedFile();
                    // Carga síncrona real para evitar que las dimensiones de la imagen retornen -1
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(archivo);
                    if (img != null) {
                        ImageIcon imgIcon = new ImageIcon(img);
                        if (accionEnviarImagen != null) {
                            accionEnviarImagen.accept(imgIcon);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error al cargar el archivo de imagen.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JPanel panelBotones = new JPanel(new BorderLayout(10, 0));
        panelBotones.setBackground(new Color(241, 245, 249));

        JPanel panelHerramientas = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panelHerramientas.setBackground(Color.WHITE);
        panelHerramientas.add(btnAbrirEmojis);
        panelHerramientas.add(btnAdjuntar);

        JPanel wrapperAuxiliares = new JPanel(new BorderLayout());
        wrapperAuxiliares.setBackground(Color.WHITE);
        wrapperAuxiliares.setBorder(new LineBorder(new Color(203, 213, 225), 1, true));
        wrapperAuxiliares.add(panelHerramientas, BorderLayout.CENTER);

        panelBotones.add(wrapperAuxiliares, BorderLayout.WEST);
        panelBotones.add(jbtnEnviar, BorderLayout.CENTER);

        JPanel panelFilaMensaje = new JPanel(new BorderLayout(10, 0));
        panelFilaMensaje.setBackground(new Color(241, 245, 249));
        panelFilaMensaje.setBorder(new EmptyBorder(0, 15, 15, 15));
        panelFilaMensaje.add(scrollInput, BorderLayout.CENTER);
        panelFilaMensaje.add(panelBotones, BorderLayout.EAST);

        this.add(panelFilaMensaje, BorderLayout.SOUTH);

        this.setSize(460, 520);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(null);
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
            btnEmoji.setHorizontalAlignment(SwingConstants.CENTER);
            btnEmoji.setMargin(new Insets(0, 0, 0, 0));

            btnEmoji.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) { btnEmoji.setBackground(new Color(241, 245, 249)); }
                public void mouseExited(java.awt.event.MouseEvent evt) { btnEmoji.setBackground(Color.WHITE); }
            });

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

    private void desplazarScrollAlFinal() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollLectura.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
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

    public void setOnImagenSeleccionada(java.util.function.Consumer<ImageIcon> accion) {
        this.accionEnviarImagen = accion;
    }

    public String getMensajeEscrito() { return taInputMensaje.getText(); }
    public void limpiarInput() { taInputMensaje.setText(""); taInputMensaje.requestFocus(); }
    public void setAccionEnviar(ActionListener accion) { jbtnEnviar.addActionListener(accion); }

    // CLASE INTERNA: Dibuja un Clip elegante de forma vectorial (Evita depender de emojis del OS)
    private static class ClipIcon implements Icon {
        private final int width = 18;
        private final int height = 18;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(71, 85, 105)); // Gris slate oscuro y elegante
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            g2.translate(x, y);

            // Trazado del clip
            g2.drawLine(12, 5, 12, 13);
            g2.drawArc(4, 9, 8, 8, 180, 180);
            g2.drawLine(4, 13, 4, 5);
            g2.drawArc(4, 2, 6, 6, 0, 180);
            g2.drawLine(10, 5, 10, 11);
            g2.drawArc(6, 9, 4, 4, 180, 180);
            g2.drawLine(6, 11, 6, 7);

            g2.dispose();
        }

        @Override
        public int getIconWidth() { return width; }
        @Override
        public int getIconHeight() { return height; }
    }
}