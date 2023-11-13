/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package io.github.stanio.xbrz.awt.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.function.Function;

import java.awt.Image;
import java.awt.image.MultiResolutionImage;

/**
 * A {@code MultiResolutionImage} that caches resolution variants as they
 * get produced.
 *
 * @implNote  Caches at most 4 variants.  This should possibly cover the
 *      most common scenario of displaying the same image (f.e. an icon)
 *      on two monitors with different scaling factors
 *      ({@code GraphicsConfiguration}s).
 */
public abstract class MultiResolutionCachedImage extends BaseMultiResolutionImage {

    private static final int CAPACITY = 4;
    private final List<CachedVariant> cache = new ArrayList<>(CAPACITY);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Constructs a new {@code MultiResolutionCachedImage} of the given
     * base width and height, and resolution variant producer function.
     *
     * @param   baseWidth  the user-space logical with of the image
     * @param   baseHeight  the user-space logical height of the image
     */
    public MultiResolutionCachedImage(int baseWidth, int baseHeight) {
        super(baseWidth, baseHeight);
    }

    public static MultiResolutionCachedImage
            of(int baseWidth, int baseHeight,
                    BiFunction<Integer, Integer, Image> variantProducer) {
        return withProducer(baseWidth, baseHeight, variantProducer);
    }

    public static MultiResolutionCachedImage
            withProducer(int baseWidth, int baseHeight,
                    BiFunction<Integer, Integer, Image> variantProducer) {
        return new MultiResolutionCachedImage(baseWidth, baseHeight) {
            @Override protected
            Image createResolutionVariant(int width, int height) {
                return variantProducer.apply(width, height);
            }
        };
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
     * <code>    public static Image createDisabledImage(Image image) {
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
     *     }</code></pre>
     */
    public static Image map(MultiResolutionImage mrImage, Function<Image, Image> mapper) {
        Image image = (Image) mrImage;
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        return MultiResolutionCachedImage.withProducer(width, height,
                (w, h) -> mapper.apply(mrImage.getResolutionVariant(w, h)));
    }

    @Override
    public Image getResolutionVariant(double destWidth, double destHeight) {
        int width = (int) Math.ceil(destWidth);
        int height = (int) Math.ceil(destHeight);
        Image variant = findCachedVariant(width, height);
        if (variant == null) {
            variant = createResolutionVariant(width, height);
            cacheVariant(width, height, variant);
            preloadDimensions(variant);
        }
        return variant;
    }

    private Image findCachedVariant(int width, int height) {
        lock.readLock().lock();
        try {
            for (int i = cache.size() - 1; i >= 0; i--) {
                CachedVariant variant = cache.get(i);
                Image image = variant.get();
                if (image == null)
                    continue;

                if (variant.width == width
                        && variant.height == height)
                    return image;
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    private void cacheVariant(int width, int height, Image image) {
        lock.writeLock().lock();
        try {
            for (int i = cache.size() - 1; i >= 0; i--) {
                CachedVariant variant = cache.get(i);
                if (variant.get() == null) {
                    cache.remove(i);
                } else if (variant.width == width
                        && variant.height == height) {
                    // Possibly the same size has been added concurrently.
                    return;
                }
            }
            if (cache.size() >= CAPACITY) {
                cache.remove(0);
            }
            cache.add(new CachedVariant(width, height, image));
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected abstract Image createResolutionVariant(int width, int height);

    @Override
    public List<Image> getResolutionVariants() {
        if (cache.size() == 0) {
            return Collections.singletonList(getBaseImage());
        }

        List<Image> variants = new ArrayList<>(cache.size());
        lock.readLock().lock();
        try {
            for (int i = cache.size() - 1; i >= 0; i--) {
                Image image = cache.get(i).get();
                if (image != null && !variants.contains(image)) {
                    variants.add(image);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return variants;
    }


    private static class CachedVariant extends WeakReference<Image> {

        final int width;
        final int height;

        CachedVariant(int width, int height, Image image) {
            super(image);
            this.width = width;
            this.height = height;
        }

    }


}
