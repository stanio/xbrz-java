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
