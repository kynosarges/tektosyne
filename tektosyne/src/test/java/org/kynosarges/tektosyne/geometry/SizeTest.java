package org.kynosarges.tektosyne.geometry;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Provides unit tests for classes {@link SizeD} and {@link SizeI}.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class SizeTest {

    private final SizeD sizeD = new SizeD(1, 2);
    private final SizeI sizeI = new SizeI(1, 2);

    @Test
    public void testCtor() {
        try {
            new SizeD(-1, 2);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) { }
        try {
            new SizeD(1, -2);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) { }
        
        try {
            new SizeI(-1, 2);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) { }
        try {
            new SizeI(1, -2);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) { }
    }

    @Test
    public void testAdd() {
        assertEquals(new SizeD(4, 6), sizeD.add(new SizeD(3, 4)));
        assertEquals(new SizeI(4, 6), sizeI.add(new SizeI(3, 4)));

        try {
            sizeI.add(new SizeI(Integer.MAX_VALUE, Integer.MAX_VALUE));
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }

    @Test
    public void testEquals() {
        assertTrue(sizeD.equals(new SizeD(1, 2)));
        assertTrue(sizeI.equals(new SizeI(1, 2)));
        assertTrue(SizeD.equals(sizeD, new SizeD(1.1, 1.9), 0.2));
    }

    @Test
    public void testRestrict() {
        assertEquals(sizeD, new SizeD(0, 0).restrict(1, 2, 9, 9));
        assertEquals(sizeD, new SizeD(9, 9).restrict(0, 0, 1, 2));
        try {
            sizeD.restrict(0, 0, -1, -2);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) { }
        
        assertEquals(sizeI, new SizeI(0, 0).restrict(1, 2, 9, 9));
        assertEquals(sizeI, new SizeI(9, 9).restrict(0, 0, 1, 2));
        try {
            sizeI.restrict(0, 0, -1, -2);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) { }
    }

    @Test
    public void testRound() {
        assertEquals(sizeI, sizeD.round());
        assertEquals(sizeI, new SizeD(0.6, 1.6).round());
        assertEquals(sizeI, new SizeD(1.4, 2.4).round());

        try {
            new SizeD(1e100, 1e100).round();
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }

    @Test
    public void testSubtract() {
        assertEquals(new SizeD(2, 2), new SizeD(3, 4).subtract(sizeD));
        assertEquals(new SizeI(2, 2), new SizeI(3, 4).subtract(sizeI));

        try {
            sizeD.subtract(new SizeD(3, 4));
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) { }
        try {
            sizeI.subtract(new SizeI(3, 4));
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) { }
    }

    @Test
    public void testToSizeX() {
        assertEquals(sizeD, sizeI.toSizeD());
        assertEquals(sizeI, sizeD.toSizeI());

        assertEquals(new SizeI(0, 1), new SizeD(0.6, 1.6).toSizeI());
        assertEquals(sizeI, new SizeD(1.4, 2.4).toSizeI());

        try {
            new SizeD(1e100, 1e100).toSizeI();
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }
}
