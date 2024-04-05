package io.github.stanio.xbrz;

import static io.github.stanio.xbrz.BlendInfo.*;
import static io.github.stanio.xbrz.BlendType.*;
import static io.github.stanio.xbrz.RotationDegree.*;
import static java.lang.Math.multiplyExact;

import java.util.function.Supplier;

/**
 * Defines the main API for xBRZ scaling.  Instances are configured with specific
 * scale factor, color blending type (alpha vs. no alpha) and {@linkplain
 * ColorDistance color distance} function.  A single instance could be used to
 * scale multiple images concurrently.
 * <p>
 * <em>Sample usage:</em></p>
 * <pre>
 * import java.awt.image.BufferedImage;
 *
 *     BufferedImage source = ...;
 *     int srcWidth = source.getWidth();
 *     int srcHeight = source.getHeight();
 *     int[] srcPixels = source.getRGB(0, 0, srcWidth, srcHeight, null, 0, srcWidth);
 *
 *     int factor = 2;
 *     int destWidth = srcWidth * factor;
 *     int destHeight = srcHeight * factor;
 *     boolean hasAlpha = source.getColorModel().hasAlpha();
 *     int[] destPixels = Xbrz.scaleImage(factor, hasAlpha, srcPixels, null, srcWidth, srcHeight);
 *
 *     BufferedImage scaled = new BufferedImage(destWidth, destHeight,
 *                                              hasAlpha ? BufferedImage.TYPE_INT_ARGB
 *                                                       : BufferedImage.TYPE_INT_RGB);
 *     scaled.setRGB(0, 0, destWidth, destHeight, destPixels, 0, destWidth);</pre>
 */
public class Xbrz {


    /** <i>ScalerCfg</i> */
    public static final class ScalerCfg
    {
        /** <i>luminance weight</i> */
        public final double luminanceWeight;
        /** <i>equal color tolerance</i> */
        public final double equalColorTolerance;
        /** <i>center direction bias</i> */
        public final double centerDirectionBias;
        /** <i>dominant direction threshold</i> */
        public final double dominantDirectionThreshold;
        /** <i>steep direction threshold</i> */
        public final double steepDirectionThreshold;

        public ScalerCfg() {
            this(1, 30, 4, 3.6, 2.2);
        }

        public ScalerCfg(double luminanceWeight,
                         double equalColorTolerance,
                         double centerDirectionBias,
                         double dominantDirectionThreshold,
                         double steepDirectionThreshold) {
            this.luminanceWeight = luminanceWeight;
            this.equalColorTolerance = equalColorTolerance;
            this.centerDirectionBias = centerDirectionBias;
            this.dominantDirectionThreshold = dominantDirectionThreshold;
            this.steepDirectionThreshold = steepDirectionThreshold;
        }

        public ScalerCfg withLuminanceWeight(double luminanceWeight) {
            return new ScalerCfg(luminanceWeight, equalColorTolerance,
                    centerDirectionBias, dominantDirectionThreshold, steepDirectionThreshold);
        }

        public ScalerCfg withEqualColorTolerance(double equalColorTolerance) {
            return new ScalerCfg(luminanceWeight, equalColorTolerance,
                    centerDirectionBias, dominantDirectionThreshold, steepDirectionThreshold);
        }

        public ScalerCfg withCenterDirectionBias(double centerDirectionBias) {
            return new ScalerCfg(luminanceWeight, equalColorTolerance,
                    centerDirectionBias, dominantDirectionThreshold, steepDirectionThreshold);
        }

        public ScalerCfg withDominantDirectionThreshold(double dominantDirectionThreshold) {
            return new ScalerCfg(luminanceWeight, equalColorTolerance,
                    centerDirectionBias, dominantDirectionThreshold, steepDirectionThreshold);
        }

