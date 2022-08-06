/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package io.github.stanio.xbrz.awt;

import static org.junit.Assert.*;

import java.awt.Image;

import javax.swing.ImageIcon;

import org.junit.Test;

public class XbrzImageTest {

    @Test
    public void mrImageVariant() throws Exception {
        Image image = loadImage("demo/editbookmarks.png");

        Image scaled = XbrzImage.mrImage(image).getResolutionVariant(32, 48);

        assertEquals("scaled.width", 32, scaled.getWidth(null));
        assertEquals("scaled.height", 48, scaled.getHeight(null));
    }

    private static Image loadImage(String name) {
        return new ImageIcon(XbrzImageTest.class.getResource(name)).getImage();
    }

}
