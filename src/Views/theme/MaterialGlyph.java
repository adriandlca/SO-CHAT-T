package Views.theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Iconos vectoriales "Material Symbols" (línea 1.5px) en grilla 24×24.
 * Los paths están simplificados para que se vean bien incluso a 16-20px.
 *
 * Sólo se exponen los iconos realmente usados por las pantallas rediseñadas.
 * Se han quitado los iconos sin uso (videocam, error, info…) para no
 * inflar la clase con paths que nadie consume.
 */
public class MaterialGlyph implements javax.swing.Icon {

    private final int size;
    private final Color color;
    private final float[][] lines;     // pares (x1,y1,x2,y2) en grilla 24x24
    private final float[][] rects;     // (x,y,w,h) o (x,y,w,h,r)
    private final float[] dots;        // tripletas (cx,cy,r)

    private MaterialGlyph(int size, Color color, float[][] lines, float[][] rects, float[] dots) {
        this.size = size;
        this.color = color;
        this.lines = lines;
        this.rects = rects;
        this.dots = dots;
    }

    public static MaterialGlyph of(int size, Color color, float[][] lines) {
        return new MaterialGlyph(size, color, lines, null, null);
    }
    public static MaterialGlyph of(int size, Color color, float[][] lines, float[][] rects) {
        return new MaterialGlyph(size, color, lines, rects, null);
    }
    public static MaterialGlyph of(int size, Color color, float[][] lines, float[][] rects, float[] dots) {
        return new MaterialGlyph(size, color, lines, rects, dots);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setColor(color);
        g2.translate(x, y);

        // Rectángulos / tarjetas
        if (rects != null) {
            for (float[] r : rects) {
                if (r.length >= 6) g2.drawRoundRect(Math.round(r[0]), Math.round(r[1]),
                        Math.round(r[2]), Math.round(r[3]),
                        Math.round(r[4]), Math.round(r[4]));
                else if (r.length >= 4) g2.drawRect(Math.round(r[0]), Math.round(r[1]),
                        Math.round(r[2]), Math.round(r[3]));
            }
        }

        // Puntos (dots)
        if (dots != null) {
            for (int i = 0; i + 2 < dots.length; i += 3) {
                int r = Math.round(dots[i + 2]);
                g2.fillOval(Math.round(dots[i]) - r, Math.round(dots[i + 1]) - r, r * 2, r * 2);
            }
        }

        // Líneas (1.5 stroke)
        if (lines != null) {
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (float[] l : lines) {
                if (l.length == 4) g2.drawLine(Math.round(l[0]), Math.round(l[1]),
                                               Math.round(l[2]), Math.round(l[3]));
            }
        }
        g2.dispose();
    }

    @Override public int getIconWidth()  { return size; }
    @Override public int getIconHeight() { return size; }

    /* ============================================================
       Fábricas. Geometría en grilla 24x24, normalizada.
       Cada icono está simplificado para verse nítido a 16-22px.
       ============================================================ */

    /** Lupa de búsqueda: círculo + mango diagonal. */
    public static MaterialGlyph search(int size, Color color) {
        return of(size, color,
                new float[][]{{16, 16, 21, 21}},
                new float[][]{{10, 2, 12, 12, 6, 6}});
    }

    /** Flecha atrás: dos segmentos formando una "V" invertida apuntando a la izquierda. */
    public static MaterialGlyph arrowBack(int size, Color color) {
        return of(size, color, new float[][]{
                {20, 12, 5, 12},   // cuerpo horizontal corto
                {11, 5,  5, 12},   // diagonal superior -> punta
                {11, 19, 5, 12}    // diagonal inferior -> punta
        });
    }

    /** Tres puntos verticales (menú k/m). */
    public static MaterialGlyph moreVert(int size, Color color) {
        return of(size, color, null, null,
                new float[]{12, 5, 1.3f, 12, 12, 1.3f, 12, 19, 1.3f});
    }

    /** "+" centrado. */
    public static MaterialGlyph add(int size, Color color) {
        return of(size, color, new float[][]{{12, 5, 12, 19}, {5, 12, 19, 12}});
    }