        public ScalerCfg withSteepDirectionThreshold(double steepDirectionThreshold) {
            return new ScalerCfg(luminanceWeight, equalColorTolerance,
                    centerDirectionBias, dominantDirectionThreshold, steepDirectionThreshold);
        }

    } // class ScalerCfg


    /** The JVM may reserve some header words in an array. */
    private static final int SOFT_MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    private static final boolean DEBUG = false;

    private final Scaler scaler;
    private final ScalerCfg cfg;
    private final ColorDistance dist;
    private final boolean withAlpha;

    /**
     * Constructs a new xBRZ scaler.
     *
     * @param   factor  the scale factor to apply
     * @throws  IllegalArgumentException
     *          if the specified scale factor is &lt; 2 or &gt; 6
     */
    public Xbrz(int factor) {
        this(factor, true);
    }

    public Xbrz(int factor, boolean withAlpha) {
        this(factor, withAlpha, new ScalerCfg());
    }

    public Xbrz(int factor, boolean withAlpha, ScalerCfg cfg) {
        this(factor, withAlpha, cfg, ColorDistance.yCbCr(cfg.luminanceWeight));
    }

    public Xbrz(int factor, boolean withAlpha, ScalerCfg cfg, ColorDistance colorDistance) {
        this.scaler = Scaler.forFactor(factor, withAlpha);
        this.cfg = cfg;
        this.dist = withAlpha ? ColorDistance.withAlpha(colorDistance) : colorDistance;
        this.withAlpha = withAlpha;
    }

    /**
     * The factor this {@code Xbrz} instance applies when scaling images.
     *
     * @return  The configured scaling factor
     */
    public int factor() {
        return scaler.scale();
    }

    /**
     * {@code factor} alias.
     *
     * @see     #factor()
     */
    public int scale() {
        return factor();
    }

    private final double dist(int pix1, int pix2) { return dist.calc(pix1, pix2); }

    private final boolean eq(int pix1, int pix2) { return dist(pix1, pix2) < cfg.equalColorTolerance; }

    /* detect blend direction

    preprocessing blend result:
    ---------
    | F | G |   evaluate corner between F, G, J, K
    |---+---|   current input pixel is at position F
    | J | K |
    ---------   F, G, J, K corners of "BlendType" */
    private void preProcessCorners(Kernel_4x4 ker, BlendResult result) {
        result.reset();

        if ((ker.f == ker.g &&
             ker.j == ker.k) ||
            (ker.f == ker.j &&
             ker.g == ker.k))
            return;

        final double jg = dist(ker.i, ker.f) + dist(ker.f, ker.c) + dist(ker.n, ker.k) + dist(ker.k, ker.h) + cfg.centerDirectionBias * dist(ker.j, ker.g);
        final double fk = dist(ker.e, ker.j) + dist(ker.j, ker.o) + dist(ker.b, ker.g) + dist(ker.g, ker.l) + cfg.centerDirectionBias * dist(ker.f, ker.k);

        if (jg < fk)
        {
            final boolean dominantGradient = cfg.dominantDirectionThreshold * jg < fk;
            if (ker.f != ker.g && ker.f != ker.j)
                result.blend_f = dominantGradient ? BLEND_DOMINANT : BLEND_NORMAL;

            if (ker.k != ker.j && ker.k != ker.g)
                result.blend_k = dominantGradient ? BLEND_DOMINANT : BLEND_NORMAL;
        }
        else if (fk < jg)
        {
            final boolean dominantGradient = cfg.dominantDirectionThreshold * fk < jg;
            if (ker.j != ker.f && ker.j != ker.k)
                result.blend_j = dominantGradient ? BLEND_DOMINANT : BLEND_NORMAL;

            if (ker.g != ker.f && ker.g != ker.k)
                result.blend_g = dominantGradient ? BLEND_DOMINANT : BLEND_NORMAL;
        }
    }

