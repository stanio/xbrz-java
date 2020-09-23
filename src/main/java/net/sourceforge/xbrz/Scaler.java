package net.sourceforge.xbrz;

import static net.sourceforge.xbrz.Color.*;
import static net.sourceforge.xbrz.ColorGradient.gradientARGB;
import static net.sourceforge.xbrz.ColorGradient.gradientRGB;

interface Scaler {

    int scale();

    void blendLineShallow(int col, OutputMatrix out);

    void blendLineSteep(int col, OutputMatrix out);

    void blendLineSteepAndShallow(int col, OutputMatrix out);

    void blendLineDiagonal(int col, OutputMatrix out);

    void blendCorner(int col, OutputMatrix out);

    static Scaler forFactor(int factor, boolean withAlpha) {
        switch (factor) {
        case 2: return new Scaler2x(withAlpha);
        case 3: return new Scaler3x(withAlpha);
        case 4: return new Scaler4x(withAlpha);
        case 5: return new Scaler5x(withAlpha);
        case 6: return new Scaler6x(withAlpha);
        default:
            throw new IllegalArgumentException("Illegal scaling factor: " + factor);
        }
    }

}


abstract class AbstractScaler implements Scaler {

    protected final int scale;

    private ColorGradient colorGradient;

    protected AbstractScaler(int scale, boolean withAlpha) {
        this(scale, withAlpha ? gradientARGB() : gradientRGB());
    }

    protected AbstractScaler(int scale, ColorGradient colorGradient) {
        this.scale = scale;
        this.colorGradient = colorGradient;
    }

    @Override
    public final int scale() {
        return scale;
    }

    protected final int alphaGrad(int M, int N, int pixBack, int pixFront) {
        return colorGradient.alphaGrad(M, N, pixBack, pixFront);
    }

}


class Scaler2x extends AbstractScaler {

    public Scaler2x(boolean withAlpha) {
        super(2, withAlpha);
    }

    @Override
    public void blendLineShallow(int col, OutputMatrix out) {
        out.set(scale - 1, 0, ref -> alphaGrad(1, 4, ref, col));
        out.set(scale - 1, 1, ref -> alphaGrad(3, 4, ref, col));
    }

    @Override
    public void blendLineSteep(int col, OutputMatrix out) {
        out.set(0, scale - 1, ref -> alphaGrad(1, 4, ref, col));
        out.set(1, scale - 1, ref -> alphaGrad(3, 4, ref, col));
    }

    @Override
    public void blendLineSteepAndShallow(int col, OutputMatrix out) {
        out.set(1, 0, ref -> alphaGrad(1, 4, ref, col));
        out.set(0, 1, ref -> alphaGrad(1, 4, ref, col));
        out.set(1, 1, ref -> alphaGrad(5, 6, ref, col)); // [!] fixes 7/8 used in xBR
    }

    @Override
    public void blendLineDiagonal(int col, OutputMatrix out) {
        out.set(1, 1, ref -> alphaGrad(1, 2, ref, col));
    }

    @Override
    public void blendCorner(int col, OutputMatrix out) {
        // model a round corner
        out.set(1, 1, ref -> alphaGrad(21, 100, ref, col)); // exact: 1 - pi/4 = 0.2146018366
    }

}


class Scaler3x extends AbstractScaler {

    public Scaler3x(boolean withAlpha) {
        super(3, withAlpha);
    }

    @Override
    public void blendLineShallow(int col, OutputMatrix out) {
        out.set(scale - 1, 0, ref -> alphaGrad(1, 4, ref, col));
        out.set(scale - 2, 2, ref -> alphaGrad(1, 4, ref, col));

        out.set(scale - 1, 1, ref -> alphaGrad(3, 4, ref, col));
        out.set(scale - 1, 2, col);
    }

    @Override
    public void blendLineSteep(int col, OutputMatrix out) {
        out.set(0, scale - 1, ref -> alphaGrad(1, 4, ref, col));
        out.set(2, scale - 2, ref -> alphaGrad(1, 4, ref, col));

        out.set(1, scale - 1, ref -> alphaGrad(3, 4, ref, col));
        out.set(2, scale - 1, col);
    }

    @Override
    public void blendLineSteepAndShallow(int col, OutputMatrix out) {
        out.set(2, 0, ref -> alphaGrad(1, 4, ref, col));
        out.set(0, 2, ref -> alphaGrad(1, 4, ref, col));
        out.set(2, 1, ref -> alphaGrad(3, 4, ref, col));
        out.set(1, 2, ref -> alphaGrad(3, 4, ref, col));
        out.set(2, 2, col);
    }

