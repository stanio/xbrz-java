package net.sourceforge.xbrz;

import org.junit.BeforeClass;

public class Scaler2xTest extends AbstractScalerTest {

    private static Xbrz xbrz2;

    @BeforeClass
    public static void suiteSetUp() {
        xbrz2 = new Xbrz(2);
    }

    @Override
    protected Xbrz xbrz() {
        return xbrz2;
    }

}
