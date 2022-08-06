/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package io.github.stanio.xbrz.awt.demo;

import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;

import java.util.function.BiFunction;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import io.github.stanio.xbrz.awt.AwtXbrz;
import io.github.stanio.xbrz.awt.XbrzImage;
import io.github.stanio.xbrz.awt.util.MultiResolutionCachedImage;

@SuppressWarnings("serial")
public class IconsDemo extends JFrame {

    public IconsDemo() {
        super("Icon Scaling");
        super.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
    }

    private static Image loadImage(String name) {
        return new ImageIcon(IconsDemo.class.getResource(name)).getImage();
    }

    private void initUI() {
        Image baseImage = loadImage("editbookmarks.png");

        JComponent content = Box.createVerticalBox();
        content.add(createIcons("Default", (w, h) -> baseImage));
        content.add(createIcons("SCALE_AREA_AVERAGING",
                (w, h) -> baseImage.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING)));
        content.add(createIcons("CUSTOM_AREA_AVERAGING", scaleAverage(baseImage)));
        content.add(createIcons("INTERPOLATION_BILINEAR",
                (w, h) -> scaleImage(baseImage, w, h, VALUE_INTERPOLATION_BILINEAR)));
        content.add(createIcons("xBRZ", scaleBRZ(baseImage)));

        Image hiresImage = XbrzImage.mrImage(baseImage).getResolutionVariant(32, 32);
        content.add(createIcons2("Default (hires base)", (w, h) -> hiresImage));
        content.add(createIcons2("SCALE_AREA_AVERAGING (hires base)",
                (w, h) -> hiresImage.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING)));
        content.add(createIcons2("CUSTOM_AREA_AVERAGING (hires base)", scaleAverage(hiresImage)));
        content.add(createIcons2("xBRZ (hires base)", scaleBRZ(hiresImage)));
        content.add(createIcons2("INTERPOLATION_BILINEAR (hires base)",
                (w, h) -> scaleImage(hiresImage, w, h, VALUE_INTERPOLATION_BILINEAR)));

        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        super.add(content);
    }

    private static JComponent
            createIcons(String label, BiFunction<Integer, Integer, Image> scaler) {
        return createIcons(label, scaler, 16);
    }

    private static JComponent
            createIcons2(String label, BiFunction<Integer, Integer, Image> scaler) {
        return createIcons(label, scaler, 32);
    }

    private static JComponent createIcons(String label,
            BiFunction<Integer, Integer, Image> scaler, int hiliteSize) {
        JComponent comp = createHorizontalBox();
        for (int size : new int[] { 16, 20, 24, 28, 32, 36, 40, 44, 48 }) {
            JLabel icon = new JLabel(new ImageIcon(
                    MultiResolutionCachedImage.of(size, size, scaler)));
            comp.add(icon);
            comp.add(Box.createHorizontalStrut(5));
            if (size == hiliteSize) {
                icon.setBackground(new Color(0, 0xFF, 0, 0x80));
                icon.setOpaque(true);
            }
        }
        comp.add(new JLabel(label));
        comp.add(Box.createHorizontalGlue());
        return comp;
    }

    private static JComponent createHorizontalBox() {
        return new Box(BoxLayout.LINE_AXIS) {
            @Override
            public Dimension getMaximumSize() {
                Dimension size = super.getMaximumSize();
                size.height = getPreferredSize().height;
                return size;
            }
        };
    }

    private static BiFunction<Integer, Integer, Image> scaleAverage(Image baseImage) {
        return (w, h) -> {
            Image integralBase = upscaleIntegral(baseImage, w, h,
                    (iw, ih) -> scaleImage(baseImage, iw, ih, VALUE_INTERPOLATION_NEAREST_NEIGHBOR));
            return scaleImage(integralBase, w, h, VALUE_INTERPOLATION_BILINEAR);
        };
    }

    private static BiFunction<Integer, Integer, Image> scaleBRZ(Image baseImage) {
        return (w, h) -> {
            Image integralBase = upscaleIntegral(baseImage, w, h,
                    (iw, ih) -> AwtXbrz.scaleImage(baseImage, iw, ih));
            return scaleImage(integralBase, w, h, VALUE_INTERPOLATION_BILINEAR);
        };
    }

    private static Image upscaleIntegral(Image image,
            int destWidth, int destHeight, BiFunction<Integer, Integer, Image> scaler) {
        int integralWidth = image.getWidth(null);
        int integralHeight = image.getHeight(null);
        if (integralWidth < destWidth || integralHeight < destHeight) {
            while (integralWidth < destWidth) integralWidth *= 2;
            while (integralHeight < destHeight) integralHeight *= 2;
            return scaler.apply(integralWidth, integralHeight);
        }
        return image;
    }

    private static BufferedImage scaleImage(Image image,
            int destWidth, int destHeight, Object interpolation) {
        BufferedImage scaled = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
        try {
            g.drawImage(image, 0, 0, destWidth, destHeight, null);
        } finally {
            g.dispose();
        }
        return scaled;
    }

    /**
     * Try with {@code -Dsun.java2d.uiScale=1.0}, also.
     *
     * @param   args  <i>unused</i>
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            IconsDemo frame = new IconsDemo();
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

}
