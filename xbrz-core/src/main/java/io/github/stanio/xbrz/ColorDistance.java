package io.github.stanio.xbrz;

import static io.github.stanio.xbrz.Color.*;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Calculates color distance between two pixels.
 */
@FunctionalInterface
public interface ColorDistance {

    double calc(int pix1, int pix2);

    static ColorDistance rgb() {
        return (pix1, pix2) -> {
            final int r_diff = getRed  (pix1) - getRed  (pix2);
            final int g_diff = getGreen(pix1) - getGreen(pix2);
            final int b_diff = getBlue (pix1) - getBlue (pix2);

            //euklidean RGB distance
            return Math.sqrt(r_diff * r_diff + g_diff * g_diff + b_diff * b_diff);
        };
    }

    static ColorDistance yCbCr(double lumaWeight) {
        return new ColorDistanceYCbCr(lumaWeight);
    }

    static ColorDistance integerYCbCr(double lumaWeight) {
        return new ColorDistanceYCbCrInteger(lumaWeight);
    }

    static ColorDistance bufferedYCbCr(int sigBits) {
        return ColorDistanceYCbCrBuffered.instance(sigBits);
    }

    static ColorDistance withAlpha(ColorDistance dist) {
        return (pix1, pix2) -> {
            final int a1 = getAlpha(pix1);
            final int a2 = getAlpha(pix2);
            /*
            Requirements for a color distance handling alpha channel: with a1, a2 in [0, 1]

                1. if a1 = a2, distance should be: a1 * distYCbCr()
                2. if a1 = 0,  distance should be: a2 * distYCbCr(black, white) = a2 * 255
                3. if a1 = 1,  ??? maybe: 255 * (1 - a2) + a2 * distYCbCr()
            */
            final double d = dist.calc(pix1, pix2);
            return (a1 < a2) ? a1 / 255.0 * d + (a2 - a1)
                             : a2 / 255.0 * d + (a1 - a2);
        };
    }

}


class ColorDistanceYCbCr implements ColorDistance {

    final double lumaWeight;

    public ColorDistanceYCbCr(double lumaWeigth) {
        this.lumaWeight = lumaWeigth;
    }

    //static final double k_b = 0.0722; // ITU-R BT.709 conversion
    //static final double k_r = 0.2126; //
    static final double k_b = 0.0593; // ITU-R BT.2020 conversion
    static final double k_r = 0.2627; //
    static final double k_g = 1 - k_b - k_r;

    static final double scale_b = 0.5 / (1 - k_b);
    static final double scale_r = 0.5 / (1 - k_r);

    @Override
    public double calc(int pix1, int pix2) {
        // https://en.wikipedia.org/wiki/YCbCr#ITU-R_BT.601_conversion
        // YCbCr conversion is a matrix multiplication => take advantage of linearity by subtracting first!
        final int r_diff = getRed  (pix1) - getRed  (pix2); // we may delay division by 255 to after matrix multiplication
        final int g_diff = getGreen(pix1) - getGreen(pix2); //
        final int b_diff = getBlue (pix1) - getBlue (pix2); // substraction for int is noticeable faster than for double!

        final double y   = k_r * r_diff + k_g * g_diff + k_b * b_diff; //[!], analog YCbCr!
        final double c_b = scale_b * (b_diff - y);
        final double c_r = scale_r * (r_diff - y);

        // we skip division by 255 to have similar range like other distance functions
        return Math.sqrt(square(lumaWeight == 1.0 ? y : lumaWeight * y)
                         + square(c_b) + square(c_r));
    }

    static double square(double value) { return value * value; }

}


class ColorDistanceYCbCrInteger implements ColorDistance {

    private static final long PRECISION = 100_000L;
    private static final long PRECISION_SQ = square(PRECISION);

    final long lumaWeight;

    public ColorDistanceYCbCrInteger(double lumaWeigth) {
        this.lumaWeight = (long) (PRECISION * lumaWeigth);
    }

