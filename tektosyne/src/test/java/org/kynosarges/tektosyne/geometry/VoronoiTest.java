package org.kynosarges.tektosyne.geometry;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;
import org.kynosarges.tektosyne.subdivision.*;

/**
 * Provides unit tests for class {@link Voronoi} and related classes.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class VoronoiTest {

    @Test
    public void testVoronoi() {
        final int count = 10 + (int) (Math.random() * 91);
        final PointD[] points = new PointD[count];
        for (int i = 0; i < points.length; i++)
            points[i] = GeoUtils.randomPoint(-1000, -1000, 2000, 2000);

        final VoronoiResults results = Voronoi.findAll(points, new RectD(-1000, -1000, 2000, 2000));
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
