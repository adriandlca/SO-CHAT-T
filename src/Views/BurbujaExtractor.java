package Views;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JEditorPane;

/**
 * Pequeño helper para extraer texto plano de las burbujas cuando se hace
 * click en un resultado de búsqueda y hay que hacer scroll al mensaje.
 */
final class BurbujaExtractor {

    private BurbujaExtractor() {}

    static String extraer(BurbujaMensaje b) {
        return walk(b, 0);
    }

    private static String walk(Component c, int depth) {
        if (c == null || depth > 6) return null;
        if (c instanceof JEditorPane) {
            try {
                javax.swing.text.Document doc = ((JEditorPane) c).getDocument();
                String t = doc.getText(0, doc.getLength()).replace("\u200B", "");
                if (!t.trim().isEmpty()) return t;
            } catch (Exception ignored) {}
        }
        if (c instanceof JEditorPane) {
            // segundo intento: leer texto del JEditorPane sin filtrar
            JEditorPane p = (JEditorPane) c;
            String t = p.getText();
            if (t != null && !t.trim().isEmpty()) return t;
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                String t = walk(child, depth + 1);
                if (t != null && !t.trim().isEmpty()) return t;
            }
        }
        return null;
    }
}
