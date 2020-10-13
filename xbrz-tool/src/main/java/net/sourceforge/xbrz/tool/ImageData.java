package net.sourceforge.xbrz.tool;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;

public final class ImageData {

    public final int width;
    public final int height;
    public final boolean hasAlpha;
    final int[] pixels;

    ImageData(BufferedImage image) {
        width = image.getWidth();
        height = image.getHeight();
        hasAlpha = image.getColorModel().hasAlpha();
        pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
    }

    ImageData(PixelGrabber image) {
        width = image.getWidth();
        height = image.getHeight();
        hasAlpha = image.getColorModel().hasAlpha();
        pixels = (int[]) image.getPixels();
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
            grabber.grabPixels();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("ImageData.get: " + e);
            return null;
        }
        if ((grabber.getStatus() & ImageObserver.ABORT) != 0) {
            System.err.println("ImageData.get: fetch aborted or errored");
            return null;
        }
        if ((grabber.getStatus() & ImageObserver.FRAMEBITS) != 0) {
            // No support for animated images.
            return null;
        }
        return new ImageData(grabber);
    }

}
