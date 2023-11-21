# xBRZ for Java AWT Images

[![xbrz-awt:1.8.2](https://img.shields.io/badge/xbrz--awt-1.8.2-blue?style=flat-square)](https://central.sonatype.com/artifact/io.github.stanio/xbrz-awt/1.8.2)

Depends on [xbrz-core](../xbrz-core).

JPMS name: `io.github.stanio.xbrz.awt` (Java 8 compatible Multi-Release JAR)

## API

-   `AwtXbrz` – supports animated (`java.awt.Toolkit` created) images
-   `XbrzOp` – single-input/single-output

Example:

    import io.github.stanio.xbrz.awt.AwtXbrz;
    import java.awt.Image;
    
        Image source;
        int factor = 2;
        ...
        Image scaled = AwtXbrz.scaleImage(image, factor);

## Java 9+

-   `XbrzImage.apply(ImageIcon)` – applies xBRZ to existing (lores) `ImageIcon`s
    to dynamically produce quality upscaled variants on hires screens;
-   `MultiResolutionCachedImage` – general purpose `java.awt.MultiResolutionImage`
    implementation.
