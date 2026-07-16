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

    // NUEVO: Diccionario de contraseñas para grupos privados.
    // Si un grupo NO aparece en este mapa, es público. Si aparece, es privado.
    private static ConcurrentHashMap<String, String> grupoContrasenas = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   SERVIDOR CENTRAL DE CHAT INICIADO     ");
        System.out.println("=========================================");
        System.out.println("Escuchando peticiones en el puerto " + PUERTO + "...");

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            while (true) {
                Socket socketCliente = serverSocket.accept();
                System.out.println("[RED] Nueva conexión detectada desde IP: " + socketCliente.getInetAddress().getHostAddress());

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
        broadcastListaUsuarios();
        // Enviamos tanto la lista de grupos como la de grupos privados
        manejador.enviarPaquete(obtenerPaqueteGrupos());
        manejador.enviarPaquete(obtenerPaqueteGruposPrivados());
    }

    public static synchronized void removerCliente(String usuario) {
        if (usuario != null && clientesConectados.containsKey(usuario)) {
            clientesConectados.remove(usuario);
            System.out.println("[INFO] Usuario desconectado: " + usuario);
            broadcastListaUsuarios();
        }
    }

    public static void enviarMensajePrivado(String remitente, String destinatario, String mensaje) {
        ManejadorCliente manejadorDestino = clientesConectados.get(destinatario);

        if (manejadorDestino != null) {
            manejadorDestino.enviarPaquete("MSG|" + remitente + "|" + mensaje);
            System.out.println("[ENRUTAMIENTO] " + remitente + " -> " + destinatario);
        } else {
            System.out.println("[ERROR] Mensaje perdido. " + destinatario + " no está online.");
        }
    }

    public static void enviarEstadoEscribiendo(String remitente, String destinatario, boolean escribiendo) {
        ManejadorCliente manejadorDestino = clientesConectados.get(destinatario);
        if (manejadorDestino != null) {
            String comando = escribiendo ? "ESCRIBIENDO" : "NO_ESCRIBIENDO";
            manejadorDestino.enviarPaquete(comando + "|" + remitente);
        }
    }

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

    /**
     * Crea un grupo. Si la contraseña es null o vacía, el grupo es público;
     * en caso contrario se almacena y se considera privado.
     */
    public static synchronized void crearGrupo(String nombreGrupo, String creador, String contrasena) {
        if (!gruposActivos.containsKey(nombreGrupo)) {
            ArrayList<String> miembros = new ArrayList<>();
            miembros.add(creador);
            gruposActivos.put(nombreGrupo, miembros);

            boolean esPrivado = contrasena != null && !contrasena.isEmpty();
            if (esPrivado) {
                grupoContrasenas.put(nombreGrupo, contrasena);
                System.out.println("[GRUPOS] " + creador + " creó el grupo PRIVADO: " + nombreGrupo);
            } else {
                System.out.println("[GRUPOS] " + creador + " creó el grupo PÚBLICO: " + nombreGrupo);
            }

            broadcastListaGrupos();
        }
    }

    /**
     * Une a un usuario a un grupo, validando la contraseña si el grupo es privado.
     * Envía GRUPO_UNIDO en caso de éxito o ERROR_GRUPO en caso de fallo.
     */
    public static synchronized void unirseGrupo(String nombreGrupo, String usuario, String contrasena) {
        ManejadorCliente cliente = clientesConectados.get(usuario);

        if (!gruposActivos.containsKey(nombreGrupo)) {
            if (cliente != null) {
                cliente.enviarPaquete("ERROR_GRUPO|" + nombreGrupo + "|El grupo no existe");
            }
            return;
        }

        String contrasenaAlmacenada = grupoContrasenas.get(nombreGrupo);
        if (contrasenaAlmacenada != null) {
            if (contrasena == null || !contrasenaAlmacenada.equals(contrasena)) {
                if (cliente != null) {
                    cliente.enviarPaquete("ERROR_GRUPO|" + nombreGrupo + "|Contraseña incorrecta");
                }
                System.out.println("[GRUPOS] " + usuario + " intentó unirse a '" + nombreGrupo + "' con contraseña incorrecta");
                return;
            }
        }

        ArrayList<String> miembros = gruposActivos.get(nombreGrupo);
        if (!miembros.contains(usuario)) {
            miembros.add(usuario);
            System.out.println("[GRUPOS] " + usuario + " se unió a: " + nombreGrupo);
        }

        if (cliente != null) {
            cliente.enviarPaquete("GRUPO_UNIDO|" + nombreGrupo);
        }
    }

    public static boolean esGrupoPrivado(String nombreGrupo) {
        return grupoContrasenas.containsKey(nombreGrupo);
    }

    public static void enviarMensajeGrupal(String remitente, String nombreGrupo, String mensaje) {
        ArrayList<String> miembros = gruposActivos.get(nombreGrupo);
        if (miembros != null) {
            for (String miembro : miembros) {
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
        String paquetePrivados = obtenerPaqueteGruposPrivados();
        for (ManejadorCliente cliente : clientesConectados.values()) {
            cliente.enviarPaquete(paquete);
            cliente.enviarPaquete(paquetePrivados);
        }
    }

    private static String obtenerPaqueteGrupos() {
        if (gruposActivos.isEmpty()) return "LISTA_GRUPOS|";
        return "LISTA_GRUPOS|" + String.join(",", gruposActivos.keySet());
    }

    private static String obtenerPaqueteGruposPrivados() {
        if (grupoContrasenas.isEmpty()) return "LISTA_GRUPOS_PRIVADOS|";
        return "LISTA_GRUPOS_PRIVADOS|" + String.join(",", grupoContrasenas.keySet());
    }
}
