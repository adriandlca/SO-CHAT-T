package Servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ManejadorCliente implements Runnable {
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String miUsuario; 

    public ManejadorCliente(Socket socket) {
        this.socket = socket;
        try {
            this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.salida = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Error al crear los flujos para un cliente entrante.");
        }
    }

    @Override
    public void run() {
        try {
            String paqueteRecibido;
            while ((paqueteRecibido = entrada.readLine()) != null) {
                
                // Limite 4 para asegurar que los mensajes grupales se corten correctamente
                String[] partes = paqueteRecibido.split("\\|", 4);
                String comando = partes[0];

                if (comando.equals("LOGIN") && partes.length >= 2) {
                    this.miUsuario = partes[1];
                    ServidorCentral.registrarCliente(miUsuario, this);

                } else if (comando.equals("MSG") && partes.length == 3) {
                    String destinatario = partes[1];
                    String mensaje = partes[2];
                    ServidorCentral.enviarMensajePrivado(miUsuario, destinatario, mensaje);
                    
                } else if (comando.equals("CREAR_GRUPO") && partes.length >= 2) {
                    String nombreGrupo = partes[1];
                    // Si viene una tercera parte, es la contraseña (grupo privado)
                    String contrasena = partes.length >= 3 ? partes[2] : null;
                    ServidorCentral.crearGrupo(nombreGrupo, miUsuario, contrasena);

                } else if (comando.equals("UNIRSE_GRUPO") && partes.length >= 2) {
                    String nombreGrupo = partes[1];
                    // Si el grupo es privado, la tercera parte trae la contraseña
                    String contrasena = partes.length >= 3 ? partes[2] : null;
                    ServidorCentral.unirseGrupo(nombreGrupo, miUsuario, contrasena);

                } else if (comando.equals("GRUPOMSG") && partes.length == 3) {
                    String nombreGrupo = partes[1];
                    String mensaje = partes[2];
                    ServidorCentral.enviarMensajeGrupal(miUsuario, nombreGrupo, mensaje);

                } else if (comando.equals("ESCRIBIENDO") && partes.length >= 2) {
                    String destinatario = partes[1];
                    ServidorCentral.enviarEstadoEscribiendo(miUsuario, destinatario, true);

                } else if (comando.equals("NO_ESCRIBIENDO") && partes.length >= 2) {
                    String destinatario = partes[1];
                    ServidorCentral.enviarEstadoEscribiendo(miUsuario, destinatario, false);

                } else if (comando.equals("GRUPO_ESCRIBIENDO") && partes.length >= 2) {
                    String nombreGrupo = partes[1];
                    ServidorCentral.enviarEstadoEscribiendoGrupal(miUsuario, nombreGrupo, true);

                } else if (comando.equals("GRUPO_NO_ESCRIBIENDO") && partes.length >= 2) {
                    String nombreGrupo = partes[1];
                    ServidorCentral.enviarEstadoEscribiendoGrupal(miUsuario, nombreGrupo, false);
                }
            }
        } catch (IOException e) {
            // El cliente se desconectó
        } finally {
            cerrarConexion();
        }
    }

    public void enviarPaquete(String paquete) {
        if (salida != null) {
            salida.println(paquete);
        }
    }

    private void cerrarConexion() {
        try {
            ServidorCentral.removerCliente(miUsuario);
            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error al cerrar el socket.");
        }
    }
}