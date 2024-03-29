/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package io.github.stanio.xbrz.awt;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.util.Objects;

/**
 * Single-input/single-output xBRZ operation.
 */
public class XbrzOp implements BufferedImageOp {

    private final int factor;

    private final RenderingHints hints;

    private final boolean directBuffer;

    public XbrzOp(int factor) {
        this(factor, null);
    }

    public XbrzOp(int factor, RenderingHints hints) {
        this(factor, false, hints);
    }

    XbrzOp(int factor, boolean direct, RenderingHints hints) {
        this.factor = factor;
        this.directBuffer = direct;
        this.hints = hints;
    }

    @Override
    public RenderingHints getRenderingHints() {
        return hints;
    }

    @Override
    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        Point2D point = (dstPt == null) ? new Point2D.Double() : dstPt;
        point.setLocation(srcPt.getX() * factor, srcPt.getY() * factor);
        return point;
    }

    @Override
    public Rectangle2D getBounds2D(BufferedImage src) {
        return new Rectangle2D.Double(0, 0, (double) src.getWidth() * factor,
                                            (double) src.getHeight() * factor);
    }

    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        Rectangle bounds = getBounds2D(src).getBounds();
        if (destCM == null) {
            return new BufferedImage(bounds.width, bounds.height,
                                     src.getColorModel().hasAlpha()
                                     ? BufferedImage.TYPE_INT_ARGB
                                     : BufferedImage.TYPE_INT_RGB);
        }
        return new BufferedImage(destCM,
                destCM.createCompatibleWritableRaster(bounds.width, bounds.height),
                destCM.isAlphaPremultiplied(), null);
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        Objects.requireNonNull(src, "src image is null");
        if (src == dst) {
            throw new IllegalArgumentException("src image cannot be the same as the dst image");
        }
        return filter(new ImageData(src, directBuffer), dst);
    }

    BufferedImage filter(ImageData src, BufferedImage dst) {
        BufferedImage xbrz = AwtXbrz.scaleImage(src, factor, directBuffer);
        if (dst == null) {
            return xbrz;
        }

        if (hints == null) {
            int[] rgb = ((DataBufferInt) xbrz.getRaster().getDataBuffer()).getData();
            int w = Math.min(xbrz.getWidth(), dst.getWidth());
            int h = Math.min(xbrz.getHeight(), dst.getHeight());
            dst.setRGB(0, 0, w, h, rgb, 0, xbrz.getWidth());
        } else {
            Graphics2D g = dst.createGraphics();
            try {
                g.setRenderingHints(hints);
                g.drawImage(xbrz, 0, 0, null);
            } finally {
                g.dispose();
            }
        }
        return dst;
    }

}
