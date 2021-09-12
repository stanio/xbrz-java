package net.sourceforge.xbrz.awt;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;

public final class ImageData {

    private static final int[] ANIMATED_PIXELS = new int[0];

    public final int width;
    public final int height;
    public final boolean hasAlpha;
    final int[] pixels;

    ImageData(BufferedImage image) {
        width = image.getWidth();
        height = image.getHeight();
        hasAlpha = image.getColorModel().hasAlpha();
        pixels = image.getRGB(0, 0, width, height, null, 0, width);
    }

    ImageData(PixelGrabber image) {
        width = image.getWidth();
        height = image.getHeight();
        hasAlpha = image.getColorModel().hasAlpha();
        pixels = (image.getStatus() & ImageObserver.ALLBITS) != 0
                 ? (int[]) image.getPixels()
                 : ANIMATED_PIXELS;
    }

    ImageData(ImageData source, int factor) {
        width = source.width * factor;
        height = source.height * factor;
        hasAlpha = source.hasAlpha;
        pixels = new int[width * height];
    }

    public static ImageData get(Image image) {
        if (image instanceof BufferedImage) {
            return new ImageData((BufferedImage) image);
        }

        PixelGrabber grabber = new PixelGrabber(image, 0, 0, -1, -1, true);
        try {
            grabber.grabPixels(60_000L);
            final int successBits = ImageObserver.ALLBITS | ImageObserver.FRAMEBITS;
            if ((grabber.getStatus() & successBits) != 0) {
                return new ImageData(grabber);
            }
        } catch (@SuppressWarnings("unused") InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    public boolean isAnimated() {
        return pixels == ANIMATED_PIXELS;
    }

}
