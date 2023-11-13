/*
 * Copyright (C) 2023 by Stanio <stanio AT yahoo DOT com>
 * Released under BSD Zero Clause License: https://spdx.org/licenses/0BSD
 */
package io.github.stanio.xbrz.awt.util;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import java.awt.Image;
import java.awt.image.AbstractMultiResolutionImage;
import java.awt.image.ImageObserver;

/**
 * Convenience base {@code MultiResolutionImage} implementation...
 */
public abstract class BaseMultiResolutionImage extends AbstractMultiResolutionImage {

    protected final int baseWidth;
    protected final int baseHeight;

    /**
     * Initializes a {@code BaseMultiResolutionImage} with the given
     * base width and height properties.
     *
     * @param   baseWidth  the user-space logical with of the image
     * @param   baseHeight  the user-space logical height of the image
     */
    protected BaseMultiResolutionImage(int baseWidth, int baseHeight) {
        this.baseWidth = baseWidth;
        this.baseHeight = baseHeight;
    }

    /**
     * Returns a {@code BaseMultiResolutionImage} delegating to the given
     * resolution {@code variantProducer}.
     * <p>
     * The returned {@code MultiResolutionImage} implementation will return
     * the last produced variant if request for the same destination width and
     * height, as at the time the variant has been created, is received.  That
     * is it won't continuously request creation of the same variant in
     * succession.</p>
     *
     * @param   baseWidth  ...
     * @param   baseHeight  ...
     * @param   variantProducer  ...
     * @return  A {@code MultiResolutionImage} ...
     */
    public static BaseMultiResolutionImage
            withProducer(int baseWidth, int baseHeight,
                    BiFunction<Integer, Integer, Image> variantProducer) {
        return new BaseMultiResolutionImage(baseWidth, baseHeight) {
            private int lastWidth;
            private int lastHeight;
            private Image lastImage;

            @Override
            public List<Image> getResolutionVariants() {
                return lastImage == null ? super.getResolutionVariants()
                                         : Collections.singletonList(lastImage);
            }

            @Override
            public Image getResolutionVariant(double destWidth, double destHeight) {
                int width = (int) Math.ceil(destWidth);
                int height = (int) Math.ceil(destHeight);
                if (width == lastWidth && height == lastHeight) {
                    return lastImage;
                }
                lastWidth = width;
                lastHeight = height;
                return lastImage = preloadDimensions(variantProducer.apply(width, height));
            }
        };
    }

    /**
     * Returns the {@code baseWidth} specified during the initialization.
     */
    @Override
    public int getWidth(ImageObserver observer) {
        return baseWidth;
    }

    /**
     * Returns the {@code baseHeight} specified during the initialization.
     */
    @Override
    public int getHeight(ImageObserver observer) {
        return baseHeight;
    }

    /**
     * This implementation delegates to
     * {@code getResolutionVariant(baseWidth, baseHeight)}.
     */
    @Override
    protected Image getBaseImage() {
        return getResolutionVariant(baseWidth, baseHeight);
    }

    /**
     * This implementation returns {@code UndefinedProperty}.
     */
    @Override
    public Object getProperty(String name, ImageObserver observer) {
        return UndefinedProperty;
    }

    /**
     * This implementation delegates to
     * {@code getResolutionVariant(width, height)}.
     */
    @Override
    public Image getScaledInstance(int width, int height, int hints) {
        return getResolutionVariant(width, height);
    }

    /**
     * This implementation returns a singleton list containing the
     * {@link #getBaseImage() baseImage}.
     */
    @Override
    public List<Image> getResolutionVariants() {
        return Collections.singletonList(getBaseImage());
    }

    @Override
    public abstract Image getResolutionVariant(double destWidth, double destHeight);

    /**
     * Preload dimensions of Toolkit images.  This is crucial for the Java 2D
     * drawing pipeline.  If dimensions are not immediately available, a
     * {@code getResolutionVariant()} request for the base width and height is
     * made, that could trigger unexpected internal loops, depending on the
     * actual implementation.
     */
    protected static Image preloadDimensions(Image image) {
        CountDownLatch loadLatch = new CountDownLatch(1);
        ImageObserver dimensionsObserver = new ImageObserver() {
            private static final int dimensionFlags = WIDTH | HEIGHT;
            private static final int errorFlags = ERROR | ABORT;

            private int availableInfo;

            @Override public boolean imageUpdate(Image img,
                    int infoflags, int x, int y, int width, int height) {
                availableInfo |= infoflags;
                if ((availableInfo & dimensionFlags) == dimensionFlags
                        || (availableInfo & errorFlags) != 0) {
                    loadLatch.countDown();
                    return false;
                }
                return true;
            }
        };

        int w = image.getWidth(dimensionsObserver);
        int h = image.getHeight(dimensionsObserver);
        if (w > 0 && h > 0) {
            return image;
        }

        try {
            loadLatch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return image;
    }

}
