package com.github.stanio.xbrz;

import org.junit.BeforeClass;

public class Scaler6xTest extends AbstractScalerTest {

    private static Xbrz xbrz6;

    @BeforeClass
    public static void suiteSetUp() {
        xbrz6 = new Xbrz(6);
    }

    @Override
    protected Xbrz xbrz() {
        return xbrz6;
    }

}
