package com.github.stanio.xbrz;

import org.junit.BeforeClass;

public class Scaler3xTest extends AbstractScalerTest {

    private static Xbrz xbrz3;

    @BeforeClass
    public static void suiteSetUp() {
        xbrz3 = new Xbrz(3);
    }

    @Override
    protected Xbrz xbrz() {
        return xbrz3;
    }

}
