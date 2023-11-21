package io.github.stanio.xbrz.awt.util;

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
 * Smoother downscale result for factors &gt; 2x.
 *
 * @see  <a href="https://web.archive.org/web/20080516181120/http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html"
 *              >The Perils of Image.getScaledInstance()</a> <i>by Chris Campbell (archived from
 *              &lt;https://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html&gt;)</i>
 * @see  <a href="https://blog.nobel-joergensen.com/2008/12/20/downscaling-images-in-java/"
 *              >Downscaling images in Java</a> <i>by Morten Nobel-Jørgensen</i>
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
        BufferedImage target = dest;
        if (target == null) {
            target = createCompatibleDestImage(src, null);
        }
        return resizeSmooth(src, target, destWidth, destHeight);
    }

    private BufferedImage resizeSmooth(BufferedImage src,
                                       BufferedImage dest,
                                       int targetWidth,
                                       int targetHeight) {
        int halfWidth = (int) Math.ceil(src.getWidth() / 2);
        int halfHeight = (int) Math.ceil(src.getHeight() / 2);

        BufferedImage source = src;
        if (targetWidth < halfWidth
                || targetHeight < halfHeight) {
            int w = (targetWidth < halfWidth) ? targetWidth * 2 : targetWidth;
            int h = (targetHeight < halfHeight) ? targetHeight * 2 : targetHeight;
            source = resizeSmooth(src, null, w, h);
        }

        BufferedImage target = dest;
        if (target == null) {
            int imageType = src.getColorModel().hasAlpha()
                            ? BufferedImage.TYPE_INT_ARGB
                            : BufferedImage.TYPE_INT_RGB;
            target = new BufferedImage(targetWidth, targetHeight, imageType);
        }
        Graphics2D g = target.createGraphics();
        if (getRenderingHints() == null) {
            setSmoothHints(g);
        } else {
            g.setRenderingHints(getRenderingHints());
        }
        try {
            g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private static void setSmoothHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                           RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    }

}
