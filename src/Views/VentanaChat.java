package Views;

import Controllers.HistorialChat;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.function.Consumer;
import javax.swing.JFrame;

/**
 * Wrapper JFrame sobre {@link ChatPanel} para preservar la API histórica
 * (multi-ventana) usada por código antiguo o tests.
 *
 * @deprecated Esta ventana abre cada chat en su propio JFrame, lo que rompe
 *             el rediseño unificado (sidebar + chat en la misma ventana).
 *             Usa {@link VentanaChatUnificado} en su lugar.
 */
@Deprecated
public class VentanaChat extends JFrame {

    private final ChatPanel chat;

    public VentanaChat(String miUsuario, String contactoDestino, boolean esGrupo) {
        super("Chat: " + contactoDestino);
        this.chat = new ChatPanel(miUsuario, contactoDestino, esGrupo);
        setContentPane(chat);
        setSize(520, 640);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    @Override public void setVisible(boolean b) {
        super.setVisible(b);
    }

    /* ============================ API delegada ============================ */

    public void mostrarMensajeConColor(String r, String m, java.awt.Color c)  { chat.mostrarMensajeConColor(r, m, c); }
    public void mostrarImagenConColor(String r, javax.swing.ImageIcon i, java.awt.Color c) { chat.mostrarImagenConColor(r, i, c); }
    public void mostrarStickerConColor(String r, javax.swing.ImageIcon s, java.awt.Color c) { chat.mostrarStickerConColor(r, s, c); }
    public void mostrarBotonDescargaArchivo(String r, String n, String b, java.awt.Color c) { chat.mostrarBotonDescargaArchivo(r, n, b, c); }
    public void mostrarMensajePlano(String t) { chat.mostrarMensajePlano(t); }
    public void mostrarEscribiendo(boolean e) { chat.mostrarEscribiendo(e); }
    public void mostrarEscribiendoGrupal(String r, boolean e) { chat.mostrarEscribiendoGrupal(r, e); }

    public void setAccionEscribiendo(Consumer<Boolean> a) { chat.setAccionEscribiendo(a); }
    public void setAccionEnviar(ActionListener a) { chat.setAccionEnviar(a); }
    public void setOnImagenSeleccionada(Consumer<File> a) { chat.setOnImagenSeleccionada(a); }
    public void setOnArchivoSeleccionado(Consumer<File> a) { chat.setOnArchivoSeleccionado(a); }
    public void setOnStickerSeleccionado(Consumer<String> a) { chat.setOnStickerSeleccionado(a); }

    public String getMensajeEscrito() { return chat.getMensajeEscrito(); }
    public void limpiarInput() { chat.limpiarInput(); }

    /** Reproduce la firma original: la ventana sigue siendo un JFrame. */
    public boolean isActive() { return chat.isActive(); }

    /** Api interna usada por el sidebar del rediseño. */
    public ChatPanel getChatPanel() { return chat; }
}
