package com.github.stanio.xbrz.awt;

import java.awt.Image;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Encapsulates packed image data in a convenient form for input to {@code Xbrz}.
 */
public final class ImageData {

    private static final int[] ANIMATED_PIXELS = new int[0];

    private static final ColorSpace CS_LINEAR_RGB =
            ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);

    private static final boolean DEBUG = false;

    public final int width;
    public final int height;
    public final boolean hasAlpha;
    final int[] pixels;

    ImageData(BufferedImage image) {
        this(image, false);
    }

    ImageData(BufferedImage image, boolean untracked) {
        width = image.getWidth();
        height = image.getHeight();
        hasAlpha = image.getColorModel().hasAlpha();
        pixels = getRGB(image, untracked);
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

    private static int[] getRGB(BufferedImage image, boolean untracked) {
        int width = image.getWidth();
        int height = image.getHeight();
        int imageType = image.getType();
        ColorSpace colorSpace = image.getColorModel().getColorSpace();
        if ((imageType == BufferedImage.TYPE_INT_ARGB
                        || imageType == BufferedImage.TYPE_INT_RGB)
                && (colorSpace.isCS_sRGB() || colorSpace == CS_LINEAR_RGB)) {
            Raster raster = image.getRaster();
            if (untracked && raster.getDataBuffer().getNumBanks() == 1) {
                return ((DataBufferInt) raster.getDataBuffer()).getData();
            }
            return (int[]) raster.getDataElements(0, 0, width, height, null);
        }
        return image.getRGB(0, 0, width, height, null, 0, width);
    }

    /**
     * Obtains image-data from the given source.
     * <p>
     * Note, the returned data may indicate {@code animated == true} in which case
     * the instance doesn't contain pixel data and can't be used as argument to
     * methods that otherwise accept image-data as input.</p>
     *
     * @param   image  source image to obtain image-data from
     * @return  image-data of the given source, or {@code null} if error occurs
     * @see     #isAnimated()
     */
    public static ImageData get(Image image) {
        if (image instanceof BufferedImage) {
            return new ImageData((BufferedImage) image);
        }

        AtomicReference<Boolean> hasTranslucency = new AtomicReference<>();
        PixelGrabber grabber = new PixelGrabber(image, 0, 0, -1, -1, true) {
            @Override public void setColorModel(ColorModel model) {
                if (model != ColorModel.getRGBdefault()) {
                    hasTranslucency.set(model.hasAlpha());
                }
            }
            @Override public void setPixels(int srcX, int srcY, int srcW, int srcH,
                    ColorModel model, int[] pixels, int srcOff, int srcScan) {
                super.setPixels(srcX, srcY, srcW, srcH, model, pixels, srcOff, srcScan);
                if (hasTranslucency.get() == null) {
                    if (!model.hasAlpha()) {
                        hasTranslucency.set(false);
                        return;
                    }
                    for (int i = 0, len = pixels.length; i < len; i++) {
                        if (model.getAlpha(pixels[i]) < 255) {
                            hasTranslucency.set(true);
                            break;
                        }
                    }
                }
            }
        };

        try {
            if (grabber.grabPixels(60_000L)) {
                return new ImageData(grabber, Boolean.TRUE.equals(hasTranslucency.get()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (DEBUG) {
                e.printStackTrace();
            }
        }
        if (DEBUG) {
            System.out.println("Pixel grabber status: " + grabber.getStatus());
        }
        return null;
    }

    public boolean isAnimated() {
        return pixels == ANIMATED_PIXELS;
    }

}
