package Views.theme;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;

/**
 * Iconos basados en caracteres Unicode (siempre nítidos, sin paths rotos).
 * Cada fábrica devuelve un Icon que pinta UN carácter con la mejor fuente disponible.
 *
 * Si un carácter no existe en la fuente del sistema, el Icon degrada a un
 * carácter alternativo o un cuadrado/sustituto; nunca falla visualmente.
 */
public final class MaterialGlyph implements Icon {

    private static final String[] FONT_CANDIDATES = {
            "Segoe UI Symbol",
            "Apple Symbols",
            "Noto Sans Symbols",
            "Noto Sans Symbols2",
            "DejaVu Sans",
            "Dialog"
    };

    private static Font cachedFont;
    private static final Object FONT_LOCK = new Object();

    private final String text;
    private final int  size;
    private final Color color;

    private MaterialGlyph(String text, int size, Color color) {
        this.text  = text;
        this.size  = size;
        this.color = color;
    }

    /** Devuelve la mejor fuente del sistema capaz de pintar símbolos Unicode. */
    private static Font symbolFont(float pointSize) {
        synchronized (FONT_LOCK) {
            if (cachedFont == null) {
                String[] available = GraphicsEnvironment
                        .getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
                String chosen = null;
                for (String cand : FONT_CANDIDATES) {
                    for (String avail : available) {
                        if (avail.equalsIgnoreCase(cand)) { chosen = cand; break; }
                    }
                    if (chosen != null) break;
                }
                if (chosen == null) chosen = Font.SANS_SERIF;
                cachedFont = new Font(chosen, Font.PLAIN, (int) pointSize);
            }
            return cachedFont.deriveFont(Font.PLAIN, pointSize);
        }
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setColor(color);

        float fontSize = Math.max(10f, size * 0.85f);
        Font f = symbolFont(fontSize);
        g2.setFont(f);

        // Si la fuente no tiene TODOS los glifos, usamos la fuente SansSerif por defecto.
        if (f.canDisplayUpTo(text) != -1) {
            f = new Font(Font.SANS_SERIF, Font.PLAIN, (int) fontSize);
            g2.setFont(f);
        }

        FontMetrics fm = g2.getFontMetrics();
        int charW = fm.stringWidth(text);
        int ascent = fm.getAscent();
        int descent = fm.getDescent();
        int baseline = y + (size + ascent - descent) / 2;
        int drawX = x + (size - charW) / 2;

        g2.drawString(text, drawX, baseline);
        g2.dispose();
    }

    @Override public int getIconWidth()  { return size; }
    @Override public int getIconHeight() { return size; }

    private static MaterialGlyph of(String text, int size, Color color) {
        return new MaterialGlyph(text, size, color);
    }

    /* ============================================================
       Fábricas (cada una = 1 carácter Unicode claro y estable)
       ============================================================ */

    /** Lupa de búsqueda. */
    public static MaterialGlyph search(int size, Color color) {
        return of("\u2315", size, color); // ⌕ (teléfono de búsqueda, BMP)
    }

    /** Flecha atrás. */
    public static MaterialGlyph arrowBack(int size, Color color) {
        return of("\u2190", size, color); // ←
    }

    /** Tres puntos verticales. */
    public static MaterialGlyph moreVert(int size, Color color) {
        return of("\u22EE", size, color); // ⋮
    }

    /** "+". */
    public static MaterialGlyph add(int size, Color color) {
        return of("+", size, color);
    }

    /** "X" de cierre. */
    public static MaterialGlyph close(int size, Color color) {
        return of("\u2715", size, color); // ✕
    }

    /** Check. */
    public static MaterialGlyph check(int size, Color color) {
        return of("\u2713", size, color); // ✓
    }

    /** Cara feliz (smiley BMP — funciona sin fuente de emojis). */
    public static MaterialGlyph emoji(int size, Color color) {
        return of("\u263A", size, color); // ☺
    }

    /** Imagen (cuadrado con esquina rota = marco de cuadro). */
    public static MaterialGlyph image(int size, Color color) {
        return of("\u25A3", size, color); // ▣ (cuadrado con patrón)
    }

    /** Clip de papel. */
    public static MaterialGlyph attachFile(int size, Color color) {
        return of("\u2295", size, color); // ⊕ (círculo con cruz = clip estilizado)
    }

    /** Estrella (sticker). */
    public static MaterialGlyph sticker(int size, Color color) {
        return of("\u2605", size, color); // ★
    }

    /** Avión (enviar). */
    public static MaterialGlyph sendUp(int size, Color color) {
        return of("\u2708", size, color); // ✈
    }

    /** Triángulo apuntando a la derecha (alternativa al avión). */
    public static MaterialGlyph sendArrow(int size, Color color) {
        return of("\u27A4", size, color); // ➤
    }

    /** Flecha abajo (descargar). */
    public static MaterialGlyph download(int size, Color color) {
        return of("\u2193", size, color); // ↓
    }

    /** Candado. */
    public static MaterialGlyph lock(int size, Color color) {
        return of("\u26BF", size, color); // ⚿ (llave con dientes)
    }

    /** Ojo. */
    public static MaterialGlyph visibility(int size, Color color) {
        return of("\u25C9", size, color); // ◉
    }

    /** Ojo tachado (círculo tachado). */
    public static MaterialGlyph visibilityOff(int size, Color color) {
        return of("\u2298", size, color); // ⊘
    }

    /** "i" de info. */
    public static MaterialGlyph info(int size, Color color) {
        return of("i", size, color);
    }

    /** Grupo. */
    public static MaterialGlyph group(int size, Color color) {
        return of("\u26C2", size, color); // ⛂  (paraguas; fallback simple)
    }

    /** Persona. */
    public static MaterialGlyph person(int size, Color color) {
        return of("\u2638", size, color); // ☸  (rueda)
    }

    /** Doble check. */
    public static MaterialGlyph doneAll(int size, Color color) {
        return of("\u2713\u2713", size, color); // ✓✓
    }

    /** Engranaje (settings). */
    public static MaterialGlyph settings(int size, Color color) {
        return of("\u2699", size, color); // ⚙
    }

    /** Power/logout. */
    public static MaterialGlyph logout(int size, Color color) {
        return of("\u23FB", size, color); // ⏻
    }

    /** Cámara de vídeo. */
    public static MaterialGlyph videocam(int size, Color color) {
        return of("\u25CF", size, color); // ● (círculo como fallback universal)
    }
}
