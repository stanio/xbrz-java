# xBRZ in Java

[xBRZ](https://sourceforge.net/projects/xbrz/): "Scale by rules" - high quality image upscaling filter by Zenju.

Java port of xBRZ 1.8 by Stanio.

## Command-line

    Usage: java -jar xbrz.jar <source> [scaling_factor]

## API

        BufferedImage source = ....;
        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();
        int[] srcPixels = new int[srcWidth * srcHeight];
        source.getRGB(0, 0, srcWidth, srcHeight, srcPixels, 0, srcWidth);
        
        int factor = 2;
        int destWidth = srcWidth * factor;
        int destHeight = srcHeight * factor;
        int[] destPixels = new int[destWidth * destHeight];
        Xbrz.scaleImage(factor, true, srcPixels, destPixels, srcWidth, srcHeight);
        
        BufferedImage scaled = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_ARGB);
        scaled.setRGB(0, 0, destWidth, destHeight, destPixels, 0, destWidth);

### Maven

        <dependency>
            <groupId>net.sourceforge.xbrz</groupId>
            <artifactId>xbrz</artifactId>
            <version>1.8</version>
        </dependency>

### Gradle

        implementation 'net.sourceforge.xbrz:xbrz:1.8'
