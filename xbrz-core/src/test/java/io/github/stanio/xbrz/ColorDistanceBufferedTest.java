package io.github.stanio.xbrz;

import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

public class ColorDistanceBufferedTest extends ColorDistanceTest {

    private static Double defaultDelta = 8.0; // full 8 bits - 5 bits = 3 bits

    static ColorDistance colorDistance;

    @BeforeClass
    public static void setUpSuite() {
        colorDistance = new ColorDistanceYCbCrBuffered(5);
    }

    @Parameters(name = "{index}: {0} - {1}")
    public static Object[][] data() {
        Object[][] params = ColorDistanceTest.data();
        for (Object[] row : params) {
            row[3] = defaultDelta;
        }
        return params;
    }

}
