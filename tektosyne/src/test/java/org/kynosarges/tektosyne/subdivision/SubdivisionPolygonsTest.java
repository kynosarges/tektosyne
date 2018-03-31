package org.kynosarges.tektosyne.subdivision;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;
import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides unit tests for {@link Subdivision} and related classes.
 * Also provides factory methods to create {@link Subdivision} instances from polygons.
 * 
 * @author Christoph Nahr
 * @version 6.2.0
 */
public class SubdivisionPolygonsTest {
    /*
     *                  e2→           
     *      (-1,+2)------------(+1,+2)
     *         |       ←e3        |
     *         |                  |
     *     e0↑ | ↓e1          e5↑ | ↓e4
     *         |                  |
     *         |        e7→       |
     *      (-1,-2)------------(+1,-2)
     *                 ←e6
     */

    @Test
    public void fromPolygonsSquare() {
        createSquare(true);
    }

    public Subdivision createSquare(boolean test) {
        final PointD[][] polygons = {
                { new PointD(-1, -2), new PointD(-1, 2), new PointD(1, 2), new PointD(1, -2) }
        };
        final Subdivision division = Subdivision.fromPolygons(polygons, 0);
        division.validate();
        if (!test) return division;

        final PointD[][] divisionPolygons = division.toPolygons();
        assertEquals(polygons.length, divisionPolygons.length);
        checkUnorderedEquals(polygons[0], divisionPolygons[0], true);

        final SubdivisionEdge[] edges = division.edges().values().toArray(new SubdivisionEdge[4]);
        final SubdivisionFace[] faces = division.faces().values().toArray(new SubdivisionFace[1]);

        final TreeMap<PointD, SubdivisionEdge> vertices = new TreeMap<>(division.vertices().comparator());
        vertices.put(polygons[0][0], edges[0]);
        vertices.put(polygons[0][3], edges[5]);
        vertices.put(polygons[0][1], edges[1]);
        vertices.put(polygons[0][2], edges[3]);
        assertEquals(vertices, division.vertices());

        assertArrayEquals(new SubdivisionEdge[] {
                new SubdivisionEdge(0, polygons[0][0], edges[1], faces[0], edges[2], edges[6]),
                new SubdivisionEdge(1, polygons[0][1], edges[0], faces[1], edges[7], edges[3]),
                new SubdivisionEdge(2, polygons[0][1], edges[3], faces[0], edges[4], edges[0]),
                new SubdivisionEdge(3, polygons[0][2], edges[2], faces[1], edges[1], edges[5]),
                new SubdivisionEdge(4, polygons[0][2], edges[5], faces[0], edges[6], edges[2]),
                new SubdivisionEdge(5, polygons[0][3], edges[4], faces[1], edges[3], edges[7]),
                new SubdivisionEdge(6, polygons[0][3], edges[7], faces[0], edges[0], edges[4]),
                new SubdivisionEdge(7, polygons[0][0], edges[6], faces[1], edges[5], edges[1]),
        }, edges);

        assertArrayEquals(new SubdivisionFace[] {
                new SubdivisionFace(division, 0, null, Collections.singletonList(edges[0])),
                new SubdivisionFace(division, 1, edges[1], null),
        }, faces);

        checkFace(edges[0], polygons[0], -8, PointD.EMPTY);
        Collections.reverse(Arrays.asList(polygons[0]));
        checkFace(edges[5], polygons[0], +8, PointD.EMPTY);

        final List<SubdivisionEdge> cycles = division.getZeroAreaCycles();
        assertEquals(0, cycles.size());
        return division;
    }

    /*
     *                 ----(0,+6)----
     *                /              \      
     *           e0↑ / ↓e1        e3↑ \ ↓e2
     *              /                  \   
     *             /         e8→       \
     *            / (-1,+2)------(+1,+2) \
     *           /    \     ←e9      /    \ 
     *          /      \            /      \
     *         /    e6↑ \ ↓e7  ↑e11/ e10↓   \
     *        /          \        /          \
     *       /            -(0, 0)-            \
     *      /                                  \ 
     *      |                e5→               |  
     *      (-5,-4)----------------------(+5,-4)
     *                      ←e4
     */