    @Override
    public void blendLineDiagonal(int col, OutputMatrix out) {
        out.set(1, 2, ref -> alphaGrad(1, 8, ref, col)); // conflict with other rotations for this odd scale
        out.set(2, 1, ref -> alphaGrad(1, 8, ref, col));
        out.set(2, 2, ref -> alphaGrad(7, 8, ref, col)); //
    }

    @Override
    public void blendCorner(int col, OutputMatrix out) {
        // model a round corner
        out.set(2, 2, ref -> alphaGrad(45, 100, ref, col)); // exact: 0.4545939598
        //out.set(2, 1, ref -> alphaGrad(7, 256, ref, col)); // 0.02826017254 -> negligible + avoid conflicts with other rotations for this odd scale
        //out.set(1, 2, ref -> alphaGrad(7, 256, ref, col)); // 0.02826017254
    }

}


class Scaler4x extends AbstractScaler {

    public Scaler4x(boolean withAlpha) {
        super(4, withAlpha);
    }

    @Override
    public void blendLineShallow(int col, OutputMatrix out) {
        out.set(scale - 1, 0, ref -> alphaGrad(1, 4, ref, col));
        out.set(scale - 2, 2, ref -> alphaGrad(1, 4, ref, col));

        out.set(scale - 1, 1, ref -> alphaGrad(3, 4, ref, col));
        out.set(scale - 2, 3, ref -> alphaGrad(3, 4, ref, col));

        out.set(scale - 1, 2, col);
        out.set(scale - 1, 3, col);
    }

    @Override
    public void blendLineSteep(int col, OutputMatrix out) {
        out.set(0, scale - 1, ref -> alphaGrad(1, 4, ref, col));
        out.set(2, scale - 2, ref -> alphaGrad(1, 4, ref, col));

        out.set(1, scale - 1, ref -> alphaGrad(3, 4, ref, col));
        out.set(3, scale - 2, ref -> alphaGrad(3, 4, ref, col));

        out.set(2, scale - 1, col);
        out.set(3, scale - 1, col);
    }

    @Override
    public void blendLineSteepAndShallow(int col, OutputMatrix out) {
        out.set(3, 1, ref -> alphaGrad(3, 4, ref, col));
        out.set(1, 3, ref -> alphaGrad(3, 4, ref, col));
        out.set(3, 0, ref -> alphaGrad(1, 4, ref, col));
        out.set(0, 3, ref -> alphaGrad(1, 4, ref, col));

        out.set(2, 2, ref -> alphaGrad(1, 3, ref, col)); //[!] fixes 1/4 used in xBR

        out.set(3, 3, col);
        out.set(3, 2, col);
        out.set(2, 3, col);
    }

    @Override
    public void blendLineDiagonal(int col, OutputMatrix out) {
        out.set(scale - 1, scale / 2    , ref -> alphaGrad(1, 2, ref, col));
        out.set(scale - 2, scale / 2 + 1, ref -> alphaGrad(1, 2, ref, col));
        out.set(scale - 1, scale - 1, col);
    }

    @Override
    public void blendCorner(int col, OutputMatrix out) {
        // model a round corner
        out.set(3, 3, ref -> alphaGrad(68, 100, ref, col)); // exact: 0.6848532563
        out.set(3, 2, ref -> alphaGrad( 9, 100, ref, col)); // 0.08677704501
        out.set(2, 3, ref -> alphaGrad( 9, 100, ref, col)); // 0.08677704501
    }

}


class Scaler5x extends AbstractScaler {

    public Scaler5x(boolean withAlpha) {
        super(5, withAlpha);
    }

    @Override
    public void blendLineShallow(int col, OutputMatrix out) {
        out.set(scale - 1, 0, ref -> alphaGrad(1, 4, ref, col));
        out.set(scale - 2, 2, ref -> alphaGrad(1, 4, ref, col));
        out.set(scale - 3, 4, ref -> alphaGrad(1, 4, ref, col));

        out.set(scale - 1, 1, ref -> alphaGrad(3, 4, ref, col));
        out.set(scale - 2, 3, ref -> alphaGrad(3, 4, ref, col));

        out.set(scale - 1, 2, col);
        out.set(scale - 1, 3, col);
        out.set(scale - 1, 4, col);
        out.set(scale - 2, 4, col);
    }

