package org.kynosarges.tektosyne.subdivision;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;
import org.kynosarges.tektosyne.geometry.*;
import org.kynosarges.tektosyne.graph.*;

/**
 * Provides unit tests for class {@link Subdivision} and related classes.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class SubdivisionTest {

    @Test
    public void testGraph() {
        final PointD[] points = {
            new PointD(0, 0), new PointD(-1, -2),
            new PointD(-1, 2), new PointD(1, 2), new PointD(1, -2)
        };
        final LineD[] lines = {
            new LineD(points[0], points[1]), new LineD(points[0], points[2]),
            new LineD(points[0], points[3]), new LineD(points[4], points[0])
        };
        final Subdivision division = Subdivision.fromLines(lines, 0);
        division.validate();
        final Graph<PointD> graph = division;

        assertEquals(4, graph.connectivity());
        assertEquals(5, graph.nodeCount());
        for (PointD node: graph.nodes())
            assertTrue(Arrays.asList(points).contains(node));

        for (PointD point: points) {
            assertTrue(graph.contains(point));
            assertEquals(point, graph.getWorldLocation(point));

            final PointD near = new PointD(point.x + 0.1, point.y - 0.1);
            assertFalse(graph.contains(near));
            assertEquals(point, graph.findNearestNode(near));
        }

        final PointD[] center = { points[0] };
        final PointD[] neighbors = new PointD[points.length - 1];
        double distance = Math.sqrt(5.0);

        for (int i = 1; i < points.length; i++) {
            assertEquals(distance, graph.getDistance(points[0], points[i]), 0);
            assertCollectionEquivalent(Arrays.asList(center), graph.getNeighbors(points[i]));
            neighbors[i - 1] = points[i];
        }

        assertCollectionEquivalent(Arrays.asList(neighbors), graph.getNeighbors(points[0]));
    }

    @Test
    public void testFindEdge() {
        final PointD[] points = {
            new PointD(0, 0), new PointD(-1, -2), 
            new PointD(-1, 2), new PointD(1, 2), new PointD(1, -2)
        };
        final LineD[] lines = {
            new LineD(points[1], points[0]), new LineD(points[0], points[2]),
            new LineD(points[0], points[3]), new LineD(points[4], points[0])
        };
        final Subdivision division = Subdivision.fromLines(lines, 0);
        division.validate();

        // check all existing half-edges
        for (SubdivisionEdge edge: division.edges().values())
            assertSame(edge, division.findEdge(edge.origin(), edge.destination()));

        for (int i = 0; i < points.length; i++) {
            // check half-edges including one non-existing vertex
            assertNull(division.findEdge(points[i], new PointD(1, 0)));
            assertNull(division.findEdge(new PointD(1, 0), points[i]));

            // check nonexistent half-edges between vertices
            if (i == 0) continue;
            for (int j = 1; j < points.length; j++)
                if (j != i) assertNull(division.findEdge(points[i], points[j]));
        }
    }

    @Test
    public void testFindEdgeEpsilon() {
        final PointD[] points = {
            new PointD(0, 0), new PointD(-1, -2), 
            new PointD(-1, 2), new PointD(1, 2), new PointD(1, -2)
        };
        final LineD[] lines = {
            new LineD(points[1], points[0]), new LineD(points[0], points[2]),
            new LineD(points[0], points[3]), new LineD(points[4], points[0])
        };
        final Subdivision division = Subdivision.fromLines(lines, 0.2);
        division.validate();

        // check all existing half-edges
        final PointD offset = new PointD(0.1, -0.1);
        for (SubdivisionEdge edge: division.edges().values()) {
            assertSame(edge, division.findEdge(edge.origin(), edge.destination()));
            assertSame(edge, division.findEdge(
                edge.origin().add(offset), edge.destination().add(offset)));
        }

        for (int i = 0; i < points.length; i++) {
            // check half-edges including one non-existing vertex
            assertNull(division.findEdge(points[i], new PointD(1, 0)));
            assertNull(division.findEdge(new PointD(1, 0), points[i]));

            // check nonexistent half-edges between vertices
            if (i == 0) continue;
            for (int j = 1; j < points.length; j++)
                if (j != i) assertNull(division.findEdge(points[i], points[j]));
        }
    }

    @Test
    public void testFindFacePoint() {
        Subdivision division = SubdivisionLinesTest.createSquareStar(false);

        assertEquals(division.faces().get(0), division.findFace(new PointD(-2, 0)));
        assertEquals(division.faces().get(1), division.findFace(new PointD(-0.5, 0)));
        assertEquals(division.faces().get(2), division.findFace(new PointD(0, 0.5)));
        assertEquals(division.faces().get(3), division.findFace(new PointD(0.5, 0)));
        assertEquals(division.faces().get(4), division.findFace(new PointD(0, -0.5)));

        // four nested triangles
        final PointD[][] polygons = {
            { new PointD(-8, -8), new PointD(0, 8), new PointD(8, -8) },
            { new PointD(-6, -6), new PointD(0, 6), new PointD(6, -6) },
            { new PointD(-4, -4), new PointD(0, 4), new PointD(4, -4) },
            { new PointD(-2, -2), new PointD(0, 2), new PointD(2, -2) }
        };
        division = Subdivision.fromPolygons(polygons, 0);
        division.validate();

        assertEquals(division.faces().get(0), division.findFace(new PointD(0, 10)));
        assertEquals(division.faces().get(1), division.findFace(new PointD(0, 7)));
        assertEquals(division.faces().get(2), division.findFace(new PointD(0, 5)));
        assertEquals(division.faces().get(3), division.findFace(new PointD(0, 3)));
        assertEquals(division.faces().get(4), division.findFace(new PointD(0, 1)));
    }

    @Test
    public void testFindFacePolygon() {
        final PointD[] polygon = { new PointD(0, 0), new PointD(1, 1), new PointD(2, 0) };
        final Subdivision division = Subdivision.fromPolygons(new PointD[][] { polygon }, 0);
        division.validate();
        final SubdivisionFace face = division.faces().get(1);

        // original sequence, any starting vertex
        assertSame(face, division.findFace(new PointD[] { polygon[0], polygon[1], polygon[2] }, false));
        assertSame(face, division.findFace(new PointD[] { polygon[1], polygon[2], polygon[0] }, false));
        assertSame(face, division.findFace(new PointD[] { polygon[2], polygon[0], polygon[1] }, false));

        // inverted sequence, any starting vertex
        assertSame(face, division.findFace(new PointD[] { polygon[2], polygon[1], polygon[0] }, false));
        assertSame(face, division.findFace(new PointD[] { polygon[0], polygon[2], polygon[1] }, false));
        assertSame(face, division.findFace(new PointD[] { polygon[1], polygon[0], polygon[2] }, false));

        // sequence including nonexistent point
        final PointD point = new PointD(2, 1);
        assertNull(division.findFace(new PointD[] { point, polygon[1], polygon[2] }, false));
        assertNull(division.findFace(new PointD[] { polygon[0], point, polygon[2] }, false));
        assertNull(division.findFace(new PointD[] { polygon[0], polygon[1], point }, false));
    }

    @Test
    public void testFindNearestEdge() {
        final Subdivision division = SubdivisionLinesTest.createSquareStar(false);

        assertEquals(division.edges().get(0), division.findNearestEdge(new PointD(-1.1, 0)).edge);
        assertEquals(division.edges().get(1), division.findNearestEdge(new PointD(-0.9, 0)).edge);
        assertEquals(division.edges().get(2), division.findNearestEdge(new PointD(0, 2.1)).edge);
        assertEquals(division.edges().get(3), division.findNearestEdge(new PointD(0, 1.9)).edge);
        assertEquals(division.edges().get(4), division.findNearestEdge(new PointD(0.9, 0)).edge);
        assertEquals(division.edges().get(5), division.findNearestEdge(new PointD(1.1, 0)).edge);
        assertEquals(division.edges().get(6), division.findNearestEdge(new PointD(0, -1.9)).edge);
        assertEquals(division.edges().get(7), division.findNearestEdge(new PointD(0, -2.1)).edge);

        assertEquals(division.edges().get(8), division.findNearestEdge(new PointD(-0.5, -1.1)).edge);
        assertEquals(division.edges().get(9), division.findNearestEdge(new PointD(-0.5, -0.9)).edge);
        assertEquals(division.edges().get(10), division.findNearestEdge(new PointD(-0.5, 0.9)).edge);
        assertEquals(division.edges().get(11), division.findNearestEdge(new PointD(-0.5, 1.1)).edge);
        assertEquals(division.edges().get(12), division.findNearestEdge(new PointD(0.5, 1.1)).edge);
        assertEquals(division.edges().get(13), division.findNearestEdge(new PointD(0.5, 0.9)).edge);
        assertEquals(division.edges().get(14), division.findNearestEdge(new PointD(0.5, -0.9)).edge);
        assertEquals(division.edges().get(15), division.findNearestEdge(new PointD(0.5, -1.1)).edge);
    }

    @Test
    public void testLocateInFace() {
        final PointD[] polygon = { new PointD(0, 0), new PointD(1, 1), new PointD(2, 0) };
        final Subdivision division = Subdivision.fromPolygons(new PointD[][] { polygon }, 0);
        division.validate();

        final SubdivisionEdge edge = division.faces().get(1).outerEdge();
        assertEquals(PolygonLocation.INSIDE, edge.locate(new PointD(1.0, 0.5)));

        assertEquals(PolygonLocation.OUTSIDE, edge.locate(new PointD(0.0, 0.5)));
        assertEquals(PolygonLocation.OUTSIDE, edge.locate(new PointD(2.0, 0.5)));
        assertEquals(PolygonLocation.OUTSIDE, edge.locate(new PointD(1.0, -0.5)));
        assertEquals(PolygonLocation.OUTSIDE, edge.locate(new PointD(1.0, 2.5)));

        assertEquals(PolygonLocation.EDGE, edge.locate(new PointD(1.0, 0.0)));
        assertEquals(PolygonLocation.EDGE, edge.locate(new PointD(0.5, 0.5)));
        assertEquals(PolygonLocation.EDGE, edge.locate(new PointD(1.5, 0.5)));

        assertEquals(PolygonLocation.VERTEX, edge.locate(new PointD(0.0, 0.0)));
        assertEquals(PolygonLocation.VERTEX, edge.locate(new PointD(1.0, 1.0)));
        assertEquals(PolygonLocation.VERTEX, edge.locate(new PointD(2.0, 0.0)));
    }

    @Test
    public void testLocateInFaceEpsilon() {
        final PointD[] polygon = { new PointD(0, 0), new PointD(1, 1), new PointD(2, 0) };
        final Subdivision division = Subdivision.fromPolygons(new PointD[][] { polygon }, 0);
        division.validate();

        final SubdivisionEdge edge = division.faces().get(1).outerEdge();
        assertEquals(PolygonLocation.INSIDE, edge.locate(new PointD(1.0, 0.5), 0.2));

        assertEquals(PolygonLocation.OUTSIDE, edge.locate(new PointD(1.0, -0.5), 0.2));
        assertEquals(PolygonLocation.OUTSIDE, edge.locate(new PointD(0.0, 0.5), 0.2));
        assertEquals(PolygonLocation.OUTSIDE, edge.locate(new PointD(2.0, 0.5), 0.2));

        assertEquals(PolygonLocation.VERTEX, edge.locate(new PointD(1.0, 0.9), 0.2));
        assertEquals(PolygonLocation.VERTEX, edge.locate(new PointD(0.0, 0.1), 0.2));
        assertEquals(PolygonLocation.VERTEX, edge.locate(new PointD(2.1, 0.0), 0.2));

        assertEquals(PolygonLocation.EDGE, edge.locate(new PointD(1.0, -0.1), 0.2));
        assertEquals(PolygonLocation.EDGE, edge.locate(new PointD(0.6, 0.5), 0.2));
        assertEquals(PolygonLocation.EDGE, edge.locate(new PointD(1.4, 0.5), 0.2));
    }

    @Test
    public void testStructureEquals() {
        final Subdivision division = SubdivisionLinesTest.createTriforce(false);
        division.validate();

        // check overall structural equality
        final Subdivision clone = division.copy();
        clone.validate();
        assertTrue(division.structureEquals(clone));

        // check individual edges
        assertEquals(division.edges().size(), clone.edges().size());
        for (int i = 0; i < division.edges().size(); i++) {
            final SubdivisionEdge edge = division.edges().get(i);
            final SubdivisionEdge cloneEdge = clone.edges().get(i);
            assertEquals(edge, cloneEdge);
        }

        // check individual faces
        assertEquals(division.faces().size(), clone.faces().size());
        for (int i = 0; i < division.faces().size(); i++) {
            final SubdivisionFace face = division.faces().get(i);
            final SubdivisionFace cloneFace = clone.faces().get(i);
            assertEquals(face, cloneFace);
        }
    }
    
    private static <T> void assertCollectionEquivalent(Collection<T> a, Collection<T> b) {
        assertEquals(a.size(), b.size());
        assertTrue(a.containsAll(b));
        assertTrue(b.containsAll(a));
    }
}
