package io.github.stanio.xbrz.awt;

import static io.github.stanio.xbrz.Xbrz.targetArraySize;

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
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;

import java.util.Objects;
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

    ImageData(int width, int height, boolean hasAlpha, int[] pixels) {
        this.width = width;
        this.height = height;
        this.hasAlpha = hasAlpha;
        this.pixels = Objects.requireNonNull(pixels);
    }

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
        pixels = new int[targetArraySize(source.width, source.height, factor)];
    }

    private static int[] getRGB(BufferedImage image, boolean untracked) {
        return getRGB(image.getRaster(), image.getColorModel(), untracked);
    }

    static int[] getRGB(Raster raster, ColorModel colorModel, boolean untracked) {
        // Try to extract integer-packed RGB data more efficiently
        if (isDefaultRGB(colorModel)) {
            if (untracked && hasDirectIntBuffer(raster)) {
                return ((DataBufferInt) raster.getDataBuffer()).getData();
            }
            // Could Raster.transferType differ from Raster.dataBuffer.dataType?
            if (raster.getTransferType() == DataBuffer.TYPE_INT) {
                return (int[]) raster.getDataElements(raster.getMinX(),
                        raster.getMinY(), raster.getWidth(), raster.getHeight(), null);
            }
        }
        return getRGB(raster, colorModel);
    }

    /**
     * @see     BufferedImage#getRGB(int, int, int, int, int[], int, int)
     */
    private static int[] getRGB(Raster raster, ColorModel colorModel) {
        final int width = raster.getWidth();
        final int height = raster.getHeight();
        final int offset = 0;
        final int scansize = width;
        final int[] rgbArray = new int[offset + height * scansize];

        Object pixel = null;
        for (int y = raster.getMinY(), yend = y + height,
                yoff = offset; y < yend; y++, yoff += scansize) {

            for (int x = raster.getMinX(), xend = x + width,
                    off = yoff; x < xend; x++, off++) {

                pixel = raster.getDataElements(x, y, pixel);
                try {
                    rgbArray[off] = colorModel.getRGB(pixel);
                } catch (RuntimeException e) {
                    throw new IllegalStateException("Raster " + raster
                            + " is incompatible with color model " + colorModel, e);
                }
            }
        }
        return rgbArray;
    }

    private static boolean hasDirectIntBuffer(Raster raster) {
        // Raster origin should map to the beginning of the data
        // buffer and there should be no padding to the scanlines
        SampleModel sampleModel = raster.getSampleModel();
        DataBuffer dataBuffer = raster.getDataBuffer();
        return raster.getMinX() - raster.getSampleModelTranslateX() == 0
                && raster.getMinY() - raster.getSampleModelTranslateY() == 0
                && sampleModel instanceof SinglePixelPackedSampleModel
                && ((SinglePixelPackedSampleModel) sampleModel).getScanlineStride() == raster.getWidth()
                // Though DataBuffer.dataType == TYPE_INT, the actual class
                // may not be a DataBufferInt subclass, f.e. DataBufferNative
                && dataBuffer instanceof DataBufferInt
                && dataBuffer.getNumBanks() == 1
                && dataBuffer.getOffset() == 0;
    }

    private static boolean isDefaultRGB(ColorModel colorModel) {
        if (colorModel instanceof DirectColorModel) {
            return colorModel.getTransferType() == DataBuffer.TYPE_INT
                    && !colorModel.isAlphaPremultiplied()
                    && isDefaultRGB(colorModel.getColorSpace());
        }
        return false;
    }

    private static boolean isDefaultRGB(ColorSpace colorSpace) {
        // For the time being, treat both of sRGB and linear RGB the same
        return colorSpace.isCS_sRGB() || colorSpace == CS_LINEAR_RGB;
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
