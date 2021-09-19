/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package net.sourceforge.xbrz.awt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.junit.Rule;
import org.junit.rules.TestName;

public abstract class AbstractAwtXbrzTest {

    @Rule
    public TestName testName = new TestName();

    protected static Toolkit toolkit() {
        return Toolkit.getDefaultToolkit();
    }

    protected void assertEqualPixels(BufferedImage actual, String expected) throws IOException {
        assertEqualPixels(testName.getMethodName(), actual, expected);
    }

    protected static void assertEqualPixels(String name, BufferedImage actual, String expected) throws IOException {
        assertPixels(name, getPixels(actual), getPixels(ImageIO.read(resource(expected))), 0.0001);
    }

    private static void assertPixels(String name, int[] destPixels, int[] refPixels, double deviation) {
        assertEquals("pixels size", refPixels.length, destPixels.length);
        int mismatch = 0;
        for (int i = 0, len = refPixels.length; i < len; i++) {
            if (destPixels[i] != refPixels[i]) {
                mismatch += 1;
            }
        }
        double percent = mismatch * 100.0 / destPixels.length;
        String message = String.format("Pixel mismatch: %d (%.3f%%)", mismatch, percent);
        System.out.printf("%s [%s]%n", message, name);
        assertTrue(message, percent <= deviation);
    }

    private static int[] getPixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    protected static URL resource(String name) {
        return AbstractAwtXbrzTest.class.getResource("images/" + name);
    }

}
