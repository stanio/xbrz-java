package net.sourceforge.xbrz;

import static net.sourceforge.xbrz.RotationDegree.*;

/* input kernel area naming convention:
-----------------
| A | B | C | D |
|---|---|---|---|
| E | F | G | H |
|---|---|---|---|   input pixel is at position F
| I | J | K | L |
|---|---|---|---|
| M | N | O | P |
-----------------
*/
final class Kernel_4x4 {

    int
    a, b, c, //
    e, f, g, // support reinterpret_cast from Kernel_4x4 => Kernel_3x3
    i, j, k, //
    m, n, o,
    d, h, l, p;

    private final int[] src;
    private final int srcWidth;
    private final int srcHeight;
    private final boolean withAlpha;

    private int s_m1;
    private int s_0;
    private int s_p1;
    private int s_p2;

    Kernel_4x4(int[] src, int srcWidth, int srcHeight, boolean withAlpha) {
        this.src = src;
        this.srcWidth = srcWidth;
        this.srcHeight = srcHeight;
        this.withAlpha = withAlpha;
    }

    final void positionY(int y) {
        if (withAlpha) {
            positionYTransparent(y);
        } else {
            positionYDuplicate(y);
        }

        readDhlp(-4); //hack: read a, e, i, m at x = -1
        a = d;
        e = h;
        i = l;
        m = p;

        readDhlp(-3);
        b = d;
        f = h;
        j = l;
        n = p;

        readDhlp(-2);
        c = d;
        g = h;
        k = l;
        o = p;

        readDhlp(-1);
    }

    private final void positionYTransparent(int y) {
        s_m1 = 0 <= y - 1 && y - 1 < srcHeight ? srcWidth * (y - 1) : -1;
        s_0  = 0 <= y     && y     < srcHeight ? srcWidth *  y      : -1;
        s_p1 = 0 <= y + 1 && y + 1 < srcHeight ? srcWidth * (y + 1) : -1;
        s_p2 = 0 <= y + 2 && y + 2 < srcHeight ? srcWidth * (y + 2) : -1;
    }

    private final void positionYDuplicate(int y) {
        s_m1 = srcWidth * clamp(y - 1, 0, srcHeight - 1);
        s_0  = srcWidth * clamp(y,     0, srcHeight - 1);
        s_p1 = srcWidth * clamp(y + 1, 0, srcHeight - 1);
        s_p2 = srcWidth * clamp(y + 2, 0, srcHeight - 1);
    }

    static int clamp(int v, int lo, int hi) {
        return (v < lo) ? lo : (v > hi) ? hi : v;
    }

    final void readDhlp(int x) //(x, y) is at kernel position F
    {
        if (withAlpha) {
            readDhlpTransparent(x);
        } else {
            readDhlpDuplicate(x);
        }
    }

    private final void readDhlpTransparent(int x) {
        final int x_p2 = x + 2;
        if (0 <= x_p2 && x_p2 < srcWidth)
        {
            d = (s_m1 >= 0) ? src[s_m1 + x_p2] : 0;
            h = (s_0  >= 0) ? src[s_0  + x_p2] : 0;
            l = (s_p1 >= 0) ? src[s_p1 + x_p2] : 0;
            p = (s_p2 >= 0) ? src[s_p2 + x_p2] : 0;
        }
        else
        {
            d = 0;
            h = 0;
            l = 0;
            p = 0;
        }
    }

    private final void readDhlpDuplicate(int x) {
        final int xc_p2 = clamp(x + 2, 0, srcWidth - 1);
        d = src[s_m1 + xc_p2];
        h = src[s_0  + xc_p2];
        l = src[s_p1 + xc_p2];
        p = src[s_p2 + xc_p2];
    }

    final void shift() {
        a = b;    //shift kernel to the left
        e = f;    // -----------------
        i = j;    // | A | B | C | D |
        m = n;    // |---|---|---|---|
        /**/      // | E | F | G | H |
        b = c;    // |---|---|---|---|
        f = g;    // | I | J | K | L |
        j = k;    // |---|---|---|---|
        n = o;    // | M | N | O | P |
        /**/      // -----------------
        c = d;
        g = h;
        k = l;
        o = p;
    }

    //@Override
    //public String toString() {
    //    return String.format("| %08X | %08X | %08X | %08X |%n"
    //                         + "| %08X | %08X | %08X | %08X |%n"
    //                         + "| %08X | %08X | %08X | %08X |%n"
    //                         + "| %08X | %08X | %08X | %08X |",
    //                         a, b, c, d,
    //                         e, f, g, h,
    //                         i, j, k, l,
    //                         m, n, o, p);
    //}

}


/* input kernel area naming convention:
-------------
| A | B | C |
|---|---|---|
| D | E | F | input pixel is at position E
|---|---|---|
| G | H | I |
-------------
*/
final class Kernel_3x3 {

    private final Kernel_4x4 ker4;
    private RotationDegree rotDeg = ROT_0;

    Kernel_3x3(Kernel_4x4 ker4) {
        this.ker4 = ker4;
    }

    final int a() {
        switch (rotDeg) {
        default:      return ker4.a;
        case ROT_90:  return ker4.i;
        case ROT_180: return ker4.k;
        case ROT_270: return ker4.c;
        }
    }

    final int b() {
        switch (rotDeg) {
        default:      return ker4.b;
        case ROT_90:  return ker4.e;
        case ROT_180: return ker4.j;
        case ROT_270: return ker4.g;
        }
    }

    final int c() {
        switch (rotDeg) {
        default:      return ker4.c;
        case ROT_90:  return ker4.a;
        case ROT_180: return ker4.i;
        case ROT_270: return ker4.k;
        }
    }

    final int d() {
        switch (rotDeg) {
        default:      return ker4.e;
        case ROT_90:  return ker4.j;
        case ROT_180: return ker4.g;
        case ROT_270: return ker4.b;
        }
    }

    final int e() {
        return ker4.f; // center
    }

    final int f() {
        switch (rotDeg) {
        default:      return ker4.g;
        case ROT_90:  return ker4.b;
        case ROT_180: return ker4.e;
        case ROT_270: return ker4.j;
        }
    }

    final int g() {
        switch (rotDeg) {
        default:      return ker4.i;
        case ROT_90:  return ker4.k;
        case ROT_180: return ker4.c;
        case ROT_270: return ker4.a;
        }
    }

    final int h() {
        switch (rotDeg) {
        default:      return ker4.j;
        case ROT_90:  return ker4.g;
        case ROT_180: return ker4.b;
        case ROT_270: return ker4.e;
        }
    }

    final int i() {
        switch (rotDeg) {
        default:      return ker4.k;
        case ROT_90:  return ker4.c;
        case ROT_180: return ker4.a;
        case ROT_270: return ker4.i;
        }
    }

    final void rotDeg(RotationDegree deg) {
        this.rotDeg = deg;
    }

    //@Override
    //public String toString() {
    //    return String.format("| %08X | %08X | %08X |%n"
    //                         + "| %08X | %08X | %08X |%n"
    //                         + "| %08X | %08X | %08X |",
    //                         a(), b(), c(),
    //                         d(), e(), f(),
    //                         g(), h(), i());
    //}

}