    @Test
    public void fromPolygonsTriforce() {
        createTriforce(true);
    }

    public static Subdivision createTriforce(boolean test) {
        final PointD[][] polygons = {
                { new PointD(-5, -4), new PointD(0, 6), new PointD(5, -4) },
                { new PointD(0, 0), new PointD(-1, 2), new PointD(1, 2) }
        };
        final Subdivision division = Subdivision.fromPolygons(polygons, 0);
        division.validate();
        if (!test) return division;

        final PointD[][] divisionPolygons = division.toPolygons();
        assertEquals(polygons.length, divisionPolygons.length);
        checkUnorderedEquals(polygons[0], divisionPolygons[0], true);
        checkUnorderedEquals(polygons[1], divisionPolygons[1], true);

        final SubdivisionEdge[] edges = division.edges().values().toArray(new SubdivisionEdge[4]);
        final SubdivisionFace[] faces = division.faces().values().toArray(new SubdivisionFace[1]);

        final TreeMap<PointD, SubdivisionEdge> vertices = new TreeMap<>(division.vertices().comparator());
        vertices.put(polygons[0][0], edges[0]);
        vertices.put(polygons[0][2], edges[3]);
        vertices.put(polygons[1][0], edges[6]);
        vertices.put(polygons[1][1], edges[7]);
        vertices.put(polygons[1][2], edges[9]);
        vertices.put(polygons[0][1], edges[1]);
        assertEquals(vertices, division.vertices());

        assertArrayEquals(new SubdivisionEdge[] {
                new SubdivisionEdge(0, polygons[0][0], edges[1], faces[0], edges[2], edges[4]),
                new SubdivisionEdge(1, polygons[0][1], edges[0], faces[1], edges[5], edges[3]),
                new SubdivisionEdge(2, polygons[0][1], edges[3], faces[0], edges[4], edges[0]),
                new SubdivisionEdge(3, polygons[0][2], edges[2], faces[1], edges[1], edges[5]),
                new SubdivisionEdge(4, polygons[0][2], edges[5], faces[0], edges[0], edges[2]),
                new SubdivisionEdge(5, polygons[0][0], edges[4], faces[1], edges[3], edges[1]),
                new SubdivisionEdge(6, polygons[1][0], edges[7], faces[1], edges[8], edges[10]),
                new SubdivisionEdge(7, polygons[1][1], edges[6], faces[2], edges[11], edges[9]),
                new SubdivisionEdge(8, polygons[1][1], edges[9], faces[1], edges[10], edges[6]),
                new SubdivisionEdge(9, polygons[1][2], edges[8], faces[2], edges[7], edges[11]),
                new SubdivisionEdge(10, polygons[1][2], edges[11], faces[1], edges[6], edges[8]),
                new SubdivisionEdge(11, polygons[1][0], edges[10], faces[2], edges[9], edges[7]),
        }, edges);

        assertArrayEquals(new SubdivisionFace[] {
                new SubdivisionFace(division, 0, null, Collections.singletonList(edges[0])),
                new SubdivisionFace(division, 1, edges[1], Collections.singletonList(edges[6])),
                new SubdivisionFace(division, 2, edges[7], null),
        }, faces);

        PointD centroid = new PointD(0, -2 / 3.0);
        checkFace(edges[0], polygons[0], -50, centroid);
        Collections.reverse(Arrays.asList(polygons[0]));
        checkFace(edges[3], polygons[0], +50, centroid);

        centroid = new PointD(0, 4 / 3.0);
        checkFace(edges[6], polygons[1], -2, centroid);
        Collections.reverse(Arrays.asList(polygons[1]));
        checkFace(edges[9], polygons[1], +2, centroid);

        final List<SubdivisionEdge> cycles = division.getZeroAreaCycles();
        assertEquals(0, cycles.size());
        return division;
    }

