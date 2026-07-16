package Controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class HiloCliente implements Runnable {
    private Socket socket;
    private ControladorCliente controlador;
    private BufferedReader entrada;

    public HiloCliente(Socket socket, ControladorCliente controlador) {
        this.socket = socket;
        this.controlador = controlador;
        try {
            this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println("Error al iniciar el hilo de escucha.");
        }
    }

    @Override
    public void run() {
        try {
            String paqueteRecibido;
            while ((paqueteRecibido = entrada.readLine()) != null) {
                
                String[] partes = paqueteRecibido.split("\\|", 4);
                String comando = partes[0];

                if (comando.equals("LISTA")) {
                    String data = partes.length > 1 ? partes[1] : "";
                    String[] usuarios = data.isEmpty() ? new String[0] : data.split(",");
                    controlador.actualizarListaContactos(usuarios);

                } else if (comando.equals("MSG") && partes.length == 3) {
                    String remitente = partes[1];
                    String mensaje = partes[2];
                    controlador.recibirMensaje(remitente, mensaje);
                    
                } else if (comando.equals("GRUPOMSG") && partes.length == 4) {
                    String nombreGrupo = partes[1];
                    String remitente = partes[2];
                    String mensaje = partes[3];
                    controlador.recibirMensajeGrupal(nombreGrupo, remitente, mensaje);
                    
                } else if (comando.equals("LISTA_GRUPOS")) {
                    String data = partes.length > 1 ? partes[1] : "";
                    String[] grupos = data.isEmpty() ? new String[0] : data.split(",");
                    controlador.actualizarListaGrupos(grupos);

                } else if (comando.equals("LISTA_GRUPOS_PRIVADOS")) {
                    String data = partes.length > 1 ? partes[1] : "";
                    String[] privados = data.isEmpty() ? new String[0] : data.split(",");
                    controlador.actualizarGruposPrivados(privados);

                } else if (comando.equals("GRUPO_UNIDO") && partes.length >= 2) {
                    String nombreGrupo = partes[1];
                    controlador.grupoUnidoExitosamente(nombreGrupo);

                } else if (comando.equals("ERROR_GRUPO") && partes.length >= 3) {
                    String nombreGrupo = partes[1];
                    String mensaje = partes[2];
                    controlador.errorUnirseGrupo(nombreGrupo, mensaje);

                } else if (comando.equals("ESCRIBIENDO") && partes.length >= 2) {
                    String remitente = partes[1];
                    controlador.recibirEstadoEscribiendo(remitente, true);

                } else if (comando.equals("NO_ESCRIBIENDO") && partes.length >= 2) {
                    String remitente = partes[1];
                    controlador.recibirEstadoEscribiendo(remitente, false);

                } else if (comando.equals("GRUPO_ESCRIBIENDO") && partes.length >= 3) {
                    String nombreGrupo = partes[1];
                    String remitente = partes[2];
                    controlador.recibirEstadoEscribiendoGrupal(nombreGrupo, remitente, true);

                } else if (comando.equals("GRUPO_NO_ESCRIBIENDO") && partes.length >= 3) {
                    String nombreGrupo = partes[1];
                    String remitente = partes[2];
                    controlador.recibirEstadoEscribiendoGrupal(nombreGrupo, remitente, false);
                }
            }
        } catch (IOException e) {
            System.out.println("Desconectado del servidor central.");
        }
    }
}