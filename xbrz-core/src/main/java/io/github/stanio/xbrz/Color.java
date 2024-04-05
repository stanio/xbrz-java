package io.github.stanio.xbrz;

class Color {

    static int getAlpha(int pix) { return (pix >> 24) & 0xFF; }
    static int getRed  (int pix) { return (pix >> 16) & 0xFF; }
    static int getGreen(int pix) { return (pix >> 8)  & 0xFF; }
    static int getBlue (int pix) { return (pix >> 0)  & 0xFF; }

    static int makePixel(int a, int r, int g, int b) { return    (a << 24) | (r << 16) | (g << 8) | b; }
    static int makePixel(       int r, int g, int b) { return (0xFF << 24) | (r << 16) | (g << 8) | b; }

}
