package Views;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.View;

public class BurbujaMensaje extends JPanel {

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

        JEditorPane areaTexto = new JEditorPane() {
            @Override
            public Dimension getPreferredSize() {
                int maxAncho = 350;
                View view = getUI().getRootView(this);
                view.setSize(maxAncho, Integer.MAX_VALUE);
                float w = view.getPreferredSpan(View.X_AXIS);
                float h = view.getPreferredSpan(View.Y_AXIS);

                int anchoFinal = (int) Math.min(maxAncho, w + 30);
                anchoFinal = Math.max(anchoFinal, 120);

                return new Dimension(anchoFinal, (int) h + 10);
            }
        };

        areaTexto.setContentType("text/html");
        areaTexto.setEditable(false);
        areaTexto.setOpaque(false);
        areaTexto.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        areaTexto.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));

        String colorLink = esMio ? "#93C5FD" : "#2563EB";
        String colorTexto = esMio ? "#FFFFFF" : "#0F172A";

        String textoHtml = procesarEnlaces(texto, colorLink);
        areaTexto.setText("<html><body style='color: " + colorTexto + "; margin: 0; padding: 0;'>" + textoHtml + "</body></html>");

        areaTexto.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        contenedorTexto.add(areaTexto, BorderLayout.CENTER);

        JLabel lblHora = new JLabel(obtenerHoraActual());
        lblHora.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblHora.setForeground(esMio ? new Color(191, 219, 254) : new Color(148, 163, 184));
        lblHora.setBorder(new EmptyBorder(0, 10, 0, 0));

        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panelSur.setOpaque(false);
        panelSur.add(lblHora);

        contenedorTexto.add(panelSur, BorderLayout.SOUTH);

        inicializarBurbuja(contenedorTexto, esMio, 6, 10);
    }

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

        JLabel lblHora = new JLabel(obtenerHoraActual());
        lblHora.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblHora.setForeground(esMio ? new Color(191, 219, 254) : new Color(148, 163, 184));
        lblHora.setBorder(new EmptyBorder(4, 10, 0, 0));

        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panelSur.setOpaque(false);
        panelSur.add(lblHora);

        contenedorImagen.add(panelSur, BorderLayout.SOUTH);

        inicializarBurbuja(contenedorImagen, esMio, 4, 4);
    }

    private String obtenerHoraActual() {
        LocalTime hora = LocalTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("h:mm a");
        return hora.format(dtf).toLowerCase().replace("am", "a.m.").replace("pm", "p.m.");
    }

    private String procesarEnlaces(String texto, String colorLink) {
        texto = texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");

        String regex = "(https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher m = p.matcher(texto);
        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String url = m.group(1);
            String urlVisible = url.replaceAll("(.{35})", "$1 ");
            m.appendReplacement(sb, "<a href=\"" + url + "\" style=\"color: " + colorLink + "; text-decoration: none;\">" + urlVisible + "</a>");
        }
        m.appendTail(sb);
        return sb.toString();
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
