package io.github.stanio.xbrz.awt;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageFilter;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.ImageProducer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import io.github.stanio.xbrz.Xbrz;
import io.github.stanio.xbrz.awt.util.SmoothResizeOp;

/**
 * AWT image resizing functions using xBRZ.
 *
 * @see  Xbrz
 */
public final class AwtXbrz {

    private static final ColorModel RGB_OPAQUE =
            new DirectColorModel(24, 0x00FF0000, 0x0000FF00, 0x000000FF, 0);

    private AwtXbrz() { /* no instances */ }

    public static BufferedImage scaleImage(ImageData source, int factor) {
        return scaleImage(source, factor, false);
    }

    /**
     * Scales the given pixel data by the specified factor.
     * <p>
     * The {@code untrackedData} parameter controls whether the result image
     * will be backed by a {@linkplain DataBufferInt##optimizations <i>tracked</i>
     * data buffer} or not.  Having a <i>tracked</i> data buffer will likely
     * benefit on-screen display performance, while it may incur some
     * initialization slow down.  If you only need to process images off
     * screen, f.e. read a source image, scale it, and save the result, then
     * you may specify {@code untracked = true} to save up on result
     * initialization cost.</p>
     *
     * @param   source  ...
     * @param   factor  ...
     * @param   untrackedData  ...
     * @return  ...
     * @see     DataBufferInt##optimizations  DataBuffer performance optimizations
     */
    public static BufferedImage scaleImage(ImageData source, int factor, boolean untrackedData) {
        ImageData target = new ImageData(source, factor);
        Xbrz xbrz = ScalerPool.getScaler(factor, source.hasAlpha);
        xbrz.scaleImage(source.pixels, target.pixels, source.width, source.height);
        return untrackedData ? makeImage(target) : makeTracked(target);
    }

    private static BufferedImage makeImage(ImageData data) {
        return makeImage(data.pixels, data.width, data.height, data.hasAlpha);
    }

    private static BufferedImage makeImage(int[] pixels, int width, int height, boolean hasAlpha) {
        DataBufferInt dataBuffer = new DataBufferInt(pixels, pixels.length);
        return makeImage(dataBuffer, width, height, hasAlpha);
    }