    static final long k_b = (long) (PRECISION * 0.0593);
    static final long k_r = (long) (PRECISION * 0.2627);
    static final long k_g = PRECISION - k_b - k_r;

    static final long denom_b = 2 * (PRECISION - k_b);
    static final long denom_r = 2 * (PRECISION - k_r);

    @Override
    public double calc(int pix1, int pix2) {
        final long r_diff = getRed  (pix1) - getRed  (pix2);
        final long g_diff = getGreen(pix1) - getGreen(pix2);
        final long b_diff = getBlue (pix1) - getBlue (pix2);

        final long y   = k_r * r_diff + k_g * g_diff + k_b * b_diff;
        final long c_b = PRECISION * (PRECISION * b_diff - y) / denom_b;
        final long c_r = PRECISION * (PRECISION * r_diff - y) / denom_r;

        return Math.sqrt((square(lumaWeight == PRECISION ? y : y * lumaWeight / PRECISION)
                          + square(c_b) + square(c_r)) / PRECISION_SQ);
    }

    static long square(long value) { return value * value; }

}


class ColorDistanceYCbCrBuffered extends ColorDistanceYCbCr {

    // -255 .. 255
    private static final int diffSize = 9;

    private final int sigBits;
    private final int adjBits; // diffSize - sigBits
    private final float diffToDist[];

    // (1 << (3 * 5 sigBits)) = 32K * Float.BYTES = 128K buffer
    // (1 << (3 * 8 sigBits)) = 16M * Float.BYTES = 64M buffer
    public ColorDistanceYCbCrBuffered(int sigBits) {
        super(1);
        if (sigBits < 2 || sigBits > 8) {
            throw new IllegalArgumentException("Illegal sigBits: " + sigBits);
        }

        this.sigBits = sigBits;
        this.adjBits = diffSize - sigBits;
        this.diffToDist = new float[1 << (3 * sigBits)];

        int bitMask = (1 << sigBits) - 1;
        for (int i = 0, len = diffToDist.length; i < len; i++) {
            // compressed values
            int r_diff = i >> (sigBits << 1) & bitMask;
            int g_diff = i >>  sigBits       & bitMask;
            int b_diff = i                   & bitMask;
            // expanded values
            r_diff = (r_diff << adjBits) - 255 + (1 << (adjBits - 1));
            g_diff = (g_diff << adjBits) - 255 + (1 << (adjBits - 1));
            b_diff = (b_diff << adjBits) - 255 + (1 << (adjBits - 1));

            final double y   = k_r * r_diff + k_g * g_diff + k_b * b_diff; //[!], analog YCbCr!
            final double c_b = scale_b * (b_diff - y);
            final double c_r = scale_r * (r_diff - y);

            diffToDist[i] = (float) Math.sqrt(square(y) + square(c_b) + square(c_r));
        }
    }

    private static ConcurrentMap<Integer, WeakReference<ColorDistanceYCbCrBuffered>>
            instances = new ConcurrentHashMap<>();

    public static ColorDistanceYCbCrBuffered instance(int sigBits) {
        ColorDistanceYCbCrBuffered[] ptr = { null };
        instances.compute(sigBits, (k, ref) -> {
            ColorDistanceYCbCrBuffered obj = (ref == null) ? null : ref.get();
            try {
                if (obj == null) {
                    obj = new ColorDistanceYCbCrBuffered(k);
                    return new WeakReference<>(obj);
                }
                return ref;
            } finally {
                ptr[0] = obj;
            }
        });
        return ptr[0];
    }

    @Override
    public double calc(int pix1, int pix2) {
        final int r_diff = getRed  (pix1) - getRed  (pix2) + 255;
        final int g_diff = getGreen(pix1) - getGreen(pix2) + 255;
        final int b_diff = getBlue (pix1) - getBlue (pix2) + 255;

        // compressed index
        final int index = ((r_diff >> adjBits) << (sigBits << 1)) |
                          ((g_diff >> adjBits) << sigBits) |
                          (b_diff >> adjBits);
        return diffToDist[index];
    }

}
