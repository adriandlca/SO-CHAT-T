package Controllers;

import Views.VentanaChat;
import Views.VentanaContactos;
import Views.VentanaPrincipal;
import Views.ConversorImagen;
import Views.ManejadorArchivos;
import Views.SonidoNotificacion;
import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

public class ControladorCliente implements VentanaPrincipal.ConexionListener {

    private VentanaPrincipal ventanaPrincipal;
    private VentanaContactos ventanaContactos;
    private Map<String, VentanaChat> chatsAbiertos;
    private Set<String> misGrupos;

    private Conexion conexion;
    private PrintWriter salida;
    private String miUsuario;

    private Map<String, Color> coloresUsuarios;
    private Color[] listaColores = {
            new Color(255, 87, 51), new Color(0, 150, 136), new Color(51, 87, 255),
            new Color(156, 39, 176), new Color(255, 51, 209), new Color(255, 152, 0)
    };
    private int indiceColor = 0;

    public ControladorCliente() {
        chatsAbiertos = new HashMap<>();
        coloresUsuarios = new HashMap<>();
        misGrupos = new HashSet<>();

        SwingUtilities.invokeLater(() -> {
            ventanaPrincipal = new VentanaPrincipal();
            ventanaPrincipal.setConexionListener(this);
            ventanaPrincipal.setVisible(true);
        });
    }

