package Controllers;

import java.io.IOException;
import java.net.Socket;

public class Conexion {
    protected Socket socket;

    public Conexion(String ipDestino, int puerto) throws IOException {
        // Solo actúa como cliente, conectándose a la IP y puerto indicados
        socket = new Socket(ipDestino, puerto);
    }
}