    private void blendPixel(RotationDegree rotDeg,
                            Kernel_3x3 ker,
                            OutputMatrix out,
                            byte blendInfo) //result of preprocessing all four corners of pixel "e"
    {
        byte blend = BlendInfo.rotate(blendInfo, rotDeg);

        if (BlendInfo.getBottomR(blend) >= BLEND_NORMAL)
        {
            ker.rotDeg(rotDeg);
            out.rotDeg(rotDeg);

            final int e = ker.e();
            final int f = ker.f();
            final int h = ker.h();

            final int g = ker.g();
            final int c = ker.c();
            final int i = ker.i();

            boolean doLineBlend;

            if (BlendInfo.getBottomR(blend) >= BLEND_DOMINANT)
                doLineBlend = true;

            //make sure there is no second blending in an adjacent rotation for this pixel: handles insular pixels, mario eyes
            else if (BlendInfo.getTopR(blend) != BLEND_NONE && !eq(e, g)) //but support double-blending for 90° corners
                doLineBlend = false;
            else if (BlendInfo.getBottomL(blend) != BLEND_NONE && !eq(e, c))
                doLineBlend = false;

            //no full blending for L-shapes; blend corner only (handles "mario mushroom eyes")
            else if (!eq(e, i) && eq(g, h) && eq(h, i) && eq(i, f) && eq(f, c))
                doLineBlend = false;

            else
                doLineBlend = true;

            final int px = dist(e, f) <= dist(e, h) ? f : h; //choose most similar color

            if (doLineBlend)
            {
                final double fg = dist(f, g);
                final double hc = dist(h, c);

                final boolean haveShallowLine = cfg.steepDirectionThreshold * fg <= hc && e != g && ker.d() != g;
                final boolean haveSteepLine   = cfg.steepDirectionThreshold * hc <= fg && e != c && ker.b() != c;

                if (haveShallowLine)
                {
                    if (haveSteepLine)
                        scaler.blendLineSteepAndShallow(px, out);
                    else
                        scaler.blendLineShallow(px, out);
                }
                else
                {
                    if (haveSteepLine)
                        scaler.blendLineSteep(px, out);
                    else
                        scaler.blendLineDiagonal(px, out);
                }
            }
            else
                scaler.blendCorner(px, out);
        }
    }

    /**
     * Scales the {@code src} pixels to the {@code trg} buffer.
     * <p>
     * The pixels are expected to be packed as <abbr>ARGB</abbr> {@code int}
     * (32-bit) values.</p>
     */
    public int[] scaleImage(int[] src, int[] trg, int srcWidth, int srcHeight) {
        if (trg == null) {
            trg = new int[targetArraySize(srcWidth, srcHeight, factor())];
        }
        scaleImage(src, trg, srcWidth, srcHeight, 0, srcHeight);
        return trg;
    }

