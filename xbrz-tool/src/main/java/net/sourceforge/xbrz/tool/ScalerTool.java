package net.sourceforge.xbrz.tool;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Provides command-line entry point for using xBRZ.
 */
public class ScalerTool {

    public static BufferedImage scaleImage(File source, int factor) throws IOException {
        ImageData sourceData = new ImageData(ImageIO.read(source));
        return AwtXbrz.scaleImage(sourceData, factor);
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
