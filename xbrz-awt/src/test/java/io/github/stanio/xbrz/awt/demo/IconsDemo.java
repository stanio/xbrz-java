/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package io.github.stanio.xbrz.awt.demo;

import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;

import java.util.function.BiFunction;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import io.github.stanio.xbrz.awt.AwtXbrz;
import io.github.stanio.xbrz.awt.util.MultiResolutionCachedImage;

@SuppressWarnings("serial")
public class IconsDemo extends JFrame {

    public IconsDemo() {
        this("editbookmarks2");
    }

    public IconsDemo(String iconName) {
        super("Icon Scaling");
        super.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        super.getRootPane().getActionMap().put("exit", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                dispatchEvent(new WindowEvent(IconsDemo.this, WindowEvent.WINDOW_CLOSING));
                dispose();
            }
        });
        super.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                           .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "exit");
        initUI(iconName);
    }

    private static Image loadImage(String name) {
        return new ImageIcon(IconsDemo.class.getResource(name)).getImage();
    }

    private void initUI(String imageName) {
        Image loresImage = loadImage(imageName + "-16.png");
        Image hiresImage = loadImage(imageName + "-32.png");

        JComponent content = Box.createVerticalBox();
        content.setBackground(new javax.swing.JToolBar().getBackground());
        content.setOpaque(true);

        content.add(createIcons("Default (nearest-neighbor)", (w, h) -> loresImage));
        // https://bugs.openjdk.org/browse/JDK-8243061 randomly results in
        // java.lang.ClassCastException: class [I cannot be cast to class [B
        content.add(createIcons("SCALE_AREA_AVERAGING",
                (w, h) -> loresImage.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING)));
        //content.add(createIcons("CUSTOM_AREA_AVERAGING", scaleAverage(baseImage)));
        content.add(createIcons("xBRZ", scaleBRZ(loresImage)));
        content.add(createIcons("INTERPOLATION_BICUBIC",
                (w, h) -> scaleImage(loresImage, w, h, VALUE_INTERPOLATION_BICUBIC)));

        content.add(createIcons2("Default (hires base)", (w, h) -> hiresImage));
        // https://bugs.openjdk.org/browse/JDK-8243061 randomly results in
        // java.lang.ClassCastException: class [I cannot be cast to class [B
        content.add(createIcons2("SCALE_AREA_AVERAGING (hires base)",
                (w, h) -> hiresImage.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING)));
        //content.add(createIcons2("CUSTOM_AREA_AVERAGING (hires base)", scaleAverage(hiresImage)));
        content.add(createIcons2("xBRZ (hires base)", scaleBRZ(hiresImage)));
        content.add(createIcons2("INTERPOLATION_BICUBIC (hires base)",
                (w, h) -> scaleImage(hiresImage, w, h, VALUE_INTERPOLATION_BICUBIC)));

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
        comp.setOpaque(false);
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int size : new int[] { 16, 20, 24, 28, 32, 36, 40, 44, 48 }) {
            JLabel icon = new JLabel(new ImageIcon(
                    MultiResolutionCachedImage.of(size, size, scaler)));
            comp.add(icon, getHorizontalConstraints());
            comp.add(Box.createHorizontalStrut(10), getHorizontalConstraints());
            if (size == hiliteSize) {
                icon.setBackground(new Color(0xFF, 0xFF, 0xFF, 0x80));
                icon.setOpaque(true);
            }
        }
        JLabel caption = new JLabel(label);
        caption.setFont(Font.decode("Segoe UI-18"));
        comp.add(caption, getHorizontalConstraints());
        comp.add(Box.createHorizontalGlue(), getHorizontalConstraints(1.0));
        return comp;
    }

    private static JComponent createHorizontalBox() {
        return new JPanel(new GridBagLayout()) {
            @Override
            public Dimension getMaximumSize() {
                Dimension size = super.getMaximumSize();
                size.height = getPreferredSize().height;
                return size;
            }
        };
    }

    private static GridBagConstraints getHorizontalConstraints() {
        return getHorizontalConstraints(0);
    }

    private static GridBagConstraints getHorizontalConstraints(double weightx) {
        return new GridBagConstraints(GridBagConstraints.RELATIVE, 0, 1, 1, weightx, 0,
                                      GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
                                      new Insets(0, 0, 1, 0), 2, 1);
    }

    static BiFunction<Integer, Integer, Image> scaleAverage(Image baseImage) {
        return (w, h) -> {
            Image integralBase = upscaleIntegral(baseImage, w, h,
                    (iw, ih) -> scaleImage(baseImage, iw, ih, VALUE_INTERPOLATION_NEAREST_NEIGHBOR));
            return scaleImage(integralBase, w, h, VALUE_INTERPOLATION_BICUBIC);
        };
    }

    private static BiFunction<Integer, Integer, Image> scaleBRZ(Image baseImage) {
        return (w, h) -> {
            Image integralBase = upscaleIntegral(baseImage, w, h,
                    (iw, ih) -> AwtXbrz.scaleImage(baseImage, iw, ih));
            return scaleImage(integralBase, w, h, VALUE_INTERPOLATION_BICUBIC);
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
        final String iconName = (args.length == 1) ? args[0] : "editbookmarks2";
        SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception e) {
                System.err.println(e);
            }
            IconsDemo frame = new IconsDemo(iconName);
            frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

}
