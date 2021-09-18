/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package net.sourceforge.xbrz.awt;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.util.Objects;

public class XbrzOp implements BufferedImageOp {

    private final int factor;

    public XbrzOp(int factor) {
        this.factor = factor;
    }

    @Override
    public RenderingHints getRenderingHints() {
        return null;
    }

    @Override
    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        Point2D point = (dstPt == null) ? new Point2D.Double() : dstPt;
        point.setLocation(srcPt.getX() * factor, srcPt.getY() * factor);
        return point;
    }

    @Override
    public Rectangle2D getBounds2D(BufferedImage src) {
        Point2D bottomRight = new Point2D.Double(src.getWidth(), src.getHeight());
        getPoint2D(bottomRight, bottomRight);
        return new Rectangle2D.Double(0, 0, bottomRight.getX(), bottomRight.getY());
    }

    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        Rectangle bounds = getBounds2D(src).getBounds();
        if (destCM == null) {
            return new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
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

        BufferedImage xbrz = AwtXbrz.scaleImage(src, factor);
        if (dst == null) {
            return xbrz;
        }

        Graphics2D g = dst.createGraphics();
        try {
            g.drawImage(xbrz, 0, 0, null);
        } finally {
            g.dispose();
        }
        return dst;
    }

}