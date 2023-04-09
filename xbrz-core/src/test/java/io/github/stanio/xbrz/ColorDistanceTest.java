package io.github.stanio.xbrz;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

import java.awt.Color;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ColorDistanceTest {

    static final Double defaultDelta = 0.000005;

    static ColorDistance colorDistance;

    @Parameter(0)
    public String pix1;

    @Parameter(1)
    public String pix2;

    @Parameter(2)
    public double expectedDistance;

    @Parameter(3)
    public double floatDelta;

    @BeforeClass
    public static void setUpSuite() {
        colorDistance = new ColorDistanceYCbCr(1);
    }

    @Parameters(name = "{index}: {0} - {1}")
    public static Object[][] data() {
        return new Object[][] {
            { "#000000", "#000000",   0.0,      defaultDelta },
            { "#000000", "#404040",  64.0,      defaultDelta },
            { "#000000", "#808080", 128.0,      defaultDelta },
            { "#000000", "#C0C0C0", 192.0,      defaultDelta },
            { "#000000", "#FFFFFF", 255.0,      defaultDelta },
            { "#403050", "#000000",  56.226890, defaultDelta },
            { "#403050", "#404040",  18.236254, defaultDelta },
            { "#403050", "#808080",  75.469587, defaultDelta },
            { "#403050", "#C0B0D0", 128.0,      defaultDelta },
            { "#403050", "#FFFFFF", 201.482146, defaultDelta },
            { "#808080", "#000000", 128.0,      defaultDelta },
            { "#808080", "#404040",  64.0,      defaultDelta },
            { "#808080", "#808080",   0.0,      defaultDelta },
            { "#808080", "#C0C0C0",  64.0,      defaultDelta },
            { "#808080", "#FFFFFF", 127.0,      defaultDelta },
            { "#C0D0B0", "#000000", 202.479267, defaultDelta },
            { "#C0D0B0", "#405030", 128.0,      defaultDelta },
            { "#C0D0B0", "#808080",  75.469587, defaultDelta },
            { "#C0D0B0", "#C0C0C0",  18.236254, defaultDelta },
            { "#C0D0B0", "#FFFFFF",  55.265375, defaultDelta },
            { "#FFFFFF", "#000000", 255.0,      defaultDelta },
            { "#FFFFFF", "#404040", 191.0,      defaultDelta },
            { "#FFFFFF", "#808080", 127.0,      defaultDelta },
            { "#FFFFFF", "#C0C0C0",  63.0,      defaultDelta },
            { "#FFFFFF", "#FFFFFF",   0.0,      defaultDelta },
        };
    }

    protected ColorDistance colorDistance() {
        try {
            return (ColorDistance) getClass()
                    .getDeclaredField("colorDistance")
                    .get(this);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void calc() {
        assertThat("color distance",
                colorDistance().calc(Color.decode(pix1).getRGB(),
                                     Color.decode(pix2).getRGB()),
                closeTo(expectedDistance, floatDelta));
    }

}
