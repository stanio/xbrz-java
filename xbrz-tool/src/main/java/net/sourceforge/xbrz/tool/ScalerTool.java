package net.sourceforge.xbrz.tool;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Provides command-line entry point for using xBRZ.
 */
public class ScalerTool {

    public static Iterator<BufferedImage> scaleImage(File source, int factor) throws IOException {
        return new ScaledImageIterator(source, factor);
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        int factor = 2;
        if (args.length > 1) {
            try {
                factor = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println(e.toString());
                printUsage();
                System.exit(2);
            }
        }

        String source = args[0];
        String targetBase = source.replaceFirst("((?<!^|[/\\\\])\\.[^.]+)?$", "");

        int index = 1;
        Iterator<BufferedImage> frames = scaleImage(new File(source), factor);
        while (frames.hasNext()) {
            BufferedImage current = frames.next();
            String target;
            if (index > 1 || frames.hasNext()) {
                target = targetBase + "-" + index + "@" + factor + "x.png";
            } else {
                target = targetBase + "@" + factor + "x.png";
            }
            ImageIO.write(current, "png", new File(target));
            System.out.println(target);
            index += 1;
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java -jar xbrz.jar <source> [scaling_factor]");
    }


    private static class ScaledImageIterator implements Iterator<BufferedImage> {

        private int factor;
        private InputStream sourceStream;
        private ImageInputStream imageStream;
        private ImageReader reader;
        private int index = 0;
        private BufferedImage nextImage;

        ScaledImageIterator(File source, int factor) throws IOException {
            this.factor = factor;
            this.sourceStream = new FileInputStream(source);
            this.imageStream = ImageIO.createImageInputStream(sourceStream);
            if (imageStream == null) {
                throw new IIOException("Can't create an ImageInputStream!");
            }

            Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageStream);
            if (!imageReaders.hasNext()) {
                throw new IIOException("Unsupported image format: " + source);
            }
            this.reader = imageReaders.next();
            reader.setInput(imageStream, true, true);
        }

        @Override
        public boolean hasNext() {
            if (nextImage == null && reader != null) {
                readNext();
            }
            return (nextImage != null);
        }

        private void readNext() {
            try {
                nextImage = reader.read(index);
                index += 1;
            } catch (@SuppressWarnings("unused") IndexOutOfBoundsException e) {
                close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public BufferedImage next() {
            if (hasNext()) {
                ImageData sourceData = new ImageData(nextImage);
                nextImage = null;
                return AwtXbrz.scaleImage(sourceData, factor);
            }
            throw new NoSuchElementException();
        }

        public void close() {
            reader.dispose();
            reader = null;
            try (Closeable is1 = sourceStream; Closeable is2 = imageStream) {
                // auto-close
            } catch (IOException ioe) {
                ioe.printStackTrace(System.out);
            }
            imageStream = null;
            sourceStream = null;
        }
    }

}
