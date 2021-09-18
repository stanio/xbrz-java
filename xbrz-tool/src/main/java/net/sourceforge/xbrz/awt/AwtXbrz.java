package net.sourceforge.xbrz.awt;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageFilter;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.FilteredImageSource;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import net.sourceforge.xbrz.Xbrz;

/**
 * @see  Xbrz
 */
public final class AwtXbrz {

    private static final ColorModel RGB_OPAQUE =
            new DirectColorModel(24, 0x00FF0000, 0x0000FF00, 0x000000FF, 0);

    private AwtXbrz() { /* no instances */ }

    public static BufferedImage scaleImage(ImageData source, int factor) {
        ImageData target = new ImageData(source, factor);
        Xbrz xbrz = ScalerPool.getScaler(factor, source.hasAlpha);
        xbrz.scaleImage(source.pixels, target.pixels, source.width, source.height);
        return makeImage(target.pixels, target.width, target.height, target.hasAlpha);
    }

    private static BufferedImage makeImage(int[] pixels, int width, int height, boolean hasAlpha) {
        DataBufferInt dataBuffer = new DataBufferInt(pixels, pixels.length);
        return makeImage(dataBuffer, width, height, hasAlpha);
    }

    static BufferedImage makeTracked(BufferedImage source) {
        int[] pixels = ((DataBufferInt) source.getRaster().getDataBuffer()).getData();
        return makeTracked(pixels, source.getWidth(), source.getHeight(), source.getColorModel().hasAlpha());
    }

    private static BufferedImage makeTracked(int[] pixels, int width, int height, boolean hasAlpha) {
        DataBufferInt dataBuffer = new DataBufferInt(pixels.length);
        for (int i = 0, len = pixels.length; i < len; i++) {
            dataBuffer.setElem(i, pixels[i]);
        }
        return makeImage(dataBuffer, width, height, hasAlpha);
    }

    private static BufferedImage makeImage(DataBuffer dataBuffer, int width, int height, boolean hasAlpha) {
        ColorModel colorModel = hasAlpha ? ColorModel.getRGBdefault() : RGB_OPAQUE;
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(width, height);
        WritableRaster raster = WritableRaster.createWritableRaster(sampleModel, dataBuffer, null);
        return new BufferedImage(colorModel, raster, false, null);
    }

    private static Image makeFiltered(Image base, int factor) {
        BufferedImageFilter xbrzFilter = new BufferedImageFilter(new XbrzOp(factor));
        return Toolkit.getDefaultToolkit()
                .createImage(new FilteredImageSource(base.getSource(), xbrzFilter));
    }

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

    public static Image scaleImage(Image source, int targetWidth, int targetHeight) {
        ImageData imageData = ImageData.get(source);
        if (imageData == null) {
            return source;
        }

        int factor = getFactor(imageData.width, imageData.height, targetWidth, targetHeight);
        if (factor == 1) {
            return source;
        }

        if (imageData.isAnimated()) {
            return makeFiltered(source, factor);
        }
        return (factor == 1) ? source : scaleImage(imageData, factor);
    }

    private static int getFactor(int sourceWidth, int sourceHeight,
                                 int targetWidth, int targetHeight) {
        int scaledWidth = sourceWidth;
        int scaledHeight = sourceHeight;
        int factor = 1;
        final int maxFactor = 6;
        while ((scaledWidth < targetWidth || scaledHeight < targetHeight) && factor <= maxFactor) {
            scaledWidth <<= 1;
            scaledHeight <<= 1;
            factor += 1;
        }
        return factor;
    }

}
