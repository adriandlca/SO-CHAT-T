package Views.theme;

import java.awt.Color;
import java.awt.Font;
import javax.swing.UIManager;

/**
 * Tokens visuales del rediseño JAM-Chat (sistema Stitch → Material 3).
 *
 * Sólo apariencia: paleta, tipografía, radios. La lógica de negocio
 * no se ve afectada. Para activar el nuevo look-and-feel basta con:
 *
 *     Theme.install(Theme.Mode.LIGHT);
 *
 * al inicio de la aplicación (antes de crear cualquier ventana).
 */
public final class Theme {

    public enum Mode { LIGHT, DARK }

    private static Mode currentMode = Mode.LIGHT;

    /* ============================ TOKENS ============================ */
    /* Paleta derivada del design system Stitch (Material 3 tones).     */
    /* Cada color tiene una constante LIGHT y una _DARK.                */

    // Accent
    public static final Color PRIMARY           = new Color(0x00, 0x4A, 0xC6);
    public static final Color PRIMARY_DARK      = new Color(0xB4, 0xC5, 0xFF);
    public static final Color PRIMARY_CONTAINER       = new Color(0x25, 0x63, 0xEB);
    public static final Color ON_PRIMARY         = new Color(0xFF, 0xFF, 0xFF);
    public static final Color ON_PRIMARY_CONTAINER     = new Color(0xEE, 0xEF, 0xFF);
    public static final Color TERTIARY_LIGHT    = new Color(0x94, 0x37, 0x00);
    public static final Color TERTIARY_DARK     = new Color(0xFF, 0xB5, 0x96);

    // Fondos
    public static final Color BACKGROUND_LIGHT  = new Color(0xFA, 0xF8, 0xFF);
    public static final Color BACKGROUND_DARK   = new Color(0x19, 0x1B, 0x23);
    public static final Color SURFACE_LIGHT     = new Color(0xFA, 0xF8, 0xFF);
    public static final Color SURFACE_DARK      = new Color(0x11, 0x13, 0x18);
    public static final Color SURFACE_CONTAINER_LOW_LIGHT       = new Color(0xF3, 0xF3, 0xFE);
    public static final Color SURFACE_CONTAINER_LOW_DARK        = new Color(0x1D, 0x1F, 0x26);
    public static final Color SURFACE_CONTAINER_HIGH_LIGHT      = new Color(0xE7, 0xE7, 0xF3);
    public static final Color SURFACE_CONTAINER_HIGH_DARK       = new Color(0x2E, 0x30, 0x39);
    public static final Color SURFACE_CONTAINER_LOWEST_LIGHT    = new Color(0xFF, 0xFF, 0xFF);
    public static final Color SURFACE_CONTAINER_LOWEST_DARK     = new Color(0x0B, 0x0D, 0x11);

    // Textos
    public static final Color ON_SURFACE_LIGHT      = new Color(0x19, 0x1B, 0x23);
    public static final Color ON_SURFACE_DARK       = new Color(0xE1, 0xE2, 0xED);
    public static final Color ON_SURFACE_VARIANT_LIGHT = new Color(0x43, 0x46, 0x55);
    public static final Color ON_SURFACE_VARIANT_DARK  = new Color(0xC3, 0xC6, 0xD7);
    public static final Color OUTLINE_LIGHT         = new Color(0x73, 0x76, 0x86);
    public static final Color OUTLINE_DARK          = new Color(0x73, 0x76, 0x86);
    public static final Color OUTLINE_VARIANT_LIGHT = new Color(0xC3, 0xC6, 0xD7);
    public static final Color OUTLINE_VARIANT_DARK  = new Color(0x43, 0x46, 0x55);

    // Estado
    public static final Color ERROR                = new Color(0xBA, 0x1A, 0x1A);
    public static final Color SUCCESS              = new Color(0x22, 0xC5, 0x5E);
    public static final Color WARNING              = new Color(0xF5, 0x9E, 0x0B);

    // Header oscuro del perfil (siempre oscuro en ambos modos, como Stitch)
    public static final Color PROFILE_HEADER       = new Color(0x0F, 0x17, 0x2A);

    /* Tipografía */
    public static final String FONT_FAMILY         = "Inter";
    public static final String FONT_FAMILY_FALLBACK = "Segoe UI";
    public static final String FONT_FAMILY_EMOJI   = "Segoe UI Emoji";
    public static final String FONT_FAMILY_MONO    = "JetBrains Mono";

    /* Radios (px) */
    public static final int RADIUS_SM   = 6;
    public static final int RADIUS_MD   = 8;
    public static final int RADIUS_LG   = 12;
    public static final int RADIUS_XL   = 22;
    public static final int RADIUS_PILL = 999;

    /* Espaciados (px) */
    public static final int GUTTER    = 16;
    public static final int STACK_GAP = 8;
    public static final int BUBBLE_GAP = 4;

    /* ============================ HELPERS ============================ */

