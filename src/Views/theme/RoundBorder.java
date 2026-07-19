package Views.theme;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.border.AbstractBorder;

/**
 * Borde redondeado "soft-geometric" según la guía de Stitch:
 * - Inputs:  6-8 px
 * - Cards:   10-12 px
 * - Bubbles: 14 px
 *
 * Puede combinar varios bordes (focus / error) componiendo con un CompoundBorder.
 */
public class RoundBorder extends AbstractBorder {

    private final int radius;
    private final java.awt.Color color;

    public RoundBorder(int radius, java.awt.Color color) {
        this.radius = radius;
        this.color = color;
    }

    public int getRadius() { return radius; }
    public java.awt.Color getColor() { return color; }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        // 1 px exacto, no se duplica con el área de fill del componente.
        g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, width - 1f, height - 1f, radius, radius));
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(1, 1, 1, 1);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(1, 1, 1, 1);
        return insets;
    }
}
