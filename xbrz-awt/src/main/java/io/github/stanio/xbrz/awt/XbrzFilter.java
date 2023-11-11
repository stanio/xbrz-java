/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package io.github.stanio.xbrz.awt;

import java.util.Arrays;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;

import io.github.stanio.xbrz.Xbrz;

/**
 * xBRZ filter for the Image Producer/Consumer paradigm.  The main use-case
 * for class is with animated GIFs loaded via {@code Toolkit.createImage(URL)}
 * (or {@code Toolkit.getImage(URL)}).
 * <p>
 * Note, this filter performs only integral-factor scaling.  If you want to
 * further scale smoothly into non-integral scale dimensions, set up {@code
 * Graphics2D.setRenderingHint(KEY_INTERPOLATION)} and use one of the scaling
 * {@code Graphics.drawImage()} variants:</p>
 * <pre>
 * <code>    Image xbrzImage;
 *     int targetWidth, targetHeight;
 *     Graphics2D g;
 *     ...
 *     g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
 *                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
 *
 *     g.drawImage(xbrzImage, x, y, targetWidth, targetHeight, this);</code></pre>
 * <p>
 * Remember to specify the {@code ImageObserver} to {@code drawImage()}, that is
 * usually the AWT component being painted on.</p>
 *
 * @implNote  The built-in GIF decoder doesn't account for processing time
 * when performing the delay between animation frames, so frame rates are
 * always a tad bit lower than originally intended.  For animations, this
 * filter performs the scaling asynchronously to minimize the overhead of
 * the scaling.
 */
public class XbrzFilter extends ImageFilter {

    private static final ColorModel rgbDefault = ColorModel.getRGBdefault();
    private static final ColorModel rgbOpaque =
            new DirectColorModel(24, 0x00ff0000, 0x0000ff00, 0x000000ff);

    private int sourceWidth = -1;
    private int sourceHeight = -1;
    private int[] sourcePixels;

    private Xbrz scaler;
    private volatile boolean copySourcePixels;
    private final Queue<int[]> pixelBuffer = new ArrayBlockingQueue<>(1);
    private volatile boolean setTargetSize;
    private int[] targetPixels;

    /**
     * Constructs a new {@code XbrzFilter} using a default xBRZ configuration
     * for the given scale factor.
     *
     * @param   factor  the xBRZ factor to scale the source with (2-6)
     * @throws  IllegalArgumentException  if given unsupported scale factor
     * @see     Xbrz#Xbrz(int)
     */
    public XbrzFilter(int factor) {
        this(new Xbrz(factor));
    }

    /**
     * Constructs a new {@code XbrzFilter} using the given xBRZ scaler instance.
     *
     * @param   xbrz  scaler instance to use for filtering the source
     * @throws  NullPointerException  if given {@code null} xBRZ scaler
     */
    public XbrzFilter(Xbrz xbrz) {
        this.scaler = Objects.requireNonNull(xbrz, "null xbrz scaler");
    }

    /**
     * {@code createScaledImage(image, new Xbrz(factor))}
     *
     * @param   image  the source image to filter
     * @param   factor  the factor to scale the source with (2-6)
     * @return  an image produced by scaling the source
     * @throws  NullPointerException  if given {@code null} image
     * @throws  IllegalArgumentException  if given unsupported scale factor
     * @see     #createScaledImage(Image, Xbrz)
     */
    public static Image createScaledImage(Image image, int factor) {
        return createScaledImage(image, new Xbrz(factor));
    }

    /**
     * Creates a xBRZ filtered image.
     * <pre>
     * <code>XbrzFilter xbrzFilter = new XbrzFilter(xbrz);
     * ImageProducer filteredSource =
     *         new FilteredImageSource(image.getSource(), xbrzFilter);
     * return Toolkit.getDefaultToolkit().createImage(filteredSource);</code></pre>
     *
     * @param   image  the source image to filter
     * @param   xbrz  scaler instance to use for filtering the source
     * @return  an image produced by scaling the source
     * @throws  NullPointerException  if given {@code null} image, or
     *          {@code null} xBRZ scaler
     */
    public static Image createScaledImage(Image image, Xbrz xbrz) {
        XbrzFilter xbrzFilter = new XbrzFilter(xbrz);
        ImageProducer filteredSource =
                new SuspendableFilteredSource(image.getSource(), xbrzFilter);
        return Toolkit.getDefaultToolkit().createImage(filteredSource);
    }

    void setScaler(Xbrz xbrz) {
        int oldScale = scaler.factor();
        this.scaler = Objects.requireNonNull(xbrz, "null xbrz scaler");
        if (oldScale != xbrz.factor()) {
            setTargetSize = true;
            targetPixels = null;
        }
    }

