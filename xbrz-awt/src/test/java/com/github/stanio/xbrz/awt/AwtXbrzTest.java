/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package com.github.stanio.xbrz.awt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;

import org.junit.Test;

public class AwtXbrzTest extends AbstractAwtXbrzTest {

    private void assertEqualPixels(Image actual, String expected) throws IOException {
        assertTrue("result not a BufferedImage", actual instanceof BufferedImage);
        assertEqualPixels((BufferedImage) actual, expected);
    }

    @Test
    public void toolkitSourceWithoutAlpha() throws Exception {
        Image source = toolkit().createImage(resource("gbamockup-indexcolor.png"));

        Image scaled = AwtXbrz.scaleImage(source, 3);

        assertEqualPixels(scaled, "gbamockup@3xbrz.png");
    }

    @Test
    public void toolkitSourceWithAlpha() throws Exception {
        Image source = toolkit().createImage(resource("open-folder.png"));

        Image scaled = AwtXbrz.scaleImage(source, 3);

        assertEqualPixels(scaled, "open-folder@3xbrz.png");
    }

    @Test
    public void bufferedSourceWithoutAlpha() throws Exception {
        Image source = ImageIO.read(resource("gbamockup-indexcolor.png"));

        Image scaled = AwtXbrz.scaleImage(source, 3);

        assertEqualPixels(scaled, "gbamockup@3xbrz.png");
    }

    @Test
    public void bufferedSourceWithAlpha() throws Exception {
        Image source = ImageIO.read(resource("open-folder.png"));

        Image scaled = AwtXbrz.scaleImage(source, 3);

        assertEqualPixels(scaled, "open-folder@3xbrz.png");
    }

    @Test
    public void nearestResolutionVariant() throws Exception {
        BufferedImage source = ImageIO.read(resource("open-folder.png"));
        int targetWidth = Math.round(source.getWidth() * 2.5f);
        int targetHeight = Math.round(source.getHeight() * 2.5f);

        Image scaled = AwtXbrz.scaleImage(source, targetWidth, targetHeight);

        //assertEqualPixels(scaled, "open-folder@3xbrz.png");
        assertEquals("scaled.width", targetWidth, scaled.getWidth(null));
        assertEquals("scaled.height", targetHeight, scaled.getHeight(null));
    }

    @Test
    public void animatedSource() throws Exception {
        Image source = toolkit().createImage(resource("loading.gif"));

        Image scaled = AwtXbrz.scaleImage(source, 3);

        List<BufferedImage> frames = captureFrames(scaled);
        for (int i = 0; i < frames.size(); i++) {
            assertEqualPixels("animatedSource-frame" + (i + 1), frames.get(i), "loading-frame" + (i + 1) + ".png");
        }
    }

    private static List<BufferedImage> captureFrames(Image scaled) throws InterruptedException {
        List<BufferedImage> frames = Collections.synchronizedList(new ArrayList<>());
        PixelGrabber grabber = new PixelGrabber(scaled, 0, 0, -1, -1, true) {
            @Override
            public synchronized void imageComplete(int status) {
                if (status == SINGLEFRAMEDONE) {
                    BufferedImage aframe = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                    aframe.setRGB(0, 0, aframe.getWidth(), aframe.getHeight(), (int[]) getPixels(), 0, aframe.getWidth());
                    frames.add(aframe);
                    if (frames.size() < 9) {
                        return;
                    }
                }
                super.imageComplete(status);
            }
        };
        if (!grabber.grabPixels(10_000L)) {
            throw new IllegalStateException("status: " + grabber.status());
        } else if (frames.size() < 9) {
            throw new IllegalStateException("frame count < 9: " + frames.size());
        }
        return frames;
    }

}