    public static Color primary()        { return currentMode == Mode.DARK ? PRIMARY_DARK : PRIMARY; }
    public static Color primaryContainer() { return PRIMARY_CONTAINER; }
    public static Color onPrimary()        { return ON_PRIMARY; }
    public static Color background()      { return currentMode == Mode.DARK ? BACKGROUND_DARK : BACKGROUND_LIGHT; }
    public static Color surface()         { return currentMode == Mode.DARK ? SURFACE_DARK : SURFACE_LIGHT; }
    public static Color surfaceLow()      { return currentMode == Mode.DARK ? SURFACE_CONTAINER_LOW_DARK : SURFACE_CONTAINER_LOW_LIGHT; }
    public static Color surfaceHigh()     { return currentMode == Mode.DARK ? SURFACE_CONTAINER_HIGH_DARK : SURFACE_CONTAINER_HIGH_LIGHT; }
    public static Color surfaceLowest()   { return currentMode == Mode.DARK ? SURFACE_CONTAINER_LOWEST_DARK : SURFACE_CONTAINER_LOWEST_LIGHT; }
    public static Color onSurface()       { return currentMode == Mode.DARK ? ON_SURFACE_DARK : ON_SURFACE_LIGHT; }
    public static Color onSurfaceVariant(){ return currentMode == Mode.DARK ? ON_SURFACE_VARIANT_DARK : ON_SURFACE_VARIANT_LIGHT; }
    public static Color outline()         { return currentMode == Mode.DARK ? OUTLINE_DARK : OUTLINE_LIGHT; }
    public static Color outlineVariant()  { return currentMode == Mode.DARK ? OUTLINE_VARIANT_DARK : OUTLINE_VARIANT_LIGHT; }
    public static Color tertiary()        { return currentMode == Mode.DARK ? TERTIARY_DARK : TERTIARY_LIGHT; }

    /** Devuelve el Font solicitado. Si Inter no está instalado, cae a Segoe UI. */
    public static Font font(int size, int style) {
        String requested = FONT_FAMILY;
        if (!isFamilyInstalled(requested)) requested = FONT_FAMILY_FALLBACK;
        if (!isFamilyInstalled(requested)) requested = Font.SANS_SERIF;
        return new Font(requested, style, size);
    }

    public static Font fontBase()  { return font(14, Font.PLAIN); }
    public static Font fontMd()    { return font(15, Font.PLAIN); }
    public static Font fontBold()  { return font(14, Font.BOLD); }
    public static Font fontLabel() { return font(12, Font.BOLD); }
    public static Font fontTitle() { return font(18, Font.BOLD); }
    public static Font fontMono(int size) { return new Font(FONT_FAMILY_MONO, Font.PLAIN, size); }
    public static Font fontEmoji(int size, int style) { return new Font(FONT_FAMILY_EMOJI, style, size); }

    private static boolean isFamilyInstalled(String name) {
        String[] families = java.awt.GraphicsEnvironment
                .getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String f : families) if (f.equalsIgnoreCase(name)) return true;
        return false;
    }

    /* ====================== INSTALL (UI MANAGER) ===================== */

    /**
     * Aplica los defaults al UIManager. Llamar ANTES de crear ventanas.
     * Si ya hay ventanas abiertas no se ven reflejadas; use updateUI() en
     * cada raíz Swing si quiere conmutar en caliente.
     */
    public static void install(Mode mode) {
        currentMode = mode;
        applyDefaults();
    }

    public static Mode currentMode() { return currentMode; }

    private static void applyDefaults() {
        UIManager.put("Panel.background", surfaceLowest());
        UIManager.put("Panel.foreground", onSurface());

        UIManager.put("OptionPane.background", surfaceLowest());
        UIManager.put("OptionPane.foreground", onSurface());
        UIManager.put("OptionPane.messageBackground", surfaceLowest());
        UIManager.put("OptionPane.messageForeground", onSurface());

        UIManager.put("Label.foreground", onSurface());
        UIManager.put("Label.font", fontBase());

        UIManager.put("Button.font", fontBold());
        UIManager.put("Button.foreground", onSurface());
        UIManager.put("Button.background", surface());
        UIManager.put("Button.focus", new Color(0, 0, 0, 0));
        UIManager.put("Button.select", surfaceHigh());

        UIManager.put("TextField.background", surfaceLowest());
        UIManager.put("TextField.foreground", onSurface());
        UIManager.put("TextField.caretForeground", primary());
        UIManager.put("TextField.font", fontBase());

        UIManager.put("PasswordField.background", surfaceLowest());
        UIManager.put("PasswordField.foreground", onSurface());
        UIManager.put("PasswordField.caretForeground", primary());
        UIManager.put("PasswordField.font", fontBase());

        UIManager.put("TextArea.background", surfaceLowest());
        UIManager.put("TextArea.foreground", onSurface());
        UIManager.put("TextArea.caretForeground", primary());
        UIManager.put("TextArea.font", fontBase());

        UIManager.put("CheckBox.background", surfaceLowest());
        UIManager.put("CheckBox.foreground", onSurface());
        UIManager.put("CheckBox.font", fontBase());

        UIManager.put("List.background", surfaceLowest());
        UIManager.put("List.foreground", onSurface());
        UIManager.put("List.selectionBackground", surfaceHigh());
        UIManager.put("List.selectionForeground", primary());
        UIManager.put("List.font", fontBase());

        UIManager.put("ScrollPane.background", surfaceLowest());
        UIManager.put("ScrollPane.foreground", onSurface());
        UIManager.put("ScrollBar.background", surfaceLowest());
        UIManager.put("ScrollBar.thumb", outlineVariant());
        UIManager.put("ScrollBar.track", surfaceLow());

        UIManager.put("MenuBar.background", surfaceLowest());
        UIManager.put("Menu.background", surfaceLowest());
        UIManager.put("Menu.foreground", onSurface());
        UIManager.put("MenuItem.background", surfaceLowest());
        UIManager.put("MenuItem.foreground", onSurface());

        UIManager.put("PopupMenu.background", surfaceLowest());
        UIManager.put("PopupMenu.border", new javax.swing.border.LineBorder(outlineVariant(), 1, true));

        UIManager.put("ToolTip.background", surfaceLowest());
        UIManager.put("ToolTip.foreground", onSurface());
        UIManager.put("ToolTip.border", new javax.swing.border.LineBorder(outlineVariant(), 1, true));

        UIManager.put("FileChooser.background", surfaceLowest());
        UIManager.put("FileChooser.foreground", onSurface());
        UIManager.put("FileChooser.listBackground", surfaceLowest());
    }
}
