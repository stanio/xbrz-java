package net.sourceforge.xbrz.tool;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sourceforge.xbrz.Xbrz;

/**
 * @see  Xbrz
 */
public final class AwtXbrz {


    private static final class ScalerKey {

        final int scale;
        final boolean hasAlpha;
        private final int hash;

        private ScalerKey(int scale, boolean hasAlpha) {
            this.scale = scale;
            this.hasAlpha = hasAlpha;

            final int prime = 31;
            int hashCode = 1;
            hashCode = prime * hashCode + (hasAlpha ? 1231 : 1237);
            hashCode = prime * hashCode + scale;
            this.hash = hashCode;
        }

        static ScalerKey valueOf(int scale, boolean hasAlpha) {
            return new ScalerKey(scale, hasAlpha);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ScalerKey) {
                ScalerKey other = (ScalerKey) obj;
                return scale == other.scale && hasAlpha == other.hasAlpha;
            }
            return false;
        }

    }


    private static Map<ScalerKey, Xbrz> scalers = new ConcurrentHashMap<>();

    private AwtXbrz() {
        // no instances
    }

    public static BufferedImage scaleImage(ImageData source, int factor) {
        ImageData target = new ImageData(source, factor);
        Xbrz xbrz = scalers.computeIfAbsent(ScalerKey.valueOf(factor, source.hasAlpha),
                                            key -> new Xbrz(key.scale, key.hasAlpha));
        xbrz.scaleImage(source.pixels, target.pixels, source.width, source.height);
        return makeImage(target.pixels, target.width, target.height, target.hasAlpha);
    }

    private static BufferedImage makeImage(int[] pixels, int width, int height, boolean hasAlpha) {
        DataBufferInt dataBuffer = new DataBufferInt(pixels, pixels.length);
        DirectColorModel colorModel = new DirectColorModel(hasAlpha ? 32 : 24,
                                                           0x00FF0000,
                                                           0x0000FF00,
                                                           0x000000FF,
                                                           hasAlpha ? 0xFF000000 : 0);
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(width, height);
        WritableRaster raster = WritableRaster.createWritableRaster(sampleModel, dataBuffer, null);
        return new BufferedImage(colorModel, raster, false, null);
    }

    public static Image scaleImage(Image source, int factor) {
        ImageData imageData = ImageData.get(source);
        // REVISIT: Maybe use java.awt.image.MemoryImageSource to produce animated
        // images (would need extra source info like frame "delayTime", and maybe
        // "disposalMethod"?), or signal with an exception.
        return (imageData == null) ? source : scaleImage(imageData, factor);
    }

    public static Image scaleImage(Image source, int targetWidth, int targetHeight) {
        ImageData imageData = ImageData.get(source);
        // REVISIT: Maybe use java.awt.image.MemoryImageSource to produce animated
        // images (would need extra source info like frame "delayTime", and maybe
        // "disposalMethod"?), or signal with an exception.
        if (imageData == null) {
            return source;
        }
        int scaledWidth = imageData.width;
        int scaledHeight = imageData.height;
        int factor = 1;
        while ((scaledWidth < targetWidth || scaledHeight < targetHeight) && factor <= 6) {
            scaledWidth <<= 1;
            scaledHeight <<= 1;
            factor += 1;
        }
        return (factor == 1) ? source : scaleImage(imageData, factor);
    }

}
