package org.kynosarges.tektosyne;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Provides unit tests for class {@link Fortran}.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class FortranTest {
    
    private final float DELTA = 0.001f;

    @Test
    public void testAint() {
        assertEquals(3d, Fortran.aint(3.6d), DELTA);
        assertEquals(3d, Fortran.aint(3.4d), DELTA);

        assertEquals(-3d, Fortran.aint(-3.6d), DELTA);
        assertEquals(-3d, Fortran.aint(-3.4d), DELTA);

        assertEquals(3f, Fortran.aint(3.6f), DELTA);
        assertEquals(3f, Fortran.aint(3.4f), DELTA);

        assertEquals(-3f, Fortran.aint(-3.6f), DELTA);
        assertEquals(-3f, Fortran.aint(-3.4f), DELTA);
    }
    
    @Test
    public void testAnint() {
        assertEquals(4d, Fortran.anint(3.6d), DELTA);
        assertEquals(4d, Fortran.anint(3.5d), DELTA);
        assertEquals(3d, Fortran.anint(3.4d), DELTA);

        assertEquals(-4d, Fortran.anint(-3.6d), DELTA);
        assertEquals(-4d, Fortran.anint(-3.5d), DELTA);
        assertEquals(-3d, Fortran.anint(-3.4d), DELTA);

        assertEquals(4f, Fortran.anint(3.6f), DELTA);
        assertEquals(4f, Fortran.anint(3.5f), DELTA);
        assertEquals(3f, Fortran.anint(3.4f), DELTA);

        assertEquals(-4f, Fortran.anint(-3.6f), DELTA);
        assertEquals(-4f, Fortran.anint(-3.5f), DELTA);
        assertEquals(-3f, Fortran.anint(-3.4f), DELTA);
    }
    
    @Test
    public void testCeiling() {
        assertEquals(4, Fortran.ceiling(3.6d));
        assertEquals(4, Fortran.ceiling(3.4d));

        assertEquals(-3, Fortran.ceiling(-3.6d));
        assertEquals(-3, Fortran.ceiling(-3.4d));

        assertEquals(4, Fortran.ceiling(3.6f));
        assertEquals(4, Fortran.ceiling(3.4f));

        assertEquals(-3, Fortran.ceiling(-3.6f));
        assertEquals(-3, Fortran.ceiling(-3.4f));

        try {
            Fortran.ceiling(1e100);
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }

        try {
            Fortran.ceiling(1e30f);
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }

    @Test
    public void testFloor() {
        assertEquals(3, Fortran.floor(3.6d));
        assertEquals(3, Fortran.floor(3.4d));

        assertEquals(-4, Fortran.floor(-3.6d));
        assertEquals(-4, Fortran.floor(-3.4d));

        assertEquals(3, Fortran.floor(3.6f));
        assertEquals(3, Fortran.floor(3.4f));

        assertEquals(-4, Fortran.floor(-3.6f));
        assertEquals(-4, Fortran.floor(-3.4f));

        try {
            Fortran.floor(1e100);
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }

        try {
            Fortran.floor(1e30f);
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }

    @Test
    public void testKnint() {
        assertEquals(4L, Fortran.knint(3.6d));
        assertEquals(4L, Fortran.knint(3.5d));
        assertEquals(3L, Fortran.knint(3.4d));

        assertEquals(-4L, Fortran.knint(-3.6d));
        assertEquals(-4L, Fortran.knint(-3.5d));
        assertEquals(-3L, Fortran.knint(-3.4d));

        assertEquals(4L, Fortran.knint(3.6f));
        assertEquals(4L, Fortran.knint(3.5f));
        assertEquals(3L, Fortran.knint(3.4f));

        assertEquals(-4L, Fortran.knint(-3.6f));
        assertEquals(-4L, Fortran.knint(-3.5f));
        assertEquals(-3L, Fortran.knint(-3.4f));

        try {
            Fortran.knint(1e100);
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }

        try {
            Fortran.knint(1e30f);
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }

    @Test
    public void testMax() {
        assertEquals(Double.NEGATIVE_INFINITY, Fortran.max(new double[] {}), DELTA);
        assertEquals(5d, Fortran.max(new double[] { 5 }), DELTA);
        assertEquals(5d, Fortran.max(new double[] { 2, 5, 1, 3, 4 }), DELTA);
        assertEquals(-1d, Fortran.max(new double[] { -2, -5, -1, -3, -4 }), DELTA);

        assertEquals(Float.NEGATIVE_INFINITY, Fortran.max(new float[] {}), DELTA);
        assertEquals(5f, Fortran.max(new float[] { 5 }), DELTA);
        assertEquals(5f, Fortran.max(new float[] { 2, 5, 1, 3, 4 }), DELTA);
        assertEquals(-1f, Fortran.max(new float[] { -2, -5, -1, -3, -4 }), DELTA);

        assertEquals(Integer.MIN_VALUE, Fortran.max(new int[] {}));
        assertEquals(5, Fortran.max(new int[] { 5 }));
        assertEquals(5, Fortran.max(new int[] { 2, 5, 1, 3, 4 }));
        assertEquals(-1, Fortran.max(new int[] { -2, -5, -1, -3, -4 }));

        assertEquals(Long.MIN_VALUE, Fortran.max(new long[] {}));
        assertEquals(5L, Fortran.max(new long[] { 5 }));
        assertEquals(5L, Fortran.max(new long[] { 2, 5, 1, 3, 4 }));
        assertEquals(-1L, Fortran.max(new long[] { -2, -5, -1, -3, -4 }));
    }

    @Test
    public void testMin() {
        assertEquals(Double.MAX_VALUE, Fortran.min(new double[] {}), DELTA);
        assertEquals(5d, Fortran.min(new double[] { 5 }), DELTA);
        assertEquals(1d, Fortran.min(new double[] { 2, 5, 1, 3, 4 }), DELTA);

        assertEquals(Float.MAX_VALUE, Fortran.min(new float[] {}), DELTA);
        assertEquals(5f, Fortran.min(new float[] { 5 }), DELTA);
        assertEquals(1f, Fortran.min(new float[] { 2, 5, 1, 3, 4 }), DELTA);

        assertEquals(Integer.MAX_VALUE, Fortran.min(new int[] {}));
        assertEquals(5, Fortran.min(new int[] { 5 }));
        assertEquals(1, Fortran.min(new int[] { 2, 5, 1, 3, 4 }));

        assertEquals(Long.MAX_VALUE, Fortran.min(new long[] {}));
        assertEquals(5L, Fortran.min(new long[] { 5 }));
        assertEquals(1L, Fortran.min(new long[] { 2, 5, 1, 3, 4 }));
    }

    @Test
    public void testModulo() {
        assertEquals(1d, Fortran.modulo(4d, 3d), DELTA);
        assertEquals(1f, Fortran.modulo(4f, 3f), DELTA);
        assertEquals(2d, Fortran.modulo(12d, 5d), DELTA);
        assertEquals(2f, Fortran.modulo(12f, 5f), DELTA);
        assertEquals(0.1d, Fortran.modulo(9.7d, 4.8d), DELTA);
        assertEquals(0.1f, Fortran.modulo(9.7f, 4.8f), DELTA);

        assertEquals(1, Fortran.modulo(4, 3));
        assertEquals(1L, Fortran.modulo(4L, 3L));
        assertEquals(2, Fortran.modulo(12, 5));
        assertEquals(2L, Fortran.modulo(12L, 5L));

        assertEquals(-2d, Fortran.modulo(4d, -3d), DELTA);
        assertEquals(-2f, Fortran.modulo(4f, -3f), DELTA);
        assertEquals(-3d, Fortran.modulo(12d, -5d), DELTA);
        assertEquals(-3f, Fortran.modulo(12f, -5f), DELTA);
        assertEquals(-4.7d, Fortran.modulo(9.7d, -4.8d), DELTA);
        assertEquals(-4.7f, Fortran.modulo(9.7f, -4.8f), DELTA);

        assertEquals(-2, Fortran.modulo(4, -3));
        assertEquals(-2L, Fortran.modulo(4L, -3L));
        assertEquals(-3, Fortran.modulo(12, -5));
        assertEquals(-3L, Fortran.modulo(12L, -5L));

        assertEquals(2d, Fortran.modulo(-4d, 3d), DELTA);
        assertEquals(2f, Fortran.modulo(-4f, 3f), DELTA);
        assertEquals(3d, Fortran.modulo(-12d, 5d), DELTA);
        assertEquals(3f, Fortran.modulo(-12f, 5f), DELTA);
        assertEquals(4.7d, Fortran.modulo(-9.7d, 4.8d), DELTA);
        assertEquals(4.7f, Fortran.modulo(-9.7f, 4.8f), DELTA);

        assertEquals(2, Fortran.modulo(-4, 3));
        assertEquals(2L, Fortran.modulo(-4L, 3L));
        assertEquals(3, Fortran.modulo(-12, 5));
        assertEquals(3L, Fortran.modulo(-12L, 5L));

        assertEquals(-1d, Fortran.modulo(-4d, -3d), DELTA);
        assertEquals(-1f, Fortran.modulo(-4f, -3f), DELTA);
        assertEquals(-2d, Fortran.modulo(-12d, -5d), DELTA);
        assertEquals(-2f, Fortran.modulo(-12f, -5f), DELTA);
        assertEquals(-0.1d, Fortran.modulo(-9.7d, -4.8d), DELTA);
        assertEquals(-0.1f, Fortran.modulo(-9.7f, -4.8f), DELTA);

        assertEquals(-1, Fortran.modulo(-4, -3));
        assertEquals(-1L, Fortran.modulo(-4L, -3L));
        assertEquals(-2, Fortran.modulo(-12, -5));
        assertEquals(-2L, Fortran.modulo(-12L, -5L));
    }
    
    @Test(expected=ArithmeticException.class)
    public void testModuloDoubleZero() {
        Fortran.modulo(10d, 0d);
    }
    
    @Test(expected=ArithmeticException.class)
    public void testModuloFloatZero() {
        Fortran.modulo(10f, 0f);
    }
    
    @Test(expected=ArithmeticException.class)
    public void testModuloIntegerZero() {
        Fortran.modulo(10, 0);
    }
    
    @Test(expected=ArithmeticException.class)
    public void testModuloLongZero() {
        Fortran.modulo(10L, 0L);
    }
    
    @Test
    public void testNint() {
        assertEquals(4, Fortran.nint(3.6d));
        assertEquals(4, Fortran.nint(3.5d));
        assertEquals(3, Fortran.nint(3.4d));

        assertEquals(-4, Fortran.nint(-3.6d));
        assertEquals(-4, Fortran.nint(-3.5d));
        assertEquals(-3, Fortran.nint(-3.4d));

        assertEquals(4, Fortran.nint(3.6f));
        assertEquals(4, Fortran.nint(3.5f));
        assertEquals(3, Fortran.nint(3.4f));

        assertEquals(-4, Fortran.nint(-3.6f));
        assertEquals(-4, Fortran.nint(-3.5f));
        assertEquals(-3, Fortran.nint(-3.4f));

        try {
            Fortran.nint(1e100);
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }

        try {
            Fortran.nint(1e30f);
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }

    @Test
    public void testSum() {
        assertEquals(0d, Fortran.sum(new double[] {}), DELTA);
        assertEquals(5d, Fortran.sum(new double[] { 5 }), DELTA);
        assertEquals(15d, Fortran.sum(new double[] { 2, 5, 1, 3, 4 }), DELTA);

        assertEquals(0f, Fortran.sum(new float[] {}), DELTA);
        assertEquals(5f, Fortran.sum(new float[] { 5 }), DELTA);
        assertEquals(15f, Fortran.sum(new float[] { 2, 5, 1, 3, 4 }), DELTA);

        assertEquals(0, Fortran.sum(new int[] {}));
        assertEquals(5, Fortran.sum(new int[] { 5 }));
        assertEquals(15, Fortran.sum(new int[] { 2, 5, 1, 3, 4 }));

        assertEquals(0L, Fortran.sum(new long[] {}));
        assertEquals(5L, Fortran.sum(new long[] { 5 }));
        assertEquals(15L, Fortran.sum(new long[] { 2, 5, 1, 3, 4 }));

        try {
            Fortran.sum(new int[] { Integer.MAX_VALUE, 1 });
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }

        try {
            Fortran.sum(new long[] { Long.MAX_VALUE, 1 });
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }
}
