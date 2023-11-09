package io.github.stanio.xbrz;

import static io.github.stanio.xbrz.BlendInfo.*;
import static io.github.stanio.xbrz.BlendType.*;
import static io.github.stanio.xbrz.RotationDegree.*;

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
        /** <i>luminanceWeight</i> */
        public double luminanceWeight            = 1;
        /** <i>equalColorTolerance</i> */
        public double equalColorTolerance        = 30;
        /** <i>centerDirectionBias</i> */
        public double centerDirectionBias        = 4;
        /** <i>dominantDirectionThreshold</i> */
        public double dominantDirectionThreshold = 3.6;
        /** <i>steepDirectionThreshold</i> */
        public double steepDirectionThreshold    = 2.2;
    }


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
    public int scale() {
        return scaler.scale();
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
            trg = new int[srcWidth * scale() * srcHeight * scale()];
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

        Kernel_3x3 ker3 = Kernel_3x3.instance(ker4);

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

}


enum RotationDegree { //clock-wise
    ROT_0,
    ROT_90,
    ROT_180,
    ROT_270
}


class Color {

    static int getAlpha(int pix) { return (pix >> 24) & 0xFF; }
    static int getRed  (int pix) { return (pix >> 16) & 0xFF; }
    static int getGreen(int pix) { return (pix >> 8)  & 0xFF; }
    static int getBlue (int pix) { return (pix >> 0)  & 0xFF; }

    static int makePixel(int a, int r, int g, int b) { return    (a << 24) | (r << 16) | (g << 8) | b; }
    static int makePixel(       int r, int g, int b) { return (0xFF << 24) | (r << 16) | (g << 8) | b; }

}
