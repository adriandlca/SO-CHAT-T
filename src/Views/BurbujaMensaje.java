package Views;

import Views.theme.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.View;

/**
 * Burbuja de mensaje rediseñada con el sistema Stitch (estilo pill Material 3):
 * - Esquinas totalmente redondeadas (radio 22) en todas las direcciones.
 * - Sombra de una sola capa (sin escalonado).
 * - Burbujas propias en accent container con texto blanco.
 * - Burbujas de terceros en blanco con borde sutil.
 * - Tipografía Inter (con fallback Segoe UI), timestamps en mono.
 *
 * API intacta: 2 constructores con las mismas firmas que la versión anterior.
 */
public class BurbujaMensaje extends JPanel {

    public BurbujaMensaje(String remitente, String texto, Color colorRemitente, boolean esMio) {
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(Theme.BUBBLE_GAP / 2, 8, Theme.BUBBLE_GAP / 2, 8));

        JPanel contenedorTexto = new JPanel(new BorderLayout()) {
            @Override public boolean isOpaque() { return false; }
        };

        if (!esMio) {
            JLabel lblNombre = new JLabel(remitente);
            lblNombre.setFont(Theme.font(12, Font.BOLD));
            lblNombre.setForeground(colorRemitente);
            lblNombre.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 0));
            contenedorTexto.add(lblNombre, BorderLayout.NORTH);
        }

        JEditorPane area = new JEditorPane() {
            @Override public Dimension getPreferredSize() {
                int maxAncho = 340;
                View view = getUI().getRootView(this);
                view.setSize(maxAncho, Integer.MAX_VALUE);
                float w = view.getPreferredSpan(View.X_AXIS);
                float h = view.getPreferredSpan(View.Y_AXIS);
                int anchoFinal = (int) Math.min(maxAncho, w + 16);
                anchoFinal = Math.max(anchoFinal, 80);
                return new Dimension(anchoFinal, (int) h + 6);
            }
        };
        area.setContentType("text/html");
        area.setEditable(false);
        area.setOpaque(false);
        area.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        area.setFont(Theme.font(15, Font.PLAIN));

        String colorTexto = esMio ? "#FFFFFF" : "#191B23";
        String colorLink  = esMio
                ? "#BFD6FF"
                : String.format("#%02X%02X%02X", Theme.primary().getRed(), Theme.primary().getGreen(), Theme.primary().getBlue());

        area.setText("<html><body style='color: " + colorTexto + "; margin: 0; padding: 0; font-family: Inter, \"Segoe UI\", sans-serif;'>"
                + procesarEnlaces(texto, colorLink) + "</body></html>");
        area.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try { Desktop.getDesktop().browse(e.getURL().toURI()); }
                catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        contenedorTexto.add(area, BorderLayout.CENTER);

        JLabel lblHora = new JLabel(obtenerHoraActual());
        lblHora.setFont(Theme.fontMono(11));
        lblHora.setForeground(esMio ? new Color(0xBF, 0xD6, 0xFF) : new Color(0x94, 0xA3, 0xB8));
        lblHora.setHorizontalAlignment(SwingConstants.RIGHT);
        lblHora.setBorder(BorderFactory.createEmptyBorder(4, 6, 0, 0));

        JPanel panelSur = new JPanel(new BorderLayout()) {
            @Override public boolean isOpaque() { return false; }
        };
        panelSur.add(lblHora, BorderLayout.EAST);
        contenedorTexto.add(panelSur, BorderLayout.SOUTH);

        wrapBurbuja(contenedorTexto, esMio);
    }

    public BurbujaMensaje(String remitente, ImageIcon iconoOriginal, Color colorRemitente, boolean esMio) {
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(Theme.BUBBLE_GAP / 2, 8, Theme.BUBBLE_GAP / 2, 8));

        JPanel contenedorImagen = new JPanel(new BorderLayout()) {
            @Override public boolean isOpaque() { return false; }
        };

        if (!esMio) {
            JLabel lblNombre = new JLabel(remitente);
            lblNombre.setFont(Theme.font(12, Font.BOLD));
            lblNombre.setForeground(colorRemitente);
            lblNombre.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 0));
            contenedorImagen.add(lblNombre, BorderLayout.NORTH);
        }

        ImageIcon redimensionado = redimensionarImagen(iconoOriginal, 250, 250);
        JLabel lblImagen = new JLabel(redimensionado);
        contenedorImagen.add(lblImagen, BorderLayout.CENTER);

        JLabel lblHora = new JLabel(obtenerHoraActual());
        lblHora.setFont(Theme.fontMono(11));
        lblHora.setForeground(esMio ? new Color(0xBF, 0xD6, 0xFF) : new Color(0x94, 0xA3, 0xB8));
        lblHora.setHorizontalAlignment(SwingConstants.RIGHT);
        lblHora.setBorder(BorderFactory.createEmptyBorder(4, 6, 0, 0));

        JPanel panelSur = new JPanel(new BorderLayout()) {
            @Override public boolean isOpaque() { return false; }
        };
        panelSur.add(lblHora, BorderLayout.EAST);
        contenedorImagen.add(panelSur, BorderLayout.SOUTH);

        wrapBurbuja(contenedorImagen, esMio);
    }

    /**
     * Pinta la burbuja como un pill (rectángulo totalmente redondeado).
     * - Sin cola: corner radius uniforme.
     * - Sombra de UNA sola capa (no escalonada) para evitar el artefacto
     *   "stepping" de la versión anterior.
     */
    private void wrapBurbuja(JPanel contenido, boolean esMio) {
        final int radius = Theme.RADIUS_XL;

        JPanel bubble = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                Color fill   = esMio ? Theme.primaryContainer() : Theme.surfaceLowest();
                Color border = esMio ? Theme.primaryContainer() : Theme.outlineVariant();

                // 1) Sombra única suave (drop shadow simple). Solo para burbujas
                //    de terceros, las azules sobre fondo claro ya destacan solas.
                if (!esMio) {
                    g2.setColor(new Color(0x0F, 0x17, 0x2A, 24));
                    g2.fill(new RoundRectangle2D.Float(0, 1, w, h, radius, radius));
                }

                // 2) Cuerpo del bubble (pill perfecto)
                g2.setColor(fill);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, radius, radius));

                // 3) Borde sutil solo en burbujas de terceros
                if (!esMio) {
                    g2.setColor(border);
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, radius, radius));
                }

                g2.dispose();
            }
        };
        bubble.setOpaque(false);
        bubble.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        bubble.add(contenido, BorderLayout.CENTER);

        JPanel alineador = new JPanel(new FlowLayout(esMio ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0)) {
            @Override public boolean isOpaque() { return false; }
        };
        alineador.add(bubble);

        this.add(alineador, BorderLayout.CENTER);
    }

    private ImageIcon redimensionarImagen(ImageIcon icono, int maxAncho, int maxAlto) {
        Image img = icono.getImage();
        int wo = img.getWidth(null), ho = img.getHeight(null);
        if (wo <= 0 || ho <= 0) return icono;
        double ratio = Math.min((double) maxAncho / wo, (double) maxAlto / ho);
        if (ratio < 1.0) {
            return new ImageIcon(img.getScaledInstance((int)(wo * ratio), (int)(ho * ratio), Image.SCALE_SMOOTH));
        }
        return icono;
    }

    private String obtenerHoraActual() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a")).toLowerCase()
                .replace("am", "a.m.").replace("pm", "p.m.");
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

    @Override public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
