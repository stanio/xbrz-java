# xBRZ for Java AWT Images

[![xbrz-awt](https://img.shields.io/maven-metadata/v.svg?style=flat-square&label=xbrz-awt&color=blue&logo=java&metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fio%2Fgithub%2Fstanio%2Fxbrz-awt%2Fmaven-metadata.xml)](https://search.maven.org/search?q=g:%22io.github.stanio%22%20AND%20a:%22xbrz-awt%22)

Depends on [xbrz-core](../xbrz-core).

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
