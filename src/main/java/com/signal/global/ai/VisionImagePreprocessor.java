package com.signal.global.ai;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

/**
 * 비전 LLM 전송 전 이미지를 줄이는 전처리기. Groq는 base64 이미지 4MB, 해상도 제한이 있어 폰으로 찍은
 * 큰 사진은 그대로 보내면 호출이 실패하고 룰 기반 폴백으로 넘어간다. 긴 변이 {@value #MAX_DIMENSION}px를
 * 넘거나 원본이 {@value #MAX_RAW_BYTES}바이트를 넘으면 축소 후 JPEG로 재인코딩한다. 읽을 수 없는
 * 이미지는 원본을 그대로 반환한다(호출부가 실패 처리).
 */
public final class VisionImagePreprocessor {

    static final int MAX_DIMENSION = 1568;
    static final int MAX_RAW_BYTES = 3 * 1024 * 1024;

    public record Prepared(byte[] bytes, String mimeType) {
    }

    private VisionImagePreprocessor() {
    }

    public static Prepared prepare(byte[] content, String mimeType) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(content));
            if (source == null) {
                return new Prepared(content, mimeType);
            }
            int longest = Math.max(source.getWidth(), source.getHeight());
            if (longest <= MAX_DIMENSION && content.length <= MAX_RAW_BYTES) {
                return new Prepared(content, mimeType);
            }

            double scale = Math.min(1.0, (double) MAX_DIMENSION / longest);
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
            BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = resized.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.drawImage(source, 0, 0, width, height, java.awt.Color.WHITE, null);
            } finally {
                graphics.dispose();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpg", out);
            return new Prepared(out.toByteArray(), "image/jpeg");
        } catch (Exception e) {
            return new Prepared(content, mimeType);
        }
    }
}