    public void scaleImage(int[] src, int[] trg, int srcWidth, int srcHeight, int yFirst, int yLast) {
        yFirst = Math.max(yFirst, 0);
        yLast  = Math.min(yLast, srcHeight);
        if (yFirst >= yLast || srcWidth <= 0)
            return;

        byte[] preProcBuf = new byte[srcWidth];
        Kernel_4x4 ker4 = Kernel_4x4.instance(src, srcWidth, srcHeight, withAlpha);
        OutputMatrix out = OutputMatrix.instance(scaler.scale(), trg, srcWidth * scaler.scale());

        final BlendResult res = BlendResult.instance();

        //initialize preprocessing buffer for first row of current stripe: detect upper left and right corner blending
        {
            ker4.positionY(yFirst - 1);

            {
                preProcessCorners(ker4, res);
                clearAddTopL(preProcBuf, 0, res.blend_k); //set 1st known corner for (0, yFirst)
            }

            for (int x = 0; x < srcWidth; ++x)
            {
                ker4.shift();     //shift previous kernel to the left
                ker4.readDhlp(x); // (x, yFirst - 1) is at position F

                preProcessCorners(ker4, res);
                addTopR(preProcBuf, x, res.blend_j); //set 2nd known corner for (x, yFirst)

                if (x + 1 < srcWidth)
                    clearAddTopL(preProcBuf, x + 1, res.blend_k); //set 1st known corner for (x + 1, yFirst)
            }
        }
        //------------------------------------------------------------------------------------

        Kernel_3x3 ker3 = ker4.kernel_3x3();

        for (int y = yFirst; y < yLast; ++y)
        {
            out.positionY(y);
            //initialize at position x = -1
            ker4.positionY(y);

            byte blend_xy1; //corner blending for current (x, y + 1) position
            {
                preProcessCorners(ker4, res);
                blend_xy1 = clearAddTopL(res.blend_k); //set 1st known corner for (0, y + 1) and buffer for use on next column

                addBottomL(preProcBuf, 0, res.blend_g); //set 3rd known corner for (0, y)
            }

            for (int x = 0; x < srcWidth; ++x, out.incrementX())
            {
                ker4.shift();     //shift previous kernel to the left
                ker4.readDhlp(x); // (x, y) is at position F

                //evaluate the four corners on bottom-right of current pixel
                byte blend_xy = preProcBuf[x]; //for current (x, y) position
                {
                    preProcessCorners(ker4, res);
                    blend_xy = addBottomR(blend_xy, res.blend_f); //all four corners of (x, y) have been determined at this point due to processing sequence!

                    blend_xy1 = addTopR(blend_xy1, res.blend_j); //set 2nd known corner for (x, y + 1)
                    preProcBuf[x] = blend_xy1; //store on current buffer position for use on next row

                    if (x + 1 < srcWidth)
                    {
                        //blend_xy1 -> blend_x1y1
                        blend_xy1 = clearAddTopL(res.blend_k); //set 1st known corner for (x + 1, y + 1) and buffer for use on next column

                        addBottomL(preProcBuf, x + 1, res.blend_g); //set 3rd known corner for (x + 1, y)
                    }
                }

                out.fillBlock(ker4.f);

                //blend all four corners of current pixel
                if (BlendInfo.blendingNeeded(blend_xy))
                {
                    blendPixel(ROT_0,   ker3, out, blend_xy);
                    blendPixel(ROT_90,  ker3, out, blend_xy);
                    blendPixel(ROT_180, ker3, out, blend_xy);
                    blendPixel(ROT_270, ker3, out, blend_xy);
                }
            }
        }
    }

    /**
     * {@code new Xbrz(factor, hasAlpha).scaleImage(src, trg, srcWidth, srcHeight)}
     *
     * @see  #scaleImage(int[], int[], int, int)
     */
    public static int[] scaleImage(int factor, boolean hasAlpha, int[] src, int[] trg, int srcWidth, int srcHeight) {
        return new Xbrz(factor, hasAlpha).scaleImage(src, trg, srcWidth, srcHeight);
    }

    public static int targetArraySize(int sourceWidth, int sourceHeight, int factor) {
        Supplier<String> message = () -> "Target size exceeds implementation limits (sourceWidth: "
                + sourceWidth + ", sourceHeight: " + sourceHeight + ", scaleFactor: " + factor + ")";
        try {
            int targetSize = multiplyExact(multiplyExact(sourceWidth, factor),
                                           multiplyExact(sourceHeight, factor));
            if (targetSize > SOFT_MAX_ARRAY_LENGTH) {
                throw new OutOfMemoryError(message.get());
            }
            return targetSize;
        } catch (ArithmeticException e) {
            if (DEBUG) {
                System.err.print("DEBUG: ");
                e.printStackTrace(System.err);
            }
            throw new OutOfMemoryError(message.get());
        }
    }

}


/* input kernel area naming convention:
-----------------
| A | B | C | D |
|---|---|---|---|
| E | F | G | H |
|---|---|---|---|   input pixel is at position F
| I | J | K | L |
|---|---|---|---|
| M | N | O | P |
-----------------
*/
final class Kernel_4x4 {

