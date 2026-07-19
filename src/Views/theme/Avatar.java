package Views.theme;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * Avatar circular con iniciales sobre fondo en gradiente.
 * Replica el patrón "JC" / "Adrian" del mock de Stitch.
 */
public class Avatar extends JPanel {

    private final String initials;
    private Color colorA;
    private Color colorB;
    private final int diameter;
    private final Font font;

    public Avatar(String name, int diameter) {
        this.diameter = diameter;
        this.initials = extractInitials(name);
        this.colorA = Theme.primary();
        this.colorB = Theme.tertiary();
        this.font = Theme.font(diameter / 2 + 2, Font.BOLD);

        setOpaque(false);
        Dimension d = new Dimension(diameter, diameter);
        setPreferredSize(d);
        setMaximumSize(d);
        setMinimumSize(d);
    }

    public Avatar(String name, int diameter, Color colorA, Color colorB) {
        this(name, diameter);
        this.colorA = colorA;
        this.colorB = colorB;
    }

    public void setGradient(Color a, Color b) {
        this.colorA = a;
        this.colorB = b;
        repaint();
    }

    private static String extractInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            String p = parts[0];
            String second = p.length() > 1 ? "" + p.charAt(1) : "";
            return ("" + p.charAt(0)).toUpperCase() + second;
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int d = diameter;
        g2.setPaint(new GradientPaint(0, 0, colorA, d, d, colorB));
        g2.fillOval(0, 0, d, d);

        g2.setColor(Color.WHITE);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int x = (d - fm.stringWidth(initials)) / 2;
        int y = (d - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(initials, x, y);

        g2.dispose();
    }
}
