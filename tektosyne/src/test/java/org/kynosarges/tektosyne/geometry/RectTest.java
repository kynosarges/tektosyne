package org.kynosarges.tektosyne.geometry;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Provides unit tests for classes {@link RectD} and {@link RectI}.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class RectTest {

    private final RectD rectD = new RectD(1, 2, 5, 7);
    private final RectI rectI = new RectI(1, 2, 5, 7);

    private final PointD rectDminXmaxY = new PointD(rectD.min.x, rectD.max.y);
    private final PointD rectDmaxXminY = new PointD(rectD.max.x, rectD.min.y);

    private final PointI rectIminXmaxY = new PointI(rectI.min.x, rectI.max.y);
    private final PointI rectImaxXminY = new PointI(rectI.max.x, rectI.min.y);
    
    @Test
    public void testConstructor() {
        assertEquals(rectD, new RectD(rectD.min, rectD.max));
        assertEquals(rectI, new RectI(rectI.min, rectI.max));

        try {
            new RectD(1, 2, 0, 1);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) { }
        try {
            new RectI(1, 2, 0, 1);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) { }
    }

    @Test
    public void testCircumscribe() {
        assertEquals(rectI, rectD.circumscribe());
        assertEquals(rectI, new RectD(1.6, 2.4, 4.8, 6.5).circumscribe());
        assertEquals(new RectI(-3, -4, 20, 30), new RectD(-2.1, -3.2, 19.8, 29.1).circumscribe());
        
        try {
            new RectD(0, 0, 1.0 + Integer.MAX_VALUE, 1.0 + Integer.MAX_VALUE).circumscribe();
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }

    @Test
    public void testCircumscribePoints() {
        assertEquals(rectD, RectD.circumscribe(rectD.min, rectD.max));
        assertEquals(rectD, RectD.circumscribe(rectDminXmaxY, rectDmaxXminY));
        assertEquals(new RectD(-4, -3, -2, -1), RectD.circumscribe(new PointD(-4, -3), new PointD(-2, -1)));

        assertEquals(rectI, RectI.circumscribe(rectI.min, rectI.max));
        assertEquals(rectI, RectI.circumscribe(rectIminXmaxY, rectImaxXminY));
        assertEquals(new RectI(-4, -3, -2, -1), RectI.circumscribe(new PointI(-4, -3), new PointI(-2, -1)));
    }

    @Test
    public void testContainsPoint() {
        assertTrue(rectD.contains(rectD.min));
        assertTrue(rectD.contains(rectD.max));
        assertTrue(rectD.contains(rectDminXmaxY));
        assertTrue(rectD.contains(rectDmaxXminY));
        assertTrue(rectD.contains(3, 4));
        assertFalse(rectD.contains(0, 1));

        assertTrue(rectI.contains(rectI.min));
        assertTrue(rectI.contains(rectI.max));
        assertTrue(rectI.contains(rectIminXmaxY));
        assertTrue(rectI.contains(rectImaxXminY));
        assertTrue(rectI.contains(3, 4));
        assertFalse(rectI.contains(0, 1));
    }

    @Test
    public void testContainsPointOpen() {
        assertTrue(rectD.containsOpen(rectD.min));
        assertFalse(rectD.containsOpen(rectDminXmaxY));
        assertFalse(rectD.containsOpen(rectDmaxXminY));
        assertFalse(rectD.containsOpen(rectD.max));
        assertTrue(rectD.containsOpen(3, 4));
        assertFalse(rectD.containsOpen(0, 1));

        assertTrue(rectI.containsOpen(rectI.min));
        assertFalse(rectI.containsOpen(rectIminXmaxY));
        assertFalse(rectI.containsOpen(rectImaxXminY));
        assertFalse(rectI.containsOpen(rectI.max));
        assertTrue(rectI.containsOpen(3, 4));
        assertFalse(rectI.containsOpen(0, 1));
    }
    
    @Test
    public void testContainsRect() {
        assertTrue(rectD.contains(rectD));
        assertTrue(rectD.contains(new RectD(2, 3, 4, 6)));
        assertFalse(rectD.contains(new RectD(0, 1, 4, 6)));
        assertFalse(rectD.contains(new RectD(10, 2, 14, 7)));

        assertTrue(rectI.contains(rectI));
        assertTrue(rectI.contains(new RectI(2, 3, 4, 6)));
        assertFalse(rectI.contains(new RectI(0, 1, 4, 6)));
        assertFalse(rectI.contains(new RectI(10, 2, 14, 7)));
    }
    
    @Test
    public void testDimensions() {
        assertEquals(5, rectD.height(), 0);
        assertEquals(4, rectD.width(), 0);

        assertEquals(5L, rectI.height());
        assertEquals(4L, rectI.width());
    }

    @Test
    public void testDistanceVector() {
        assertEquals(new PointD(-1, -2), rectD.distanceVector(PointD.EMPTY));
        assertEquals(PointD.EMPTY, rectD.distanceVector(rectD.min));
        assertEquals(PointD.EMPTY, rectD.distanceVector(new PointD(3, 6)));
        assertEquals(PointD.EMPTY, rectD.distanceVector(rectD.max));
        assertEquals(new PointD(+1, +2), rectD.distanceVector(new PointD(6, 9)));

        assertEquals(new PointI(-1, -2), rectI.distanceVector(PointI.EMPTY));
        assertEquals(PointI.EMPTY, rectI.distanceVector(rectI.min));
        assertEquals(PointI.EMPTY, rectI.distanceVector(new PointI(3, 6)));
        assertEquals(PointI.EMPTY, rectI.distanceVector(rectI.max));
        assertEquals(new PointI(+1, +2), rectI.distanceVector(new PointI(6, 9)));
        
        try {
            new RectI(Integer.MIN_VALUE, Integer.MIN_VALUE,
                    Integer.MIN_VALUE + 1, Integer.MIN_VALUE + 1).distanceVector(
                            new PointI(Integer.MAX_VALUE, Integer.MAX_VALUE));
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }

    @Test
    public void testEqualsEpsilon() {
        assertTrue(RectD.equals(rectD, new RectD(1.1, 1.9, 4.9, 7.1), 0.2));
        assertFalse(RectD.equals(rectD, new RectD(1.1, 1.9, 4.9, 7.1), 0.01));
    }

    @Test
    public void testIntersect() {
        assertEquals(rectD, rectD.intersect(rectD));
        assertEquals(new RectD(2, 3, 4, 6), rectD.intersect(new RectD(2, 3, 4, 6)));
        assertEquals(new RectD(1, 2, 4, 6), rectD.intersect(new RectD(0, 1, 4, 6)));
        assertNull(rectD.intersect(new RectD(10, 3, 14, 8)));

        assertEquals(rectI, rectI.intersect(rectI));
        assertEquals(new RectI(2, 3, 4, 6), rectI.intersect(new RectI(2, 3, 4, 6)));
        assertEquals(new RectI(1, 2, 4, 6), rectI.intersect(new RectI(0, 1, 4, 6)));
        assertNull(rectI.intersect(new RectI(10, 3, 14, 8)));
    }

    @Test
    public void testIntersectLine() {
        assertEquals(new LineD(1, 2, 5, 7), rectD.intersect(new LineD(1, 2, 5, 7)));
        assertEquals(new LineD(2, 6, 4, 3), rectD.intersect(new LineD(2, 6, 4, 3)));
        assertEquals(new LineD(3, 2, 3, 7), rectD.intersect(new LineD(3, 1, 3, 8)));
        assertEquals(new LineD(1, 4, 5, 4), rectD.intersect(new LineD(0, 4, 6, 4)));

        assertNull(rectD.intersect(new LineD(0, 1, 0, 8)));
        assertNull(rectD.intersect(new LineD(0, 1, 6, 1)));
        assertNull(rectD.intersect(new LineD(6, 1, 6, 8)));
        assertNull(rectD.intersect(new LineD(0, 8, 6, 8)));
        assertNull(rectD.intersect(new LineD(-2, 3, 2, -3)));
    }

    @Test
    public void testIntersectPolygon() {
        PointD[] polyD = { new PointD(3, 3) };
        assertArrayEquals(polyD, rectD.intersect(polyD));

        polyD = new PointD[] { rectDminXmaxY, rectD.max, rectDmaxXminY, rectD.min };
        assertArrayEquals(polyD, rectD.intersect(polyD));

        polyD = new PointD[] { rectDminXmaxY, rectDmaxXminY, rectD.min };
        assertArrayEquals(polyD, rectD.intersect(polyD));

        polyD = new PointD[] { new PointD(0, 1), new PointD(6, 1), new PointD(6, 8), new PointD(0, 8) };
        assertArrayEquals(new PointD[] {
            rectDminXmaxY, rectD.min, rectDmaxXminY, rectD.max
        }, rectD.intersect(polyD));

        polyD = new PointD[] { new PointD(2, 0), new PointD(4, 0), new PointD(4, 8), new PointD(2, 8) };
        assertArrayEquals(new PointD[] {
            new PointD(2, 7), new PointD(2, 2), new PointD(4, 2), new PointD(4, 7)
        }, rectD.intersect(polyD));

        polyD = new PointD[] { new PointD(0, 3), new PointD(0, 6), new PointD(6, 6), new PointD(6, 3) };
        assertArrayEquals(new PointD[] {
            new PointD(5, 3), new PointD(1, 3), new PointD(1, 6), new PointD(5, 6)
        }, rectD.intersect(polyD));

        polyD = new PointD[] { new PointD(6, 3), new PointD(6, 6), new PointD(8, 5) };
        assertNull(rectD.intersect(polyD));
    }

    @Test
    public void testIntersectsWith() {
        assertTrue(rectD.intersectsWith(rectD));
        assertTrue(rectD.intersectsWith(new RectD(2, 3, 4, 6)));
        assertTrue(rectD.intersectsWith(new RectD(0, 1, 4, 6)));
        assertFalse(rectD.intersectsWith(new RectD(10, 3, 14, 8)));

        assertTrue(rectI.intersectsWith(rectI));
        assertTrue(rectI.intersectsWith(new RectI(2, 3, 4, 6)));
        assertTrue(rectI.intersectsWith(new RectI(0, 1, 4, 6)));
        assertFalse(rectI.intersectsWith(new RectI(10, 3, 14, 8)));
    }

    @Test
    public void testIntersectsWithLine() {
        assertTrue(rectD.intersectsWith(new LineD(1, 2, 5, 7)));
        assertTrue(rectD.intersectsWith(new LineD(3, 1, 3, 8)));
        assertTrue(rectD.intersectsWith(new LineD(0, 4, 6, 4)));
        assertFalse(rectD.intersectsWith(new LineD(0, 1, 0, 8)));
        assertFalse(rectD.intersectsWith(new LineD(0, 1, 6, 1)));
        assertFalse(rectD.intersectsWith(new LineD(-2, 3, 2, -3)));
    }

    @Test
    public void testLocate() {
        assertEquals(new RectLocation(LineLocation.BEFORE, LineLocation.BEFORE), rectD.locate(new PointD(0, 1)));
        assertEquals(new RectLocation(LineLocation.START, LineLocation.START), rectD.locate(new PointD(1, 2)));
        assertEquals(new RectLocation(LineLocation.BETWEEN, LineLocation.BETWEEN), rectD.locate(new PointD(3, 4)));
        assertEquals(new RectLocation(LineLocation.END, LineLocation.END), rectD.locate(new PointD(5, 7)));
        assertEquals(new RectLocation(LineLocation.AFTER, LineLocation.AFTER), rectD.locate(new PointD(6, 8)));

        assertEquals(new RectLocation(LineLocation.BEFORE, LineLocation.BEFORE), rectI.locate(new PointI(0, 1)));
        assertEquals(new RectLocation(LineLocation.START, LineLocation.START), rectI.locate(new PointI(1, 2)));
        assertEquals(new RectLocation(LineLocation.BETWEEN, LineLocation.BETWEEN), rectI.locate(new PointI(3, 4)));
        assertEquals(new RectLocation(LineLocation.END, LineLocation.END), rectI.locate(new PointI(5, 7)));
        assertEquals(new RectLocation(LineLocation.AFTER, LineLocation.AFTER), rectI.locate(new PointI(6, 8)));
    }

    @Test
    public void testLocateEpsilon() {
        assertEquals(new RectLocation(LineLocation.BEFORE, LineLocation.BEFORE), rectD.locate(new PointD(0.1, 0.9), 0.2));
        assertEquals(new RectLocation(LineLocation.START, LineLocation.START), rectD.locate(new PointD(0.9, 2.1), 0.2));
        assertEquals(new RectLocation(LineLocation.BETWEEN, LineLocation.BETWEEN), rectD.locate(new PointD(3.1, 3.9), 0.2));
        assertEquals(new RectLocation(LineLocation.END, LineLocation.END), rectD.locate(new PointD(4.9, 7.1), 0.2));
        assertEquals(new RectLocation(LineLocation.AFTER, LineLocation.AFTER), rectD.locate(new PointD(5.9, 8.1), 0.2));
    }

    @Test
    public void testOffset() {
        assertEquals(new RectD(4, 6, 8, 11), rectD.offset(3, 4));
        assertEquals(new RectD(4, 6, 8, 11), rectD.offset(new PointD(3, 4)));

        assertEquals(new RectI(4, 6, 8, 11), rectI.offset(3, 4));
        assertEquals(new RectI(4, 6, 8, 11), rectI.offset(new PointI(3, 4)));
        
        try {
            new RectI(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE).offset(1, 1);
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }

    @Test
    public void testRound() {
        assertEquals(rectI, rectD.round());
        assertEquals(rectI, new RectD(0.6, 1.6, 5.2, 7.2).round());
        assertEquals(rectI, new RectD(1.4, 2.4, 4.8, 6.8).round());
        
        try {
            new RectD(0, 0, 1.0 + Integer.MAX_VALUE, 1.0 + Integer.MAX_VALUE).round();
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }

    @Test
    public void testToRectX() {
        assertEquals(rectD, rectI.toRectD());
        assertEquals(rectI, rectD.toRectI());
        assertEquals(new RectI(0, 1, 3, 4), new RectD(0.6, 1.6, 3.6, 4.6).toRectI());
        
        try {
            new RectD(0, 0, 1.0 + Integer.MAX_VALUE, 1.0 + Integer.MAX_VALUE).toRectI();
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }

    @Test
    public void testUnion() {
        assertEquals(rectD, rectD.union(rectD));
        assertEquals(rectD, rectD.union(new RectD(2, 3, 4, 6)));
        assertEquals(new RectD(0, 1, 5, 7), rectD.union(new RectD(0, 1, 4, 6)));
        assertEquals(new RectD(1, 2, 14, 8), rectD.union(new RectD(10, 3, 14, 8)));

        assertEquals(rectI, rectI.union(rectI));
        assertEquals(rectI, rectI.union(new RectI(2, 3, 4, 6)));
        assertEquals(new RectI(0, 1, 5, 7), rectI.union(new RectI(0, 1, 4, 6)));
        assertEquals(new RectI(1, 2, 14, 8), rectI.union(new RectI(10, 3, 14, 8)));
    }
}