    @Override
    public void blendLineSteep(int col, OutputMatrix out) {
        out.set(0, scale - 1, ref -> alphaGrad(1, 4, ref, col));
        out.set(2, scale - 2, ref -> alphaGrad(1, 4, ref, col));
        out.set(4, scale - 3, ref -> alphaGrad(1, 4, ref, col));

        out.set(1, scale - 1, ref -> alphaGrad(3, 4, ref, col));
        out.set(3, scale - 2, ref -> alphaGrad(3, 4, ref, col));

        out.set(2, scale - 1, col);
        out.set(3, scale - 1, col);
        out.set(4, scale - 1, col);
        out.set(4, scale - 2, col);
    }

    @Override
    public void blendLineSteepAndShallow(int col, OutputMatrix out) {
        out.set(0, scale - 1, ref -> alphaGrad(1, 4, ref, col));
        out.set(2, scale - 2, ref -> alphaGrad(1, 4, ref, col));
        out.set(1, scale - 1, ref -> alphaGrad(3, 4, ref, col));

        out.set(scale - 1, 0, ref -> alphaGrad(1, 4, ref, col));
        out.set(scale - 2, 2, ref -> alphaGrad(1, 4, ref, col));
        out.set(scale - 1, 1, ref -> alphaGrad(3, 4, ref, col));

        out.set(3, 3, ref -> alphaGrad(2, 3, ref, col));

        out.set(2, scale - 1, col);
        out.set(3, scale - 1, col);
        out.set(4, scale - 1, col);

        out.set(scale - 1, 2, col);
        out.set(scale - 1, 3, col);
    }

    @Override
    public void blendLineDiagonal(int col, OutputMatrix out) {
        out.set(scale - 1, scale / 2    , ref -> alphaGrad(1, 8, ref, col)); //conflict with other rotations for this odd scale
        out.set(scale - 2, scale / 2 + 1, ref -> alphaGrad(1, 8, ref, col));
        out.set(scale - 3, scale / 2 + 2, ref -> alphaGrad(1, 8, ref, col)); //

        out.set(4, 3, ref -> alphaGrad(7, 8, ref, col));
        out.set(3, 4, ref -> alphaGrad(7, 8, ref, col));

        out.set(4, 4, col);
    }

    @Override
    public void blendCorner(int col, OutputMatrix out) {
        // model a round corner
        out.set(4, 4, ref -> alphaGrad(86, 100, ref, col)); // exact: 0.8631434088
        out.set(4, 3, ref -> alphaGrad(23, 100, ref, col)); // 0.2306749731
        out.set(3, 4, ref -> alphaGrad(23, 100, ref, col)); // 0.2306749731
        //out.set(4, 2, ref -> alphaGrad(1, 64, ref, col)); // 0.01676812367 -> negligible + avoid conflicts with other rotations for this odd scale
        //out.set(2, 4, ref -> alphaGrad(1, 64, ref, col)); // 0.01676812367
    }

}


class Scaler6x extends AbstractScaler {

    public Scaler6x(boolean withAlpha) {
        super(6, withAlpha);
    }

    @Override
    public void blendLineShallow(int col, OutputMatrix out) {
        out.set(scale - 1, 0, ref -> alphaGrad(1, 4, ref, col));
        out.set(scale - 2, 2, ref -> alphaGrad(1, 4, ref, col));
        out.set(scale - 3, 4, ref -> alphaGrad(1, 4, ref, col));

        out.set(scale - 1, 1, ref -> alphaGrad(3, 4, ref, col));
        out.set(scale - 2, 3, ref -> alphaGrad(3, 4, ref, col));
        out.set(scale - 3, 5, ref -> alphaGrad(3, 4, ref, col));

        out.set(scale - 1, 2, col);
        out.set(scale - 1, 3, col);
        out.set(scale - 1, 4, col);
        out.set(scale - 1, 5, col);

        out.set(scale - 2, 4, col);
        out.set(scale - 2, 5, col);
    }

    @Override
    public void blendLineSteep(int col, OutputMatrix out) {
        out.set(0, scale - 1, ref -> alphaGrad(1, 4, ref, col));
        out.set(2, scale - 2, ref -> alphaGrad(1, 4, ref, col));
        out.set(4, scale - 3, ref -> alphaGrad(1, 4, ref, col));

        out.set(1, scale - 1, ref -> alphaGrad(3, 4, ref, col));
        out.set(3, scale - 2, ref -> alphaGrad(3, 4, ref, col));
        out.set(5, scale - 3, ref -> alphaGrad(3, 4, ref, col));

        out.set(2, scale - 1, col);
        out.set(3, scale - 1, col);
        out.set(4, scale - 1, col);
        out.set(5, scale - 1, col);

        out.set(4, scale - 2, col);
        out.set(5, scale - 2, col);
    }

