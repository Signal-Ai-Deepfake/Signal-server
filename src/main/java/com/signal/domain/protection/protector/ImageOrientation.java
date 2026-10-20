package com.signal.domain.protection.protector;

import java.awt.image.BufferedImage;

/** EXIF Orientation(1~8) 값을 픽셀에 반영해 올바른 방향의 이미지로 되돌린다. */
final class ImageOrientation {

    private ImageOrientation() {
    }

    static BufferedImage apply(BufferedImage source, int orientation) {
        if (orientation < 2 || orientation > 8) {
            return source;
        }
        int w = source.getWidth();
        int h = source.getHeight();
        boolean swap = orientation >= 5;
        BufferedImage result = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int nx;
                int ny;
                switch (orientation) {
                    case 2 -> { nx = w - 1 - x; ny = y; }
                    case 3 -> { nx = w - 1 - x; ny = h - 1 - y; }
                    case 4 -> { nx = x; ny = h - 1 - y; }
                    case 5 -> { nx = y; ny = x; }
                    case 6 -> { nx = h - 1 - y; ny = x; }
                    case 7 -> { nx = h - 1 - y; ny = w - 1 - x; }
                    default -> { nx = y; ny = w - 1 - x; }
                }
                result.setRGB(nx, ny, source.getRGB(x, y));
            }
        }
        return result;
    }
}
