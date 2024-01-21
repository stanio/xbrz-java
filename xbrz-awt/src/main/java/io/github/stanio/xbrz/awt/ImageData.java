package io.github.stanio.xbrz.awt;

import static io.github.stanio.xbrz.Xbrz.targetArraySize;

import java.awt.Image;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;

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
        pixels = new int[targetArraySize(source.width, source.height, factor)];
    }

    private static int[] getRGB(BufferedImage image, boolean untracked) {
        int width = image.getWidth();
        int height = image.getHeight();
        int imageType = image.getType();
        ColorSpace colorSpace = image.getColorModel().getColorSpace();
        // Try to extract integer-packed RGB data more efficiently
        if ((imageType == BufferedImage.TYPE_INT_ARGB
                        || imageType == BufferedImage.TYPE_INT_RGB)
                && (colorSpace.isCS_sRGB() || colorSpace == CS_LINEAR_RGB)) {
            Raster raster = image.getRaster();
            if (untracked && hasDirectIntBuffer(raster)) {
                return ((DataBufferInt) raster.getDataBuffer()).getData();
            }
            // Could Raster.transferType differ from Raster.dataBuffer.dataType?
            if (raster.getTransferType() == DataBuffer.TYPE_INT) {
                // Any Raster used to construct a BufferedImage must have minX=0 and minY=0.
                return (int[]) raster.getDataElements(0, 0, width, height, null);
            }
        }
        // All BufferedImage objects have an upper left corner coordinate of (0, 0).
        return image.getRGB(0, 0, width, height, null, 0, width);
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
