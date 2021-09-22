package net.sourceforge.xbrz.awt.util;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;

/**
 * Smoother downscale result for factors > 2x.
 *
 * @see "<i>The Perils of Image.getScaledInstance()</i> by Chris Campbell
 * &lt;https://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html&gt;
 * (<a href='https://web.archive.org/web/20080516181120/http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html'
 * >archive.org</a>, <a href='https://archive.fo/dArdh'>archive.fo</a>)"
 * @see "<a href='https://blog.nobel-joergensen.com/2008/12/20/downscaling-images-in-java/'
 * ><i>Downscaling images in Java</i></a> by Morten Nobel-Jørgensen"
 */
public class SmoothResizeOp implements BufferedImageOp {

    private static final ColorModel RGB_OPAQUE =
            new DirectColorModel(24, 0x00FF0000, 0x0000FF00, 0x000000FF, 0);

    private final int destWidth;
    private final int destHeight;

    public SmoothResizeOp(int destWidth, int destHeight) {
        this.destWidth = destWidth;
        this.destHeight = destHeight;
    }

    @Override
    public RenderingHints getRenderingHints() {
        return null;
    }

    @Override
    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        Point2D result = (dstPt == null) ? (Point2D) srcPt.clone() : dstPt;
        result.setLocation(Math.min(srcPt.getX(), destWidth),
                           Math.min(srcPt.getY(), destHeight));
        return result;
    }

    @Override
    public Rectangle2D getBounds2D(BufferedImage src) {
        return new Rectangle(destWidth, destHeight);
    }

    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        ColorModel colorModel = destCM;
        if (colorModel == null) {
            colorModel = src.getColorModel().hasAlpha() ? ColorModel.getRGBdefault()
                                                        : RGB_OPAQUE;
        }
        return new BufferedImage(colorModel,
                colorModel.createCompatibleWritableRaster(destWidth, destHeight),
                colorModel.isAlphaPremultiplied(), null);
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        BufferedImage source = scaleBelowThreshold(src);
        BufferedImage target = dest;
        if (target == null) {
            target = createCompatibleDestImage(src, null);
        }

        Graphics2D g = target.createGraphics();
        if (getRenderingHints() == null) {
            setSmoothHints(g);
        } else {
            g.setRenderingHints(getRenderingHints());
        }
        try {
            g.drawImage(source, 0, 0, destWidth, destHeight, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private BufferedImage scaleBelowThreshold(BufferedImage src) {
        final int thresholdWidth = destWidth << 1;
        final int thresholdHeight = destHeight << 1;
        BufferedImage current = src;
        int currentWidth = src.getWidth();
        int currentHeight = src.getHeight();
        while (currentWidth > thresholdWidth || currentHeight > thresholdHeight) {
            if (currentWidth > thresholdWidth)
                currentWidth /= 2;
            if (currentHeight > thresholdHeight)
                currentHeight /= 2;

            current = scaleSmooth(current, currentWidth, currentHeight);
        }
        return current;
    }

    private static BufferedImage scaleSmooth(BufferedImage source,
                                             int destWidth,
                                             int destHeight) {
        BufferedImage variant = new BufferedImage(destWidth, destHeight,
                                                  source.getColorModel().hasAlpha()
                                                  ? BufferedImage.TYPE_INT_ARGB
                                                  : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = variant.createGraphics();
        setSmoothHints(g);
        try {
            g.drawImage(source, 0, 0, destWidth, destHeight, null);
        } finally {
            g.dispose();
        }
        return variant;
    }

    private static void setSmoothHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                           RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    }

}
