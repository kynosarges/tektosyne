package org.kynosarges.tektosyne.subdivision;

import org.junit.*;
import static org.junit.Assert.*;

import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides unit tests for class {@link SubdivisionSearch}.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class SubdivisionSearchTest {

    @Test
    public void testEmpty() {
        final SubdivisionSearch search = new SubdivisionSearch(new Subdivision(0), false);
        search.validate();

        for (int i = 0; i < 10; i++) {
            final PointD q = GeoUtils.randomPoint(-100, -100, 200, 200);
            assertTrue(search.find(q).isUnboundedFace());
            assertTrue(search.source.find(q, 0).isUnboundedFace());
        }
    }

    @Test
    public void testSingleEdgeX() {
        final SubdivisionSearch search = checkSearch(new LineD(-5, 0, +5, 0));

        assertTrue(search.find(new PointD(0, -1)).isUnboundedFace());
        assertTrue(search.find(new PointD(0, +1)).isUnboundedFace());
        assertTrue(search.find(new PointD(-6, 0)).isUnboundedFace());
        assertTrue(search.find(new PointD(+6, 0)).isUnboundedFace());
    }

    @Test
    public void testSingleEdgeY() {
        final SubdivisionSearch search = checkSearch(new LineD(0, -5, 0, +5));

        assertTrue(search.find(new PointD(-1, 0)).isUnboundedFace());
        assertTrue(search.find(new PointD(+1, 0)).isUnboundedFace());
        assertTrue(search.find(new PointD(0, -6)).isUnboundedFace());
        assertTrue(search.find(new PointD(0, +6)).isUnboundedFace());
    }

    @Test
    public void testDoubleEdgeX() {
        final LineD line = new LineD(-5, 0, +5, 0);

        // parallel horizontal edges
        checkSearch(line, new LineD(-4, +2, +4, +2));
        checkSearch(line, new LineD(-5, +2, +4, +2));
        checkSearch(line, new LineD(-5, +2, +5, +2)); // multi-cell intersection
        checkSearch(line, new LineD(-4, -2, +4, -2));
        checkSearch(line, new LineD(-4, -2, +5, -2));
        checkSearch(line, new LineD(-5, -2, +5, -2)); // multi-cell intersection

        // horizontal and vertical edge
        checkSearch(line, new LineD(0, +1, 0, +4));
        checkSearch(line, new LineD(0, -1, 0, -4));

        // horizontal and diagonal edge
        checkSearch(line, new LineD(-5,  0, +4, +2));
        checkSearch(line, new LineD(-5,  0, +5, +2)); // multi-cell intersection
        checkSearch(line, new LineD(-5,  0, +5, -2));
        checkSearch(line, new LineD(-5, +2, +5,  0));
        checkSearch(line, new LineD(-4, -2, +5,  0));
        checkSearch(line, new LineD(-5, -2, +5,  0)); // multi-cell intersection
    }

    @Test
    public void testDoubleEdgeY() {
        final LineD line = new LineD(0, -5, 0, +5);

        // parallel vertical edges
        checkSearch(line, new LineD(+2, -4, +2, +4));
        checkSearch(line, new LineD(+2, -5, +2, +5));
        checkSearch(line, new LineD(-2, -4, -2, +4));
        checkSearch(line, new LineD(-2, -5, -2, +5));

        // vertical and horizontal edge
        checkSearch(line, new LineD(+1, 0, +4, 0));
        checkSearch(line, new LineD(-1, 0, -4, 0));

        // vertical and diagonal edge
        checkSearch(line, new LineD(+1, -5, +2, +5));
        checkSearch(line, new LineD( 0, -5, +2, +5)); // multi-cell intersection
        checkSearch(line, new LineD( 0, -5, -2, +5));
        checkSearch(line, new LineD(+2, -5,  0, +5));
    }

    @Test
    public void testFromLines() {
        Subdivision division = SubdivisionLinesTest.createSquareStar(false);
        checkSearch(division);

        division = SubdivisionLinesTest.createTriforce(false);
        checkSearch(division);
    }

    @Test
    public void testPolygon() {
        for (int i = 3; i < 9; i++) {
            final RegularPolygon polygon = new RegularPolygon(30.0 / i, i, PolygonOrientation.ON_EDGE);
            final LineD[] lines = GeoUtils.connectPoints(true, polygon.vertices);
            checkSearch(lines);
        }
    }

    @Test
    public void testPolygonGrid() {
        final RegularPolygon polygon = new RegularPolygon(5, 4, PolygonOrientation.ON_EDGE);
        final PolygonGrid grid = new PolygonGrid(polygon);
        grid.setSize(new SizeI(10, 10));

        PolygonGridMap map = new PolygonGridMap(grid, PointD.EMPTY, 0);
        checkSearch(map.source());

        grid.setElement(new RegularPolygon(5, 4, PolygonOrientation.ON_VERTEX));
        map = new PolygonGridMap(grid, PointD.EMPTY, 0);
        checkSearch(map.source());

        grid.setElement(new RegularPolygon(5, 6, PolygonOrientation.ON_EDGE));
        map = new PolygonGridMap(grid, PointD.EMPTY, 0);
        checkSearch(map.source());

        grid.setElement(new RegularPolygon(5, 6, PolygonOrientation.ON_VERTEX));
        map = new PolygonGridMap(grid, PointD.EMPTY, 0);
        checkSearch(map.source());
    }

    private SubdivisionSearch checkSearch(LineD... lines) {
        final Subdivision division = Subdivision.fromLines(lines, 0);
        division.validate();
        final SubdivisionSearch search = new SubdivisionSearch(division, false);
        search.validate();
        checkVertices(search);
        checkEdges(search);

        PointD[] points = { 
            new PointD(+1, +1), new PointD(+1, -1),
            new PointD(-1, +1), new PointD(-1, -1)
        };

        if (division.faces().size() == 1) {
            for (PointD point: points) {
                assertTrue(search.find(point).isUnboundedFace());
                assertTrue(division.find(point, 0).isUnboundedFace());
            }
        } else {
            final SubdivisionElement element = new SubdivisionElement(division.faces().get(1));
            for (PointD point: points) {
                assertEquals(element, search.find(point));
                assertEquals(element, division.find(point, 0));
            }
        }

        points = new PointD[] {
            new PointD(+10, +10), new PointD(+10, -10),
            new PointD(-10, +10), new PointD(-10, -10)
        };

        for (PointD point: points) {
            assertTrue(search.find(point).isUnboundedFace());
            assertTrue(division.find(point, 0).isUnboundedFace());
        }
        return search;
    }

    private SubdivisionSearch checkSearch(Subdivision division) {
        division.validate();
        final SubdivisionSearch search = new SubdivisionSearch(division, false);
        search.validate();
        checkVertices(search);
        checkEdges(search);

        for (SubdivisionFace face: division.faces().values()) {
            if (face.outerEdge() == null) continue;
            final PointD centroid = face.outerEdge().cycleCentroid();
            final SubdivisionElement element = new SubdivisionElement(face);
            assertEquals(element, search.find(centroid));
            assertEquals(element, division.find(centroid, 0));
        }

        return search;
    }

    private void checkVertices(SubdivisionSearch search) {

        for (PointD vertex: search.source.vertices().keySet()) {
            final SubdivisionElement element = new SubdivisionElement(vertex);
            assertEquals(element, search.find(vertex));
            assertEquals(element, search.source.find(vertex, 0));

            // brute force search also supports comparison epsilon
            final PointD offset = GeoUtils.randomPoint(-0.1, -0.1, 0.2, 0.2);
            assertEquals(element, search.source.find(vertex.add(offset), 0.25));
        }
    }

    private void checkEdges(SubdivisionSearch search) {

        for (SubdivisionEdge edge: search.source.edges().values()) {
            final SubdivisionElement edgeElement = new SubdivisionElement(edge);
            final SubdivisionElement twinElement = new SubdivisionElement(edge._twin);

            // SubdivisionSearch always returns lexicographically increasing half-edges
            final PointD start = edge._origin, end = edge._twin._origin;
            int result = PointDComparatorX.compareEpsilon(start, end, search.source.epsilon);
            final SubdivisionElement element = (result < 0 ? edgeElement : twinElement);

            final PointD q = new PointD((start.x + end.x) / 2, (start.y + end.y) / 2);
            assertEquals(element, search.find(q));

            // brute force search may return half-edge or its twin
            SubdivisionElement found = search.source.find(q, 1e-10);
            assertTrue(found.equals(edgeElement) || found.equals(twinElement));

            // brute force search also supports comparison epsilon
            final PointD offset = GeoUtils.randomPoint(-0.1, -0.1, 0.2, 0.2);
            found = search.source.find(q.add(offset), 0.25);
            assertTrue(found.equals(edgeElement) || found.equals(twinElement));
        }
    }
}
