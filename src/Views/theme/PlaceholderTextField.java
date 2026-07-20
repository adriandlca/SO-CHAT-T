package Views.theme;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.JTextField;
import javax.swing.text.Document;

/**
 * JTextField con placeholder real: el texto del placeholder se dibuja sobre el
 * componente cuando está vacío y sin foco, pero NUNCA se inserta en el
 * documento. Así {@link #getText()} siempre devuelve el contenido real y los
 * {@code DocumentListener} no se disparan al mostrar/ocultar el placeholder.
 *
 * Comportamiento esperado:
 *  - Al mostrar el campo: aparece el placeholder en gris.
 *  - Al hacer clic y empezar a escribir: el primer carácter reemplaza el
 *    placeholder visualmente (no hay que borrar nada).
 *  - Al perder el foco con el campo vacío: vuelve a aparecer el placeholder.
 */
public class PlaceholderTextField extends JTextField {

    private final String placeholder;
    private Color placeholderColor = Theme.onSurfaceVariant();

    public PlaceholderTextField(String placeholder) {
        super();
        this.placeholder = placeholder == null ? "" : placeholder;
        setOpaque(false);
    }

    public PlaceholderTextField(String placeholder, Color placeholderColor) {
        this(placeholder);
        if (placeholderColor != null) this.placeholderColor = placeholderColor;
    }

    /** @return true cuando no hay texto real (es decir, se está mostrando el placeholder). */
    public boolean isPlaceholderActive() {
        return getText() == null || getText().isEmpty();
    }

    public String getPlaceholder() {
        return placeholder;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!isPlaceholderActive() || placeholder.isEmpty() || isFocusOwner()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            g2.setColor(placeholderColor);

            Insets ins = getInsets();
            int x = ins.left;
            int width = getWidth() - ins.left - ins.right;

            int baseline = getBaseline(width, getHeight());
            int y;
            if (baseline >= 0) {
                y = baseline;
            } else {
                java.awt.FontMetrics fm = g2.getFontMetrics();
                y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            }
            g2.drawString(placeholder, x, y);
        } finally {
            g2.dispose();
        }
    }

    /* getDocument() queda intacto: nunca metemos el placeholder en el Document,
       por lo que DocumentListener.insertUpdate/removeUpdate solo se disparan
       con contenido real escrito por el usuario. */
}
