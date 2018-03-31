package org.kynosarges.tektosyne.geometry;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.kynosarges.tektosyne.subdivision.*;

/**
 * Provides unit tests for class {@link Voronoi} and related classes.
 * Also provides a factory method to create random {@link Voronoi} diagrams.
 *
 * @author Christoph Nahr
 * @version 6.2.0
 */
public class VoronoiTest {

    private final static ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    @Test
    public void testVoronoi() {
        for (int i = 0; i < 10; i++)
            testVoronoiResults(createRandom(100, -1000, -1000, 2000, 2000));
    }

    public static VoronoiResults createRandom(int count, double x, double y, double width, double height) {
        final int minCount = Math.max(2, count / 10);
        final int maxCount = Math.max(3, count + 1);
        final PointD[] points = new PointD[RANDOM.nextInt(minCount, maxCount)];

        final RectD bounds = GeoUtils.randomRect(x, y, width, height);
        for (int i = 0; i < points.length; i++)
            points[i] = GeoUtils.randomPoint(bounds);

        return Voronoi.findAll(points, bounds);
    }

    @Test
    public void testVoronoiWithEdgesHittingCorners() {
        final RectD clip = new RectD(-10, -10, 10, 10);

        final PointD[] points1 = new PointD[]{
                new PointD(-5, 0),
                new PointD(0, 5),
                new PointD(5, 0)
        };
        testVoronoiResults(Voronoi.findAll(points1, clip));

        final PointD[] points2 = new PointD[]{
                new PointD(0, 5),
                new PointD(5, 0),
                new PointD(0, -5)
        };
        testVoronoiResults(Voronoi.findAll(points2, clip));

        final PointD[] points3 = new PointD[]{
                new PointD(5, 0),
                new PointD(0, -5),
                new PointD(-5, 0),
        };
        testVoronoiResults(Voronoi.findAll(points3, clip));

        final PointD[] points4 = new PointD[]{
                new PointD(0, -5),
                new PointD(-5, 0),
                new PointD(0, 5)
        };
        testVoronoiResults(Voronoi.findAll(points4, clip));
    }

    @Test
    public void testVoronoiWithRegionTouchingThreeCorners() {
        final RectD clip = new RectD(-10, -10, 10, 10);

        final PointD[] points1 = new PointD[]{
                new PointD(-1, -1), // key region
                new PointD(-9, -7),
                new PointD(-8, -9),
        };
        testVoronoiResults(Voronoi.findAll(points1, clip));

        final PointD[] points2 = new PointD[]{
                new PointD(1, -1), // key region
                new PointD(9, -7),
                new PointD(8, -9),
        };
        testVoronoiResults(Voronoi.findAll(points2, clip));

        final PointD[] points3 = new PointD[]{
                new PointD(-1, 1), // key region
                new PointD(-9, 7),
                new PointD(-8, 9),
        };
        testVoronoiResults(Voronoi.findAll(points3, clip));

        final PointD[] points4 = new PointD[]{
                new PointD(1, 1), // key region
                new PointD(9, 7),
                new PointD(8, 9),
        };
        testVoronoiResults(Voronoi.findAll(points4, clip));
    }

    @Test
    public void testVoronoiWithRegionTouchingTwoOppositeSides() {
        final RectD clip = new RectD(-10, -10, 10, 10);

        final PointD[] points1 = new PointD[]{
                new PointD(-5, 0),
                new PointD(0, 0), // key region
                new PointD(5, 0),
        };
        testVoronoiResults(Voronoi.findAll(points1, clip));

        final PointD[] points2 = new PointD[]{
                new PointD(0, -5),
                new PointD(0, 0), // key region
                new PointD(0, 5),
        };
        testVoronoiResults(Voronoi.findAll(points2, clip));

        final PointD[] points3 = new PointD[]{
                new PointD(-5, -5),
                new PointD(0, 0), // key region
                new PointD(5, 5),
        };
        testVoronoiResults(Voronoi.findAll(points3, clip));

        final PointD[] points4 = new PointD[]{
                new PointD(-5, 5),
                new PointD(0, 0), // key region
                new PointD(5, -5),
        };
        testVoronoiResults(Voronoi.findAll(points4, clip));
    }

    @Test
    public void testVoronoiWithTwoSites() {
        final RectD clip = new RectD(-10, -10, 10, 10);

        final PointD[] points1 = new PointD[]{
                new PointD(-5, 0),
                new PointD(5, 0)
        };
        testVoronoiResults(Voronoi.findAll(points1, clip));

        final PointD[] points2 = new PointD[]{
                new PointD(0, 5),
                new PointD(0, -5)
        };
        testVoronoiResults(Voronoi.findAll(points2, clip));

        final PointD[] points3 = new PointD[]{
                new PointD(-5, 5),
                new PointD(5, -5),
        };
        testVoronoiResults(Voronoi.findAll(points3, clip));

        final PointD[] points4 = new PointD[]{
                new PointD(-5, -5),
                new PointD(5, 5)
        };
        testVoronoiResults(Voronoi.findAll(points4, clip));
    }

    private static void testVoronoiResults(VoronoiResults results) {
        final Subdivision delaunay = results.toDelaunaySubdivision(true);
        delaunay.validate();

        // compare original and subdivision’s Delaunay edges
        final LineD[] delaunayEdges = delaunay.toLines();
        assertEquals(results.delaunayEdges().length, delaunayEdges.length);

        for (LineD edge: results.delaunayEdges())
            if (PointDComparatorY.compareExact(edge.start, edge.end) > 0)
                assertTrue(Arrays.asList(delaunayEdges).contains(edge.reverse()));
            else
                assertTrue(Arrays.asList(delaunayEdges).contains(edge));

        final VoronoiMap voronoi = new VoronoiMap(results);
        voronoi.source().validate();

        // compare original and subdivision’s Voronoi regions
        final NavigableMap<Integer, SubdivisionFace> voronoiFaces = voronoi.source().faces();
        assertEquals(results.voronoiRegions().length, voronoiFaces.size() - 1);

        for (SubdivisionFace face: voronoiFaces.values()) {
            if (face.outerEdge() == null) continue;
            final int index = voronoi.fromFace(face);

            final PointD[] polygon = results.voronoiRegions()[index];
            assertArrayEquivalent(polygon, face.outerEdge().cyclePolygon());

            final PointD site = results.generatorSites[index];
            assertNotEquals(PolygonLocation.OUTSIDE, face.outerEdge().locate(site));
        }
    }

    private static <T> void assertArrayEquivalent(T[] a, T[] b) {
        assertCollectionEquivalent(Arrays.asList(a), Arrays.asList(b));
    }

    private static <T> void assertCollectionEquivalent(Collection<T> a, Collection<T> b) {
        assertEquals(a.size(), b.size());
        assertTrue(a.containsAll(b));
        assertTrue(b.containsAll(a));
    }
}
