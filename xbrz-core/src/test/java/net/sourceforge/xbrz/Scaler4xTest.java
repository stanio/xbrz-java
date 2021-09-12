package net.sourceforge.xbrz;

import org.junit.BeforeClass;

public class Scaler4xTest extends AbstractScalerTest {

    private static Xbrz xbrz4;

    @BeforeClass
    public static void suiteSetUp() {
        xbrz4 = new Xbrz(4);
    }

    @Override
    protected Xbrz xbrz() {
        return xbrz4;
    }

}
