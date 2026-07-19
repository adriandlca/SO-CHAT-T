package Views.theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Botón "ghost" / "secondary" plano: fondo transparente, redondeado,
 * hover con surfaceHigh. Soporta un icono MaterialGlyph o texto.
 *
 * - Sin icono: aparece como etiqueta clickeable (chips, tabs).
 * - Con icono: icono centrado, opcional tooltip.
 *
 * Para resetear a un botón estándar Swing use un JButton normal.
 */
public class GhostButton extends javax.swing.JButton {

    private final int radius;
    private final Color baseColor;
    private final Color hoverColor;
    private final Color pressColor;

    public GhostButton(String text) {
        this(text, null, Theme.RADIUS_MD);
    }

    public GhostButton(String text, javax.swing.Icon icon, int radius) {
        super(text, icon);
        this.radius = radius;
        this.baseColor = Theme.surfaceLowest();
        this.hoverColor = Theme.surfaceHigh();
        this.pressColor = Theme.surfaceHigh();
        configure();
    }

    private void configure() {
        setUI(new BasicButtonUI());
        setFont(Theme.fontBold());
        setForeground(Theme.onSurface());
        setBackground(baseColor);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setBorder(new RoundedPaddingBorder(radius));
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        // bg animado
        if (getModel().isPressed()) g2.setColor(pressColor);
        else if (getModel().isRollover()) g2.setColor(hoverColor);
        else g2.setColor(baseColor);
        g2.fillRoundRect(0, 0, w, h, radius, radius);
        g2.dispose();
        super.paintComponent(g);
    }

    /** Padding interior coherente con el sistema (8px vertical / 14px horizontal). */
    private static class RoundedPaddingBorder extends AbstractBorder {
        private final int radius;
        RoundedPaddingBorder(int radius) { this.radius = radius; }

        @Override public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            AbstractButton b = (AbstractButton) c;
            if (b.getModel().isRollover()) {
                g2.setColor(Theme.outlineVariant());
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, width - 1, height - 1, radius, radius);
            }
            g2.dispose();
        }
        @Override public java.awt.Insets getBorderInsets(Component c) {
            return new java.awt.Insets(8, 14, 8, 14);
        }
    }
}
