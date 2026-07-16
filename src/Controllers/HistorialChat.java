package Controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HistorialChat {

    /**
     * Guarda un mensaje en el archivo de historial.
     * @param esGrupo indica si el chat es grupal. Si es true usa chat_grupo_*.txt,
     *                si es false usa chat_priv_*.txt. Esto evita que el historial
     *                de un contacto y un grupo con el mismo nombre se mezclen.
     */
    public static void guardarMensaje(String miUsuario, String contacto, String remitente, String mensaje, boolean esGrupo) {
        try {
            File carpeta = new File("historiales");
            if (!carpeta.exists()) {
                carpeta.mkdir();
            }

            String prefijo = esGrupo ? "grupo_" : "priv_";
            String nombreArchivo = "chat_" + prefijo + miUsuario.toLowerCase() + "_" + contacto.toLowerCase() + ".txt";
            File archivo = new File(carpeta, nombreArchivo);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String fechaHora = dtf.format(LocalDateTime.now());

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivo, true))) {
                bw.write("[" + fechaHora + "] " + remitente + ": " + mensaje);
                bw.newLine();
            }

        } catch (IOException e) {
            System.err.println("Error al guardar el historial de chat: " + e.getMessage());
        }
    }

    public static class Mensaje {
        private final String remitente;
        private String texto;

        public Mensaje(String remitente, String texto) {
            this.remitente = remitente;
            this.texto = texto;
        }

        public String getRemitente() {
            return remitente;
        }

        public String getTexto() {
            return texto;
        }

        public void setTexto(String texto) {
            this.texto = texto;
        }
    }

    /**
     * Busca mensajes dentro del archivo de historial cuyo contenido (después del
     * remitente) contenga el filtro indicado. La búsqueda ignora mayúsculas /
     * minúsculas y solo revisa el texto del mensaje, no el nombre del remitente.
     * @return líneas completas con formato "[fecha hora] Remitente: mensaje"
     */
    public static List<String> buscarMensajes(String miUsuario, String contacto, String filtro, boolean esGrupo) {
        List<String> resultados = new ArrayList<>();
        if (filtro == null || filtro.trim().isEmpty()) return resultados;

        String prefijo = esGrupo ? "grupo_" : "priv_";
        String nombreArchivo = "chat_" + prefijo + miUsuario.toLowerCase() + "_" + contacto.toLowerCase() + ".txt";
        File archivo = new File("historiales", nombreArchivo);

        if (archivo.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
                String linea;
                String filtroLower = filtro.toLowerCase();
                while ((linea = br.readLine()) != null) {
                    int idxCierre = linea.indexOf("] ");
                    if (idxCierre != -1) {
                        String resto = linea.substring(idxCierre + 2);
                        int idxDosPuntos = resto.indexOf(": ");

                        if (idxDosPuntos != -1) {
                            String contenidoMensaje = resto.substring(idxDosPuntos + 2);
                            if (contenidoMensaje.toLowerCase().contains(filtroLower)) {
                                resultados.add(linea.replace("<BR>", " "));
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error al buscar en el historial: " + e.getMessage());
            }
        }
        return resultados;
    }

    /**
     * Carga el historial de un chat. Si esGrupo=true lee el archivo de grupo,
     * si es false lee el archivo privado. Mantiene separados los históricos.
     */
    public static List<Mensaje> cargarHistorial(String miUsuario, String contacto, boolean esGrupo) {
        List<Mensaje> historial = new ArrayList<>();
        String prefijo = esGrupo ? "grupo_" : "priv_";
        String nombreArchivo = "chat_" + prefijo + miUsuario.toLowerCase() + "_" + contacto.toLowerCase() + ".txt";
        File archivo = new File("historiales", nombreArchivo);

        if (archivo.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
                String linea;
                Mensaje ultimoMensaje = null;
                while ((linea = br.readLine()) != null) {
                    int indiceCierre = linea.indexOf("] ");

                    if (linea.startsWith("[") && indiceCierre != -1) {
                        String resto = linea.substring(indiceCierre + 2);
                        int colonIndex = resto.indexOf(": ");
                        if (colonIndex != -1) {
                            String remitente = resto.substring(0, colonIndex);
                            String texto = resto.substring(colonIndex + 2);
                            ultimoMensaje = new Mensaje(remitente, texto);
                            historial.add(ultimoMensaje);
                        } else {
                            ultimoMensaje = new Mensaje("", resto);
                            historial.add(ultimoMensaje);
                        }
                    } else {
                        if (ultimoMensaje != null) {
                            ultimoMensaje.setTexto(ultimoMensaje.getTexto() + "\n" + linea);
                        } else {
                            ultimoMensaje = new Mensaje("", linea);
                            historial.add(ultimoMensaje);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error al leer el historial de chat: " + e.getMessage());
            }
        }
        return historial;
    }
}
