package net.sourceforge.xbrz.awt;

import java.awt.Image;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ImageData {

    private static final int[] ANIMATED_PIXELS = new int[0];

    private static final ColorSpace CS_LINEAR_RGB =
            ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);

    public final int width;
    public final int height;
    public final boolean hasAlpha;
    final int[] pixels;

    ImageData(BufferedImage image) {
        width = image.getWidth();
        height = image.getHeight();
        hasAlpha = image.getColorModel().hasAlpha();
        pixels = getRGB(image);
    }

    ImageData(PixelGrabber image, boolean transparency) {
        width = image.getWidth();
        height = image.getHeight();
        //hasAlpha = image.getColorModel().hasAlpha();
        hasAlpha = transparency;
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

    private static int[] getRGB(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Raster raster = image.getRaster();
        int transferType = raster.getTransferType();
        ColorModel colorModel = image.getColorModel();
        ColorSpace colorSpace = colorModel.getColorSpace();
        if (colorModel instanceof DirectColorModel
                && transferType == DataBuffer.TYPE_INT
                && (colorSpace.isCS_sRGB() || colorSpace == CS_LINEAR_RGB)) {
            return (int[]) raster.getDataElements(0, 0, width, height, null);
        }
        return image.getRGB(0, 0, width, height, null, 0, width);
    }

    public static ImageData get(Image image) {
        if (image instanceof BufferedImage) {
            return new ImageData((BufferedImage) image);
        }

        AtomicBoolean hasAlpha = new AtomicBoolean();
        PixelGrabber grabber = new PixelGrabber(image, 0, 0, -1, -1, true) {
            @Override public void setColorModel(ColorModel model) {
                super.setColorModel(model);
                if (model.hasAlpha()) {
                    hasAlpha.set(true);
                }
            }
        };
        try {
            grabber.grabPixels(60_000L);
            final int successBits = ImageObserver.ALLBITS | ImageObserver.FRAMEBITS;
            if ((grabber.getStatus() & successBits) != 0) {
                return new ImageData(grabber, hasAlpha.get());
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
