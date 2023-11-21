# xBRZ for Java AWT Images

[![xbrz-awt](https://img.shields.io/maven-central/v/io.github.stanio/xbrz-awt?label=xbrz-awt&logo=openjdk&logoColor=silver)](https://central.sonatype.com/artifact/io.github.stanio/xbrz-awt)

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