    int
    a, b, c, //
    e, f, g, // support reinterpret_cast from Kernel_4x4 => Kernel_3x3
    i, j, k, //
    m, n, o,
    d, h, l, p;

    private int[] src;
    private int srcWidth;
    private int srcHeight;
    private boolean withAlpha;

    private int s_m1;
    private int s_0;
    private int s_p1;
    private int s_p2;

    private final Kernel_3x3 ker3;

    private Kernel_4x4() {
        this.ker3 = new Kernel_3x3(this);
    }

    private static final ThreadLocal<Kernel_4x4> instance = new ThreadLocal<>();

    static Kernel_4x4 instance(int[] src, int srcWidth, int srcHeight, boolean withAlpha) {
        Kernel_4x4 kernel = instance.get();
        if (kernel == null) {
            kernel = new Kernel_4x4();
            instance.set(kernel);
        }
        kernel.src = src;
        kernel.srcWidth = srcWidth;
        kernel.srcHeight = srcHeight;
        kernel.withAlpha = withAlpha;
        return kernel;
    }

    final Kernel_3x3 kernel_3x3() {
        return ker3;
    }

    final void positionY(int y) {
        if (withAlpha) {
            positionYTransparent(y);
        } else {
            positionYDuplicate(y);
        }

        readDhlp(-4); //hack: read a, e, i, m at x = -1
        a = d;
        e = h;
        i = l;
        m = p;

        readDhlp(-3);
        b = d;
        f = h;
        j = l;
        n = p;

        readDhlp(-2);
        c = d;
        g = h;
        k = l;
        o = p;

        readDhlp(-1);
    }

    private final void positionYTransparent(int y) {
        s_m1 = 0 <= y - 1 && y - 1 < srcHeight ? srcWidth * (y - 1) : -1;
        s_0  = 0 <= y     && y     < srcHeight ? srcWidth *  y      : -1;
        s_p1 = 0 <= y + 1 && y + 1 < srcHeight ? srcWidth * (y + 1) : -1;
        s_p2 = 0 <= y + 2 && y + 2 < srcHeight ? srcWidth * (y + 2) : -1;
    }

    private final void positionYDuplicate(int y) {
        s_m1 = srcWidth * clamp(y - 1, 0, srcHeight - 1);
        s_0  = srcWidth * clamp(y,     0, srcHeight - 1);
        s_p1 = srcWidth * clamp(y + 1, 0, srcHeight - 1);
        s_p2 = srcWidth * clamp(y + 2, 0, srcHeight - 1);
    }

    static int clamp(int v, int lo, int hi) {
        return (v < lo) ? lo : (v > hi) ? hi : v;
    }

    final void readDhlp(int x) //(x, y) is at kernel position F
    {
        if (withAlpha) {
            readDhlpTransparent(x);
        } else {
            readDhlpDuplicate(x);
        }
    }

    private final void readDhlpTransparent(int x) {
        final int x_p2 = x + 2;
        if (0 <= x_p2 && x_p2 < srcWidth)
        {
            d = (s_m1 >= 0) ? src[s_m1 + x_p2] : 0;
            h = (s_0  >= 0) ? src[s_0  + x_p2] : 0;
            l = (s_p1 >= 0) ? src[s_p1 + x_p2] : 0;
            p = (s_p2 >= 0) ? src[s_p2 + x_p2] : 0;
        }
        else
        {
            d = 0;
            h = 0;
            l = 0;
            p = 0;
        }
    }

    private final void readDhlpDuplicate(int x) {
        final int xc_p2 = clamp(x + 2, 0, srcWidth - 1);
        d = src[s_m1 + xc_p2];
        h = src[s_0  + xc_p2];
        l = src[s_p1 + xc_p2];
        p = src[s_p2 + xc_p2];
    }

