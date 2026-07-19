package Views.theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.AbstractButton;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Botón primario tipo "Action Blue" del mockup Stitch.
 * Fondo accent blanco, hover oscurece, leve sombra, radio configurable.
 */
public class PrimaryButton extends javax.swing.JButton {

    private final int radius;
    private Color base;
    private Color hover;
    private Color press;
    private Color text;

    public PrimaryButton(String text, int radius) {
        super(text);
        this.radius = radius;
        this.base   = Theme.primary();
        this.hover  = Theme.primaryContainer();
        this.press  = mix(base, Color.BLACK, 0.15f);
        this.text   = Theme.onPrimary();
        configure();
    }

    public void setBaseColor(Color c) { this.base = c; this.hover = c; repaint(); }
    public void setHoverColor(Color c) { this.hover = c; repaint(); }

    private void configure() {
        setUI(new BasicButtonUI());
        setFont(Theme.font(14, java.awt.Font.BOLD));
        setForeground(text);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setBorder(new PaddedBorder(radius));
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        if (getModel().isPressed())      g2.setColor(press);
        else if (getModel().isRollover())g2.setColor(hover);
        else                              g2.setColor(base);

        // Sombra ligera solo en estado reposo.
        if (!getModel().isPressed()) {
            g2.setColor(new Color(0, 0, 0, 28));
            g2.fillRoundRect(2, 3, w - 4, h - 2, radius, radius);
            g2.setColor(getModel().isRollover() ? hover : base);
        }
        g2.fillRoundRect(0, 0, w, h, radius, radius);

        g2.dispose();
        super.paintComponent(g);
    }

    private static Color mix(Color a, Color b, float t) {
        int r = (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t);
        return new Color(r, g, bl);
    }

    private static class PaddedBorder extends AbstractBorder {
        private final int radius;
        PaddedBorder(int radius) { this.radius = radius; }
        @Override public java.awt.Insets getBorderInsets(Component c) {
            return new java.awt.Insets(10, 18, 10, 18);
        }
    }
}
