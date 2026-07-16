package Servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorCentral {
    private static final int PUERTO = 5050;

    // Diccionario seguro para múltiples hilos: [NombreUsuario -> Su Conexión Activa]
    private static ConcurrentHashMap<String, ManejadorCliente> clientesConectados = new ConcurrentHashMap<>();
    
    // Diccionario para chats grupales: [NombreDelGrupo -> Lista de Nombres de Usuarios]
    private static ConcurrentHashMap<String, ArrayList<String>> gruposActivos = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   SERVIDOR CENTRAL DE CHAT INICIADO     ");
        System.out.println("=========================================");
        System.out.println("Escuchando peticiones en el puerto " + PUERTO + "...");

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            while (true) {
                // El servidor se queda pausado aquí hasta que un cliente intente conectar
                Socket socketCliente = serverSocket.accept();
                System.out.println("[RED] Nueva conexión detectada desde IP: " + socketCliente.getInetAddress().getHostAddress());

                // Delegamos a ese cliente a un trabajador (hilo) independiente
                ManejadorCliente manejador = new ManejadorCliente(socketCliente);
                new Thread(manejador).start();
            }
        } catch (IOException e) {
            System.err.println("Error crítico en el servidor: " + e.getMessage());
        }
    }

    // --- MÉTODOS DE ENRUTAMIENTO Y GESTIÓN ---

    public static synchronized void registrarCliente(String usuario, ManejadorCliente manejador) {
        clientesConectados.put(usuario, manejador);
        System.out.println("[INFO] Usuario registrado: " + usuario);
        // Avisamos a toda la red que alguien nuevo entró
        broadcastListaUsuarios();
        // Le enviamos la lista de grupos activos al usuario que acaba de entrar
        manejador.enviarPaquete(obtenerPaqueteGrupos());
    }

    public static synchronized void removerCliente(String usuario) {
        if (usuario != null && clientesConectados.containsKey(usuario)) {
            clientesConectados.remove(usuario);
            System.out.println("[INFO] Usuario desconectado: " + usuario);
            broadcastListaUsuarios();
        }
    }

    public static void enviarMensajePrivado(String remitente, String destinatario, String mensaje) {
        // Buscamos si el destinatario está conectado
        ManejadorCliente manejadorDestino = clientesConectados.get(destinatario);

        if (manejadorDestino != null) {
            manejadorDestino.enviarPaquete("MSG|" + remitente + "|" + mensaje);
            System.out.println("[ENRUTAMIENTO] " + remitente + " -> " + destinatario);
        } else {
            System.out.println("[ERROR] Mensaje perdido. " + destinatario + " no está offline.");
        }
    }

    // Reenvía el aviso "escribiendo" / "dejó de escribir" al chat privado correspondiente
    public static void enviarEstadoEscribiendo(String remitente, String destinatario, boolean escribiendo) {
        ManejadorCliente manejadorDestino = clientesConectados.get(destinatario);
        if (manejadorDestino != null) {
            String comando = escribiendo ? "ESCRIBIENDO" : "NO_ESCRIBIENDO";
            manejadorDestino.enviarPaquete(comando + "|" + remitente);
        }
    }

    // Reenvía el aviso "escribiendo" grupal a todos los integrantes excepto a quien escribe
    public static void enviarEstadoEscribiendoGrupal(String remitente, String nombreGrupo, boolean escribiendo) {
        ArrayList<String> miembros = gruposActivos.get(nombreGrupo);
        if (miembros != null) {
            String comando = escribiendo ? "GRUPO_ESCRIBIENDO" : "GRUPO_NO_ESCRIBIENDO";
            for (String miembro : miembros) {
                if (!miembro.equals(remitente)) {
                    ManejadorCliente manejadorDestino = clientesConectados.get(miembro);
                    if (manejadorDestino != null) {
                        manejadorDestino.enviarPaquete(comando + "|" + nombreGrupo + "|" + remitente);
                    }
                }
            }
        }
    }

    private static synchronized void broadcastListaUsuarios() {
        String nombres = String.join(",", clientesConectados.keySet());
        String paqueteLista = clientesConectados.isEmpty() ? "LISTA|" : "LISTA|" + nombres;

        for (ManejadorCliente cliente : clientesConectados.values()) {
            cliente.enviarPaquete(paqueteLista);
        }
    }

    // --- MÉTODOS DE ENRUTAMIENTO Y GESTIÓN GRUPAL ---

    public static synchronized void crearGrupo(String nombreGrupo, String creador) {
        if (!gruposActivos.containsKey(nombreGrupo)) {
            ArrayList<String> miembros = new ArrayList<>();
            miembros.add(creador);
            gruposActivos.put(nombreGrupo, miembros);
            System.out.println("[GRUPOS] " + creador + " creó el grupo: " + nombreGrupo);
            // Actualizamos a todos los clientes para que vean el nuevo grupo en la interfaz
            broadcastListaGrupos();
        }
    }

    public static synchronized void unirseGrupo(String nombreGrupo, String usuario) {
        if (gruposActivos.containsKey(nombreGrupo)) {
            ArrayList<String> miembros = gruposActivos.get(nombreGrupo);
            if (!miembros.contains(usuario)) {
                miembros.add(usuario);
                System.out.println("[GRUPOS] " + usuario + " se unió a: " + nombreGrupo);
            }
        }
    }

    public static void enviarMensajeGrupal(String remitente, String nombreGrupo, String mensaje) {
        ArrayList<String> miembros = gruposActivos.get(nombreGrupo);
        if (miembros != null) {
            for (String miembro : miembros) {
                // No rebotamos el mensaje al mismo que lo envió
                if (!miembro.equals(remitente)) {
                    ManejadorCliente manejadorDestino = clientesConectados.get(miembro);
                    if (manejadorDestino != null) {
                        manejadorDestino.enviarPaquete("GRUPOMSG|" + nombreGrupo + "|" + remitente + "|" + mensaje);
                    }
                }
            }
            System.out.println("[ENRUTAMIENTO GRUPAL] " + remitente + " -> Grupo [" + nombreGrupo + "]");
        }
    }

    private static synchronized void broadcastListaGrupos() {
        String paquete = obtenerPaqueteGrupos();
        for (ManejadorCliente cliente : clientesConectados.values()) {
            cliente.enviarPaquete(paquete);
        }
    }

    private static String obtenerPaqueteGrupos() {
        if (gruposActivos.isEmpty()) return "LISTA_GRUPOS|";
        return "LISTA_GRUPOS|" + String.join(",", gruposActivos.keySet());
    }
}