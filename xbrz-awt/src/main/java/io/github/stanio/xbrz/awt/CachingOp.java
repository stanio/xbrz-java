package io.github.stanio.xbrz.awt;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class CachingOp implements BufferedImageOp {

    private final BufferedImageOp filterOp;

    private final Map<CachingOp.Key, Reference<BufferedImage>> cache = new ConcurrentHashMap<>();

    CachingOp(BufferedImageOp filterOp) {
        this.filterOp = filterOp;
    }

    @Override
    public RenderingHints getRenderingHints() {
        return filterOp.getRenderingHints();
    }

    @Override
    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        return filterOp.getPoint2D(srcPt, dstPt);
    }

    @Override
    public Rectangle2D getBounds2D(BufferedImage src) {
        return filterOp.getBounds2D(src);
    }

    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        return filterOp.createCompatibleDestImage(src, destCM);
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        BufferedImage result = null;
        Key srcKey = (dest == null) ? Key.of(src) : null;
        if (srcKey != null) {
            Reference<BufferedImage> ref = cache.get(srcKey);
            if (ref != null) {
                result = ref.get();
            }
        }
        if (result == null) {
            result = filterOp.filter(src, dest);
            if (srcKey != null) {
                cache.put(srcKey, new SoftReference<>(result));
            }
        }
        return result;
    }


    static final class Key {

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

        Key(int width, int height, boolean hasAlpha, byte[] pixels) {
            this.width = width;
            this.height = height;
            this.hasAlpha = hasAlpha;
            this.pixelDigest = Arrays.hashCode(pixels);
            this.hashCode = Arrays.hashCode(new int[] {
                width, height, Boolean.hashCode(hasAlpha), pixelDigest
            });
        }

        static Key of(BufferedImage src) {
            DataBuffer buffer = src.getRaster().getDataBuffer();
            if (buffer instanceof DataBufferInt) {
                return new Key(src.getWidth(), src.getHeight(),
                        src.getColorModel().hasAlpha(), ((DataBufferInt) buffer).getData());
            } else if (buffer instanceof DataBufferByte) {
                return new Key(src.getWidth(), src.getHeight(),
                        src.getColorModel().hasAlpha(), ((DataBufferByte) buffer).getData());
            }
            return null;
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
            return "Key [width=" + width + ", height=" + height + ", hasAlpha=" + hasAlpha
                    + ", pixelDigest=" + Integer.toHexString(pixelDigest) + "]";
        }

    }

}
