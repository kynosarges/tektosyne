package org.kynosarges.tektosyne;

import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Provides unit tests for class {@link MathUtils}.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class MathUtilsTest {
    
    private final float EPSILON = 0.5f;

    @Test
    public void testCompare() {
        assertEquals( 0, MathUtils.compare(0.0, 0.0, EPSILON));
        assertEquals( 0, MathUtils.compare(0f, 0f, EPSILON));

        assertEquals(-1, MathUtils.compare(0.0, 1.0, EPSILON));
        assertEquals( 0, MathUtils.compare(-Double.MIN_NORMAL, Double.MIN_NORMAL, EPSILON));
        assertEquals(+1, MathUtils.compare(0.0, -1.0, EPSILON));
        assertEquals( 0, MathUtils.compare(Double.MIN_NORMAL, -Double.MIN_NORMAL, EPSILON));

        assertEquals(-1, MathUtils.compare(0f, 1f, EPSILON));
        assertEquals( 0, MathUtils.compare(-Float.MIN_NORMAL, Float.MIN_NORMAL, EPSILON));
        assertEquals(+1, MathUtils.compare(0f, -1f, EPSILON));
        assertEquals( 0, MathUtils.compare(Float.MIN_NORMAL, -Float.MIN_NORMAL, EPSILON));
    }

    @Test
    public void testCompareMin() {
        assertEquals( 0, MathUtils.compare(0.0, 0.0, Double.MIN_NORMAL));
        assertEquals( 0, MathUtils.compare(0.0, Double.MIN_NORMAL, Double.MIN_NORMAL));
        assertEquals( 0, MathUtils.compare(0.0, -Double.MIN_NORMAL, Double.MIN_NORMAL));

        assertEquals(-1, MathUtils.compare(0.0, 1.0, Double.MIN_NORMAL));
        assertEquals(-1, MathUtils.compare(-Double.MIN_NORMAL, Double.MIN_NORMAL, Double.MIN_NORMAL));
        assertEquals(+1, MathUtils.compare(0.0, -1.0, Double.MIN_NORMAL));
        assertEquals(+1, MathUtils.compare(Double.MIN_NORMAL, -Double.MIN_NORMAL, Double.MIN_NORMAL));

        assertEquals( 0, MathUtils.compare(0f, 0f, Float.MIN_NORMAL));
        assertEquals( 0, MathUtils.compare(0f, Float.MIN_NORMAL, Float.MIN_NORMAL));
        assertEquals( 0, MathUtils.compare(0f, -Float.MIN_NORMAL, Float.MIN_NORMAL));

        assertEquals(-1, MathUtils.compare(0f, 1f, Float.MIN_NORMAL));
        assertEquals(-1, MathUtils.compare(-Float.MIN_NORMAL, Float.MIN_NORMAL, Float.MIN_NORMAL));
        assertEquals(+1, MathUtils.compare(0f, -1f, Float.MIN_NORMAL));
        assertEquals(+1, MathUtils.compare(Float.MIN_NORMAL, -Float.MIN_NORMAL, Float.MIN_NORMAL));
    }

    @Test
    public void testEquals() {
        assertTrue(MathUtils.equals(0.0, 0.0, EPSILON));
        assertTrue(MathUtils.equals(0f, 0f, EPSILON));

        assertFalse(MathUtils.equals(0.0, 1.0, EPSILON));
        assertTrue(MathUtils.equals(-Double.MIN_NORMAL, Double.MIN_NORMAL, EPSILON));
        assertFalse(MathUtils.equals(0.0, -1.0, EPSILON));
        assertTrue(MathUtils.equals(Double.MIN_NORMAL, -Double.MIN_NORMAL, EPSILON));

        assertFalse(MathUtils.equals(0f, 1f, EPSILON));
        assertTrue(MathUtils.equals(-Float.MIN_NORMAL, Float.MIN_NORMAL, EPSILON));
        assertFalse(MathUtils.equals(0f, -1f, EPSILON));
        assertTrue(MathUtils.equals(Float.MIN_NORMAL, -Float.MIN_NORMAL, EPSILON));
    }

    @Test
    public void testEqualsMin() {
        assertTrue(MathUtils.equals(0.0, 0.0, Double.MIN_NORMAL));
        assertTrue(MathUtils.equals(0.0, Double.MIN_NORMAL, Double.MIN_NORMAL));
        assertTrue(MathUtils.equals(0.0, -Double.MIN_NORMAL, Double.MIN_NORMAL));

        assertFalse(MathUtils.equals(0.0, 1.0, Double.MIN_NORMAL));
        assertFalse(MathUtils.equals(-Double.MIN_NORMAL, Double.MIN_NORMAL, Double.MIN_NORMAL));
        assertFalse(MathUtils.equals(0.0, -1.0, Double.MIN_NORMAL));
        assertFalse(MathUtils.equals(Double.MIN_NORMAL, -Double.MIN_NORMAL, Double.MIN_NORMAL));

        assertTrue(MathUtils.equals(0f, 0f, Float.MIN_NORMAL));
        assertTrue(MathUtils.equals(0f, Float.MIN_NORMAL, Float.MIN_NORMAL));
        assertTrue(MathUtils.equals(0f, -Float.MIN_NORMAL, Float.MIN_NORMAL));

        assertFalse(MathUtils.equals(0f, 1f, Float.MIN_NORMAL));
        assertFalse(MathUtils.equals(-Float.MIN_NORMAL, Float.MIN_NORMAL, Float.MIN_NORMAL));
        assertFalse(MathUtils.equals(0f, -1f, Float.MIN_NORMAL));
        assertFalse(MathUtils.equals(Float.MIN_NORMAL, -Float.MIN_NORMAL, Float.MIN_NORMAL));
    }

    @Test
    public void testGetAny() {
        final String[] array = new String[] { "a" };
        assertEquals("a", MathUtils.<String>getAny(array));
        assertEquals("a", MathUtils.<String>getAny(Arrays.asList(array)));
        assertEquals("a", MathUtils.<String>getAny(new HashSet<>(Arrays.asList(array))));

        try {
            MathUtils.<String>getAny(new String[] {});
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) { }

        try {
            MathUtils.<String>getAny(new HashSet<>());
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) { }

        try {
            MathUtils.<String>getAny(new ArrayList<>());
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) { }
    }

    @Test
    public void testIsPrime() {
        assertTrue(MathUtils.isPrime(1));
        assertTrue(MathUtils.isPrime(2));
        assertTrue(MathUtils.isPrime(3));
        assertTrue(MathUtils.isPrime(3559));

        assertFalse(MathUtils.isPrime(4));
        assertFalse(MathUtils.isPrime(3561));

        try {
            MathUtils.isPrime(0);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) { }

        try {
            MathUtils.isPrime(-1);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) { }
    }

    @Test
    public void testNormalize() {
        final double[] doubles = { 3, 4, 5 };
        assertEquals(12.0, MathUtils.normalize(doubles), Double.MIN_NORMAL);
        assertEquals(3.0/12.0, doubles[0], Double.MIN_NORMAL);
        assertEquals(4.0/12.0, doubles[1], Double.MIN_NORMAL);
        assertEquals(5.0/12.0, doubles[2], Double.MIN_NORMAL);

        final float[] floats = { 3, 4, 5 };
        assertEquals(12f, MathUtils.normalize(floats), Float.MIN_NORMAL);
        assertEquals(3f/12f, floats[0], Float.MIN_NORMAL);
        assertEquals(4f/12f, floats[1], Float.MIN_NORMAL);
        assertEquals(5f/12f, floats[2], Float.MIN_NORMAL);

        final double[] doubleZeroes = { 0, 0, 0 };
        assertEquals(0.0, MathUtils.normalize(doubleZeroes), Double.MIN_NORMAL);
        assertEquals(1.0/3.0, doubleZeroes[0], Double.MIN_NORMAL);
        assertEquals(1.0/3.0, doubleZeroes[1], Double.MIN_NORMAL);
        assertEquals(1.0/3.0, doubleZeroes[2], Double.MIN_NORMAL);

        final float[] floatZeroes = { 0, 0, 0 };
        assertEquals(0f, MathUtils.normalize(floatZeroes), Float.MIN_NORMAL);
        assertEquals(1f/3f, floatZeroes[0], Float.MIN_NORMAL);
        assertEquals(1f/3f, floatZeroes[1], Float.MIN_NORMAL);
        assertEquals(1f/3f, floatZeroes[2], Float.MIN_NORMAL);

        try {
            MathUtils.normalize(new double[] { 3, -4, 5 });
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) { }

        try {
            MathUtils.normalize(new float[] { 3, -4, 5 });
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) { }
    }
    
    @Test
    public void testRestrict() {
        assertEquals(0.0, MathUtils.restrict(0.0, -10.0, +10.0), Double.MIN_NORMAL);
        assertEquals(-10.0, MathUtils.restrict(-20.0, -10.0, +10.0), Double.MIN_NORMAL);
        assertEquals(+10.0, MathUtils.restrict(+20.0, -10.0, +10.0), Double.MIN_NORMAL);

        assertEquals(0f, MathUtils.restrict(0f, -10f, +10f), Float.MIN_NORMAL);
        assertEquals(-10f, MathUtils.restrict(-20f, -10f, +10f), Float.MIN_NORMAL);
        assertEquals(+10f, MathUtils.restrict(+20f, -10f, +10f), Float.MIN_NORMAL);

        assertEquals((short) 0, MathUtils.restrict((short) 0, (short) -10, (short) +10));
        assertEquals((short) -10, MathUtils.restrict((short) -20, (short) -10, (short) +10));
        assertEquals((short) +10, MathUtils.restrict((short) +20, (short) -10, (short) +10));

        assertEquals(0, MathUtils.restrict(0, -10, +10));
        assertEquals(-10, MathUtils.restrict(-20, -10, +10));
        assertEquals(+10, MathUtils.restrict(+20, -10, +10));

        assertEquals(0L, MathUtils.restrict(0L, -10L, +10L));
        assertEquals(-10L, MathUtils.restrict(-20L, -10L, +10L));
        assertEquals(+10L, MathUtils.restrict(+20L, -10L, +10L));
    }

    @Test
    public void testToIntExact() {
        try {
            MathUtils.toIntExact(2.0 * Integer.MAX_VALUE);
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
        try {
            MathUtils.toIntExact(2.0 * Integer.MIN_VALUE);
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }

        try {
            MathUtils.toIntExact(2f * Integer.MAX_VALUE);
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
        try {
            MathUtils.toIntExact(2f * Integer.MIN_VALUE);
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }

    @Test
    public void testToLongExact() {
        try {
            MathUtils.toLongExact(2.0 * Long.MAX_VALUE);
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
        try {
            MathUtils.toLongExact(2.0 * Long.MIN_VALUE);
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }

        try {
            MathUtils.toLongExact(2f * Long.MAX_VALUE);
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
        try {
            MathUtils.toLongExact(2f * Long.MIN_VALUE);
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }
}
