package Controllers;

import Views.VentanaChatUnificado;
import Views.VentanaPrincipal;
import Views.ChatPanel;
import Views.ConversorImagen;
import Views.ManejadorArchivos;
import Views.SonidoNotificacion;
import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class ControladorCliente implements VentanaPrincipal.ConexionListener {

    private VentanaPrincipal ventanaPrincipal;
    private VentanaChatUnificado ventanaUnificada;

    /** Cache de ChatPanels por clave "priv_<name>" / "grupo_<name>" para evitar regenerar listeners. */
    private final Map<String, ChatPanel> chatsCache = new HashMap<>();

    private final Set<String> misGrupos = new HashSet<>();

    private Conexion conexion;
    private java.io.PrintWriter salida;
    private String miUsuario;

    private final Map<String, Color> coloresUsuarios = new HashMap<>();
    private final Color[] listaColores = {
            new Color(255, 87, 51), new Color(0, 150, 136), new Color(51, 87, 255),
            new Color(156, 39, 176), new Color(255, 51, 209), new Color(255, 152, 0)
    };
    private int indiceColor = 0;

    public ControladorCliente() {
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
            this.salida = conexion.getWriter();

            salida.println("LOGIN|" + miUsuario);
            HiloCliente hiloEscucha = new HiloCliente(conexion.socket, this);
            new Thread(hiloEscucha).start();

            SwingUtilities.invokeLater(() -> {
                ventanaPrincipal.dispose();
                ventanaUnificada = new VentanaChatUnificado(miUsuario);
                ventanaUnificada.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                ventanaUnificada.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override public void windowClosed(java.awt.event.WindowEvent e) {
                        // Al cerrar la ventana unificada, termina el cliente.
                        System.exit(0);
                    }
                });

                ventanaUnificada.setContactoListener(contacto -> {
                    if (!contacto.equals(miUsuario)) abrirConversacion(contacto, false);
                });

                ventanaUnificada.setGrupoListener(new VentanaChatUnificado.GrupoListener() {
                    @Override public void onCrearGrupo(String nombre, String pwd) {
                        try {
                            if (pwd != null && !pwd.isEmpty()) enviarPorRed("CREAR_GRUPO|" + nombre + "|" + pwd);
                            else                              enviarPorRed("CREAR_GRUPO|" + nombre);
                            misGrupos.add(nombre);
                            abrirConversacion(nombre, true);
                        } catch (Exception ex) {
                            System.err.println("[CREAR_GRUPO] " + ex.getMessage());
                        }
                    }
                    @Override public void onUnirseGrupo(String nombre, String pwd) {
                        try {
                            if (pwd != null && !pwd.isEmpty()) enviarPorRed("UNIRSE_GRUPO|" + nombre + "|" + pwd);
                            else                              enviarPorRed("UNIRSE_GRUPO|" + nombre);
                        } catch (Exception ex) {
                            System.err.println("[UNIRSE_GRUPO] " + ex.getMessage());
                        }
                    }
                });

                // Hook CRÍTICO: cada vez que se crea un ChatPanel nuevo (por click en
                // sidebar, por mensaje recibido, por creación de grupo, etc), el
                // controlador wirea sus listeners ANTES de cargar el historial.
                ventanaUnificada.setOnChatCreated(chat -> {
                    try {
                        configurarListenersChat(chat, chat.getContactoDestino(), chat.esGrupo());
                    } catch (Exception ex) {
                        System.err.println("[ON_CHAT_CREATED] " + ex.getMessage());
                    }
                });

                ventanaUnificada.setLogoutListener(() -> {
                    // Cierra sesión saliendo al login
                    ventanaUnificada.dispose();
                    ventanaPrincipal = new VentanaPrincipal();
                    ventanaPrincipal.setConexionListener(this);
                    ventanaPrincipal.setVisible(true);
                });

                ventanaUnificada.setVisible(true);
            });
        } catch (IOException e) {
            JOptionPane.showMessageDialog(ventanaPrincipal, "Error de Conexión", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Color obtenerColorUsuario(String usuario) {
        if (usuario.equalsIgnoreCase(miUsuario) || usuario.equalsIgnoreCase("Tú")) return new Color(80, 80, 80);
        if (!coloresUsuarios.containsKey(usuario.toLowerCase())) {
            coloresUsuarios.put(usuario.toLowerCase(), listaColores[indiceColor++ % listaColores.length]);
        }
        return coloresUsuarios.get(usuario.toLowerCase());
    }

    private String claveChat(String nombre, boolean grupo) { return (grupo ? "grupo_" : "priv_") + nombre; }

    /** Devuelve true si la ventana unificada es la activa en pantalla. */
    private boolean ventanaEstaAlFrente() {
        if (ventanaUnificada == null) return false;
        return ventanaUnificada.isActive() && ventanaUnificada.isVisible();
    }

    private ChatPanel abrirConversacion(String nombreDestino, boolean esGrupo) {
        if (ventanaUnificada == null) return null;
        ChatPanel chat = ventanaUnificada.getChat(nombreDestino, esGrupo);
        if (chat == null) {
            // VentanaChatUnificado abre/crea el ChatPanel y dispara el hook onChatCreated,
            // que ya wirea los listeners (ver setOnChatCreated en onConectar).
            chat = ventanaUnificada.abrirConversacion(nombreDestino, esGrupo);
            chatsCache.put(claveChat(nombreDestino, esGrupo), chat);
        } else {
            ventanaUnificadoMostrar(chat, nombreDestino, esGrupo);
        }
        return chat;
    }

    private void ventanaUnificadoMostrar(ChatPanel chat, String nombre, boolean grupo) {
        if (chat != null) ventanaUnificada.abrirConversacion(nombre, grupo);
    }

    /**
     * Envía un mensaje por red. Si la red falla, igual se muestra el mensaje localmente
     * (UI-first) y se loguea el error. Esto evita que un socket muerto haga parecer
     * que el botón no funciona.
     */
    private void enviarPorRed(String comando) {
        if (salida == null || conexion == null || !conexion.estaConectado()) {
            System.err.println("[NET] sin conexión activa — logueando localmente. cmd=" + comando);
            return;
        }
        try {
            salida.println(comando);
            if (salida.checkError()) {
                System.err.println("[NET] checkError tras escribir '" + comando + "' — conexión posiblemente rota.");
            }
        } catch (Exception ex) {
            System.err.println("[NET] fallo escribiendo '" + comando + "': " + ex.getMessage());
        }
    }

    private void configurarListenersChat(ChatPanel chat, String nombreDestino, boolean esGrupo) {
        final boolean esGrupoF = esGrupo;

        chat.setAccionEscribiendo(escribiendo -> {
            try {
                if (esGrupoF) {
                    String c = escribiendo ? "GRUPO_ESCRIBIENDO" : "GRUPO_NO_ESCRIBIENDO";
                    enviarPorRed(c + "|" + nombreDestino);
                } else {
                    String c = escribiendo ? "ESCRIBIENDO" : "NO_ESCRIBIENDO";
                    enviarPorRed(c + "|" + nombreDestino);
                }
            } catch (Exception ex) {
                System.err.println("[ESCRIBIENDO] " + ex.getMessage());
            }
        });

        chat.setAccionEnviar(e -> {
            try {
                String mensaje = chat.getMensajeEscrito();
                if (mensaje == null || mensaje.trim().isEmpty()) return;

                String mensajeCodificado = mensaje.replace("\n", "<BR>");
                String prefijo = esGrupoF ? "GRUPOMSG" : "MSG";
                String comando = prefijo + "|" + nombreDestino + "|" + mensajeCodificado;

                // 1) SIEMPRE pintar y guardar localmente primero (UI-first).
                HistorialChat.guardarMensaje(miUsuario, nombreDestino, "Tú", mensaje, esGrupoF);
                chat.mostrarMensajeConColor("Tú", mensaje, obtenerColorUsuario("Tú"));

                // 2) Intentar enviar por red (con manejo de error).
                enviarPorRed(comando);

                // 3) Limpiar input siempre (independiente de red).
                chat.limpiarInput();
            } catch (Exception ex) {
                System.err.println("[ENVIAR] " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        chat.setOnImagenSeleccionada(archivo -> {
            try {
                if (archivo == null) return;
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(archivo);
                if (img == null) {
                    JOptionPane.showMessageDialog(ventanaUnificada,
                            "No se pudo leer la imagen: " + archivo.getName(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                ImageIcon imagenIcon = new ImageIcon(img);
                String imagenEnTexto = ConversorImagen.imageIconToBase64(imagenIcon);
                if (imagenEnTexto == null) {
                    JOptionPane.showMessageDialog(ventanaUnificada,
                            "No se pudo codificar la imagen.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String mensaje = "[IMAGEN]" + imagenEnTexto;
                String prefijo = esGrupoF ? "GRUPOMSG" : "MSG";
                String comando = prefijo + "|" + nombreDestino + "|" + mensaje;

                HistorialChat.guardarMensaje(miUsuario, nombreDestino, "Tú", mensaje, esGrupoF);
                chat.mostrarImagenConColor("Tú", imagenIcon, obtenerColorUsuario("Tú"));
                enviarPorRed(comando);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(ventanaUnificada,
                        "Error enviando imagen: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        chat.setOnArchivoSeleccionado(archivo -> {
            try {
                if (archivo == null) return;
                String base64 = ManejadorArchivos.archivoToBase64(archivo);
                if (base64 == null) {
                    JOptionPane.showMessageDialog(ventanaUnificada,
                            "No se pudo codificar el archivo.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String nombreArchivo = archivo.getName();
                String mensaje = "[ARCHIVO]" + nombreArchivo + "<::>" + base64;
                String prefijo = esGrupoF ? "GRUPOMSG" : "MSG";
                String comando = prefijo + "|" + nombreDestino + "|" + mensaje;

                HistorialChat.guardarMensaje(miUsuario, nombreDestino, "Tú", mensaje, esGrupoF);
                chat.mostrarBotonDescargaArchivo("Tú", nombreArchivo, base64, obtenerColorUsuario("Tú"));
                enviarPorRed(comando);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(ventanaUnificada,
                        "Error enviando archivo: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        chat.setOnStickerSeleccionado(base64 -> {
            try {
                if (base64 == null) return;
                String mensaje = "[STICKER]" + base64;
                String prefijo = esGrupoF ? "GRUPOMSG" : "MSG";
                String comando = prefijo + "|" + nombreDestino + "|" + mensaje;

                HistorialChat.guardarMensaje(miUsuario, nombreDestino, "Tú", "[Sticker]", esGrupoF);
                enviarPorRed(comando);
                ImageIcon stickerIcon = ConversorImagen.base64ToImageIcon(base64);
                chat.mostrarStickerConColor("Tú", stickerIcon, obtenerColorUsuario("Tú"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    /* ============================== Listas ============================== */

    public void actualizarListaContactos(String[] usuarios) {
        SwingUtilities.invokeLater(() -> {
            if (ventanaUnificada != null) {
                java.util.List<String> lista = new java.util.ArrayList<>();
                if (usuarios != null) for (String u : usuarios) if (!u.equals(this.miUsuario) && !u.trim().isEmpty()) lista.add(u);
                ventanaUnificada.actualizarLista(lista.toArray(new String[0]));
            }
        });
    }

    public void actualizarListaGrupos(String[] grupos) {
        SwingUtilities.invokeLater(() -> {
            if (ventanaUnificada != null) ventanaUnificada.actualizarListaGrupos(grupos);
        });
    }

    public void actualizarGruposPrivados(String[] gruposPrivados) {
        SwingUtilities.invokeLater(() -> {
            if (ventanaUnificada != null) ventanaUnificada.actualizarGruposPrivados(gruposPrivados);
        });
    }

    public void grupoUnidoExitosamente(String nombreGrupo) {
        SwingUtilities.invokeLater(() -> {
            misGrupos.add(nombreGrupo);
            abrirConversacion(nombreGrupo, true);
        });
    }

    public void errorUnirseGrupo(String nombreGrupo, String mensaje) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                ventanaUnificada, mensaje,
                "No se pudo unir a '" + nombreGrupo + "'",
                JOptionPane.ERROR_MESSAGE));
    }

    /* ============================== Mensajes entrantes ============================== */

    public void recibirMensaje(String remitente, String mensaje) {
        SwingUtilities.invokeLater(() -> {
            ChatPanel chat = abrirConversacion(remitente, false);
            Color colorRemitente = obtenerColorUsuario(remitente);
            if (chat != null) chat.mostrarEscribiendo(false);
            if (!ventanaEstaAlFrente()) SonidoNotificacion.reproducir();

            if (mensaje.startsWith("[ARCHIVO]")) {
                String contenido = mensaje.substring(9);
                int d = contenido.indexOf("<::>");
                if (d != -1) {
                    String nombreArchivo = contenido.substring(0, d);
                    String base64 = contenido.substring(d + 4);
                    HistorialChat.guardarMensaje(miUsuario, remitente, remitente, mensaje, false);
                    chat.mostrarBotonDescargaArchivo(remitente, nombreArchivo, base64, colorRemitente);
                }
            } else if (mensaje.startsWith("[IMAGEN]")) {
                String base64 = mensaje.substring(8);
                ImageIcon img = ConversorImagen.base64ToImageIcon(base64);
                if (img != null) {
                    HistorialChat.guardarMensaje(miUsuario, remitente, remitente, mensaje, false);
                    chat.mostrarImagenConColor(remitente, img, colorRemitente);
                }
            } else if (mensaje.startsWith("[STICKER]")) {
                String base64 = mensaje.substring(9);
                ImageIcon st = ConversorImagen.base64ToImageIcon(base64);
                if (st != null) {
                    HistorialChat.guardarMensaje(miUsuario, remitente, remitente, "[Sticker]", false);
                    chat.mostrarStickerConColor(remitente, st, colorRemitente);
                }
            } else {
                String dec = mensaje.replace("<BR>", "\n");
                HistorialChat.guardarMensaje(miUsuario, remitente, remitente, dec, false);
                chat.mostrarMensajeConColor(remitente, dec, colorRemitente);
            }
        });
    }

    public void recibirMensajeGrupal(String grupo, String remitente, String mensaje) {
        SwingUtilities.invokeLater(() -> {
            misGrupos.add(grupo);
            ChatPanel chat = abrirConversacion(grupo, true);
            Color colorRemitente = obtenerColorUsuario(remitente);
            if (chat != null) chat.mostrarEscribiendoGrupal(remitente, false);
            if (!ventanaEstaAlFrente()) SonidoNotificacion.reproducir();

            if (mensaje.startsWith("[ARCHIVO]")) {
                String contenido = mensaje.substring(9);
                int d = contenido.indexOf("<::>");
                if (d != -1) {
                    String nombreArchivo = contenido.substring(0, d);
                    String base64 = contenido.substring(d + 4);
                    HistorialChat.guardarMensaje(miUsuario, grupo, remitente, mensaje, true);
                    chat.mostrarBotonDescargaArchivo(remitente, nombreArchivo, base64, colorRemitente);
                }
            } else if (mensaje.startsWith("[IMAGEN]")) {
                String base64 = mensaje.substring(8);
                ImageIcon img = ConversorImagen.base64ToImageIcon(base64);
                if (img != null) {
                    HistorialChat.guardarMensaje(miUsuario, grupo, remitente, mensaje, true);
                    chat.mostrarImagenConColor(remitente, img, colorRemitente);
                }
            } else if (mensaje.startsWith("[STICKER]")) {
                String base64 = mensaje.substring(9);
                ImageIcon st = ConversorImagen.base64ToImageIcon(base64);
                if (st != null) {
                    HistorialChat.guardarMensaje(miUsuario, grupo, remitente, "[Sticker]", true);
                    chat.mostrarStickerConColor(remitente, st, colorRemitente);
                }
            } else {
                String dec = mensaje.replace("<BR>", "\n");
                HistorialChat.guardarMensaje(miUsuario, grupo, remitente, dec, true);
                chat.mostrarMensajeConColor(remitente, dec, colorRemitente);
            }
        });
    }

    public void recibirEstadoEscribiendo(String remitente, boolean escribiendo) {
        SwingUtilities.invokeLater(() -> {
            ChatPanel chat = ventanaUnificada == null ? null : ventanaUnificada.getChat(remitente, false);
            if (chat != null) chat.mostrarEscribiendo(escribiendo);
        });
    }

    public void recibirEstadoEscribiendoGrupal(String grupo, String remitente, boolean escribiendo) {
        SwingUtilities.invokeLater(() -> {
            ChatPanel chat = ventanaUnificada == null ? null : ventanaUnificada.getChat(grupo, true);
            if (chat != null) chat.mostrarEscribiendoGrupal(remitente, escribiendo);
        });
    }
}
