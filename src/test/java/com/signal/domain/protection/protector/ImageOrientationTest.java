package com.signal.domain.protection.protector;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class ImageOrientationTest {

    // 3x2 이미지, (0,0)만 빨강
    private BufferedImage sample() {
        BufferedImage image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFFFF0000);
        return image;
    }

    @Test
    void 방향_1이면_그대로_반환한다() {
        BufferedImage image = sample();
        assertThat(ImageOrientation.apply(image, 1)).isSameAs(image);
    }

    @Test
    void 방향_6은_시계방향_90도_회전이다() {
        BufferedImage result = ImageOrientation.apply(sample(), 6);
        assertThat(result.getWidth()).isEqualTo(2);
        assertThat(result.getHeight()).isEqualTo(3);
        assertThat(result.getRGB(1, 0)).isEqualTo(0xFFFF0000);
    }

    @Test
    void 방향_8은_반시계방향_90도_회전이다() {
        BufferedImage result = ImageOrientation.apply(sample(), 8);
        assertThat(result.getRGB(0, 2)).isEqualTo(0xFFFF0000);
    }

    @Test
    void 방향_3은_180도_회전이다() {
        BufferedImage result = ImageOrientation.apply(sample(), 3);
        assertThat(result.getRGB(2, 1)).isEqualTo(0xFFFF0000);
    }
}
