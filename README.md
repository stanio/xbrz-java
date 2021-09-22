# xBRZ in Java

[xBRZ](https://sourceforge.net/projects/xbrz/): "Scale by rules" - high quality image upscaling filter by Zenju.

Java port of xBRZ 1.8 by Stanio.

## Command-line

    Usage: java -jar xbrz.jar <source> [scaling_factor]

## API

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

### Maven

        <dependency>
            <groupId>io.github.stanio</groupId>
            <artifactId>xbrz-core</artifactId>
            <version>1.8.0</version>
        </dependency>

### Gradle

        implementation 'io.github.stanio:xbrz-core:1.8.0'
