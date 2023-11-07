/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package io.github.stanio.xbrz.awt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AwtXbrzFindFactorTest {

    private static final int sourceWidth = 25;
    private static final int sourceHeight = 15;

    private int destWidth;
    private int destHeight;
    private int expectedFactor;

    public AwtXbrzFindFactorTest(int destWidth, int destHeight, int expectedFactor) {
        this.destWidth = destWidth;
        this.destHeight = destHeight;
        this.expectedFactor = expectedFactor;
    }

    @Parameters(name = "{index}: {0} x {1} => {2}")
    public static Object[][] parameters() {
        return new Object[][] {
            {        sourceWidth,               sourceHeight,        1 },
            {        sourceWidth - 5,           sourceHeight - 5,    1 },
            { (int) (sourceWidth * 1.2), (int) (sourceHeight * 1.2), 2 },
            { (int) (sourceWidth * 1.2),        sourceHeight - 10,   2 },
            { (int) (sourceWidth * 1.8), (int) (sourceHeight * 1.8), 2 },
            {        sourceWidth * 2,           sourceHeight * 2,    2 },
            { (int) (sourceWidth * 2.2), (int) (sourceHeight * 2.2), 3 },
            {        sourceWidth - 10,   (int) (sourceHeight * 2.2), 3 },
            { (int) (sourceWidth * 2.8), (int) (sourceHeight * 2.8), 3 },
            {        sourceWidth * 3,           sourceHeight * 3,    3 },
            { (int) (sourceWidth * 3.2), (int) (sourceHeight * 3.2), 4 },
            {        sourceWidth - 10,   (int) (sourceHeight * 3.8), 4 },
            { (int) (sourceWidth * 3.8), (int) (sourceHeight * 3.8), 4 },
            {        sourceWidth * 4,           sourceHeight * 4,    4 },
            { (int) (sourceWidth * 4.2), (int) (sourceHeight * 4.2), 5 },
            { (int) (sourceWidth * 4.8),        sourceHeight - 10,   5 },
            { (int) (sourceWidth * 4.8), (int) (sourceHeight * 4.8), 5 },
            {        sourceWidth * 5,           sourceHeight * 5,    5 },
            { (int) (sourceWidth * 5.2), (int) (sourceHeight * 5.8), 6 },
            {        sourceWidth * 6,           sourceHeight * 6,    6 },
            { (int) (sourceWidth * 6.2), (int) (sourceHeight * 5.8), 6 },
            { (int) (sourceWidth * 5.2), (int) (sourceHeight * 6.8), 6 },
            {        sourceWidth * 16,          sourceHeight * 16,   6 },
            {        sourceWidth * 33,          sourceHeight * 33,   6 },
        };
    }

    @Test
    public void findFactor() {
        assertEquals("Scale factor", expectedFactor, AwtXbrz
                .findFactor(sourceWidth, sourceHeight, destWidth, destHeight));
    }


}