    @Override
    public void onConectar(String ipDestino, int puerto, String usuario) {
        try {
            this.miUsuario = usuario;
            this.conexion = new Conexion(ipDestino, puerto);
            this.salida = new PrintWriter(conexion.socket.getOutputStream(), true);

            salida.println("LOGIN|" + miUsuario);
            HiloCliente hiloEscucha = new HiloCliente(conexion.socket, this);
            new Thread(hiloEscucha).start();

            SwingUtilities.invokeLater(() -> {
                ventanaPrincipal.dispose();
                ventanaContactos = new VentanaContactos(miUsuario);

                ventanaContactos.setContactoListener(contactoSeleccionado -> {
                    if (!contactoSeleccionado.equals(miUsuario)) abrirVentanaChat(contactoSeleccionado, false);
                });

                ventanaContactos.setGrupoListener(new VentanaContactos.GrupoListener() {
                    @Override
                    public void onCrearGrupo(String nombreGrupo, String contrasena) {
                        if (contrasena != null && !contrasena.isEmpty()) {
                            salida.println("CREAR_GRUPO|" + nombreGrupo + "|" + contrasena);
                        } else {
                            salida.println("CREAR_GRUPO|" + nombreGrupo);
                        }
                        misGrupos.add(nombreGrupo);
                        abrirVentanaChat(nombreGrupo, true);
                    }
                    @Override
                    public void onUnirseGrupo(String nombreGrupo, String contrasena) {
                        if (contrasena != null && !contrasena.isEmpty()) {
                            salida.println("UNIRSE_GRUPO|" + nombreGrupo + "|" + contrasena);
                        } else {
                            salida.println("UNIRSE_GRUPO|" + nombreGrupo);
                        }
                    }
                });
                ventanaContactos.setVisible(true);
            });
        } catch (IOException e) {
            javax.swing.JOptionPane.showMessageDialog(ventanaPrincipal, "Error de Conexión", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private Color obtenerColorUsuario(String usuario) {
        if (usuario.equalsIgnoreCase(miUsuario) || usuario.equalsIgnoreCase("Tú")) return new Color(80, 80, 80);
        if (!coloresUsuarios.containsKey(usuario.toLowerCase())) {
            coloresUsuarios.put(usuario.toLowerCase(), listaColores[indiceColor++ % listaColores.length]);
        }
        return coloresUsuarios.get(usuario.toLowerCase());
    }

    /**
     * Genera la clave interna del caché de ventanas de chat, prefijada por tipo
     * para que un contacto y un grupo con el mismo nombre no compartan ventana.
     */
    private String claveChat(String nombreDestino, boolean esGrupo) {
        return (esGrupo ? "grupo_" : "priv_") + nombreDestino;
    }

    /**
     * Reproduce el sonido de notificación solo si la ventana del chat no es la
     * activa. Si el usuario ya tiene el chat al frente, no pita.
     */
    private void notificarMensajeRecibido(VentanaChat chat) {
        if (chat == null || !chat.isActive()) {
            SonidoNotificacion.reproducir();
        }
    }

    public void actualizarListaContactos(String[] usuarios) {
        SwingUtilities.invokeLater(() -> {
            if (ventanaContactos != null) {
                java.util.List<String> listaFiltrada = new java.util.ArrayList<>();
                for (String u : usuarios) if (!u.equals(this.miUsuario) && !u.trim().isEmpty()) listaFiltrada.add(u);
                ventanaContactos.actualizarLista(listaFiltrada.toArray(new String[0]));
            }
        });
    }

    public void actualizarListaGrupos(String[] grupos) {
        SwingUtilities.invokeLater(() -> { if (ventanaContactos != null) ventanaContactos.actualizarListaGrupos(grupos); });
    }

    public void actualizarGruposPrivados(String[] gruposPrivados) {
        SwingUtilities.invokeLater(() -> { if (ventanaContactos != null) ventanaContactos.actualizarGruposPrivados(gruposPrivados); });
    }

    public void grupoUnidoExitosamente(String nombreGrupo) {
        SwingUtilities.invokeLater(() -> {
            misGrupos.add(nombreGrupo);
            abrirVentanaChat(nombreGrupo, true);
        });
    }

    public void errorUnirseGrupo(String nombreGrupo, String mensaje) {
        SwingUtilities.invokeLater(() -> {
            javax.swing.JOptionPane.showMessageDialog(
                    ventanaContactos,
                    mensaje,
                    "No se pudo unir a '" + nombreGrupo + "'",
                    javax.swing.JOptionPane.ERROR_MESSAGE
            );
        });
    }

    /**
     * Abre (o reutiliza) la ventana de chat para un destino concreto.
     * @param esGrupo true si el destino es un grupo, false si es un contacto.
     *                Esto determina tanto la clave del caché como el archivo
     *                de historial y la ruta de envío (MSG vs GRUPOMSG, etc.).
     */
    private void abrirVentanaChat(String nombreDestino, boolean esGrupo) {
        String clave = claveChat(nombreDestino, esGrupo);
        VentanaChat chat = chatsAbiertos.get(clave);

        if (chat == null || !chat.isVisible()) {
            chat = new VentanaChat(miUsuario, nombreDestino, esGrupo);
            VentanaChat ventanaActual = chat;

            // Cargamos historial desde el archivo según el tipo
            java.util.List<HistorialChat.Mensaje> historial = HistorialChat.cargarHistorial(miUsuario, nombreDestino, esGrupo);
            for (HistorialChat.Mensaje msg : historial) {
                String remitente = msg.getRemitente();
                if (remitente == null || remitente.isEmpty()) remitente = nombreDestino;
                Color colorRemitente = obtenerColorUsuario(remitente);
                String textoMsg = msg.getTexto();

                if (textoMsg != null) {
                    if (textoMsg.startsWith("[ARCHIVO]")) {
                        String contenido = textoMsg.substring(9);
                        int divisor = contenido.indexOf("<::>");
                        if (divisor != -1) {
                            String nombreArchivo = contenido.substring(0, divisor);
                            String base64 = contenido.substring(divisor + 4);
                            ventanaActual.mostrarBotonDescargaArchivo(remitente, nombreArchivo, base64, colorRemitente);
                        }
                    } else if (textoMsg.startsWith("[IMAGEN]")) {
                        String base64 = textoMsg.substring(8);
                        ImageIcon imagenRecibida = ConversorImagen.base64ToImageIcon(base64);
                        if (imagenRecibida != null) {
                            ventanaActual.mostrarImagenConColor(remitente, imagenRecibida, colorRemitente);
                        } else {
                            ventanaActual.mostrarMensajeConColor(remitente, "[Error de imagen]", colorRemitente);
                        }
                    } else {
                        ventanaActual.mostrarMensajeConColor(remitente, textoMsg, colorRemitente);
                    }
                }
            }

            final boolean esGrupoCapturado = esGrupo;

            chat.setAccionEscribiendo(escribiendo -> {
                if (esGrupoCapturado) {
                    String comando = escribiendo ? "GRUPO_ESCRIBIENDO" : "GRUPO_NO_ESCRIBIENDO";
                    salida.println(comando + "|" + nombreDestino);
                } else {
                    String comando = escribiendo ? "ESCRIBIENDO" : "NO_ESCRIBIENDO";
                    salida.println(comando + "|" + nombreDestino);
                }
            });

            chat.setAccionEnviar(e -> {
                String mensaje = ventanaActual.getMensajeEscrito();
                if (mensaje.trim().isEmpty()) return;
                String mensajeCodificado = mensaje.replace("\n", "<BR>");

                if (esGrupoCapturado) salida.println("GRUPOMSG|" + nombreDestino + "|" + mensajeCodificado);
                else salida.println("MSG|" + nombreDestino + "|" + mensajeCodificado);

                HistorialChat.guardarMensaje(miUsuario, nombreDestino, "Tú", mensaje, esGrupoCapturado);
                ventanaActual.mostrarMensajeConColor("Tú", mensaje, obtenerColorUsuario("Tú"));
                ventanaActual.limpiarInput();
            });

            chat.setOnImagenSeleccionada(archivoImagen -> {
                try {
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(archivoImagen);
                    if (img != null) {
                        ImageIcon imagenIcon = new ImageIcon(img);
                        String imagenEnTexto = ConversorImagen.imageIconToBase64(imagenIcon);

                        if (imagenEnTexto != null) {
                            String mensajeCodificado = "[IMAGEN]" + imagenEnTexto;
                            if (esGrupoCapturado) salida.println("GRUPOMSG|" + nombreDestino + "|" + mensajeCodificado);
                            else salida.println("MSG|" + nombreDestino + "|" + mensajeCodificado);

                            HistorialChat.guardarMensaje(miUsuario, nombreDestino, "Tú", mensajeCodificado, esGrupoCapturado);
                            ventanaActual.mostrarImagenConColor("Tú", imagenIcon, obtenerColorUsuario("Tú"));
                        }
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            });

            chat.setOnArchivoSeleccionado(archivo -> {
                String base64 = ManejadorArchivos.archivoToBase64(archivo);
                if (base64 != null) {
                    String nombreArchivo = archivo.getName();
                    String mensajeCodificado = "[ARCHIVO]" + nombreArchivo + "<::>" + base64;

                    if (esGrupoCapturado) salida.println("GRUPOMSG|" + nombreDestino + "|" + mensajeCodificado);
                    else salida.println("MSG|" + nombreDestino + "|" + mensajeCodificado);

                    HistorialChat.guardarMensaje(miUsuario, nombreDestino, "Tú", mensajeCodificado, esGrupoCapturado);
                    ventanaActual.mostrarBotonDescargaArchivo("Tú", nombreArchivo, base64, obtenerColorUsuario("Tú"));
                }
            });

            chat.setOnStickerSeleccionado(base64 -> {
                String mensajeCodificado = "[STICKER]" + base64;

                if (esGrupoCapturado) salida.println("GRUPOMSG|" + nombreDestino + "|" + mensajeCodificado);
                else salida.println("MSG|" + nombreDestino + "|" + mensajeCodificado);

                ImageIcon stickerIcon = ConversorImagen.base64ToImageIcon(base64);
                HistorialChat.guardarMensaje(miUsuario, nombreDestino, "Tú", "[Sticker]", esGrupoCapturado);
                ventanaActual.mostrarStickerConColor("Tú", stickerIcon, obtenerColorUsuario("Tú"));
            });

            chatsAbiertos.put(clave, chat);
            chat.setVisible(true);
        } else {
            chat.toFront();
        }
    }

    public void recibirMensaje(String remitente, String mensaje) {
        SwingUtilities.invokeLater(() -> {
            abrirVentanaChat(remitente, false); // privado
            VentanaChat chat = chatsAbiertos.get(claveChat(remitente, false));
            Color colorRemitente = obtenerColorUsuario(remitente);

            if (chat != null) chat.mostrarEscribiendo(false);
            notificarMensajeRecibido(chat);

            if (mensaje.startsWith("[ARCHIVO]")) {
                String contenido = mensaje.substring(9);
                int divisor = contenido.indexOf("<::>");
                if (divisor != -1) {
                    String nombreArchivo = contenido.substring(0, divisor);
                    String base64 = contenido.substring(divisor + 4);

                    HistorialChat.guardarMensaje(miUsuario, remitente, remitente, mensaje, false);
                    chat.mostrarBotonDescargaArchivo(remitente, nombreArchivo, base64, colorRemitente);
                }
            } else if (mensaje.startsWith("[IMAGEN]")) {
                String base64 = mensaje.substring(8);
                ImageIcon imagenRecibida = ConversorImagen.base64ToImageIcon(base64);
                if (imagenRecibida != null) {
                    HistorialChat.guardarMensaje(miUsuario, remitente, remitente, mensaje, false);
                    chat.mostrarImagenConColor(remitente, imagenRecibida, colorRemitente);
                }
            } else if (mensaje.startsWith("[STICKER]")) {
                String base64 = mensaje.substring(9);
                ImageIcon stickerRecibido = ConversorImagen.base64ToImageIcon(base64);
                if (stickerRecibido != null) {
                    HistorialChat.guardarMensaje(miUsuario, remitente, remitente, "[Sticker]", false);
                    chat.mostrarStickerConColor(remitente, stickerRecibido, colorRemitente);
                }
            } else {
                String mensajeDecodificado = mensaje.replace("<BR>", "\n");
                HistorialChat.guardarMensaje(miUsuario, remitente, remitente, mensajeDecodificado, false);
                chat.mostrarMensajeConColor(remitente, mensajeDecodificado, colorRemitente);
            }
        });
    }

    public void recibirMensajeGrupal(String grupo, String remitente, String mensaje) {
        SwingUtilities.invokeLater(() -> {
            misGrupos.add(grupo);
            abrirVentanaChat(grupo, true); // grupal
            VentanaChat chat = chatsAbiertos.get(claveChat(grupo, true));
            Color colorRemitente = obtenerColorUsuario(remitente);

            if (chat != null) chat.mostrarEscribiendoGrupal(remitente, false);
            notificarMensajeRecibido(chat);

            if (mensaje.startsWith("[ARCHIVO]")) {
                String contenido = mensaje.substring(9);
                int divisor = contenido.indexOf("<::>");
                if (divisor != -1) {
                    String nombreArchivo = contenido.substring(0, divisor);
                    String base64 = contenido.substring(divisor + 4);

                    HistorialChat.guardarMensaje(miUsuario, grupo, remitente, mensaje, true);
                    chat.mostrarBotonDescargaArchivo(remitente, nombreArchivo, base64, colorRemitente);
                }
            } else if (mensaje.startsWith("[IMAGEN]")) {
                String base64 = mensaje.substring(8);
                ImageIcon imagenRecibida = ConversorImagen.base64ToImageIcon(base64);
                if (imagenRecibida != null) {
                    HistorialChat.guardarMensaje(miUsuario, grupo, remitente, mensaje, true);
                    chat.mostrarImagenConColor(remitente, imagenRecibida, colorRemitente);
                }
            } else if (mensaje.startsWith("[STICKER]")) {
                String base64 = mensaje.substring(9);
                ImageIcon stickerRecibido = ConversorImagen.base64ToImageIcon(base64);
                if (stickerRecibido != null) {
                    HistorialChat.guardarMensaje(miUsuario, grupo, remitente, "[Sticker]", true);
                    chat.mostrarStickerConColor(remitente, stickerRecibido, colorRemitente);
                }
            } else {
                String mensajeDecodificado = mensaje.replace("<BR>", "\n");
                HistorialChat.guardarMensaje(miUsuario, grupo, remitente, mensajeDecodificado, true);
                chat.mostrarMensajeConColor(remitente, mensajeDecodificado, colorRemitente);
            }
        });
    }

    public void recibirEstadoEscribiendo(String remitente, boolean escribiendo) {
        SwingUtilities.invokeLater(() -> {
            VentanaChat chat = chatsAbiertos.get(claveChat(remitente, false));
            if (chat != null && chat.isVisible()) {
                chat.mostrarEscribiendo(escribiendo);
            }
        });
    }

    public void recibirEstadoEscribiendoGrupal(String grupo, String remitente, boolean escribiendo) {
        SwingUtilities.invokeLater(() -> {
            VentanaChat chat = chatsAbiertos.get(claveChat(grupo, true));
            if (chat != null && chat.isVisible()) {
                chat.mostrarEscribiendoGrupal(remitente, escribiendo);
            }
        });
    }
}
