/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package net.sourceforge.xbrz.awt;

import static org.junit.Assert.assertEquals;

import java.awt.Image;
import javax.imageio.ImageIO;

import org.junit.Test;

public class ImageDataTest extends AbstractAwtXbrzTest {

    @Test
    public void toolkitRGBSourceWithoutAlpha() {
        Image source = toolkit().createImage(resource("gbamockup-truecolor.png"));

        ImageData sourceData = ImageData.get(source);

        assertEquals("ImageData.hasAlpha", false, sourceData.hasAlpha);
    }

    @Test
    public void toolkitIndexedSourceWithoutAlpha() {
        Image source = toolkit().createImage(resource("gbamockup-truecolor.png"));

        ImageData sourceData = ImageData.get(source);

        assertEquals("ImageData.hasAlpha", false, sourceData.hasAlpha);
    }

    @Test
    public void toolkitSourceWithAlpha() {
        Image source = toolkit().createImage(resource("open-folder.png"));

        ImageData sourceData = ImageData.get(source);

        assertEquals("ImageData.hasAlpha", true, sourceData.hasAlpha);
    }

    @Test
    public void bufferedSourceWithoutAlpha() throws Exception {
        Image source = ImageIO.read(resource("gbamockup-truecolor.png"));

        ImageData sourceData = ImageData.get(source);

        assertEquals("ImageData.hasAlpha", false, sourceData.hasAlpha);
    }

    @Test
    public void bufferedSourceWithAlpha() throws Exception {
        Image source = ImageIO.read(resource("open-folder.png"));

        ImageData sourceData = ImageData.get(source);

        assertEquals("ImageData.hasAlpha", true, sourceData.hasAlpha);
    }

    @Test
    public void animatedSource() {
        Image source = toolkit().createImage(resource("loading.gif"));

        ImageData sourceData = ImageData.get(source);

        assertEquals("ImageData.isAnimated", true, sourceData.isAnimated());
    }

    @Test
    public void nonAnimatedSource() {
        Image source = toolkit().createImage(resource("open-folder.png"));

        ImageData sourceData = ImageData.get(source);

        assertEquals("ImageData.isAnimated", false, sourceData.isAnimated());
    }

}
