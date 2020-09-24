package net.sourceforge.xbrz.tool;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

import net.sourceforge.xbrz.Xbrz;

public class ScalerTool {

    public static BufferedImage scaleImage(BufferedImage source, int factor) {
        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();
        int[] srcPixels = new int[srcWidth * srcHeight];
        boolean hasAlpha = source.getColorModel().hasAlpha();
        source.getRGB(0, 0, srcWidth, srcHeight, srcPixels, 0, srcWidth);

        int destWidth = srcWidth * factor;
        int destHeight = srcHeight * factor;
        int[] destPixels = new int[destWidth * destHeight];
        Xbrz.scaleImage(factor, hasAlpha, srcPixels, destPixels, srcWidth, srcHeight);

        BufferedImage dest = new BufferedImage(destWidth, destHeight,
                                               hasAlpha ? BufferedImage.TYPE_INT_ARGB
                                                        : BufferedImage.TYPE_INT_RGB);
        dest.setRGB(0, 0, destWidth, destHeight, destPixels, 0, destWidth);
        return dest;
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
        BufferedImage source = ImageIO.read(new File(args[0]));
        BufferedImage scaled = scaleImage(source, factor);
        String target = args[0].replaceFirst("((?<!^|[/\\\\])\\.[^.]+)?$", "@" + factor + "x.png");
        ImageIO.write(scaled, "png", new File(target));
        System.out.println(target);
    }

    private static void printUsage() {
        System.err.println("Usage: java -jar xbrz.jar <source> [scaling_factor]");
    }

}
