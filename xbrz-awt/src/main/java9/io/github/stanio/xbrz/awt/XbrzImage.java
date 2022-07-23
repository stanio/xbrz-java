/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package io.github.stanio.xbrz.awt;

import java.util.Objects;

import java.awt.Image;
import java.awt.image.AbstractMultiResolutionImage;

import javax.swing.ImageIcon;

import io.github.stanio.xbrz.awt.util.MultiResolutionCachedImage;

/**
 * xBRZ scaled icons.
 * <p>
 * {@code new ImageIcon(...)} &#x2192; {@code XbrzImage.apply(new ImageIcon(...))}</p>
 */
public final class XbrzImage {

    private XbrzImage() {/* no instances */}

    /**
     * Sets up the given icon with a {@code MultiResolutionImage} deriving
     * resolution variants by applying xBRZ to the icon's current image.
     *
     * @param   icon  image icon to set up
     * @return  The given {@code ImageIcon} with image updated to a
     *          {@code MultiResolutionImage} deriving resolution variants by
     *          applying xBRZ to the icon's current image
     */
    public static ImageIcon apply(ImageIcon icon) {
        Image baseImage = Objects.requireNonNull(icon.getImage(), "icon.image");
        icon.setImage(MultiResolutionCachedImage
                .of(icon.getIconWidth(), icon.getIconHeight(),
                        (w, h) -> AwtXbrz.scaleImage(baseImage, w, h)));
        return icon;
    }

    /**
     * Creates a {@code MultiResolutionImage} deriving resolution variants by
     * applying xBRZ to the given image.
     *
     * @param   image  base image to derive resolution variants from
     * @return  A {@code MultiResolutionImage} dynamically producing xBRZ variants
     * @see     MultiResolutionCachedImage
     */
    public static AbstractMultiResolutionImage mrImage(Image image) {
        return (AbstractMultiResolutionImage) apply(new ImageIcon(image)).getImage();
    }

}
