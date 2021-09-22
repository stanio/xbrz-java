package io.github.stanio.xbrz.awt.util;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Combines multiple filters.
 */
public class ChainFilterOp implements BufferedImageOp {

    private List<BufferedImageOp> filterChain;

    public ChainFilterOp() {
        filterChain = new ArrayList<>();
    }

    public static ChainFilterOp first(BufferedImageOp first) {
        return new ChainFilterOp().next(first);
    }

    public ChainFilterOp next(BufferedImageOp next) {
        filterChain.add(next);
        return this;
    }

    private List<BufferedImageOp> filterChain() {
        if (filterChain.isEmpty()) {
            throw new IllegalStateException("No filters added to the chain");
        }
        return filterChain;
    }

    @Override
    public RenderingHints getRenderingHints() {
        BufferedImageOp lastOp = filterChain().get(filterChain.size() - 1);
        return lastOp.getRenderingHints();
    }

    @Override
    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        Point2D current = srcPt;
        Point2D result = dstPt;
        List<BufferedImageOp> chain = filterChain();
        for (int i = 0, len = chain.size(); i < len; i++) {
            result = current = chain.get(i).getPoint2D(current, result);
        }
        return result;
    }

    @Override
    public Rectangle2D getBounds2D(BufferedImage src) {
        BufferedImage dest = createCompatibleDestImage(src, null);
        return new Rectangle(dest.getWidth(), dest.getHeight());
    }

    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        BufferedImage result = src;
        List<BufferedImageOp> chain = filterChain();
        for (int i = 0, len = chain.size(); i < len; i++) {
            result = chain.get(i).createCompatibleDestImage(result, destCM);
        }
        return result;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        BufferedImage current = src;
        List<BufferedImageOp> chain = filterChain();
        for (int i = 0, len = chain.size() - 1; i < len; i++) {
            current = chain.get(i).filter(current, null);
        }
        return chain.get(chain.size() - 1).filter(current, dest);
    }

}
