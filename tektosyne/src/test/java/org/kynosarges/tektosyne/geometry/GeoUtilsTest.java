package org.kynosarges.tektosyne.geometry;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Provides unit tests for class {@link GeoUtils}.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class GeoUtilsTest {

    @Test
    public void testConnectPoints() {
        final PointD p0 = new PointD(0, 0), p1 = new PointD(1, 1), p2 = new PointD(2, 0);

        assertArrayEquals(new LineD[0], GeoUtils.connectPoints(false));
        assertArrayEquals(new LineD[0], GeoUtils.connectPoints(true));
        assertArrayEquals(new LineD[0], GeoUtils.connectPoints(false, p0));
        assertArrayEquals(new LineD[0], GeoUtils.connectPoints(true, p0));

        assertArrayEquals(new LineD[] { new LineD(p0, p1) },
                GeoUtils.connectPoints(false, p0, p1));
        assertArrayEquals(new LineD[] { new LineD(p0, p1), new LineD(p1, p0) },
                GeoUtils.connectPoints(true, p0, p1));

        assertArrayEquals(new LineD[] { new LineD(p0, p1), new LineD(p1, p2) },
                GeoUtils.connectPoints(false, p0, p1, p2));
        assertArrayEquals(new LineD[] { new LineD(p0, p1), new LineD(p1, p2), new LineD(p2, p0) },
                GeoUtils.connectPoints(true, p0, p1, p2));
    }

    @Test
    public void testConvexHull() {
        final PointD p0 = new PointD(0, 0), p1 = new PointD(1, 1), p2 = new PointD(2, 0);

        assertArrayEquals(new PointD[] { p0 }, GeoUtils.convexHull(p0));
        assertArrayEquals(new PointD[] { p0 }, GeoUtils.convexHull(p0, p0));
        assertArrayEquals(new PointD[] { p0 }, GeoUtils.convexHull(p0, p0, p0));

        assertArrayEquals(new PointD[] { p0, p1 }, GeoUtils.convexHull(p0, p1));
        assertArrayEquals(new PointD[] { p1, p0 }, GeoUtils.convexHull(p0, p1, p0));
        assertArrayEquals(new PointD[] { p1, p0 }, GeoUtils.convexHull(p0, p0, p1));
        assertArrayEquals(new PointD[] { p1, p0 }, GeoUtils.convexHull(p0, p1, p1));

        assertArrayEquals(new PointD[] { p1, p2, p0 }, GeoUtils.convexHull(p0, p1, p2));
        assertArrayEquals(new PointD[] { p1, p2, p0 }, GeoUtils.convexHull(p1, p0, p2));

        final PointD p3 = new PointD(1, 0);
        assertArrayEquals(new PointD[] { p1, p2, p0 }, GeoUtils.convexHull(p3, p1, p0, p2));
    }

    @Test
    public void testPointInPolygon() {
        final PointD[] p = { new PointD(0, 0), new PointD(1, 1), new PointD(2, 0) };
        assertEquals(PolygonLocation.INSIDE, GeoUtils.pointInPolygon(new PointD(1.0, 0.5), p));

        assertEquals(PolygonLocation.OUTSIDE, GeoUtils.pointInPolygon(new PointD(0.0, 0.5), p));
        assertEquals(PolygonLocation.OUTSIDE, GeoUtils.pointInPolygon(new PointD(2.0, 0.5), p));
        assertEquals(PolygonLocation.OUTSIDE, GeoUtils.pointInPolygon(new PointD(1.0, -0.5), p));
        assertEquals(PolygonLocation.OUTSIDE, GeoUtils.pointInPolygon(new PointD(1.0, 2.5), p));

        assertEquals(PolygonLocation.EDGE, GeoUtils.pointInPolygon(new PointD(1.0, 0.0), p));
        assertEquals(PolygonLocation.EDGE, GeoUtils.pointInPolygon(new PointD(0.5, 0.5), p));
        assertEquals(PolygonLocation.EDGE, GeoUtils.pointInPolygon(new PointD(1.5, 0.5), p));

        assertEquals(PolygonLocation.VERTEX, GeoUtils.pointInPolygon(new PointD(0.0, 0.0), p));
        assertEquals(PolygonLocation.VERTEX, GeoUtils.pointInPolygon(new PointD(1.0, 1.0), p));
        assertEquals(PolygonLocation.VERTEX, GeoUtils.pointInPolygon(new PointD(2.0, 0.0), p));
    }

    @Test
    public void testPointInPolygonEpsilon() {
        final PointD[] p = { new PointD(0, 0), new PointD(1, 1), new PointD(2, 0) };
        assertEquals(PolygonLocation.INSIDE, GeoUtils.pointInPolygon(new PointD(1.0, 0.5), p, 0.2));

        assertEquals(PolygonLocation.OUTSIDE, GeoUtils.pointInPolygon(new PointD(1.0, -0.5), p, 0.2));
        assertEquals(PolygonLocation.OUTSIDE, GeoUtils.pointInPolygon(new PointD(0.0, 0.5), p, 0.2));
        assertEquals(PolygonLocation.OUTSIDE, GeoUtils.pointInPolygon(new PointD(2.0, 0.5), p, 0.2));

        assertEquals(PolygonLocation.VERTEX, GeoUtils.pointInPolygon(new PointD(1.0, 0.9), p, 0.2));
        assertEquals(PolygonLocation.VERTEX, GeoUtils.pointInPolygon(new PointD(0.0, 0.1), p, 0.2));
        assertEquals(PolygonLocation.VERTEX, GeoUtils.pointInPolygon(new PointD(2.1, 0.0), p, 0.2));

        assertEquals(PolygonLocation.EDGE, GeoUtils.pointInPolygon(new PointD(1.0, -0.1), p, 0.2));
        assertEquals(PolygonLocation.EDGE, GeoUtils.pointInPolygon(new PointD(0.6, 0.5), p, 0.2));
        assertEquals(PolygonLocation.EDGE, GeoUtils.pointInPolygon(new PointD(1.4, 0.5), p, 0.2));
    }

    @Test
    public void testPolygonArea() {
        final double epsilon = 0.001;
        PointD p0 = new PointD(0, 0), p1 = new PointD(1, 1), p2 = new PointD(2, 0);
        PointD p3 = new PointD(0, 2), p4 = new PointD(2, 2);

        // triangles in both orientations
        assertEquals(-1, GeoUtils.polygonArea(p0, p1, p2), epsilon);
        assertEquals(+1, GeoUtils.polygonArea(p2, p1, p0), epsilon);

        // squares in both orientations
        assertEquals(-4, GeoUtils.polygonArea(p0, p3, p4, p2), epsilon);
        assertEquals(+4, GeoUtils.polygonArea(p2, p4, p3, p0), epsilon);

        // collinear points and star shape
        assertEquals(0, GeoUtils.polygonArea(p0, p1, p4), epsilon);
        assertEquals(0, GeoUtils.polygonArea(p0, p1, p3, p1, p2, p1, p4, p1), epsilon);
    }

    @Test
    public void testPolygonCentroid() {
        final PointD p0 = new PointD(0, 0), p1 = new PointD(1, 1), p2 = new PointD(2, 0);
        final PointD p3 = new PointD(0, 2), p4 = new PointD(2, 2);

        assertEquals(new PointD(1, 1 / 3.0), GeoUtils.polygonCentroid(p0, p1, p2));
        assertEquals(new PointD(1, 1 / 3.0), GeoUtils.polygonCentroid(p2, p1, p0));

        assertEquals(p1, GeoUtils.polygonCentroid(p0, p3, p4, p2));
        assertEquals(p1, GeoUtils.polygonCentroid(p2, p4, p3, p0));
    }

    @Test
    public void testRandomPoints() {
        final RectD bounds = new RectD(-100, -100, 200, 200);
        final PointD[] points = GeoUtils.randomPoints(100, bounds);
        for (PointD p: points)
            assertTrue(bounds.contains(p));
    }

    @Test
    public void testRandomPointsComparator() {
        randomPointsComparator(new PointDComparatorX(0));
        randomPointsComparator(new PointDComparatorY(0));
    }

    @Test
    public void testRandomPointsComparatorEpsilon() {
        randomPointsComparator(new PointDComparatorX(0.5));
        randomPointsComparator(new PointDComparatorY(0.5));
    }

    private static void randomPointsComparator(PointDComparator comparer) {
        final RectD bounds = new RectD(-100, -100, 200, 200);
        final PointD[] points = GeoUtils.randomPoints(100, bounds, comparer, 2);

        for (int i = 0; i < points.length; i++) {
            final PointD p = points[i];
            assertTrue(bounds.contains(p));
            if (i > 0)
                assertEquals(+1, comparer.compare(p, points[i - 1]));
            if (i < points.length - 1)
                assertEquals(-1, comparer.compare(p, points[i + 1]));

            for (int j = 0; j < points.length; j++) {
                if (i == j) continue;
                final double distance = p.subtract(points[j]).lengthSquared();
                assertTrue(distance >= 4);
            }
        }
    }
}