    private static BufferedImage makeImage(DataBuffer dataBuffer, int width, int height, boolean hasAlpha) {
        ColorModel colorModel = hasAlpha ? ColorModel.getRGBdefault() : RGB_OPAQUE;
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(width, height);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, null);
        return new BufferedImage(colorModel, raster, false, null);
    }

    private static BufferedImage makeTracked(ImageData source) {
        return makeTracked(source.pixels, source.width, source.height, source.hasAlpha);
    }

    private static BufferedImage makeTracked(int[] pixels, int width, int height, boolean hasAlpha) {
        DataBufferInt dataBuffer = new DataBufferInt(pixels.length);
        for (int i = 0, len = pixels.length; i < len; i++) {
            dataBuffer.setElem(i, pixels[i]);
        }
        return makeImage(dataBuffer, width, height, hasAlpha);
    }

    /**
     * Returns a copy of the given {@code BufferedImage} source using a <i>tracked</i>
     * backing data buffer subject to performance optimizations such as hardware
     * acceleration for drawing on-screen.
     * <p>
     * <em>Note,</em> the source backing data buffer becomes <i>untracked</i>.</p>
     *
     * @param   source  ...
     * @return  ... .
     * @see     DataBufferInt#optimizations
     */
    static BufferedImage makeTracked(BufferedImage source) {
        int[] pixels = ((DataBufferInt) source.getRaster().getDataBuffer()).getData();
        return makeTracked(pixels, source.getWidth(), source.getHeight(), source.getColorModel().hasAlpha());
    }

    private static Image makeFiltered(Image base, int factor) {
        return XbrzFilter.createScaledImage(base, factor);
    }

    private static Image makeFiltered(Image base, BufferedImageOp op) {
        BufferedImageFilter bufferedFilter = new BufferedImageFilter(op);
        ImageProducer filteredSource = new SuspendableFilteredSource(base.getSource(), bufferedFilter);
        return Toolkit.getDefaultToolkit().createImage(filteredSource);
    }

    /**
     * Scales the given base image by the given factor.
     * <p>
     * If the given image is static, the result is a {@code BufferedImage}.</p>
     * <p>
     * If the given image is animated, the result is a {@code Toolkit} created image
     * using filtered source, like:</p>
     * <pre>
     * Image base;
     * ...
     * int factor = 2;
     * BufferedImageFilter xbrzFilter = new BufferedImageFilter(new XbrzOp(factor));
     * Image scaled = Toolkit.getDefaultToolkit()
     *         .createImage(new FilteredImageSource(base.getSource(), xbrzFilter));</pre>
     *
     * @param   base  ...
     * @param   factor  ...
     * @return  ...
     */
    public static Image scaleImage(Image base, int factor) {
        ImageData imageData = ImageData.get(base);
        if (imageData == null || imageData.isAnimated()) {
            return makeFiltered(base, factor);
        }
        return scaleImage(imageData, factor);
    }

    public static BufferedImage scaleImage(BufferedImage source, int factor) {
        return scaleImage(new ImageData(source), factor);
    }

    /**
     * Scales the given image to the given exact target dimensions.
     * <p>
     * The image is first upscaled using xBRZ, then smoothly downscaled as
     * necessary.</p>
     * <p>
     * As with {@code scaleImage(source, factor)} this will produce a
     * {@code BufferedImage} if the source image is static, or toolkit created
     * image if the source is animated.</p>
     *
     * @param   source  ...
     * @param   targetWidth  ...
     * @param   targetHeight  ...
     * @return  ...
     * @see     #scaleImage(Image, int)
     */
    public static Image scaleImage(Image source, int targetWidth, int targetHeight) {
        ImageData imageData = ImageData.get(source);
        if (imageData == null
                || (imageData.width == targetWidth
                        && imageData.height == targetHeight)) {
            return source;
        }
        return scaleImage(imageData, source, targetWidth, targetHeight);
    }

    static Image scaleImage(ImageData imageData, Image animatedSource, int targetWidth, int targetHeight) {
        int factor = AwtXbrz.findFactor(imageData.width, imageData.height, targetWidth, targetHeight);
        SmoothResizeOp resizeOp = new SmoothResizeOp(targetWidth, targetHeight);
        if (factor == 1) {
            return imageData.isAnimated() ? makeFiltered(animatedSource, resizeOp)
                                          : resizeOp.filter(makeTracked(imageData), null);
        }

        boolean integralScale = imageData.width * factor == targetWidth
                                && imageData.height * factor == targetHeight;
        if (imageData.isAnimated()) {
            Image scaledImage = makeFiltered(animatedSource, factor);
            final int maxFactor = 6;
            boolean upscale = imageData.width * maxFactor < targetWidth
                              && imageData.height * maxFactor < targetHeight;
            if (integralScale || upscale) {
                return scaledImage;
            }
            return makeFiltered(scaledImage, resizeOp);
        }
        BufferedImage result = scaleImage(imageData, factor);
        return integralScale ? result : resizeOp.filter(result, null);
    }

    static int findFactor(int sourceWidth, int sourceHeight,
                          int targetWidth, int targetHeight) {
        int scaledWidth = sourceWidth;
        int scaledHeight = sourceHeight;
        int factor = 1;
        final int maxFactor = 6;
        while ((scaledWidth < targetWidth
                    || scaledHeight < targetHeight)
                && factor < maxFactor) {
            factor += 1;
            scaledWidth = sourceWidth * factor;
            scaledHeight = sourceHeight * factor;
        }
        return factor;
    }

}
