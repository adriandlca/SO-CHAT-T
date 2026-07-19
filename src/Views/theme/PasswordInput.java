package Views.theme;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JPasswordField;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;

/**
 * PasswordField con el mismo look que TextInput y un botón interno
 * para alternar la visibilidad del carácter (ojo/eye-off).
 */
public class PasswordInput extends JPasswordField {

    private boolean errorState = false;
    private final RoundBorder borderNormal = new RoundBorder(Theme.RADIUS_MD, Theme.outlineVariant());
    private final RoundBorder borderFocus  = new RoundBorder(Theme.RADIUS_MD, Theme.primary());
    private final RoundBorder borderError  = new RoundBorder(Theme.RADIUS_MD, Theme.ERROR);

    public PasswordInput(String initial) {
        super(initial);
        configure();
    }

    public PasswordInput() {
        this("");
    }

    private void configure() {
        setFont(Theme.fontBase());
        setForeground(Theme.onSurface());
        setBackground(Theme.surfaceLow());
        setCaretColor(Theme.primary());
        setSelectionColor(Theme.primaryContainer());
        setSelectedTextColor(Theme.onPrimary());
        setOpaque(false);
        applyBorder();

        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { applyBorder(); }
            @Override public void focusLost(FocusEvent e)  { applyBorder(); }
        });
    }

    private void applyBorder() {
        RoundBorder outer;
        if (errorState)      outer = borderError;
        else if (hasFocus()) outer = borderFocus;
        else                 outer = borderNormal;
        setBorder(new CompoundBorder(outer, new PadBorder()));
    }

    public void setError(boolean e) {
        this.errorState = e;
        applyBorder();
    }

    private static class PadBorder extends AbstractBorder {
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {}
        @Override public Insets getBorderInsets(Component c) { return new Insets(8, 12, 8, 36); }
        @Override public Insets getBorderInsets(Component c, Insets insets) { return new Insets(8, 12, 8, 36); }
    }
}
