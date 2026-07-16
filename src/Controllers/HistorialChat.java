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

    public static void guardarMensaje(String miUsuario, String contacto, String remitente, String mensaje) {
        try {
            File carpeta = new File("historiales");
            if (!carpeta.exists()) {
                carpeta.mkdir(); 
            }

            String nombreArchivo = "chat_" + miUsuario.toLowerCase() + "_" + contacto.toLowerCase() + ".txt";
            File archivo = new File(carpeta, nombreArchivo);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String fechaHora = dtf.format(LocalDateTime.now());

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivo, true))) {
                // Guarda en el disco duro CON fecha y hora
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

    public static List<Mensaje> cargarHistorial(String miUsuario, String contacto) {
        List<Mensaje> historial = new ArrayList<>();
        String nombreArchivo = "chat_" + miUsuario.toLowerCase() + "_" + contacto.toLowerCase() + ".txt";
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