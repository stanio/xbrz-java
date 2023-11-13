/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package io.github.stanio.xbrz.awt;

import java.util.Objects;

import java.awt.Image;
import java.awt.image.MultiResolutionImage;

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
     * Replaces the given icon's image with a multi-resolution one that uses
     * the current icon's image to produce resolution variants as requested.
     *
     * @param   icon  image icon to set up
     * @return  The given {@code ImageIcon} with image updated to a
     *          {@code MultiResolutionImage} deriving resolution variants by
     *          applying xBRZ to the icon's current image
     */
    public static ImageIcon apply(ImageIcon icon) {
        Image baseImage = Objects.requireNonNull(icon.getImage(), "icon.image");
        ImageData imageData = ImageData.get(baseImage);
        Image mrImage;
        if (imageData.isAnimated()) {
            mrImage = new AnimatedMultiResolutionImage(baseImage);
        } else {
            mrImage = MultiResolutionCachedImage
                    .withProducer(icon.getIconWidth(), icon.getIconHeight(),
                            (w, h) -> AwtXbrz.scaleImage(imageData, null, w, h));
        }
        icon.setImage(mrImage);
        return icon;
    }

    /**
     * Creates a {@code MultiResolutionImage} deriving resolution variants by
     * applying xBRZ to the given image.
     *
     * @param   <M> an {@code Image} subclass that implements {@code MultiResolutionImage}
     * @param   image  base image to derive resolution variants from
     * @return  A {@code MultiResolutionImage} dynamically producing xBRZ variants
     * @see     MultiResolutionCachedImage
     */
    public static <M extends Image & MultiResolutionImage> M mrImage(Image image) {
        @SuppressWarnings("unchecked")
        M mrImage = (M) apply(new ImageIcon(image)).getImage();
        return mrImage;
    }

}
