## Core Usage

```java
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
```

The given example uses Java AWT `BufferedImage` source just to demonstrate the
input to and the output from the
[`Xbrz`](apidocs/io.github.stanio.xbrz.core/io/github/stanio/xbrz/Xbrz.html)
scaler is 32-bit packed <abbr title="Alpha, Red, Green, Blue">ARGB</abbr>
pixels.  The scaler itself is not dependent on any particular graphics toolkit.

## AWT and Swing

Using [`XbrzImage`](apidocs/io.github.stanio.xbrz.awt/io/github/stanio/xbrz/awt/XbrzImage.html)
one may derive a [`MultiResolutionImage`](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/java/awt/image/MultiResolutionImage.html)
from an `Image` source:

```java
import java.awt.Image;
import io.github.stanio.xbrz.awt.XbrzImage;

    Image loResImage;
    ...
    Image xbrzImage = XbrzImage.mrImage(loResImage);
```

and/or apply directly to an
[`ImageIcon`](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/swing/ImageIcon.html):

```java
import javax.swing.ImageIcon;
import io.github.stanio.xbrz.awt.XbrzImage;

    ImageIcon icon;
    ...
    XbrzImage.apply(icon);
```

See the full [API-docs](apidocs).
