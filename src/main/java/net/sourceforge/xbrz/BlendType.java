package net.sourceforge.xbrz;

import static net.sourceforge.xbrz.BlendType.BLEND_NONE;

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

    final BlendResult reset() {
        blend_f = blend_g = blend_j = blend_k = BLEND_NONE;
        return this;
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

    byte val;

    final BlendInfo reset(BlendInfo other, RotationDegree rotDeg) {
        byte b = other.val;
        switch (rotDeg) {
        default:      val = b; break;
        case ROT_90:  val = (byte) (((b << 2) & 0xFF) | ((b & 0xFF) >> 6)); break;
        case ROT_180: val = (byte) (((b << 4) & 0xFF) | ((b & 0xFF) >> 4)); break;
        case ROT_270: val = (byte) (((b << 6) & 0xFF) | ((b & 0xFF) >> 2)); break;
        }
        return this;
    }

    final boolean blendingNeeded() {
        return val != BLEND_NONE;
    }

    //final byte getTopL   () { return (byte) (0x3 & val); }
    final byte getTopR   () { return (byte) (0x3 & (val >> 2)); }
    final byte getBottomR() { return (byte) (0x3 & (val >> 4)); }
    final byte getBottomL() { return (byte) (0x3 & (val >> 6)); }

    final void clearAddTopL(byte bt) { val = bt; }
    final void addTopR     (byte bt) { val |= bt << 2; } //buffer is assumed to be initialized before preprocessing!
    final void addBottomR  (byte bt) { val |= bt << 4; } //e.g. via clearAddTopL()
    //final void addBottomL  (byte bt) { val |= bt << 6; } //

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
