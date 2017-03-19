package org.kynosarges.tektosyne.geometry;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Provides unit tests for classes {@link LineD} and {@link LineI}.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class LineTest {

    private final double epsilon = 0.0001;

    private final LineD diagD = new LineD(0, 0, 2, 2);
    private final LineI diagI = new LineI(0, 0, 2, 2);

    private final LineD lineD = new LineD(1, 3, 4, 5);
    private final LineI lineI = new LineI(1, 3, 4, 5);

    private final LineD mirrorLineD = new LineD(1, -3, 4, -5);
    private final LineI mirrorLineI = new LineI(1, -3, 4, -5);

    @Test
    public void testAngle() {
        final double angle = 33.69 * Angle.DEGREES_TO_RADIANS;

        assertEquals(angle, lineD.angle(), epsilon);
        assertEquals(angle, lineI.angle(), epsilon);

        assertEquals(-angle, mirrorLineD.angle(), epsilon);
        assertEquals(-angle, mirrorLineI.angle(), epsilon);
    }

    @Test
    public void testDistanceSquared() {
        assertEquals(0, diagD.distanceSquared(diagD.start), epsilon);
        assertEquals(0, diagD.distanceSquared(diagD.end), epsilon);
        assertEquals(0, diagD.distanceSquared(new PointD(1, 1)), epsilon);

        assertEquals(2, diagD.distanceSquared(new PointD(-1, -1)), epsilon);
        assertEquals(2, diagD.distanceSquared(new PointD(0, 2)), epsilon);
        assertEquals(2, diagD.distanceSquared(new PointD(2, 0)), epsilon);
        assertEquals(2, diagD.distanceSquared(new PointD(3, 3)), epsilon);

        assertEquals(0, diagI.distanceSquared(diagI.start), epsilon);
        assertEquals(0, diagI.distanceSquared(diagI.end), epsilon);
        assertEquals(0, diagI.distanceSquared(new PointI(1, 1)), epsilon);

        assertEquals(2, diagI.distanceSquared(new PointI(-1, -1)), epsilon);
        assertEquals(2, diagI.distanceSquared(new PointI(0, 2)), epsilon);
        assertEquals(2, diagI.distanceSquared(new PointI(2, 0)), epsilon);
        assertEquals(2, diagI.distanceSquared(new PointI(3, 3)), epsilon);
    }

    @Test
    public void testEqualsEpsilon() {
        assertTrue(LineD.equals(lineD, new LineD(1.1, 2.9, 3.9, 5.1), 0.2));
    }

    @Test
    public void testFindX() {
        assertEquals(-0.5, lineD.findX(2), epsilon);
        assertEquals(1, lineD.findX(3), epsilon);
        assertEquals(2.5, lineD.findX(4), epsilon);
        assertEquals(4, lineD.findX(5), epsilon);
        assertEquals(5.5, lineD.findX(6), epsilon);

        assertEquals(-0.5, lineI.findX(2), epsilon);
        assertEquals(1, lineI.findX(3), epsilon);
        assertEquals(2.5, lineI.findX(4), epsilon);
        assertEquals(4, lineI.findX(5), epsilon);
        assertEquals(5.5, lineI.findX(6), epsilon);
    }

    @Test
    public void testFindY() {
        assertEquals(2, lineD.findY(-0.5), epsilon);
        assertEquals(3, lineD.findY(1), epsilon);
        assertEquals(4, lineD.findY(2.5), epsilon);
        assertEquals(5, lineD.findY(4), epsilon);
        assertEquals(6, lineD.findY(5.5), epsilon);

        assertEquals(2, lineI.findY(-0.5), epsilon);
        assertEquals(3, lineI.findY(1), epsilon);
        assertEquals(4, lineI.findY(2.5), epsilon);
        assertEquals(5, lineI.findY(4), epsilon);
        assertEquals(6, lineI.findY(5.5), epsilon);
    }

    @Test
    public void testIntersect() {
        assertEquals(diagD.start, diagD.intersect(diagD.start));
        assertEquals(diagD.end, diagD.intersect(diagD.end));

        assertEquals(diagI.start.toPointD(), diagI.intersect(diagI.start));
        assertEquals(diagI.end.toPointD(), diagI.intersect(diagI.end));

        for (int d = -2; d <= 4; d++) {
            assertEquals(new PointD(d, d), diagD.intersect(new PointD(d, d)));
            assertEquals(new PointD(d, d), diagD.intersect(new PointD(d-1, d+1)));
            assertEquals(new PointD(d, d), diagD.intersect(new PointD(d+1, d-1)));

            assertEquals(new PointD(d, d), diagI.intersect(new PointI(d, d)));
            assertEquals(new PointD(d, d), diagI.intersect(new PointI(d - 1, d + 1)));
            assertEquals(new PointD(d, d), diagI.intersect(new PointI(d + 1, d - 1)));
        }
    }

    @Test
    public void testInverseSlope() {
        assertEquals(1.5, lineD.inverseSlope(), epsilon);
        assertEquals(-1.5, mirrorLineD.inverseSlope(), epsilon);

        assertEquals(1.5, lineI.inverseSlope(), epsilon);
        assertEquals(-1.5, mirrorLineI.inverseSlope(), epsilon);
    }

    @Test
    public void testLength() {
        assertEquals(Math.sqrt(13), lineD.length(), epsilon);
        assertEquals(Math.sqrt(13), lineI.length(), epsilon);

        assertEquals(Math.sqrt(13), mirrorLineD.length(), epsilon);
        assertEquals(Math.sqrt(13), mirrorLineI.length(), epsilon);
    }

    @Test
    public void testLengthSquared() {
        assertEquals(13, lineD.lengthSquared(), epsilon);
        assertEquals(13, lineI.lengthSquared());

        assertEquals(13, mirrorLineD.lengthSquared(), epsilon);
        assertEquals(13, mirrorLineI.lengthSquared());
    }

    @Test
    public void testLocate() {
        assertEquals(LineLocation.START, diagD.locate(new PointD(0, 0)));
        assertEquals(LineLocation.END, diagD.locate(new PointD(2, 2)));
        assertEquals(LineLocation.BEFORE, diagD.locate(new PointD(-1, -1)));
        assertEquals(LineLocation.BETWEEN, diagD.locate(new PointD(1, 1)));
        assertEquals(LineLocation.AFTER, diagD.locate(new PointD(3, 3)));
        assertEquals(LineLocation.LEFT, diagD.locate(new PointD(0, 1)));
        assertEquals(LineLocation.RIGHT, diagD.locate(new PointD(1, 0)));

        assertEquals(LineLocation.START, diagI.locate(new PointI(0, 0)));
        assertEquals(LineLocation.END, diagI.locate(new PointI(2, 2)));
        assertEquals(LineLocation.BEFORE, diagI.locate(new PointI(-1, -1)));
        assertEquals(LineLocation.BETWEEN, diagI.locate(new PointI(1, 1)));
        assertEquals(LineLocation.AFTER, diagI.locate(new PointI(3, 3)));
        assertEquals(LineLocation.LEFT, diagI.locate(new PointI(0, 1)));
        assertEquals(LineLocation.RIGHT, diagI.locate(new PointI(1, 0)));
    }

    @Test
    public void testLocateReverse() {
        final LineD reverseDiagD = diagD.reverse();
        assertEquals(LineLocation.END, reverseDiagD.locate(new PointD(0, 0)));
        assertEquals(LineLocation.START, reverseDiagD.locate(new PointD(2, 2)));
        assertEquals(LineLocation.AFTER, reverseDiagD.locate(new PointD(-1, -1)));
        assertEquals(LineLocation.BETWEEN, reverseDiagD.locate(new PointD(1, 1)));
        assertEquals(LineLocation.BEFORE, reverseDiagD.locate(new PointD(3, 3)));
        assertEquals(LineLocation.RIGHT, reverseDiagD.locate(new PointD(0, 1)));
        assertEquals(LineLocation.LEFT, reverseDiagD.locate(new PointD(1, 0)));

        final LineI reverseDiagI = diagI.reverse();
        assertEquals(LineLocation.END, reverseDiagI.locate(new PointI(0, 0)));
        assertEquals(LineLocation.START, reverseDiagI.locate(new PointI(2, 2)));
        assertEquals(LineLocation.AFTER, reverseDiagI.locate(new PointI(-1, -1)));
        assertEquals(LineLocation.BETWEEN, reverseDiagI.locate(new PointI(1, 1)));
        assertEquals(LineLocation.BEFORE, reverseDiagI.locate(new PointI(3, 3)));
        assertEquals(LineLocation.RIGHT, reverseDiagI.locate(new PointI(0, 1)));
        assertEquals(LineLocation.LEFT, reverseDiagI.locate(new PointI(1, 0)));
    }

    @Test
    public void testLocateEpsilon() {
        assertEquals(LineLocation.BEFORE, diagD.locate(new PointD(-0.1, -0.1), 0.01));
        assertEquals(LineLocation.START, diagD.locate(new PointD(-0.1, -0.1), 0.5));
        assertEquals(LineLocation.AFTER, diagD.locate(new PointD(2.1, 2.1), 0.01));
        assertEquals(LineLocation.END, diagD.locate(new PointD(2.1, 2.1), 0.5));

        assertEquals(LineLocation.LEFT, diagD.locate(new PointD(0.9, 1.1), 0.01));
        assertEquals(LineLocation.BETWEEN, diagD.locate(new PointD(0.9, 1.1), 0.5));
        assertEquals(LineLocation.RIGHT, diagD.locate(new PointD(1.1, 0.9), 0.01));
        assertEquals(LineLocation.BETWEEN, diagD.locate(new PointD(1.1, 0.9), 0.5));
    }

    @Test
    public void testLocateCollinear() {
        assertEquals(LineLocation.START, diagD.locateCollinear(new PointD(0, 0)));
        assertEquals(LineLocation.END, diagD.locateCollinear(new PointD(2, 2)));
        assertEquals(LineLocation.BEFORE, diagD.locateCollinear(new PointD(-1, -1)));
        assertEquals(LineLocation.BETWEEN, diagD.locateCollinear(new PointD(1, 1)));
        assertEquals(LineLocation.AFTER, diagD.locateCollinear(new PointD(3, 3)));
        assertEquals(LineLocation.BETWEEN, diagD.locateCollinear(new PointD(0, 1)));
        assertEquals(LineLocation.BETWEEN, diagD.locateCollinear(new PointD(1, 0)));

        assertEquals(LineLocation.START, diagI.locateCollinear(new PointI(0, 0)));
        assertEquals(LineLocation.END, diagI.locateCollinear(new PointI(2, 2)));
        assertEquals(LineLocation.BEFORE, diagI.locateCollinear(new PointI(-1, -1)));
        assertEquals(LineLocation.BETWEEN, diagI.locateCollinear(new PointI(1, 1)));
        assertEquals(LineLocation.AFTER, diagI.locateCollinear(new PointI(3, 3)));
        assertEquals(LineLocation.BETWEEN, diagI.locateCollinear(new PointI(0, 1)));
        assertEquals(LineLocation.BETWEEN, diagI.locateCollinear(new PointI(1, 0)));
    }

    @Test
    public void testLocateCollinearReverse() {
        final LineD reverseDiagD = diagD.reverse();
        assertEquals(LineLocation.END, reverseDiagD.locateCollinear(new PointD(0, 0)));
        assertEquals(LineLocation.START, reverseDiagD.locateCollinear(new PointD(2, 2)));
        assertEquals(LineLocation.AFTER, reverseDiagD.locateCollinear(new PointD(-1, -1)));
        assertEquals(LineLocation.BETWEEN, reverseDiagD.locateCollinear(new PointD(1, 1)));
        assertEquals(LineLocation.BEFORE, reverseDiagD.locateCollinear(new PointD(3, 3)));
        assertEquals(LineLocation.BETWEEN, reverseDiagD.locateCollinear(new PointD(0, 1)));
        assertEquals(LineLocation.BETWEEN, reverseDiagD.locateCollinear(new PointD(1, 0)));

        final LineI reverseDiagI = diagI.reverse();
        assertEquals(LineLocation.END, reverseDiagI.locateCollinear(new PointI(0, 0)));
        assertEquals(LineLocation.START, reverseDiagI.locateCollinear(new PointI(2, 2)));
        assertEquals(LineLocation.AFTER, reverseDiagI.locateCollinear(new PointI(-1, -1)));
        assertEquals(LineLocation.BETWEEN, reverseDiagI.locateCollinear(new PointI(1, 1)));
        assertEquals(LineLocation.BEFORE, reverseDiagI.locateCollinear(new PointI(3, 3)));
        assertEquals(LineLocation.BETWEEN, reverseDiagI.locateCollinear(new PointI(0, 1)));
        assertEquals(LineLocation.BETWEEN, reverseDiagI.locateCollinear(new PointI(1, 0)));
    }

    @Test
    public void testLocateCollinearEpsilon() {
        assertEquals(LineLocation.BEFORE, diagD.locateCollinear(new PointD(-0.1, -0.1), 0.01));
        assertEquals(LineLocation.START, diagD.locateCollinear(new PointD(-0.1, -0.1), 0.5));
        assertEquals(LineLocation.AFTER, diagD.locateCollinear(new PointD(2.1, 2.1), 0.01));
        assertEquals(LineLocation.END, diagD.locateCollinear(new PointD(2.1, 2.1), 0.5));
        assertEquals(LineLocation.BETWEEN, diagD.locateCollinear(new PointD(0.9, 1.1), 0.01));
        assertEquals(LineLocation.BETWEEN, diagD.locateCollinear(new PointD(1.1, 0.9), 0.01));
    }

    @Test
    public void testReverse() {
        assertEquals(new LineD(4, 5, 1, 3), lineD.reverse());
        assertEquals(new LineI(4, 5, 1, 3), lineI.reverse());
    }

    @Test
    public void testRound() {
        assertEquals(lineI, lineD.round());
        assertEquals(lineI, new LineD(0.6, 2.6, 3.6, 4.6).round());
        assertEquals(lineI, new LineD(1.4, 3.4, 4.4, 5.4).round());

        try {
            new LineD(0, 1e100, 1e100, 0).round();
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }

    @Test
    public void testSlope() {
        assertEquals(2.0 / 3.0, lineD.slope(), epsilon);
        assertEquals(-2.0 / 3.0, mirrorLineD.slope(), epsilon);

        assertEquals(2.0 / 3.0, lineI.slope(), epsilon);
        assertEquals(-2.0 / 3.0, mirrorLineI.slope(), epsilon);
    }

    @Test
    public void testToLineX() {
        assertEquals(lineD, lineI.toLineD());
        assertEquals(lineI, lineD.toLineI());

        assertEquals(new LineI(0, 1, 3, 4), new LineD(0.6, 1.6, 3.6, 4.6).toLineI());
        assertEquals(lineI, new LineD(1.4, 3.4, 4.4, 5.4).toLineI());

        try {
            new LineD(0, 1e100, 1e100, 0).toLineI();
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }

    @Test
    public void testVector() {
        assertEquals(new PointD(3, 2), lineD.vector());
        assertEquals(new PointI(3, 2), lineI.vector());

        assertEquals(new PointD(3, -2), mirrorLineD.vector());
        assertEquals(new PointI(3, -2), mirrorLineI.vector());

        assertEquals(lineD.angle(), lineD.vector().angle(), epsilon);
        assertEquals(lineI.angle(), lineI.vector().angle(), epsilon);

        assertEquals(lineD.length(), lineD.vector().length(), epsilon);
        assertEquals(lineI.length(), lineI.vector().length(), epsilon);

        try {
            new LineI(-2, -2, Integer.MAX_VALUE, Integer.MAX_VALUE).vector();
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }
}
