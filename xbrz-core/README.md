# xBRZ for Java Core Library

[![xbrz-core](https://img.shields.io/maven-metadata/v.svg?style=flat-square&label=xbrz-core&color=blue&logo=java&metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fio%2Fgithub%2Fstanio%2Fxbrz-core%2Fmaven-metadata.xml)](https://search.maven.org/search?q=g:%22io.github.stanio%22%20AND%20a:%22xbrz-core%22)

Self-contained (no dependencies).  For using with Java AWT images one may consider [xbrz-awt](../xbrz-awt).

## API

Example using `java.awt.image.BufferedImage`:

    import io.github.stanio.xbrz.Xbrz;
    import java.awt.image.BufferedImage;
    
        BufferedImage source = ...;
        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();
        int[] srcPixels = source.getRGB(0, 0, srcWidth, srcHeight, null, 0, srcWidth);
    
        int factor = 2;
        int destWidth = srcWidth * factor;
        int destHeight = srcHeight * factor;
        boolean hasAlpha = source.getColorModel().hasAlpha();
        int[] destPixels = Xbrz.scaleImage(factor, hasAlpha, srcPixels, null, srcWidth, srcHeight);
    
        BufferedImage scaled = new BufferedImage(destWidth, destHeight,
                                                 hasAlpha ? BufferedImage.TYPE_INT_ARGB
                                                          : BufferedImage.TYPE_INT_RGB);
        scaled.setRGB(0, 0, destWidth, destHeight, destPixels, 0, destWidth);

One may reuse preconfigured `Xbrz` instance for multiple scale operations:

        int factor = 3;
        Xbrz xbrz = new Xbrz(factor);
        xbrz.scaleImage(src, trg, srcWidth, srcHeight);

The slowest part of the scaling currently is the calculation of the default `ColorDistance`.  If more speed (over quality) is required, one may configure
the `Xbrz` instance like:

        Xbrz xbrz = new Xbrz(factor, withAlpha, new ScalerCfg(),
                             ColorDistance.bufferedYCbCr(5));

or provide his/her own implementation:

        Xbrz xbrz = new Xbrz(factor, withAlpha, new ScalerCfg(),
                             (pix1, pix2) -> Math.abs(pix1 - pix2));

Note:

-   `ColorDistance.bufferedYCbCr(5)` allocates 128 KB lookup buffer
-   `ColorDistance.bufferedYCbCr(8)` allocates 64 MB lookup buffer