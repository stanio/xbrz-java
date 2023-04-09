package io.github.stanio.xbrz;

import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

public class ColorDistanceYCbCrIntegerTest extends ColorDistanceTest {

    static ColorDistance colorDistance;

    @BeforeClass
    public static void setUpSuite() {
        colorDistance = new ColorDistanceYCbCrInteger(1);
    }

    @Parameters(name = "{index}: {0} - {1}")
    public static Object[][] data() {
        Double[] deltas = {
            defaultDelta,
            defaultDelta,
            defaultDelta,
            defaultDelta,
            defaultDelta,
            0.014,
            0.043,
            0.005,
            defaultDelta,
            0.003,
            defaultDelta,
            defaultDelta,
            defaultDelta,
            defaultDelta,
            defaultDelta,
            0.003,
            defaultDelta,
            0.005,
            0.043,
            0.012,
            defaultDelta,
            defaultDelta,
            defaultDelta,
            defaultDelta,
            defaultDelta
        };

        Object[][] params = ColorDistanceTest.data();
        for (int i = 0; i < params.length; i++) {
            Object[] row = params[i];
            row[3] = deltas[i];
        }
        return params;
    }

}
