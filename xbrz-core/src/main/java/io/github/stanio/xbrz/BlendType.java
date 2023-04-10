package io.github.stanio.xbrz;

import static io.github.stanio.xbrz.BlendType.BLEND_NONE;

final class BlendType {

    static final byte BLEND_NONE = 0;
    static final byte BLEND_NORMAL = 1;   //a normal indication to blend
    static final byte BLEND_DOMINANT = 2; //a strong indication to blend

    //static String toString(byte blendType) {
    //    switch (blendType) {
    //    default:
    //    case BLEND_NONE: return "  ";
    //    case BLEND_NORMAL: return "\u2591\u2591";
    //    case BLEND_DOMINANT: return "\u2593\u2593";
    //    }
    //}

}


/*
---------
| F | G |
|---+---|   current input pixel is at position F
| J | K |
--------- */
final class BlendResult
{
    byte
    /**/blend_f, blend_g,
    /**/blend_j, blend_k;

    final void reset() {
        blend_f = blend_g = blend_j = blend_k = BLEND_NONE;
    }

    //@Override
    //public String toString() {
    //    return String.format("%s%s%n%s%s",
    //                         BlendType.toString(blend_f),
    //                         BlendType.toString(blend_g),
    //                         BlendType.toString(blend_j),
    //                         BlendType.toString(blend_k));
    //}

}


final class BlendInfo {

    static byte rotate(byte b, RotationDegree rotDeg) {
        switch (rotDeg) {
        default:      return b;
        case ROT_90:  return (byte) (((b << 2) & 0xFF) | ((b & 0xFF) >> 6));
        case ROT_180: return (byte) (((b << 4) & 0xFF) | ((b & 0xFF) >> 4));
        case ROT_270: return (byte) (((b << 6) & 0xFF) | ((b & 0xFF) >> 2));
        }
    }

    static boolean blendingNeeded(byte b) {
        return b != BLEND_NONE;
    }

    //static byte getTopL   (byte b) { return (byte) (0x3 & b); }
    static byte getTopR   (byte b) { return (byte) (0x3 & (b >> 2)); }
    static byte getBottomR(byte b) { return (byte) (0x3 & (b >> 4)); }
    static byte getBottomL(byte b) { return (byte) (0x3 & (b >> 6)); }

    static byte clearAddTopL(        byte bt) { return bt; }
    static byte addTopR     (byte b, byte bt) { return (byte) (b | (bt << 2)); } //buffer is assumed to be initialized before preprocessing!
    static byte addBottomR  (byte b, byte bt) { return (byte) (b | (bt << 4)); } //e.g. via clearAddTopL()
    //static byte addBottomL  (byte b, byte bt) { return (byte) (b | (bt << 6)); } //

    static void clearAddTopL(byte[] buf, int i, byte bt) { buf[i] = bt; }
    static void addTopR     (byte[] buf, int i, byte bt) { buf[i] |= bt << 2; } //buffer is assumed to be initialized before preprocessing!
    //static void addBottomR  (byte[] buf, int i, byte bt) { buf[i] |= bt << 4; } //e.g. via clearAddTopL()
    static void addBottomL  (byte[] buf, int i, byte bt) { buf[i] |= bt << 6; } //

    //@Override
    //public String toString() {
    //    return String.format("%s%s%n%s%s",
    //                         BlendType.toString(getTopL()),
    //                         BlendType.toString(getTopR()),
    //                         BlendType.toString(getBottomL()),
    //                         BlendType.toString(getBottomR()));
    //}

    //static String toString(byte[] buf) {
    //    StringBuilder str = new StringBuilder();
    //    BlendInfo blend = new BlendInfo();
    //    for (int i = 0; i < buf.length; i++) {
    //        blend.val = buf[i];
    //        str.append(BlendType.toString(blend.getTopL()))
    //                .append(BlendType.toString(blend.getTopR()));
    //    }
    //    str.append(System.lineSeparator());
    //    for (int i = 0; i < buf.length; i++) {
    //        blend.val = buf[i];
    //        str.append(BlendType.toString(blend.getBottomL()))
    //                .append(BlendType.toString(blend.getBottomR()));
    //    }
    //    return str.toString();
    //}

}
