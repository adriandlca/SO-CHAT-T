package Views.theme;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.border.AbstractBorder;

/**
 * Borde de sombra suave "ambient shadow" estilo Material/Linear.
 * Aplica una sola sombra difuminada (offset 0, blur ~12) y reserva
 * margen para que la sombra no sea recortada por el contenedor.
 */
public class SoftShadowBorder extends AbstractBorder {

    private final int radius;
    private final int shadowSize;
    private final Color shadowColor;

    public SoftShadowBorder(int radius) {
        this(radius, 8, new Color(15, 23, 42, 28));
    }

    public SoftShadowBorder(int radius, int shadowSize, Color shadowColor) {
        this.radius = radius;
        this.shadowSize = shadowSize;
        this.shadowColor = shadowColor;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Sombra: varias capas con alpha decreciente para simular blur.
        int layers = 6;
        for (int i = layers; i >= 1; i--) {
            float alpha = shadowColor.getAlpha() / 255f * (1f - (i / (float) layers)) * 0.35f;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(shadowColor);
            int offset = i * 2;
            g2.fill(new RoundRectangle2D.Float(
                    x + 1, y + 1 + offset / 2f,
                    width - 2, height - 2 + offset,
                    radius + i, radius + i));
        }

        // Restaurar opacidad para el resto del componente.
        g2.setComposite(AlphaComposite.SrcOver);
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(shadowSize, shadowSize, shadowSize, shadowSize);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(shadowSize, shadowSize, shadowSize, shadowSize);
        return insets;
    }
}
