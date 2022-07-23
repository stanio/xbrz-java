/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package io.github.stanio.xbrz.awt.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import java.awt.Image;
import java.awt.image.AbstractMultiResolutionImage;
import java.awt.image.ImageObserver;
import java.awt.image.MultiResolutionImage;

import javax.swing.ImageIcon;

/**
 * A {@code MultiResolutionImage} that caches resolution variants as they
 * get produced.
 *
 * @implNote  Caches at most 2 variants.  This should possibly cover the
 *      most common scenario of displaying the same image (f.e. an icon)
 *      on two monitors with different scaling factors
 *      ({@code GraphicsConfiguration}s).
 */
public class MultiResolutionCachedImage extends AbstractMultiResolutionImage {

    private static final int CAPACITY = 2;

    protected final int baseWidth;
    protected final int baseHeight;

    private final BiFunction<Integer, Integer, Image> variantProducer;

    // REVISIT: Would shared cache yield a smaller memory usage?
    private volatile CachedVariant[] cache = new CachedVariant[CAPACITY];
    private volatile int cacheSize = 0;

    /**
     * Constructs a new {@code MultiResolutionCachedImage} of the given
     * base width and height, and resolution variant producer function.
     *
     * @param   baseWidth  the user-space logical with of the image
     * @param   baseHeight  the user-space logical height of the image
     * @param   variantProducer  function producing image variants at given
     *          width and height
     */
    public MultiResolutionCachedImage(int baseWidth, int baseHeight,
            BiFunction<Integer, Integer, Image> variantProducer)
    {
        this.baseWidth = baseWidth;
        this.baseHeight = baseHeight;
        this.variantProducer = Objects.requireNonNull(variantProducer, "variantProducer");
    }

    public static MultiResolutionCachedImage of(int baseWidth, int baseHeight,
            BiFunction<Integer, Integer, Image> variantProducer) {
        return new MultiResolutionCachedImage(baseWidth, baseHeight, variantProducer);
    }

    public MultiResolutionCachedImage map(Function<Image, Image> mapper) {
        return (MultiResolutionCachedImage) map(this, mapper);
    }

    /**
     * Maps the resolution variants of the given multi-resolution image using
     * the given mapper function.
     * <p>
     * <i>Sample usage:</i></p>
     * <pre>
     *     public static Image createDisabledImage(Image image) {
     *         if (i instanceof MultiResolutionImage) {
     *             return MultiResolutionCachedImage
     *                     .map((MultiResolutionImage) image,
     *                          (img) -> createDisabledImageImpl(img));
     *         }
     *         return createDisabledImageImpl(image);
     *     }
     *
     *     private static Image createDisabledImageImpl(Image image) {
     *         GrayFilter filter = new GrayFilter(true, 50);
     *         ImageProducer producer = new FilteredImageSource(image.getSource(), filter);
     *         return Toolkit.getDefaultToolkit().createImage(producer);
     *     }</pre>
     */
    public static Image map(MultiResolutionImage mrImage, Function<Image, Image> mapper) {
        Image image = (Image) mrImage;
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        return new MultiResolutionCachedImage(width, height,
                (w, h) -> mapper.apply(mrImage.getResolutionVariant(w, h)));
    }

    @Override
    public int getWidth(ImageObserver observer) {
        return baseWidth;
    }

    @Override
    public int getHeight(ImageObserver observer) {
        return baseHeight;
    }

    @Override
    protected Image getBaseImage() {
        return getResolutionVariant(getWidth(null), getHeight(null));
    }

    @Override
    public Image getScaledInstance(int width, int height, int hints) {
        return getResolutionVariant(width, height);
    }

    @Override
    public Image getResolutionVariant(double destWidth, double destHeight) {
        int width = (int) Math.ceil(destWidth);
        int height = (int) Math.ceil(destHeight);
        for (int i = 0, size = cacheSize; i < size; i++) {
            CachedVariant variant = cache[i];
            if (variant.width == destWidth
                    && variant.height == destHeight) {
                return variant.image;
            }
        }

        Image variant = variantProducer.apply(width, height);
        synchronized (cache) {
            if (cacheSize >= cache.length) {
                cacheSize = cache.length - 1;
                System.arraycopy(cache, 1, cache, 0, cacheSize);
            }
            cache[cacheSize++] = new CachedVariant(width, height, variant);
        }
        return variant;
    }

    @Override
    public List<Image> getResolutionVariants() {
        int size = cacheSize;
        if (size == 0) {
            return Collections.singletonList(getBaseImage());
        }

        List<Image> variants = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Image image = cache[i].image;
            if (!variants.contains(image)) {
                variants.add(image);
            }
        }
        return variants;
    }


    private static class CachedVariant {

        final int width;
        final int height;
        final Image image;

        CachedVariant(int width, int height, Image image) {
            this.width = width;
            this.height = height;
            this.image = new ImageIcon(image).getImage(); // preload
        }

    }


}
