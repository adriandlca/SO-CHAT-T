package Controllers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Cliente socket hacia el servidor central.
 * Si el socket ya está cerrado el enviar fallará silenciosamente (lo registra enviarPorRed).
 */
public class Conexion {
    protected Socket socket;
    private final PrintWriter writer;

    public Conexion(String ipDestino, int puerto) throws IOException {
        socket = new Socket(ipDestino, puerto);
        OutputStream os = socket.getOutputStream();
        writer = new PrintWriter(new OutputStreamWriter(os, java.nio.charset.StandardCharsets.UTF_8), true);
    }

    public PrintWriter getWriter() { return writer; }

    public boolean estaConectado() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }
}
