package org.kynosarges.tektosyne.geometry;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Provides unit tests for classes {@link PointD} and {@link PointI}.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class PointTest {

    private final double epsilon = 0.0001;
    private final double angle = 63.435; // polar angle of (1,2)

    private final PointD pointD = new PointD(1, 2);
    private final PointI pointI = new PointI(1, 2);

    @Test
    public void testAdd() {
        assertEquals(new PointD(4, 6), pointD.add(new PointD(3, 4)));
        assertEquals(new PointI(4, 6), pointI.add(new PointI(3, 4)));

        try {
            pointI.add(new PointI(Integer.MAX_VALUE, Integer.MAX_VALUE));
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }

    @Test
    public void testAngle() {
        assertEquals(angle, pointD.angle() * Angle.RADIANS_TO_DEGREES, epsilon);
        assertEquals(angle, pointI.angle() * Angle.RADIANS_TO_DEGREES, epsilon);
    }

    @Test
    public void testAngleBetween() {

        assertEquals(0, pointD.angleBetween(pointD), epsilon);
        assertEquals(0, PointD.EMPTY.angleBetween(pointD, pointD), epsilon);
        assertEquals(Math.PI, PointD.EMPTY.angleBetween(new PointD(1, 0), new PointD(-2, 0)), epsilon);
        assertEquals(-angle, pointD.angleBetween(new PointD(2, 0)) * Angle.RADIANS_TO_DEGREES, epsilon);
        assertEquals(90 - angle, pointD.angleBetween(new PointD(0, 2)) * Angle.RADIANS_TO_DEGREES, epsilon);

        assertEquals(0, pointI.angleBetween(pointI), epsilon);
        assertEquals(0, PointI.EMPTY.angleBetween(pointI, pointI), epsilon);
        assertEquals(Math.PI, PointI.EMPTY.angleBetween(new PointI(1, 0), new PointI(-2, 0)), epsilon);
        assertEquals(-angle, pointI.angleBetween(new PointI(2, 0)) * Angle.RADIANS_TO_DEGREES, epsilon);
        assertEquals(90 - angle, pointI.angleBetween(new PointI(0, 2)) * Angle.RADIANS_TO_DEGREES, epsilon);
    }

    @Test
    public void testCrossProductLength() {
        assertEquals(0, pointD.crossProductLength(pointD), epsilon);
        assertEquals(0, pointD.crossProductLength(new PointD(-1, -2)), epsilon);
        assertEquals(-4, pointD.crossProductLength(new PointD(2, 0)), epsilon);
        assertEquals(2, pointD.crossProductLength(new PointD(0, 2)), epsilon);
        assertEquals(-4, pointD.crossProductLength(new PointD(2, 4), new PointD(1, -2)), epsilon);

        assertEquals(0L, pointI.crossProductLength(pointI));
        assertEquals(0L, pointI.crossProductLength(new PointI(-1, -2)));
        assertEquals(-4L, pointI.crossProductLength(new PointI(2, 0)));
        assertEquals(2L, pointI.crossProductLength(new PointI(0, 2)));
        assertEquals(-4L, pointI.crossProductLength(new PointI(2, 4), new PointI(1, -2)));
    }

    @Test
    public void testEquals() {
        assertTrue(pointD.equals(new PointD(1, 2)));
        assertTrue(pointI.equals(new PointI(1, 2)));
        assertTrue(PointD.equals(pointD, new PointD(1.1, 1.9), 0.2));
    }

    @Test
    public void testFromPolar() {
        assertTrue(PointD.equals(pointD, PointD.fromPolar(pointD.length(), pointD.angle()), epsilon));
        assertEquals(pointI, PointI.fromPolar(pointI.length(), pointI.angle()));

        assertTrue(PointD.equals(new PointD(-1, -2),
            PointD.fromPolar(-pointD.length(), pointD.angle()), epsilon));
        assertEquals(new PointI(-1, -2), PointI.fromPolar(-pointI.length(), pointI.angle()));

        try {
            PointI.fromPolar(1e100, angle);
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }

    @Test
    public void testIsCollinear() {
        final PointD ad = new PointD(2, 2);
        final PointI ai = new PointI(2, 2);

        assertTrue(PointD.EMPTY.isCollinear(ad, PointD.EMPTY));
        assertTrue(PointD.EMPTY.isCollinear(ad, ad));
        assertTrue(PointD.EMPTY.isCollinear(ad, new PointD(1, 1)));
        assertFalse(PointD.EMPTY.isCollinear(ad, new PointD(0, 1)));
        assertFalse(PointD.EMPTY.isCollinear(ad, new PointD(1, 0)));

        assertTrue(PointI.EMPTY.isCollinear(ai, PointI.EMPTY));
        assertTrue(PointI.EMPTY.isCollinear(ai, ai));
        assertTrue(PointI.EMPTY.isCollinear(ai, new PointI(1, 1)));
        assertFalse(PointI.EMPTY.isCollinear(ai, new PointI(0, 1)));
        assertFalse(PointI.EMPTY.isCollinear(ai, new PointI(1, 0)));
    }

    @Test
    public void testIsCollinearEpsilon() {
        final PointD ad = new PointD(2, 2);

        assertFalse(PointD.EMPTY.isCollinear(ad, new PointD(0.9, 1.1), 0.01));
        assertFalse(PointD.EMPTY.isCollinear(ad, new PointD(2.9, 3.1), 0.01));
        assertTrue(PointD.EMPTY.isCollinear(ad, new PointD(0.9, 1.1), 0.5));
        assertTrue(PointD.EMPTY.isCollinear(ad, new PointD(2.9, 3.1), 0.5));
    }

    @Test
    public void testLength() {
        assertEquals(Math.sqrt(5), pointD.length(), epsilon);
        assertEquals(Math.sqrt(5), pointI.length(), epsilon);
    }

    @Test
    public void testLengthSquared() {
        assertEquals(5.0, pointD.lengthSquared(), epsilon);
        assertEquals(5L, pointI.lengthSquared());
    }

    @Test
    public void testMove() {
        final double sqrt2 = Math.sqrt(2);

        assertEquals(pointD, pointD.move(pointD, 5));
        assertEquals(pointD, pointD.move(new PointD(3, 4), 0));
        assertEquals(new PointD(2, 2), pointD.move(new PointD(3, 2), 1));
        assertEquals(new PointD(0, 2), pointD.move(new PointD(-1, 2), 1));
        assertEquals(new PointD(1, 3), pointD.move(new PointD(1, 0), -1));
        assertEquals(new PointD(1, 1), pointD.move(new PointD(1, 4), -1));
        assertEquals(new PointD(2, 3), pointD.move(new PointD(3, 4), sqrt2));
        assertEquals(new PointD(0, 1), pointD.move(new PointD(3, 4), -sqrt2));
    }

    @Test
    public void testMultiply() {
        assertEquals(11, pointD.multiply(new PointD(3, 4)), epsilon);
        assertEquals(pointD.lengthSquared(), pointD.multiply(pointD), epsilon);

        assertEquals(11L, pointI.multiply(new PointI(3, 4)));
        assertEquals(pointI.lengthSquared(), pointI.multiply(pointI));
    }

    @Test
    public void testNormalize() {
        assertTrue(PointD.equals(new PointD(-1, 0), new PointD(-1, 0).normalize(), epsilon));
        assertTrue(PointD.equals(new PointD(0, -1), new PointD(0, -0.5).normalize(), epsilon));
        assertTrue(PointD.equals(new PointD(0.447213, 0.894428), pointD.normalize(), epsilon));
    }

    @Test
    public void testRestrict() {
        assertEquals(pointD, new PointD(0, 0).restrict(1, 2, 9, 9));
        assertEquals(pointD, new PointD(9, 9).restrict(0, 0, 1, 2));

        assertEquals(pointI, new PointI(0, 0).restrict(1, 2, 9, 9));
        assertEquals(pointI, new PointI(9, 9).restrict(0, 0, 1, 2));
    }

    @Test
    public void testRound() {
        assertEquals(pointI, pointD.round());
        assertEquals(pointI, new PointD(0.6, 1.6).round());
        assertEquals(pointI, new PointD(1.4, 2.4).round());

        try {
            new PointD(1e100, 1e100).round();
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }

    @Test
    public void testSubtract() {
        assertEquals(new PointD(-2, -2), pointD.subtract(new PointD(3, 4)));
        assertEquals(new PointI(-2, -2), pointI.subtract(new PointI(3, 4)));

        try {
            new PointI(-2, -2).subtract(new PointI(Integer.MAX_VALUE, Integer.MAX_VALUE));
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }

    @Test
    public void testToPointX() {
        assertEquals(pointD, pointI.toPointD());
        assertEquals(pointI, pointD.toPointI());

        assertEquals(new PointI(0, 1), new PointD(0.6, 1.6).toPointI());
        assertEquals(pointI, new PointD(1.4, 2.4).toPointI());

        try {
            new PointD(1e100, 1e100).toPointI();
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }
}
