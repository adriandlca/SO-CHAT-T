package Views;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import javax.swing.JOptionPane;

public class ManejadorArchivos {

    // Límite de tamaño: 5 Megabytes
    private static final long LIMITE_TAMANO = 5 * 1024 * 1024;

    public static String archivoToBase64(File archivo) {
        try {
            if (archivo.length() > LIMITE_TAMANO) {
                JOptionPane.showMessageDialog(null,
                        "El archivo seleccionado es muy pesado.\nEl límite máximo permitido es de 5 MB.",
                        "Archivo Demasiado Grande",
                        JOptionPane.WARNING_MESSAGE);
                return null;
            }

            byte[] bytes = Files.readAllBytes(archivo.toPath());
            return Base64.getEncoder().encodeToString(bytes);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Ocurrió un error al intentar leer el archivo de tu computadora:\n" + e.getMessage(),
                    "Error de Lectura",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return null;
        }
    }

    public static boolean guardarArchivoManual(String base64, File archivoDestino) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            Files.write(archivoDestino.toPath(), bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Error al guardar el archivo:\n" + e.getMessage(),
                    "Error de Escritura",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }
}