    /*
     *              -----(0,+4)-----
     *             /      /  \      \
     *        e2↑ / ↓e3  /    \  e13↑\ ↓e12
     *           /      /      \      \
     *          /      /        \      \
     *         /       |        |       \
     *        /    e5↑ |↓e4  e9↑| ↓e8    \
     *       /         |        |         \
     *    (-6,0)    (-3,0)    (+3,0)    (+6,0)
     *       \         |        |         /
     *        \    e7↑ |↓e6 e11↑| ↓e10   /
     *         \       |        |       /
     *          \      \        /      /
     *           \      \      /      /
     *        e0↑ \ ↓e1  \    /  e15↑/ ↓e14
     *             \      \  /      / 
     *              -----(0,-4)-----
     */

    @Test
    public void fromPolygonsDiamond() {
        createDiamond(true);
    }

    public static Subdivision createDiamond(boolean test) {
        final PointD[][] polygons = {
                { new PointD(0, -4), new PointD(-6, 0), new PointD(0, 4), new PointD(-3, 0) },
                { new PointD(0, -4), new PointD(-3, 0), new PointD(0, 4), new PointD(3, 0) },
                { new PointD(0, -4), new PointD(3, 0), new PointD(0, 4), new PointD(6, 0) }
        };
        final Subdivision division = Subdivision.fromPolygons(polygons, 0);
        division.validate();
        if (!test) return division;

        final PointD[][] divisionPolygons = division.toPolygons();
        assertEquals(polygons.length, divisionPolygons.length);
        checkUnorderedEquals(polygons[0], divisionPolygons[0], true);
        checkUnorderedEquals(polygons[1], divisionPolygons[1], true);
        checkUnorderedEquals(polygons[2], divisionPolygons[2], true);

        checkPolygonsDiamond(polygons, division);
        return division;
    }

    @Test
    public void fromPolygonsDiamondEpsilon() {
        createDiamondEpsilon(true);
    }

    public static Subdivision createDiamondEpsilon(boolean test) {
        final PointD[][] polygons = {
                { new PointD(0, -4), new PointD(-6, 0), new PointD(0, 4), new PointD(-3, 0) },
                { new PointD(0.1, -3.9), new PointD(-3, 0), new PointD(-0.1, 4.1), new PointD(3, 0) },
                { new PointD(-0.1, -4.1), new PointD(2.9, 0.1), new PointD(0.1, 3.9), new PointD(6, 0) }
        };
        final Subdivision division = Subdivision.fromPolygons(polygons, 0.2);
        division.validate();
        if (!test) return division;

        final PointD[][] divisionPolygons = division.toPolygons();
        assertEquals(polygons.length, divisionPolygons.length);
        checkUnorderedEquals(polygons[0], divisionPolygons[0], true);
        checkUnorderedEquals(polygons[1], divisionPolygons[1], false);
        checkUnorderedEquals(polygons[2], divisionPolygons[2], false);

        checkPolygonsDiamond(polygons, division);
        return division;
    }