    final void shift() {
        a = b;    //shift kernel to the left
        e = f;    // -----------------
        i = j;    // | A | B | C | D |
        m = n;    // |---|---|---|---|
        /**/      // | E | F | G | H |
        b = c;    // |---|---|---|---|
        f = g;    // | I | J | K | L |
        j = k;    // |---|---|---|---|
        n = o;    // | M | N | O | P |
        /**/      // -----------------
        c = d;
        g = h;
        k = l;
        o = p;
    }

    //@Override
    //public String toString() {
    //    return String.format("| %08X | %08X | %08X | %08X |%n"
    //                         + "| %08X | %08X | %08X | %08X |%n"
    //                         + "| %08X | %08X | %08X | %08X |%n"
    //                         + "| %08X | %08X | %08X | %08X |",
    //                         a, b, c, d,
    //                         e, f, g, h,
    //                         i, j, k, l,
    //                         m, n, o, p);
    //}

}


/* input kernel area naming convention:
-------------
| A | B | C |
|---|---|---|
| D | E | F | input pixel is at position E
|---|---|---|
| G | H | I |
-------------
*/
final class Kernel_3x3 {

    private final Kernel_4x4 ker4;

    private RotationDegree rotDeg = ROT_0;

    Kernel_3x3(Kernel_4x4 ker4) {
        this.ker4 = ker4;
    }

    final int a() {
        switch (rotDeg) {
        default:      return ker4.a;
        case ROT_90:  return ker4.i;
        case ROT_180: return ker4.k;
        case ROT_270: return ker4.c;
        }
    }

    final int b() {
        switch (rotDeg) {
        default:      return ker4.b;
        case ROT_90:  return ker4.e;
        case ROT_180: return ker4.j;
        case ROT_270: return ker4.g;
        }
    }

    final int c() {
        switch (rotDeg) {
        default:      return ker4.c;
        case ROT_90:  return ker4.a;
        case ROT_180: return ker4.i;
        case ROT_270: return ker4.k;
        }
    }

    final int d() {
        switch (rotDeg) {
        default:      return ker4.e;
        case ROT_90:  return ker4.j;
        case ROT_180: return ker4.g;
        case ROT_270: return ker4.b;
        }
    }

    final int e() {
        return ker4.f; // center
    }

    final int f() {
        switch (rotDeg) {
        default:      return ker4.g;
        case ROT_90:  return ker4.b;
        case ROT_180: return ker4.e;
        case ROT_270: return ker4.j;
        }
    }

    final int g() {
        switch (rotDeg) {
        default:      return ker4.i;
        case ROT_90:  return ker4.k;
        case ROT_180: return ker4.c;
        case ROT_270: return ker4.a;
        }
    }

    final int h() {
        switch (rotDeg) {
        default:      return ker4.j;
        case ROT_90:  return ker4.g;
        case ROT_180: return ker4.b;
        case ROT_270: return ker4.e;
        }
    }

    final int i() {
        switch (rotDeg) {
        default:      return ker4.k;
        case ROT_90:  return ker4.c;
        case ROT_180: return ker4.a;
        case ROT_270: return ker4.i;
        }
    }

    final void rotDeg(RotationDegree deg) {
        this.rotDeg = deg;
    }

    //@Override
    //public String toString() {
    //    return String.format("| %08X | %08X | %08X |%n"
    //                         + "| %08X | %08X | %08X |%n"
    //                         + "| %08X | %08X | %08X |",
    //                         a(), b(), c(),
    //                         d(), e(), f(),
    //                         g(), h(), i());
    //}

}


final class BlendType {

    static final byte BLEND_NONE = 0;
    static final byte BLEND_NORMAL = 1;   //a normal indication to blend
    static final byte BLEND_DOMINANT = 2; //a strong indication to blend