    @Override
    public void setDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            imageComplete(STATICIMAGEDONE);
            return;
        }
        sourceWidth = width;
        sourceHeight = height;
        setTargetSize = true;
        if (sourcePixels != null
                && sourcePixels.length != width * height) {
            sourcePixels = null;
        }
    }

    @Override
    public void setColorModel(ColorModel model) {
        consumer.setColorModel(rgbDefault);
    }

    @Override
    public void setHints(int hints) {
        // We send TOPDOWNLEFTRIGHT | COMPLETESCANLINES | SINGLEPASS and
        // conditionally SINGLEFRAME to the consumer on imageComplete().
    }

    private int[] sourcePixels() {
        int[] rgbPixels = sourcePixels;
        if (rgbPixels == null) {
            if (sourceWidth < 0 || sourceHeight < 0) {
                throw new IllegalStateException("Source dimensions have not been set");
            }
            rgbPixels = new int[sourceWidth * sourceHeight];
            sourcePixels = rgbPixels;
        } else if (copySourcePixels) {
            rgbPixels = copySourcePixels();
            pixelBuffer.offer(sourcePixels);
            sourcePixels = rgbPixels;
            copySourcePixels = false;
        }
        return rgbPixels;
    }

    private int[] copySourcePixels() {
        int[] buffer = pixelBuffer.poll();
        if (buffer == null || buffer.length != sourcePixels.length) {
            buffer = Arrays.copyOf(sourcePixels, sourcePixels.length);
        } else {
            System.arraycopy(sourcePixels, 0, buffer, 0, buffer.length);
        }
        return buffer;
    }

    @Override
    public void setPixels(int x, int y, int w, int h,
            ColorModel model, byte pixels[], int off, int scansize) {
        int[] rgbPixels = sourcePixels();
        int dstPtr = y * sourceWidth + x;
        int dstRem = sourceWidth - w;
        int srcRem = scansize - w;
        for (int sh = h; sh > 0; sh--) {
            for (int sw = w; sw > 0; sw--) {
                rgbPixels[dstPtr++] = model.getRGB(pixels[off++] & 0xff);
            }
            off += srcRem;
            dstPtr += dstRem;
        }
    }

    @Override
    public void setPixels(int x, int y, int w, int h,
            ColorModel model, int pixels[], int off, int scansize) {
        int[] rgbPixels = sourcePixels();
        int srcWidth = sourceWidth;
        int dstPtr = y * srcWidth + x;
        if (model == rgbDefault
                || model.equals(rgbOpaque)) {
            for (int sh = h; sh > 0; sh--) {
                System.arraycopy(pixels, off, rgbPixels, dstPtr, w);
                off += scansize;
                dstPtr += srcWidth;
            }
        } else {
            int dstRem = srcWidth - w;
            int srcRem = scansize - w;
            for (int sh = h; sh > 0; sh--) {
                for (int sw = w; sw > 0; sw--) {
                    rgbPixels[dstPtr++] = model.getRGB(pixels[off++]);
                }
                off += srcRem;
                dstPtr += dstRem;
            }
        }
    }

    @Override
    public void imageComplete(int status) {
        switch (status) {
        case SINGLEFRAMEDONE:
        case STATICIMAGEDONE:
            if (sourcePixels == null) break;
            completeScale(status);
            return;

        case IMAGEABORTED:
        case IMAGEERROR:
            clear();
            break;

        default:
        }
        consumer.imageComplete(status);
    }

    private void clear() {
        sourceWidth  = -1;
        sourceHeight = -1;
        sourcePixels = null;
        targetPixels = null;
        pixelBuffer.clear();
    }

    private void completeScale(int status) {
        int[] source = sourcePixels;
        copySourcePixels = true;
        int width = sourceWidth;
        int height = sourceHeight;

        Xbrz xbrz = scaler;
        int targetWidth = width * xbrz.factor();
        int targetHeight = height * xbrz.factor();

        Runnable scale = () -> {
            synchronized (consumer) {
                targetPixels = xbrz.scaleImage(source,
                        targetPixels(targetWidth, targetHeight), width, height);
                copySourcePixels = false;

                if (setTargetSize) {
                    consumer.setDimensions(targetWidth, targetHeight);
                    setTargetSize = false;
                }
                int hints = TOPDOWNLEFTRIGHT | COMPLETESCANLINES | SINGLEPASS;
                consumer.setHints(hints
                        | (status == STATICIMAGEDONE ? SINGLEFRAME : 0));
                consumer.setPixels(0, 0, targetWidth, targetHeight,
                        rgbDefault, targetPixels, 0, targetWidth);
                consumer.imageComplete(status);
            }
        };

        if (status == STATICIMAGEDONE) {
            scale.run();
            clear();
            return;
        }

        try {
            // Block here if previous scale hasn't completed, yet.
            synchronized (consumer) {
                asyncExecutor.execute(scale);
            }
        } catch (RejectedExecutionException e) {
            scale.run(); // fallback
        }
    }

    private int[] targetPixels(int width, int height) {
        if (targetPixels != null &&
                targetPixels.length != width * height) {
            targetPixels = null;
        }
        return targetPixels;
    }

    @Override
    public XbrzFilter clone() {
        // Don't share pixel buffers.
        return new XbrzFilter(scaler);
    }

    private static final Executor asyncExecutor;
    private static final AtomicInteger threadNum = new AtomicInteger();
    static {
        ThreadFactory defaultFactory = Executors.defaultThreadFactory();
        ExecutorService service = Executors.newCachedThreadPool(r -> {
            Thread th = defaultFactory.newThread(r);
            th.setName("Async xBRZ Filter " + threadNum.incrementAndGet());
            th.setDaemon(true);
            return th;
        });
        if (service instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor pool = (ThreadPoolExecutor) service;
            pool.setMaximumPoolSize(10);
            pool.setKeepAliveTime(10L, TimeUnit.SECONDS);
        }
        asyncExecutor = service;
    }

}
