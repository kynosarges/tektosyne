package org.kynosarges.tektosyne.geometry;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;
import org.kynosarges.tektosyne.graph.*;
import org.kynosarges.tektosyne.subdivision.*;

/**
 * Provides unit tests for class {@link PolygonGrid}.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class PolygonGridTest {

    final RegularPolygon element = new RegularPolygon(10, 6, PolygonOrientation.ON_EDGE);
    PolygonGrid grid, readOnly;

    @Before
    public void setup() {
        grid = new PolygonGrid(element);
        readOnly = grid.asReadOnly();
    }

    @Test
    public void testAsReadOnly() {
        assertFalse(grid.isReadOnly);
        assertTrue(readOnly.isReadOnly);
        assertNotSame(readOnly, grid.asReadOnly());
        assertSame(readOnly, readOnly.asReadOnly());
    }

    @Test
    public void testData() {

        // settable properties
        assertEquals(grid.element(), readOnly.element());
        assertEquals(grid.gridShift(), readOnly.gridShift());
        assertEquals(grid.size(), readOnly.size());

        // dependent properties
        assertEquals(grid.centerDistance(), readOnly.centerDistance());
        assertEquals(grid.worldBounds(), readOnly.worldBounds());
        assertArrayEquals(grid.edgeNeighborOffsets(), readOnly.edgeNeighborOffsets());
        assertArrayEquals(grid.neighborOffsets(), readOnly.neighborOffsets());
    }

    @Test
    public void testElement() {
        try {
            readOnly.setElement(grid.element());
            fail("expected IllegalStateException");
        } catch (IllegalStateException e) { }

        grid.setElement(new RegularPolygon(5.0, 4, PolygonOrientation.ON_EDGE));
        assertNotEquals(element, grid.element());
        testData();
    }

    @Test
    public void testGridShift() {
        try {
            readOnly.setGridShift(grid.gridShift());
            fail("expected IllegalStateException");
        } catch (IllegalStateException e) { }

        assertEquals(PolygonGridShift.COLUMN_DOWN, grid.gridShift());
        grid.setGridShift(PolygonGridShift.COLUMN_UP);
        testData();
    }

    @Test
    public void testSize() {
        try {
            readOnly.setSize(grid.size());
            fail("expected IllegalStateException");
        } catch (IllegalStateException e) { }

        assertEquals(new SizeI(1, 1), grid.size());
        grid.setSize(new SizeI(5, 5));
        testData();
    }

    @Test
    public void testGraph() {
        grid.setSize(new SizeI(2, 2));
        final Graph<PointI> graph = (Graph<PointI>) grid;
        final PointI[] locations = {
            new PointI(0, 0), new PointI(0, 1), new PointI(1, 0), new PointI(1, 1)
        };

        assertEquals(6, graph.connectivity());
        assertEquals(4, graph.nodeCount());
        for (PointI node: graph.nodes())
            assertTrue(Arrays.asList(locations).contains(node));

        final double distance = graph.getDistance(locations[0], locations[1]);
        Collection<PointI> stepNeighbors, graphStepNeighbors;

        for (PointI location: locations) {
            assertTrue(graph.contains(location));

            final PointD point = grid.gridToWorld(location);
            assertEquals(point, graph.getWorldLocation(location));
            final PointD near = new PointD(point.x + 0.1, point.y - 0.1);
            assertEquals(location, graph.findNearestNode(near));

            PointI[] neighbors = null;
            if (location == locations[0] || location == locations[3])
                neighbors = new PointI[] { locations[1], locations[2] };
            else if (location == locations[1])
                neighbors = new PointI[] { locations[0], locations[2], locations[3] };
            else if (location == locations[2])
                neighbors = new PointI[] { locations[0], locations[1], locations[3] };

            final Collection<PointI> graphNeighbors = graph.getNeighbors(location);
            assertCollectionEquivalent(Arrays.asList(neighbors), graphNeighbors);
            for (PointI neighbor: neighbors)
                assertEquals(distance, graph.getDistance(location, neighbor), 0);

            stepNeighbors = grid.getNeighbors(location, 1);
            assertCollectionEquivalent(graphNeighbors, stepNeighbors);
            graphStepNeighbors = grid.getNeighborsGraph(location, 1);
            assertCollectionEquivalent(stepNeighbors, graphStepNeighbors);
        }

        stepNeighbors = grid.getNeighbors(PointI.EMPTY, 2);
        graphStepNeighbors = grid.getNeighborsGraph(PointI.EMPTY, 2);
        assertCollectionEquivalent(stepNeighbors, graphStepNeighbors);
    }

    @Test
    public void testSubdivision() {
        grid = new PolygonGrid(new RegularPolygon(10, 4, PolygonOrientation.ON_EDGE));
        grid.setSize(new SizeI(6, 4));
        PolygonGridMap map = new PolygonGridMap(grid, PointD.EMPTY, 0);
        checkGridMap(map);

        grid = new PolygonGrid(new RegularPolygon(10, 4, PolygonOrientation.ON_VERTEX));
        grid.setSize(new SizeI(4, 6));
        map = new PolygonGridMap(grid, PointD.EMPTY, 0);
        checkGridMap(map);

        grid = new PolygonGrid(new RegularPolygon(10, 6, PolygonOrientation.ON_EDGE));
        grid.setSize(new SizeI(6, 4));
        map = new PolygonGridMap(grid, PointD.EMPTY, 0);
        checkGridMap(map);

        grid = new PolygonGrid(new RegularPolygon(10, 6, PolygonOrientation.ON_VERTEX));
        grid.setSize(new SizeI(4, 6));
        map = new PolygonGridMap(grid, PointD.EMPTY, 0);
        checkGridMap(map);
    }

    private static void checkGridMap(PolygonGridMap map) {
        map.source().validate();

        // test finding vertices with findNearestVertex
        for (PointD vertex: map.source().vertices().keySet()) {
            PointD q = vertex.add(GeoUtils.randomPoint(-2, -2, 4, 4));
            assertEquals(vertex, map.source().findNearestVertex(q));
        }

        // test getElementVertices and face mapping
        for (int x = 0; x < map.target().size().width; x++)
            for (int y = 0; y < map.target().size().height; y++) {

                final PointD[] polygon = map.target().getElementVertices(x, y);
                final SubdivisionFace face = map.source().findFace(polygon, true);

                assertSame(face, map.toFace(new PointI(x, y)));
                assertEquals(new PointI(x, y), map.fromFace(face));
            }
    }
    
    private static <T> void assertCollectionEquivalent(Collection<T> a, Collection<T> b) {
        assertEquals(a.size(), b.size());
        assertTrue(a.containsAll(b));
        assertTrue(b.containsAll(a));
    }
}
