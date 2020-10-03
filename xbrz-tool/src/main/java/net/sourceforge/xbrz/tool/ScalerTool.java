package net.sourceforge.xbrz.tool;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.sourceforge.xbrz.Xbrz;

public class ScalerTool {

    private static class ImageData {
        final int width;
        final int height;
        final int[] pixels;
        final boolean hasAlpha;
        ImageData(BufferedImage image) {
            width = image.getWidth();
            height = image.getHeight();
            pixels = new int[width * height];
            hasAlpha = image.getColorModel().hasAlpha();
            image.getRGB(0, 0, width, height, pixels, 0, width);
        }
    }

    public static BufferedImage scaleImage(BufferedImage source, int factor) {
        return scaleImage(new ImageData(source), factor);
    }

    private static BufferedImage scaleImage(File source, int factor) throws IOException {
        ImageData sourceData = new ImageData(ImageIO.read(source));
        return scaleImage(sourceData, factor);
    }

    private static BufferedImage scaleImage(ImageData source, int factor) {
        int destWidth = source.width * factor;
        int destHeight = source.height * factor;
        int[] destPixels = new int[destWidth * destHeight];
        Xbrz.scaleImage(factor, source.hasAlpha, source.pixels, destPixels, source.width, source.height);
        return makeImage(destPixels, destWidth, destHeight, source.hasAlpha);
    }

    private static BufferedImage makeImage(int[] pixels, int width, int height, boolean hasAlpha) {
        DataBufferInt dataBuffer = new DataBufferInt(pixels, pixels.length);
        DirectColorModel colorModel = new DirectColorModel(hasAlpha ? 32 : 24,
                                                           0x00FF0000,
                                                           0x0000FF00,
                                                           0x000000FF,
                                                           hasAlpha ? 0xFF000000 : 0);
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(width, height);
        WritableRaster raster = WritableRaster.createWritableRaster(sampleModel, dataBuffer, null);
        return new BufferedImage(colorModel, raster, false, null);
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
        String target = source.replaceFirst("((?<!^|[/\\\\])\\.[^.]+)?$", "@" + factor + "x.png");
        ImageIO.write(scaleImage(new File(source), factor), "png", new File(target));
        System.out.println(target);
    }

    private static void printUsage() {
        System.err.println("Usage: java -jar xbrz.jar <source> [scaling_factor]");
    }

}
