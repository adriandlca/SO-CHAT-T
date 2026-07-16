package Views;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class BurbujaMensaje extends JPanel {

    // CONSTRUCTOR PARA TEXTO
    public BurbujaMensaje(String remitente, String texto, Color colorRemitente, boolean esMio) {
        this.setOpaque(false);
        this.setLayout(new BorderLayout());
        this.setBorder(new EmptyBorder(2, 2, 2, 2));

        JPanel contenedorTexto = new JPanel(new BorderLayout());
        contenedorTexto.setOpaque(false);

        if (!esMio) {
            JLabel lblNombre = new JLabel(remitente);
            lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblNombre.setForeground(colorRemitente);
            lblNombre.setBorder(new EmptyBorder(0, 0, 2, 0)); 
            contenedorTexto.add(lblNombre, BorderLayout.NORTH);
        }

        JTextArea areaTexto = new JTextArea(texto);
        areaTexto.setEditable(false);
        areaTexto.setOpaque(false);
        areaTexto.setLineWrap(true);
        areaTexto.setWrapStyleWord(true);
        areaTexto.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        areaTexto.setForeground(esMio ? Color.WHITE : new Color(15, 23, 42));
        areaTexto.setMargin(new Insets(0, 0, 0, 0)); // Evita desalineación

        contenedorTexto.add(areaTexto, BorderLayout.CENTER);

        inicializarBurbuja(contenedorTexto, esMio, 6, 10);
    }

    // CONSTRUCTOR PARA IMÁGENES
    public BurbujaMensaje(String remitente, ImageIcon iconoOriginal, Color colorRemitente, boolean esMio) {
        this.setOpaque(false);
        this.setLayout(new BorderLayout());
        this.setBorder(new EmptyBorder(2, 2, 2, 2));

        JPanel contenedorImagen = new JPanel(new BorderLayout());
        contenedorImagen.setOpaque(false);

        if (!esMio) {
            JLabel lblNombre = new JLabel(remitente);
            lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblNombre.setForeground(colorRemitente);
            lblNombre.setBorder(new EmptyBorder(0, 0, 4, 0)); 
            contenedorImagen.add(lblNombre, BorderLayout.NORTH);
        }

        ImageIcon iconoRedimensionado = redimensionarImagen(iconoOriginal, 250, 250);
        JLabel lblImagen = new JLabel(iconoRedimensionado);
        contenedorImagen.add(lblImagen, BorderLayout.CENTER);

        inicializarBurbuja(contenedorImagen, esMio, 4, 4);
    }

    private void inicializarBurbuja(JPanel contenido, boolean esMio, int padV, int padH) {
        JPanel panelBurbuja = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(esMio ? new Color(37, 99, 235) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
            }
        };
        
        panelBurbuja.setOpaque(false);
        panelBurbuja.setBorder(new EmptyBorder(padV, padH, padV, padH)); 
        panelBurbuja.add(contenido, BorderLayout.CENTER);

        JPanel alineador = new JPanel(new FlowLayout(esMio ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        alineador.setOpaque(false);
        alineador.add(panelBurbuja);

        this.add(alineador, BorderLayout.CENTER);
    }

    private ImageIcon redimensionarImagen(ImageIcon icono, int maxAncho, int maxAlto) {
        Image img = icono.getImage();
        int anchoOriginal = img.getWidth(null);
        int altoOriginal = img.getHeight(null);
        
        if (anchoOriginal <= 0 || altoOriginal <= 0) return icono;

        double ratioAncho = (double) maxAncho / anchoOriginal;
        double ratioAlto = (double) maxAlto / altoOriginal;
        double ratio = Math.min(ratioAncho, ratioAlto);

        if (ratio < 1.0) {
            int nuevoAncho = (int) (anchoOriginal * ratio);
            int nuevoAlto = (int) (altoOriginal * ratio);
            Image imgEscalada = img.getScaledInstance(nuevoAncho, nuevoAlto, Image.SCALE_SMOOTH);
            return new ImageIcon(imgEscalada);
        }
        return icono;
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}