package net.sourceforge.xbrz;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

public class MatrixRotationTest {

    private static final int N = 4;
    private static MatrixRotation m4x4;

    @BeforeClass
    public static void setUpSuite() {
        m4x4 = MatrixRotation.of(4);
    }

    @Test
    public void matrix4rotation0() throws Exception {
        final RotationDegree rot0 = RotationDegree.ROT_0;

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                assertEquals(i + ", " + j,
                        (i << 4 | j), m4x4.calc(rot0, i, j) & 0xFF);
            }
        }
    }

    @Test
    public void matrix4rotation90() throws Exception {
        final RotationDegree rot90 = RotationDegree.ROT_90;
        int[] expected = new int[] {
            0x30, 0x20, 0x10, 0x00,
            0x31, 0x21, 0x11, 0x01,
            0x32, 0x22, 0x12, 0x02,
            0x33, 0x23, 0x13, 0x03
        };

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                assertEquals(i + ", " + j,
                        expected[i * N + j], m4x4.calc(rot90, i, j) & 0xFF);
            }
        }
    }

    @Test
    public void matrix4rotation180() throws Exception {
        final RotationDegree rot180 = RotationDegree.ROT_180;
        int[] expected = new int[] {
            0x33, 0x32, 0x31, 0x30,
            0x23, 0x22, 0x21, 0x20,
            0x13, 0x12, 0x11, 0x10,
            0x03, 0x02, 0x01, 0x00
        };

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                assertEquals(i + ", " + j,
                        expected[i * N + j], m4x4.calc(rot180, i, j) & 0xFF);
            }
        }
    }

    @Test
    public void matrix4rotation270() throws Exception {
        final RotationDegree rot270 = RotationDegree.ROT_270;
        int[] expected = new int[] {
            0x03, 0x13, 0x23, 0x33,
            0x02, 0x12, 0x22, 0x32,
            0x01, 0x11, 0x21, 0x31,
            0x00, 0x10, 0x20, 0x30
        };

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                assertEquals(i + ", " + j,
                        expected[i * 4 + j], m4x4.calc(rot270, i, j) & 0xFF);
            }
        }
    }

    @Test
    public void matrix3rotation270() throws Exception {
        final int N = 3;
        MatrixRotation m3x3 = MatrixRotation.of(N);
        final RotationDegree rot270 = RotationDegree.ROT_270;
        int[] expected = new int[] {
            0x2, 0x12, 0x22,
            0x1, 0x11, 0x21,
            0x0, 0x10, 0x20
        };

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                assertEquals(i + ", " + j,
                        expected[i * N + j], m3x3.calc(rot270, i, j) & 0xFF);
            }
        }
    }

}
