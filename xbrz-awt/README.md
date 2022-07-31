# xBRZ for Java AWT Images

[![xbrz-awt](https://img.shields.io/maven-metadata/v.svg?label=xbrz-awt&logo=OpenJDK&metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fio%2Fgithub%2Fstanio%2Fxbrz-awt%2Fmaven-metadata.xml)](https://search.maven.org/artifact/io.github.stanio/xbrz-awt)

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
