package com.dawn.common.core.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
public final class BarcodeUtils {

    private BarcodeUtils() {}

    public static String generateCode128(String text, int width, int height) {
        try {
            Code128Writer barcodeWriter = new Code128Writer();
            BitMatrix bitMatrix = barcodeWriter.encode(text, BarcodeFormat.CODE_128, width, height);

            MatrixToImageConfig config = new MatrixToImageConfig(MatrixToImageConfig.BLACK, MatrixToImageConfig.WHITE);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

            int textHeight = Math.max(15, height / 5);
            int totalHeight = height + textHeight;
            int fontSize = textHeight - 4;
//            Create new image
            BufferedImage combined = new BufferedImage(width, totalHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = combined.createGraphics();

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//            Draw white background
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, totalHeight);

//            Draw barcode
            g.drawImage(image, 0, 0, width, height, null);

//            Draw number
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, fontSize));

//            Calculate to center
            FontMetrics fm = g.getFontMetrics();
            int x = (width - fm.stringWidth(text)) / 2;
            int y = height + (textHeight + fm.getAscent()) / 2 - 2;

            g.drawString(text, x, y);
            g.dispose();


            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(combined, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException ex) {
            log.error("Barcode generation failed", ex);
            return null;
        }
    }
}