    @Override
    public void blendLineSteepAndShallow(int col, OutputMatrix out) {
        out.set(0, scale - 1, ref -> alphaGrad(1, 4, ref, col));
        out.set(2, scale - 2, ref -> alphaGrad(1, 4, ref, col));
        out.set(1, scale - 1, ref -> alphaGrad(3, 4, ref, col));
        out.set(3, scale - 2, ref -> alphaGrad(3, 4, ref, col));

        out.set(scale - 1, 0, ref -> alphaGrad(1, 4, ref, col));
        out.set(scale - 2, 2, ref -> alphaGrad(1, 4, ref, col));
        out.set(scale - 1, 1, ref -> alphaGrad(3, 4, ref, col));
        out.set(scale - 2, 3, ref -> alphaGrad(3, 4, ref, col));

        out.set(2, scale - 1, col);
        out.set(3, scale - 1, col);
        out.set(4, scale - 1, col);
        out.set(5, scale - 1, col);

        out.set(4, scale - 2, col);
        out.set(5, scale - 2, col);

        out.set(scale - 1, 2, col);
        out.set(scale - 1, 3, col);
    }

    @Override
    public void blendLineDiagonal(int col, OutputMatrix out) {
        out.set(scale - 1, scale / 2    , ref -> alphaGrad(1, 2, ref, col));
        out.set(scale - 2, scale / 2 + 1, ref -> alphaGrad(1, 2, ref, col));
        out.set(scale - 3, scale / 2 + 2, ref -> alphaGrad(1, 2, ref, col));

        out.set(scale - 2, scale - 1, col);
        out.set(scale - 1, scale - 1, col);
        out.set(scale - 1, scale - 2, col);
    }

    @Override
    public void blendCorner(int col, OutputMatrix out) {
        // model a round corner
        out.set(5, 5, ref -> alphaGrad(97, 100, ref, col)); // exact: 0.9711013910
        out.set(4, 5, ref -> alphaGrad(42, 100, ref, col)); // 0.4236372243
        out.set(5, 4, ref -> alphaGrad(42, 100, ref, col)); // 0.4236372243
        out.set(5, 3, ref -> alphaGrad( 6, 100, ref, col)); // 0.05652034508
        out.set(3, 5, ref -> alphaGrad( 6, 100, ref, col)); // 0.05652034508
    }

}


interface ColorGradient {

    int alphaGrad(int M, int N, int pixBack, int pixFront);

    static ColorGradient gradientRGB() {
        return new ColorGradientRGB();
    }

    static ColorGradient gradientARGB() {
        return new ColorGradientARGB();
    }

}


class ColorGradientRGB implements ColorGradient {

    private static int calcColor(int M, int N, int colFront, int colBack) {
        return (colFront * M + colBack * (N - M)) / N;
    }

    @Override
    // blend front color with opacity M / N over opaque background: https://en.wikipedia.org/wiki/Alpha_compositing#Alpha_blending
    public int alphaGrad(int M, int N, int pixBack, int pixFront) {
        //assert (0 < M && M < N && N <= 1000);

        return makePixel(calcColor(M, N, getRed(pixFront), getRed(pixBack)),
                         calcColor(M, N, getGreen(pixFront), getGreen(pixBack)),
                         calcColor(M, N, getBlue(pixFront), getBlue(pixBack)));
    }

}

class ColorGradientARGB implements ColorGradient {

    private static int calcColor(int weightFront, int weightBack, int weightSum, int colFront, int colBack) {
        return (colFront * weightFront + colBack * weightBack) / weightSum;
    }

    @Override
    // find intermediate color between two colors with alpha channels (=> NO alpha blending!!!)
    public int alphaGrad(int M, int N, int pixBack, int pixFront) {
        //assert (0 < M && M < N && N <= 1000);

        final int weightFront = getAlpha(pixFront) * M;
        final int weightBack = getAlpha(pixBack) * (N - M);
        final int weightSum = weightFront + weightBack;
        if (weightSum == 0)
            return 0;

        return makePixel(weightSum / N,
                         calcColor(weightFront, weightBack, weightSum, getRed(pixFront), getRed(pixBack)),
                         calcColor(weightFront, weightBack, weightSum, getGreen(pixFront), getGreen(pixBack)),
                         calcColor(weightFront, weightBack, weightSum, getBlue(pixFront), getBlue(pixBack)));
    }

}
