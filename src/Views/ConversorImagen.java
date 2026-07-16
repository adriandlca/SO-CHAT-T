package Views;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class ConversorImagen {

    public static String imageIconToBase64(ImageIcon icon) {
        try {
            BufferedImage bi = new BufferedImage(
                    icon.getIconWidth(),
                    icon.getIconHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );
            Graphics g = bi.createGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            byte[] bytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ImageIcon base64ToImageIcon(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            BufferedImage bi = ImageIO.read(bais);
            return new ImageIcon(bi);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}