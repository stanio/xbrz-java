package net.sourceforge.xbrz.awt;

import java.awt.Image;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

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
        Raster raster = image.getRaster();
        int transferType = raster.getTransferType();
        ColorModel colorModel = image.getColorModel();
        ColorSpace colorSpace = colorModel.getColorSpace();
        if (colorModel instanceof DirectColorModel
                && transferType == DataBuffer.TYPE_INT
                && (colorSpace.isCS_sRGB() || colorSpace == CS_LINEAR_RGB)) {
            if (untracked && raster.getDataBuffer().getNumBanks() == 1) {
                return ((DataBufferInt) raster.getDataBuffer()).getData();
            }
            return (int[]) raster.getDataElements(0, 0, width, height, null);
        }
        return image.getRGB(0, 0, width, height, null, 0, width);
    }

    public static ImageData get(Image image) {
        if (image instanceof BufferedImage) {
            return new ImageData((BufferedImage) image);
        }

        AtomicBoolean hasTranslucency = new AtomicBoolean();
        PixelGrabber grabber = new PixelGrabber(image, 0, 0, -1, -1, true) {
            @Override public void setPixels(int srcX, int srcY, int srcW, int srcH,
                    ColorModel model, byte[] pixels, int srcOff, int srcScan) {
                super.setPixels(srcX, srcY, srcW, srcH, model, pixels, srcOff, srcScan);
                if (model.hasAlpha() && !hasTranslucency.get()) {
                    for (int i = 0, len = pixels.length; i < len; i++) {
                        if (model.getAlpha(pixels[i] & 0xFF) < 255) {
                            hasTranslucency.set(true);
                            break;
                        }
                    }
                }
            }
            @Override public void setPixels(int srcX, int srcY, int srcW, int srcH,
                    ColorModel model, int[] pixels, int srcOff, int srcScan) {
                super.setPixels(srcX, srcY, srcW, srcH, model, pixels, srcOff, srcScan);
                if (model.hasAlpha() && !hasTranslucency.get()) {
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
                return new ImageData(grabber, hasTranslucency.get());
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

    public Key getKey() {
        return new Key(width, height, hasAlpha, pixels);
    }

    public static final class Key {

        private final int width;
        private final int height;
        private final boolean hasAlpha;
        private final int pixelDigest;
        private final int hashCode;

        Key(int width, int height, boolean hasAlpha, int[] pixels) {
            this.width = width;
            this.height = height;
            this.hasAlpha = hasAlpha;
            this.pixelDigest = Arrays.hashCode(pixels);
            this.hashCode = Arrays.hashCode(new int[] {
                width, height, Boolean.hashCode(hasAlpha), pixelDigest
            });
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Key) {
                Key other = (Key) obj;
                return pixelDigest == other.pixelDigest
                        && width == other.width
                        && height == other.height
                        && hasAlpha == other.hasAlpha;
            }
            return false;
        }

        @Override
        public String toString() {
            return "Key [width=" + width + ", height=" + height
                    + ", hasAlpha=" + hasAlpha + ", pixelDigest=" + pixelDigest + "]";
        }

    }


}
