/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package io.github.stanio.xbrz.awt.demo;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import io.github.stanio.xbrz.awt.XbrzImage;

@SuppressWarnings("serial")
public class AnimDemo extends JFrame {

    public AnimDemo() {
        this("kirby-2.gif");
    }

    public AnimDemo(String animName) {
        super("Xbrz Animation");
        super.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        super.getRootPane().getActionMap().put("exit", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                dispatchEvent(new WindowEvent(AnimDemo.this, WindowEvent.WINDOW_CLOSING));
                dispose();
            }
        });
        super.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                           .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "exit");
        initUI(animName);
    }

    private static Image loadImage(String name) {
        return new ImageIcon(Toolkit.getDefaultToolkit()
                .createImage(AnimDemo.class.getResource(name))).getImage();
    }

    private void initUI(String imageName) {
        Image baseImage = loadImage(imageName);
        super.add(dynamicCanvas(XbrzImage.mrImage(baseImage), true));
    }

    private static JComponent dynamicCanvas(Image image, boolean smooth) {
        return new JComponent() {
            private final Insets tmpInsets = new Insets(0, 0, 0, 0);
            private final int baseWidth = image.getWidth(null);
            private final int baseHeight = image.getHeight(null);
            {
                setPreferredSize(new Dimension(baseWidth, baseHeight));
            }

            @Override
            public void paint(Graphics g) {
                Insets insets = getInsets(tmpInsets);
                int availableWidth = getWidth() - insets.left - insets.right;
                int availableHeight = getHeight() - insets.top - insets.bottom;
                if (availableWidth <= 0 || availableHeight <= 0) {
                    return;
                }

                Graphics2D g2 = null;
                if (g instanceof Graphics2D && smooth) {
                    g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g = g2;
                }

                double scaleWidth = (double) availableWidth / baseWidth;
                double scaleHeight = (double) availableHeight / baseHeight;
                int targetWidth, targetHeight;
                if (scaleWidth < scaleHeight) {
                    targetWidth = availableWidth;
                    targetHeight = (int) Math.round(baseHeight * scaleWidth);
                } else {
                    targetWidth = (int) Math.round(baseWidth * scaleHeight);
                    targetHeight = availableHeight;
                }

                int x = (availableWidth - targetWidth) / 2 + insets.left;
                int y = (availableHeight - targetHeight) / 2 + insets.top;
                g.drawImage(image, x, y, targetWidth, targetHeight, this);
                //g.drawImage(mrImage, x, y, x + targetWidth, y + targetHeight,
                //                     0, 0, baseWidth, baseHeight, this);

                if (g2 != null) {
                    g2.dispose();
                }
            }
        };
    }

    public static void main(String[] args) {
        // kirby-1.gif
        // kirby-2.gif
        // horizon-1.gif
        // sbreng-1.gif
        final String animName = (args.length == 1) ? args[0] : "kirby-1.gif";
        SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception e) {
                System.err.println(e);
            }
            AnimDemo frame = new AnimDemo(animName);
            frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

}
