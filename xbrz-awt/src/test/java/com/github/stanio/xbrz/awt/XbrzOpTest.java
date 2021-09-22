/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package com.github.stanio.xbrz.awt;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Test;

public class XbrzOpTest extends AbstractAwtXbrzTest {

    private XbrzOp xbrzOp;

    @Before
    public void setUp() {
        xbrzOp = new XbrzOp(6);
    }

    @Test
    public void indexColorDestination() throws Exception {
        // Given
        BufferedImage source = ImageIO.read(resource("gbamockup-indexcolor.png"));

        ColorModel indexModel = source.getColorModel();
        WritableRaster indexRaster = indexModel.createCompatibleWritableRaster(source.getWidth() * 3, source.getHeight() * 3);
        BufferedImage result = new BufferedImage(indexModel, indexRaster, false, null);

        // When
        xbrzOp.filter(source, result);

        // Then
        assertEqualPixels(result, "gbamockup-indexcolor-part@6xbrz.png");
    }

}
