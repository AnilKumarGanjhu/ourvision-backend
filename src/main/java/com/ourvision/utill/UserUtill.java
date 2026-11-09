package com.ourvision.utill;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Component
public class UserUtill {

    /**
     * WebP data URL: frontend ne already resize kiya (400px), as-is return karo.
     * JPEG/PNG data URL: resize karke JPEG thumbnail banao.
     *
     * @param dataUrl "data:image/webp;base64,..." ya "data:image/jpeg;base64,..."
     * @param maxSize max width/height in pixels
     * @return data URL (WebP as-is, JPEG/PNG → JPEG thumbnail), ya null
     */
    public String toThumbnailBase64(String dataUrl, int maxSize) {

        // -------- 1. Null / blank --------
        if (dataUrl == null || dataUrl.isBlank()) {
            return null;
        }

        try {
            String[] parts = dataUrl.split(",", 2);

            // -------- 2. WebP → as-is return --------
            if (parts.length > 1 && parts[0].contains("image/webp")) {
                return dataUrl;
            }

            // -------- 3. JPEG / PNG → thumbnail --------
            String base64 = parts.length > 1 ? parts[1] : parts[0];
            byte[] bytes = Base64.getDecoder().decode(base64);

            BufferedImage original = ImageIO.read(new ByteArrayInputStream(bytes));
            if (original == null) {
                // Unsupported (e.g. WebP without prefix) — return as-is
                return dataUrl;
            }

            // -------- 4. Resize --------
            int w = original.getWidth();
            int h = original.getHeight();

            double scale = Math.min((double) maxSize / w, (double) maxSize / h);
            if (scale > 1) scale = 1;

            int newW = Math.max(1, (int) (w * scale));
            int newH = Math.max(1, (int) (h * scale));

            BufferedImage thumb = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumb.createGraphics();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, newW, newH);
                g.drawImage(
                    original.getScaledInstance(newW, newH, Image.SCALE_SMOOTH),
                    0, 0, null
                );
            } finally {
                g.dispose();
            }

            // -------- 5. Encode JPEG --------
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(thumb, "jpg", out);

            return "data:image/jpeg;base64,"
                    + Base64.getEncoder().encodeToString(out.toByteArray());

        } catch (Exception e) {
            System.err.println("Thumbnail generation failed: " + e.getMessage());
            return dataUrl;   // fallback: return original
        }
    }
}