    private static void checkPolygonsDiamond(PointD[][] polygons, Subdivision division) {
        final SubdivisionEdge[] edges = division.edges().values().toArray(new SubdivisionEdge[4]);
        final SubdivisionFace[] faces = division.faces().values().toArray(new SubdivisionFace[1]);

        final TreeMap<PointD, SubdivisionEdge> vertices = new TreeMap<>(division.vertices().comparator());
        vertices.put(polygons[0][0], edges[0]);
        vertices.put(polygons[0][1], edges[1]);
        vertices.put(polygons[0][3], edges[5]);
        vertices.put(polygons[1][3], edges[9]);
        vertices.put(polygons[2][3], edges[13]);
        vertices.put(polygons[0][2], edges[3]);
        assertEquals(vertices, division.vertices());

        assertArrayEquals(new SubdivisionEdge[] {
            new SubdivisionEdge(0, polygons[0][0], edges[1], faces[0], edges[2], edges[14]),
            new SubdivisionEdge(1, polygons[0][1], edges[0], faces[1], edges[7], edges[3]),
            new SubdivisionEdge(2, polygons[0][1], edges[3], faces[0], edges[12], edges[0]),
            new SubdivisionEdge(3, polygons[0][2], edges[2], faces[1], edges[1], edges[5]),
            new SubdivisionEdge(4, polygons[0][2], edges[5], faces[2], edges[6], edges[9]),
            new SubdivisionEdge(5, polygons[0][3], edges[4], faces[1], edges[3], edges[7]),
            new SubdivisionEdge(6, polygons[0][3], edges[7], faces[2], edges[11], edges[4]),
            new SubdivisionEdge(7, polygons[0][0], edges[6], faces[1], edges[5], edges[1]),
            new SubdivisionEdge(8, polygons[0][2], edges[9], faces[3], edges[10], edges[13]),
            new SubdivisionEdge(9, polygons[1][3], edges[8], faces[2], edges[4], edges[11]),
            new SubdivisionEdge(10, polygons[1][3], edges[11], faces[3], edges[15], edges[8]),
            new SubdivisionEdge(11, polygons[0][0], edges[10], faces[2], edges[9], edges[6]),
            new SubdivisionEdge(12, polygons[0][2], edges[13], faces[0], edges[14], edges[2]),
            new SubdivisionEdge(13, polygons[2][3], edges[12], faces[3], edges[8], edges[15]),
            new SubdivisionEdge(14, polygons[2][3], edges[15], faces[0], edges[0], edges[12]),
            new SubdivisionEdge(15, polygons[0][0], edges[14], faces[3], edges[13], edges[10]),
        }, edges);

        assertArrayEquals(new SubdivisionFace[] {
            new SubdivisionFace(division, 0, null, Collections.singletonList(edges[0])),
            new SubdivisionFace(division, 1, edges[1], null),
            new SubdivisionFace(division, 2, edges[4], null),
            new SubdivisionFace(division, 3, edges[8], null),
        }, faces);

        checkFace(edges[0], new PointD[] {
            polygons[0][0], polygons[0][1], polygons[0][2], polygons[2][3] }, -48, PointD.EMPTY);
        checkFace(edges[1], new PointD[] {
            polygons[0][1], polygons[0][0], polygons[0][3], polygons[0][2] }, +12, new PointD(-3, 0));
        checkFace(edges[6], new PointD[] {
            polygons[0][3], polygons[0][0], polygons[1][3], polygons[0][2] }, +24, PointD.EMPTY);
        checkFace(edges[10], new PointD[] {
            polygons[1][3], polygons[0][0], polygons[2][3], polygons[0][2] }, +12, new PointD(3, 0));

        final List<SubdivisionEdge> cycles = division.getZeroAreaCycles();
        assertEquals(0, cycles.size());
    }
    
    private static void checkFace(
        SubdivisionEdge edge, PointD[] polygon, double area, PointD centroid) {

        assertArrayEquals(polygon, edge.cyclePolygon());
        assertEquals(area, edge.cycleArea(), 0);
        assertEquals((area == 0), edge.isCycleAreaZero());
        if (area != 0) assertEquals(centroid, edge.cycleCentroid());
    }

    private static void checkUnorderedEquals(PointD[] a, PointD[] b, boolean equals) {
        assertEquals(a.length, b.length);
        final PointD[] aCopy = Arrays.copyOf(a, a.length);
        final PointD[] bCopy = Arrays.copyOf(b, b.length);

        final PointDComparatorY compare = new PointDComparatorY(0);
        Arrays.sort(aCopy, compare);
        Arrays.sort(bCopy, compare);
        if (equals)
            assertArrayEquals(aCopy, bCopy);
        else
            assertNotEquals(Arrays.asList(aCopy), Arrays.asList(bCopy));
    }
}
