package Controllers;

import Views.VentanaChat;
import Views.VentanaContactos;
import Views.VentanaPrincipal;
import Views.ConversorImagen;
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
        new Color(255, 87, 51),   
        new Color(0, 150, 136),   
        new Color(51, 87, 255),   
        new Color(156, 39, 176),  
        new Color(255, 51, 209),  
        new Color(255, 152, 0)    
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
                    if (!contactoSeleccionado.equals(miUsuario)) {
                        abrirVentanaChat(contactoSeleccionado);
                    }
                });

                ventanaContactos.setGrupoListener(new VentanaContactos.GrupoListener() {
                    @Override
                    public void onCrearGrupo(String nombreGrupo) {
                        misGrupos.add(nombreGrupo);
                        salida.println("CREAR_GRUPO|" + nombreGrupo);
                        abrirVentanaChat(nombreGrupo);
                    }
                    @Override
                    public void onUnirseGrupo(String nombreGrupo) {
                        misGrupos.add(nombreGrupo);
                        salida.println("UNIRSE_GRUPO|" + nombreGrupo);
                        abrirVentanaChat(nombreGrupo);
                    }
                });

                ventanaContactos.setVisible(true);
            });

        } catch (IOException e) {
            javax.swing.JOptionPane.showMessageDialog(ventanaPrincipal,
                    "No se pudo conectar al servidor en " + ipDestino + ":" + puerto,
                    "Error de Conexión", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private Color obtenerColorUsuario(String usuario) {
        if (usuario.equalsIgnoreCase(miUsuario) || usuario.equalsIgnoreCase("Tú")) {
            return new Color(80, 80, 80); 
        }
        
        if (!coloresUsuarios.containsKey(usuario.toLowerCase())) {
            Color colorAsignado = listaColores[indiceColor % listaColores.length];
            coloresUsuarios.put(usuario.toLowerCase(), colorAsignado);
            indiceColor++;
        }
        return coloresUsuarios.get(usuario.toLowerCase());
    }

    public void actualizarListaContactos(String[] usuarios) {
        SwingUtilities.invokeLater(() -> {
            if (ventanaContactos != null) {
                java.util.List<String> listaFiltrada = new java.util.ArrayList<>();
                for (String u : usuarios) {
                    if (!u.equals(this.miUsuario) && !u.trim().isEmpty()) {
                        listaFiltrada.add(u);
                    }
                }
                ventanaContactos.actualizarLista(listaFiltrada.toArray(new String[0]));
            }
        });
    }

    public void actualizarListaGrupos(String[] grupos) {
        SwingUtilities.invokeLater(() -> {
            if (ventanaContactos != null) ventanaContactos.actualizarListaGrupos(grupos);
        });
    }

    private void abrirVentanaChat(String nombreDestino) {
        VentanaChat chat = chatsAbiertos.get(nombreDestino);

        if (chat == null || !chat.isVisible()) {
            chat = new VentanaChat(nombreDestino);
            VentanaChat ventanaActual = chat; 

            java.util.List<HistorialChat.Mensaje> historial = HistorialChat.cargarHistorial(miUsuario, nombreDestino);
            for (HistorialChat.Mensaje msg : historial) {
                String remitente = msg.getRemitente();
                if (remitente == null || remitente.isEmpty()) {
                    remitente = nombreDestino;
                }
                Color colorRemitente = obtenerColorUsuario(remitente);
                ventanaActual.mostrarMensajeConColor(remitente, msg.getTexto(), colorRemitente);
            }

            // ACCIÓN: ENVIAR TEXTO
            chat.setAccionEnviar(e -> {
                String mensaje = ventanaActual.getMensajeEscrito();
                if (mensaje.trim().isEmpty()) return; 

                String mensajeCodificado = mensaje.replace("\n", "<BR>");

                if (misGrupos.contains(nombreDestino)) {
                    salida.println("GRUPOMSG|" + nombreDestino + "|" + mensajeCodificado);
                } else {
                    salida.println("MSG|" + nombreDestino + "|" + mensajeCodificado);
                }
                
                HistorialChat.guardarMensaje(miUsuario, nombreDestino, "Tú", mensaje);
                Color colorTu = obtenerColorUsuario("Tú");
                ventanaActual.mostrarMensajeConColor("Tú", mensaje, colorTu);
                ventanaActual.limpiarInput();
            });

            // ACCIÓN: ENVIAR IMAGEN
            chat.setOnImagenSeleccionada(imagenIcon -> {
                String imagenEnTexto = ConversorImagen.imageIconToBase64(imagenIcon);
                if (imagenEnTexto != null) {
                    String mensajeCodificado = "[IMAGEN]" + imagenEnTexto;
                    
                    if (misGrupos.contains(nombreDestino)) {
                        salida.println("GRUPOMSG|" + nombreDestino + "|" + mensajeCodificado);
                    } else {
                        salida.println("MSG|" + nombreDestino + "|" + mensajeCodificado);
                    }
                    
                    HistorialChat.guardarMensaje(miUsuario, nombreDestino, "Tú", "[Imagen adjunta]");
                    Color colorTu = obtenerColorUsuario("Tú");
                    ventanaActual.mostrarImagenConColor("Tú", imagenIcon, colorTu);
                }
            });

            chatsAbiertos.put(nombreDestino, chat);
            chat.setVisible(true);
        } else {
            chat.toFront();
        }
    }

    public void recibirMensaje(String remitente, String mensaje) {
        SwingUtilities.invokeLater(() -> {
            abrirVentanaChat(remitente); 
            VentanaChat chat = chatsAbiertos.get(remitente);
            Color colorRemitente = obtenerColorUsuario(remitente);
            
            if (mensaje.startsWith("[IMAGEN]")) {
                String base64 = mensaje.substring(8); // Quita el "[IMAGEN]"
                ImageIcon imagenRecibida = ConversorImagen.base64ToImageIcon(base64);
                if (imagenRecibida != null) {
                    HistorialChat.guardarMensaje(miUsuario, remitente, remitente, "[Imagen adjunta]");
                    chat.mostrarImagenConColor(remitente, imagenRecibida, colorRemitente);
                }
            } else {
                String mensajeDecodificado = mensaje.replace("<BR>", "\n");
                HistorialChat.guardarMensaje(miUsuario, remitente, remitente, mensajeDecodificado);
                chat.mostrarMensajeConColor(remitente, mensajeDecodificado, colorRemitente);
            }
        });
    }

    public void recibirMensajeGrupal(String grupo, String remitente, String mensaje) {
        SwingUtilities.invokeLater(() -> {
            misGrupos.add(grupo); 
            abrirVentanaChat(grupo); 
            VentanaChat chat = chatsAbiertos.get(grupo);
            Color colorRemitente = obtenerColorUsuario(remitente);
            
            if (mensaje.startsWith("[IMAGEN]")) {
                String base64 = mensaje.substring(8); // Quita el "[IMAGEN]"
                ImageIcon imagenRecibida = ConversorImagen.base64ToImageIcon(base64);
                if (imagenRecibida != null) {
                    HistorialChat.guardarMensaje(miUsuario, grupo, remitente, "[Imagen adjunta]");
                    chat.mostrarImagenConColor(remitente, imagenRecibida, colorRemitente);
                }
            } else {
                String mensajeDecodificado = mensaje.replace("<BR>", "\n");
                HistorialChat.guardarMensaje(miUsuario, grupo, remitente, mensajeDecodificado);
                chat.mostrarMensajeConColor(remitente, mensajeDecodificado, colorRemitente);
            }
        });
    }
}