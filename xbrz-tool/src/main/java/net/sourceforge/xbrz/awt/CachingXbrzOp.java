/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package net.sourceforge.xbrz.awt;

import java.awt.image.BufferedImage;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sourceforge.xbrz.awt.ImageData.Key;

public class CachingXbrzOp extends XbrzOp {

    private final Map<Key, Reference<BufferedImage>> cache = new ConcurrentHashMap<>();

    public CachingXbrzOp(int factor) {
        super(factor);
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        ImageData srcData = new ImageData(src, true);
        Key srcKey = srcData.getKey();
        Reference<BufferedImage> ref = cache.get(srcKey);
        BufferedImage result = null;
        if (ref != null) {
            result = ref.get();
        }
        if (result == null) {
            result = filter(srcData, dest);
            cache.put(srcKey, new SoftReference<>(result));
        }
        return result;
    }

}
