# xBRZ for Java Core Library

[![xbrz-core](https://img.shields.io/maven-central/v/io.github.stanio/xbrz-core?label=xbrz-core&logo=openjdk&logoColor=silver)](https://central.sonatype.com/artifact/io.github.stanio/xbrz-core)

Self-contained (no dependencies).  For using with Java AWT images one may consider [xbrz-awt](../xbrz-awt).

JPMS name: `io.github.stanio.xbrz.core` (Java 8 compatible Multi-Release JAR)

## License

This module is distributed under the GNU General Public License v3, with the
[Classpath Exception](https://en.wikipedia.org/wiki/GPL_linking_exception#The_Classpath_exception).
See [LICENSE](LICENSE).

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
