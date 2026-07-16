package Views;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.File;

public class SonidoNotificacion {

    private static final String CARPETA = "sonido-notificacion";
    private static final String ARCHIVO = "notificacion.wav";

    private static final float SAMPLE_RATE = 44100f;
    private static final float FRECUENCIA = 880f;
    private static final float DURACION = 0.18f;

    public static void reproducir() {
        new Thread(() -> {
            File archivo = new File(CARPETA, ARCHIVO);
            if (archivo.exists() && archivo.isFile() && archivo.length() > 0) {
                try {
                    reproducirArchivo(archivo);
                    return;
                } catch (Exception ignored) {
                }
            }
            reproducirTono();
        }, "hilo-sonido-chat").start();
    }

    private static void reproducirArchivo(File archivo) throws Exception {
        SourceDataLine linea = null;
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(archivo)) {
            AudioFormat formato = ais.getFormat();
            linea = AudioSystem.getSourceDataLine(formato);
            linea.open(formato);
            linea.start();

            byte[] chunk = new byte[4096];
            int leido;
            while ((leido = ais.read(chunk)) != -1) {
                linea.write(chunk, 0, leido);
            }
            linea.drain();
            linea.stop();
        } finally {
            if (linea != null) {
                try { linea.close(); } catch (Exception ignored) {}
            }
        }
    }

    private static void reproducirTono() {
        SourceDataLine linea = null;
        try {
            int numMuestras = (int) (SAMPLE_RATE * DURACION);
            byte[] buffer = new byte[numMuestras * 2];

            for (int i = 0; i < numMuestras; i++) {
                double t = i / SAMPLE_RATE;
                double ataque = Math.min(1.0, t / 0.01);
                double caida  = Math.min(1.0, (DURACION - t) / 0.05);
                double envolvente = Math.max(0.0, Math.min(ataque, caida));
                short muestra = (short) (Math.sin(2 * Math.PI * FRECUENCIA * t)
                        * envolvente * Short.MAX_VALUE * 0.4);
                buffer[i * 2]     = (byte) (muestra & 0xff);
                buffer[i * 2 + 1] = (byte) ((muestra >> 8) & 0xff);
            }

            AudioFormat formato = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            linea = AudioSystem.getSourceDataLine(formato);
            linea.open(formato);
            linea.start();
            linea.write(buffer, 0, buffer.length);
            linea.drain();
            linea.stop();
        } catch (Exception ignored) {
        } finally {
            if (linea != null) {
                try { linea.close(); } catch (Exception ignored) {}
            }
        }
    }
}
