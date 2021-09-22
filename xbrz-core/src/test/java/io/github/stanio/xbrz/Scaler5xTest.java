package io.github.stanio.xbrz;

import org.junit.BeforeClass;

public class Scaler5xTest extends AbstractScalerTest {

    private static Xbrz xbrz5;

    @BeforeClass
    public static void suiteSetUp() {
        xbrz5 = new Xbrz(5);
    }

    @Override
    protected Xbrz xbrz() {
        return xbrz5;
    }

}
