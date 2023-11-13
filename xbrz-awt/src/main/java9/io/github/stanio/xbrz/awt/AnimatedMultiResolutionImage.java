/*
 * Copyright (C) 2023 by Stanio <stanio AT yahoo DOT com>
 * Released under BSD Zero Clause License: https://spdx.org/licenses/0BSD
 */
package io.github.stanio.xbrz.awt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.awt.Image;
import java.awt.image.ImageProducer;

import io.github.stanio.xbrz.ColorDistance;
import io.github.stanio.xbrz.Xbrz;
import io.github.stanio.xbrz.Xbrz.ScalerCfg;
import io.github.stanio.xbrz.awt.util.BaseMultiResolutionImage;

class AnimatedMultiResolutionImage extends BaseMultiResolutionImage {

    private final Image baseImage;

    private int lastFactor;
    private Image lastVariant;

    AnimatedMultiResolutionImage(Image baseImage) {
        super(preloadDimensions(baseImage).getWidth(null),
                baseImage.getHeight(null));
        this.baseImage = baseImage;
    }

    @Override
    protected Image getBaseImage() {
        return baseImage;
    }

    @Override
    public Image getResolutionVariant(double destWidth, double destHeight) {
        int width = (int) Math.ceil(destWidth);
        int height = (int) Math.ceil(destHeight);
        int factor = AwtXbrz.findFactor(baseWidth, baseHeight, width, height);
        if (factor == lastFactor) {
            return lastVariant;
        }

        ImageProducer lastSource = (lastVariant == null) ? null
                                                         : lastVariant.getSource();
        if (lastSource instanceof SuspendableFilteredSource) {
            ((SuspendableFilteredSource) lastSource).stopProduction();
        }

        Xbrz scaler = scalers.computeIfAbsent(factor, k ->
                new Xbrz(k, true, new ScalerCfg(), ColorDistance.bufferedYCbCr(5)));
        lastFactor = factor;
        return lastVariant = preloadDimensions(
                XbrzFilter.createScaledImage(baseImage, scaler));
    }

    private static final Map<Integer, Xbrz> scalers = new ConcurrentHashMap<>();

}
