package com.signal.global.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class VisionImagePreprocessorTest {

    private byte[] png(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    void 작은_이미지는_그대로_반환한다() throws Exception {
        byte[] small = png(100, 100);
        VisionImagePreprocessor.Prepared prepared = VisionImagePreprocessor.prepare(small, "image/png");
        assertThat(prepared.bytes()).isSameAs(small);
        assertThat(prepared.mimeType()).isEqualTo("image/png");
    }

    @Test
    void 해상도가_큰_이미지는_긴_변을_맞춰_JPEG로_축소한다() throws Exception {
        VisionImagePreprocessor.Prepared prepared = VisionImagePreprocessor.prepare(png(4000, 3000), "image/png");
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(prepared.bytes()));
        assertThat(prepared.mimeType()).isEqualTo("image/jpeg");
        assertThat(result.getWidth()).isEqualTo(VisionImagePreprocessor.MAX_DIMENSION);
        assertThat(result.getHeight()).isEqualTo(1176);
    }

    @Test
    void 이미지로_읽을_수_없으면_원본을_그대로_반환한다() {
        byte[] junk = {1, 2, 3};
        assertThat(VisionImagePreprocessor.prepare(junk, "image/png").bytes()).isSameAs(junk);
    }
}