    //static String toString(byte blendType) {
    //    switch (blendType) {
    //    default:
    //    case BLEND_NONE: return "  ";
    //    case BLEND_NORMAL: return "\u2591\u2591";
    //    case BLEND_DOMINANT: return "\u2593\u2593";
    //    }
    //}

}


/*
---------
| F | G |
|---+---|   current input pixel is at position F
| J | K |
--------- */
final class BlendResult
{
    byte
    /**/blend_f, blend_g,
    /**/blend_j, blend_k;

    private BlendResult() {}

    private static final ThreadLocal<BlendResult> instance = new ThreadLocal<>();

    static BlendResult instance() {
        BlendResult result = instance.get();
        if (result == null) {
            result = new BlendResult();
            instance.set(result);
        }
        return result;
    }

    final void reset() {
        blend_f = blend_g = blend_j = blend_k = BLEND_NONE;
    }

    //@Override
    //public String toString() {
    //    return String.format("%s%s%n%s%s",
    //                         BlendType.toString(blend_f),
    //                         BlendType.toString(blend_g),
    //                         BlendType.toString(blend_j),
    //                         BlendType.toString(blend_k));
    //}

}


final class BlendInfo {

    static byte rotate(byte b, RotationDegree rotDeg) {
        switch (rotDeg) {
        default:      return b;
        case ROT_90:  return (byte) (((b << 2) & 0xFF) | ((b & 0xFF) >> 6));
        case ROT_180: return (byte) (((b << 4) & 0xFF) | ((b & 0xFF) >> 4));
        case ROT_270: return (byte) (((b << 6) & 0xFF) | ((b & 0xFF) >> 2));
        }
    }

    static boolean blendingNeeded(byte b) {
        return b != BLEND_NONE;
    }

    //static byte getTopL   (byte b) { return (byte) (0x3 & b); }
    static byte getTopR   (byte b) { return (byte) (0x3 & (b >> 2)); }
    static byte getBottomR(byte b) { return (byte) (0x3 & (b >> 4)); }
    static byte getBottomL(byte b) { return (byte) (0x3 & (b >> 6)); }

    static byte clearAddTopL(        byte bt) { return bt; }
    static byte addTopR     (byte b, byte bt) { return (byte) (b | (bt << 2)); } //buffer is assumed to be initialized before preprocessing!
    static byte addBottomR  (byte b, byte bt) { return (byte) (b | (bt << 4)); } //e.g. via clearAddTopL()
    //static byte addBottomL  (byte b, byte bt) { return (byte) (b | (bt << 6)); } //

    static void clearAddTopL(byte[] buf, int i, byte bt) { buf[i] = bt; }
    static void addTopR     (byte[] buf, int i, byte bt) { buf[i] |= bt << 2; } //buffer is assumed to be initialized before preprocessing!
    //static void addBottomR  (byte[] buf, int i, byte bt) { buf[i] |= bt << 4; } //e.g. via clearAddTopL()
    static void addBottomL  (byte[] buf, int i, byte bt) { buf[i] |= bt << 6; } //

    //@Override
    //public String toString() {
    //    return String.format("%s%s%n%s%s",
    //                         BlendType.toString(getTopL()),
    //                         BlendType.toString(getTopR()),
    //                         BlendType.toString(getBottomL()),
    //                         BlendType.toString(getBottomR()));
    //}

    //static String toString(byte[] buf) {
    //    StringBuilder str = new StringBuilder();
    //    BlendInfo blend = new BlendInfo();
    //    for (int i = 0; i < buf.length; i++) {
    //        blend.val = buf[i];
    //        str.append(BlendType.toString(blend.getTopL()))
    //                .append(BlendType.toString(blend.getTopR()));
    //    }
    //    str.append(System.lineSeparator());
    //    for (int i = 0; i < buf.length; i++) {
    //        blend.val = buf[i];
    //        str.append(BlendType.toString(blend.getBottomL()))
    //                .append(BlendType.toString(blend.getBottomR()));
    //    }
    //    return str.toString();
    //}

}