    /** "X". */
    public static MaterialGlyph close(int size, Color color) {
        return of(size, color, new float[][]{{6, 6, 18, 18}, {18, 6, 6, 18}});
    }

    /** Check. */
    public static MaterialGlyph check(int size, Color color) {
        return of(size, color, new float[][]{{4, 12, 10, 18}, {10, 18, 20, 6}});
    }

    /** Cara feliz (emoji btn). */
    public static MaterialGlyph emoji(int size, Color color) {
        return of(size, color, null,
                new float[][]{{2, 2, 20, 20, 10, 10}},
                new float[]{8, 9, 1.3f, 16, 9, 1.3f});
    }

    /** Imagen (montaña + sol). */
    public static MaterialGlyph image(int size, Color color) {
        return of(size, color,
                new float[][]{{2, 16, 8, 10}, {8, 10, 12, 14}, {11, 13, 14, 10}, {14, 10, 22, 18}},
                new float[][]{{2, 4, 20, 16, 3, 3}});
    }

    /** Clip / adjuntar archivo. */
    public static MaterialGlyph attachFile(int size, Color color) {
        return of(size, color, new float[][]{
                {16, 7,  16, 14},   // cuerpo vertical derecho
                {16, 14, 15, 15},
                {10, 15, 7,  12},
                {7, 12, 7,  9},
                {7, 9,  9,  7},
                {9, 7,  12, 7}
        });
    }

    /** Estrella de 4 puntas (sticker). */
    public static MaterialGlyph sticker(int size, Color color) {
        return of(size, color, new float[][]{
                {12, 4, 12, 20}, {4, 12, 20, 12},
                {6, 6, 18, 18}, {18, 6, 6, 18}
        });
    }

    /** Flecha "send" arriba (esquinas). */
    public static MaterialGlyph sendUp(int size, Color color) {
        return of(size, color, new float[][]{{12, 5, 12, 19}, {7, 10, 12, 5}, {17, 10, 12, 5}});
    }

    /** Flecha abajo: descarga. */
    public static MaterialGlyph download(int size, Color color) {
        return of(size, color, new float[][]{
                {12, 4, 12, 16}, {7, 11, 12, 16}, {17, 11, 12, 16},
                {5, 20, 19, 20}
        });
    }

    /** Candado. */
    public static MaterialGlyph lock(int size, Color color) {
        return of(size, color, new float[][]{{8, 11, 8, 8}, {8, 8, 16, 8}, {16, 8, 16, 11}},
                new float[][]{{4, 11, 16, 10, 2, 2}});
    }

    /** Ojo (visibility). */
    public static MaterialGlyph visibility(int size, Color color) {
        return of(size, color, null,
                new float[][]{{2, 12, 22, 0, 0, 0}},
                new float[]{12, 12, 4, 4});
    }

    /** Ojo tachado (visibility_off). */
    public static MaterialGlyph visibilityOff(int size, Color color) {
        return of(size, color,
                new float[][]{{3, 3, 21, 21}, {10, 9, 10, 15}, {14, 9, 14, 13}},
                new float[][]{{2, 12, 22, 0, 0, 0}});
    }

    /** "i" de info. */
    public static MaterialGlyph info(int size, Color color) {
        return of(size, color,
                new float[][]{{12, 11, 12, 17}, {12, 7, 12, 7}});
    }

    /** Grupo (3 figuras). */
    public static MaterialGlyph group(int size, Color color) {
        return of(size, color, null,
                new float[][]{{9, 12, 4, 4, 0, 0}, {16, 9, 7, 7, 0, 0}},
                new float[]{9, 9, 3, 3, 16, 8, 2.5f});
    }

    /** Persona (para fallback). */
    public static MaterialGlyph person(int size, Color color) {
        return of(size, color, null,
                new float[][]{{7, 14, 17, 14, 0, 0}},
                new float[]{12, 8, 3, 3});
    }

    /** Doble check (enviado y recibido). */
    public static MaterialGlyph doneAll(int size, Color color) {
        return of(size, color, new float[][]{
                {2, 12, 7, 17}, {7, 17, 13, 11},
                {9, 12, 14, 17}, {14, 17, 22, 9}
        });
    }
}
