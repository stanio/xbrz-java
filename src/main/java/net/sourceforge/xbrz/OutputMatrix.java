package net.sourceforge.xbrz;

import static net.sourceforge.xbrz.MatrixRotation.HALF_BYTE;
import static net.sourceforge.xbrz.RotationDegree.*;

//access matrix area, top-left at current position
final class OutputMatrix {

    private final int N;
    private final int[] out;
    private final int outWidth;

    private int offset;
    private RotationDegree rotDeg = ROT_0;
    private final MatrixRotation rot;

    OutputMatrix(int N, int[] out, int outWidth) {
        this.N = N;
        this.out = out;
        this.outWidth = outWidth;
        this.rot = new MatrixRotation(N);
    }

    final void positionY(int y) {
        offset = N * y * outWidth;
    }

    final void incrementX() {
        offset += N;
    }

    final void rotDeg(RotationDegree deg) {
        this.rotDeg = deg;
    }

    private final int position(final int I, final int J) {
        final byte IJ_old = rot.calc(rotDeg, I, J);
        final int I_old = IJ_old >> HALF_BYTE & 0xF;
        final int J_old = IJ_old & 0xF;
        return offset + J_old + I_old * outWidth;
    }

    final void set(int I, int J, int val) {
        out[position(I, J)] = val;
    }

    final void set(int I, int J, IntFunction func) {
        final int pos = position(I, J);
        out[pos] = func.apply(out[pos]);
    }

    //fill block of size scale * scale with the given color
    final void fillBlock(int col) {
        fillBlock(col, N, N);
    }

    final void fillBlock(int col, int blockWidth, int blockHeight) {
        for (int y = 0, trg = y * outWidth + offset; y < blockHeight; ++y, trg += outWidth)
            for (int x = 0; x < blockWidth; ++x)
                out[trg + x] = col;
    }

}


@FunctionalInterface interface IntFunction {
    int apply(int a);
}


final class MatrixRotation {

    static final int HALF_BYTE = Byte.SIZE / 2;

    private final int N;
    private final int Nsq;
    private final byte[] lookup;

    MatrixRotation(int N) {
        this.N = N;
        this.Nsq = N * N;
        if (N > 16) {
            throw new IllegalArgumentException("N should be <= 16");
        }

        byte[] lookup = new byte[4 * Nsq];
        for (int rotDeg = 0; rotDeg < 4; rotDeg++) {
            int offset = rotDeg * Nsq;
            for (int I = 0; I < N; I++) {
                for (int J = 0; J < N; J++) {
                    lookup[offset + I * N + J] =
                            calc(rotDeg, (byte) ((I << HALF_BYTE) | J));
                }
            }
        }
        this.lookup = lookup;
    }

    private final byte calc(int rotDeg, byte IJ) {
        if (rotDeg == 0) {
            return IJ;
        }
        byte IJ_old = calc(rotDeg - 1, IJ);
        int J_old = IJ_old         & 0xF;
        int I_old = IJ_old >> HALF_BYTE & 0xF;

        int rot_I = N - 1 - J_old;
        int rot_J =         I_old;
        return (byte) (rot_I << HALF_BYTE | rot_J);
    }

    final byte calc(RotationDegree rotDeg, int I, int J) {
        final int offset = rotDeg.ordinal() * Nsq;
        return lookup[offset + I * N + J];
    }

}
