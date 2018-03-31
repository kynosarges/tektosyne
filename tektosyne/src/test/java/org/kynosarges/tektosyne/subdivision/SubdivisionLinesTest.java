package org.kynosarges.tektosyne.subdivision;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;
import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides unit tests for {@link Subdivision} and related classes.
 * Also provides factory methods to create {@link Subdivision} instances from line collections.
 * 
 * @author Christoph Nahr
 * @version 6.2.0
 */
public class SubdivisionLinesTest {
    /*
     *                  e2→
     *      (-1,+2)------------(+1,+2)
     *                 ←e3
     *
     *
     *
     *                  e0→
     *      (-1,-2)------------(+1,-2)
     *                 ←e1
     */
    @Test
    public void fromLinesHorizontal() {
        final PointD[] points = {
            new PointD(-1, -2), new PointD(-1, 2), new PointD(1, -2), new PointD(1, 2)
        };
        final LineD[] lines = {
            new LineD(points[0], points[2]), new LineD(points[1], points[3])
        };
        final Subdivision division = Subdivision.fromLines(lines, 0);
        division.validate();

        assertArrayEquals(lines, division.toLines());
        final SubdivisionEdge[] edges = division.edges().values().toArray(new SubdivisionEdge[4]);
        final SubdivisionFace[] faces = division.faces().values().toArray(new SubdivisionFace[1]);

        final TreeMap<PointD, SubdivisionEdge> vertices = new TreeMap<>(division.vertices().comparator());
        vertices.put(points[0], edges[0]);
        vertices.put(points[2], edges[1]);
        vertices.put(points[1], edges[2]);
        vertices.put(points[3], edges[3]);
        assertEquals(vertices, division.vertices());

        assertArrayEquals(new SubdivisionEdge[] {
            new SubdivisionEdge(0, points[0], edges[1], faces[0], edges[1], edges[1]),
            new SubdivisionEdge(1, points[2], edges[0], faces[0], edges[0], edges[0]),
            new SubdivisionEdge(2, points[1], edges[3], faces[0], edges[3], edges[3]),
            new SubdivisionEdge(3, points[3], edges[2], faces[0], edges[2], edges[2]),
        }, edges);

        assertArrayEquals(new SubdivisionFace[] {
            new SubdivisionFace(division, 0, null, Arrays.asList(edges[0], edges[2])),
        }, faces);

        checkFace(edges[0], new PointD[] { points[0], points[2] }, 0, PointD.EMPTY);
        checkFace(edges[2], new PointD[] { points[1], points[3] }, 0, PointD.EMPTY);

        final List<SubdivisionEdge> cycles = division.getZeroAreaCycles();
        assertEquals(2, cycles.size());
        assertEquals(edges[0], cycles.get(0));
        assertEquals(edges[2], cycles.get(1));
    }

    /*
     *      (-1,+2)            (+1,+2)
     *         |                  |
     *         |                  |
     *     e0↑ | ↓e1          e2↑ | ↓e3
     *         |                  |
     *         |                  |
     *      (-1,-2)            (+1,-2)
     */

    @Test
    public void fromLinesVertical() {
        final PointD[] points = {
            new PointD(-1, -2), new PointD(-1, 2), new PointD(1, -2), new PointD(1, 2)
        };
        final LineD[] lines = {
            new LineD(points[0], points[1]), new LineD(points[2], points[3])
        };
        final Subdivision division = Subdivision.fromLines(lines, 0);
        division.validate();

        assertArrayEquals(lines, division.toLines());
        final SubdivisionEdge[] edges = division.edges().values().toArray(new SubdivisionEdge[4]);
        final SubdivisionFace[] faces = division.faces().values().toArray(new SubdivisionFace[1]);

        final TreeMap<PointD, SubdivisionEdge> vertices = new TreeMap<>(division.vertices().comparator());
        vertices.put(points[0], edges[0]);
        vertices.put(points[2], edges[2]);
        vertices.put(points[1], edges[1]);
        vertices.put(points[3], edges[3]);
        assertEquals(vertices, division.vertices());

        assertArrayEquals(new SubdivisionEdge[] {
            new SubdivisionEdge(0, points[0], edges[1], faces[0], edges[1], edges[1]),
            new SubdivisionEdge(1, points[1], edges[0], faces[0], edges[0], edges[0]),
            new SubdivisionEdge(2, points[2], edges[3], faces[0], edges[3], edges[3]),
            new SubdivisionEdge(3, points[3], edges[2], faces[0], edges[2], edges[2]),
        }, edges);

        assertArrayEquals(new SubdivisionFace[] {
            new SubdivisionFace(division, 0, null, Arrays.asList(edges[0], edges[2])),
        }, faces);

        checkFace(edges[0], new PointD[] { points[0], points[1] }, 0, PointD.EMPTY);
        checkFace(edges[2], new PointD[] { points[2], points[3] }, 0, PointD.EMPTY);

        final List<SubdivisionEdge> cycles = division.getZeroAreaCycles();
        assertEquals(2, cycles.size());
        assertEquals(edges[0], cycles.get(0));
        assertEquals(edges[2], cycles.get(1));
    }

    /*
     *                  e2→
     *      (-1,+2)------------(+1,+2)
     *         |       ←e3        |
     *         |                  |
     *     e0↑ | ↓e1          e4↑ | ↓e5
     *         |                  |
     *         |        e6→       |
     *      (-1,-2)------------(+1,-2)
     *                 ←e7
     */

    @Test
    public void fromLinesSquare() {
        createSquare(true);
    }

    public static Subdivision createSquare(boolean test) {
        final PointD[] points = {
                new PointD(-1, -2), new PointD(-1, 2), new PointD(1, 2), new PointD(1, -2)
        };
        final LineD[] lines = {
                new LineD(points[0], points[1]), new LineD(points[1], points[2]),
                new LineD(points[3], points[2]), new LineD(points[0], points[3])
        };
        final Subdivision division = Subdivision.fromLines(lines, 0);
        division.validate();
        if (!test) return division;

        assertArrayEquals(lines, division.toLines());
        checkLinesSquare(points, division);
        return division;
    }

    @Test
    public void fromLinesSquareEpsilon() {
        final PointD[] points = {
            new PointD(-1, -2), new PointD(-1, 2), new PointD(1, 2), new PointD(1, -2)
        };
        final LineD[] lines = {
            new LineD(points[0], points[1]),
            new LineD(new PointD(-1.1, 1.9), points[2]),
            new LineD(points[3], new PointD(0.9, 2.1)),
            new LineD(new PointD(-0.9, -2.1), new PointD(1.1, -1.9))
        };
        final Subdivision division = Subdivision.fromLines(lines, 0.2);
        division.validate();

        assertNotEquals(Arrays.asList(lines), Arrays.asList(division.toLines()));
        checkLinesSquare(points, division);
    }

    private static void checkLinesSquare(PointD[] points, Subdivision division) {
        final SubdivisionEdge[] edges = division.edges().values().toArray(new SubdivisionEdge[8]);
        final SubdivisionFace[] faces = division.faces().values().toArray(new SubdivisionFace[2]);

        final TreeMap<PointD, SubdivisionEdge> vertices = new TreeMap<>(division.vertices().comparator());
        vertices.put(points[0], edges[0]);
        vertices.put(points[3], edges[4]);
        vertices.put(points[1], edges[1]);
        vertices.put(points[2], edges[3]);
        assertEquals(vertices, division.vertices());

        assertArrayEquals(new SubdivisionEdge[] {
            new SubdivisionEdge(0, points[0], edges[1], faces[0], edges[2], edges[7]),
            new SubdivisionEdge(1, points[1], edges[0], faces[1], edges[6], edges[3]),
            new SubdivisionEdge(2, points[1], edges[3], faces[0], edges[5], edges[0]),
            new SubdivisionEdge(3, points[2], edges[2], faces[1], edges[1], edges[4]),
            new SubdivisionEdge(4, points[3], edges[5], faces[1], edges[3], edges[6]),
            new SubdivisionEdge(5, points[2], edges[4], faces[0], edges[7], edges[2]),
            new SubdivisionEdge(6, points[0], edges[7], faces[1], edges[4], edges[1]),
            new SubdivisionEdge(7, points[3], edges[6], faces[0], edges[0], edges[5]),
        }, edges);

        assertArrayEquals(new SubdivisionFace[] {
            new SubdivisionFace(division, 0, null, Collections.singletonList(edges[0])),
            new SubdivisionFace(division, 1, edges[1], null),
        }, faces);

        checkFace(edges[0], points, -8, PointD.EMPTY);
        Collections.reverse(Arrays.asList(points));
        checkFace(edges[4], points, +8, PointD.EMPTY);

        final List<SubdivisionEdge> cycles = division.getZeroAreaCycles();
        assertEquals(0, cycles.size());
    }

    /*
     *      (-1,+2)            (+1,+2)
     *            \            /
     *             \ ↓e3  e4↑ /
     *          e2↑ \        / ↓e5
     *               \      /
     *                (0, 0)
     *               /      \      
     *          e0↑ /        \ ↓e7
     *             / ↓e1  e6↑ \    
     *            /            \   
     *      (-1,-2)            (+1,-2)
     */

    @Test
    public void fromLinesStar() {
        createStar(true);
    }

    public static Subdivision createStar(boolean test) {
        final PointD[] points = {
                new PointD(-1, -2), new PointD(0, 0),
                new PointD(-1, 2), new PointD(1, 2), new PointD(1, -2)
        };
        final LineD[] lines = {
                new LineD(points[0], points[1]), new LineD(points[1], points[2]),
                new LineD(points[1], points[3]), new LineD(points[4], points[1])
        };
        final Subdivision division = Subdivision.fromLines(lines, 0);
        division.validate();
        if (!test) return division;

        assertArrayEquals(lines, division.toLines());
        checkLinesStar(points, division);
        return division;
    }

    @Test
    public void fromLinesStarEpsilon() {
        final PointD[] points = {
            new PointD(-1, -2), new PointD(0, 0),
            new PointD(-1, 2), new PointD(1, 2), new PointD(1, -2)
        };
        final LineD[] lines = {
            new LineD(points[0], points[1]),
            new LineD(new PointD(0.1, -0.1), points[2]),
            new LineD(new PointD(-0.1, 0.1), points[3]),
            new LineD(points[4], new PointD(0.1, 0.1))
        };
        final Subdivision division = Subdivision.fromLines(lines, 0.2);
        division.validate();

        assertNotEquals(Arrays.asList(lines), Arrays.asList(division.toLines()));
        checkLinesStar(points, division);
    }

    private static void checkLinesStar(PointD[] points, Subdivision division) {
        final SubdivisionEdge[] edges = division.edges().values().toArray(new SubdivisionEdge[8]);
        final SubdivisionFace[] faces = division.faces().values().toArray(new SubdivisionFace[1]);

        final TreeMap<PointD, SubdivisionEdge> vertices = new TreeMap<>(division.vertices().comparator());
        vertices.put(points[0], edges[0]);
        vertices.put(points[4], edges[6]);
        vertices.put(points[1], edges[1]);
        vertices.put(points[2], edges[3]);
        vertices.put(points[3], edges[5]);
        assertEquals(vertices, division.vertices());

        assertArrayEquals(new SubdivisionEdge[] {
            new SubdivisionEdge(0, points[0], edges[1], faces[0], edges[2], edges[1]),
            new SubdivisionEdge(1, points[1], edges[0], faces[0], edges[0], edges[6]),
            new SubdivisionEdge(2, points[1], edges[3], faces[0], edges[3], edges[0]),
            new SubdivisionEdge(3, points[2], edges[2], faces[0], edges[4], edges[2]),
            new SubdivisionEdge(4, points[1], edges[5], faces[0], edges[5], edges[3]),
            new SubdivisionEdge(5, points[3], edges[4], faces[0], edges[7], edges[4]),
            new SubdivisionEdge(6, points[4], edges[7], faces[0], edges[1], edges[7]),
            new SubdivisionEdge(7, points[1], edges[6], faces[0], edges[6], edges[5]),
        }, edges);

        assertArrayEquals(new SubdivisionFace[] {
            new SubdivisionFace(division, 0, null, Collections.singletonList(edges[0])),
        }, faces);

        checkFace(edges[0], new PointD[] {
            points[0], points[1], points[2], points[1],
            points[3], points[1], points[4], points[1]
        }, 0, PointD.EMPTY);

        final List<SubdivisionEdge> cycles = division.getZeroAreaCycles();
        assertEquals(1, cycles.size());
        assertEquals(edges[0], cycles.get(0));
    }

    /*
     *                      e2→
     *      (-1,+2)--------------------(+1,+2)
     *         |  \        ←e3         /  |
     *         |   \↓e11          e12↑/   |
     *         |    e10↑\   f2   /↓e13    |
     *         |         \      /         |
     *     e0↑ | ↓e1  f1  (0, 0)  f3  e4↑ | ↓e5
     *         |         /      \         |
     *         |    e9↑_/   f4   \↓e14    |
     *         |   /↓e8           e15↑\   |
     *         |  /         e6→        \  |
     *      (-1,-2)--------------------(+1,-2)
     *                     ←e7
     */

    @Test
    public void fromLinesSquareStar() {
        createSquareStar(true);
    }

    public static Subdivision createSquareStar(boolean test) {
        final PointD[] points = {
            new PointD(), new PointD(-1, -2), new PointD(-1, 2), new PointD(1, 2), new PointD(1, -2) 
        };
        final LineD[] lines = {
            new LineD(points[1], points[2]), new LineD(points[2], points[3]),
            new LineD(points[4], points[3]), new LineD(points[1], points[4]),
            new LineD(points[0], points[1]), new LineD(points[0], points[2]),
            new LineD(points[0], points[3]), new LineD(points[0], points[4])
        };
        final Subdivision division = Subdivision.fromLines(lines, 0);
        division.validate();
        if (!test) return division;

        final SubdivisionEdge[] edges = division.edges().values().toArray(new SubdivisionEdge[16]);
        final SubdivisionFace[] faces = division.faces().values().toArray(new SubdivisionFace[5]);
        assertEquals(16, edges.length);
        assertEquals(5, faces.length);

        assertArrayEquals(new SubdivisionEdge[] {
            new SubdivisionEdge(0, points[1], edges[1], faces[0], edges[2], edges[7]),
            new SubdivisionEdge(1, points[2], edges[0], faces[1], edges[9], edges[10]),
            new SubdivisionEdge(2, points[2], edges[3], faces[0], edges[5], edges[0]),
            new SubdivisionEdge(3, points[3], edges[2], faces[2], edges[11], edges[12]),
            new SubdivisionEdge(4, points[4], edges[5], faces[3], edges[13], edges[14]),
            new SubdivisionEdge(5, points[3], edges[4], faces[0], edges[7], edges[2]),
            new SubdivisionEdge(6, points[1], edges[7], faces[4], edges[15], edges[8]),
            new SubdivisionEdge(7, points[4], edges[6], faces[0], edges[0], edges[5]),

            new SubdivisionEdge( 8, points[0], edges[9], faces[4], edges[6], edges[15]),
            new SubdivisionEdge( 9, points[1], edges[8], faces[1], edges[10], edges[1]),
            new SubdivisionEdge(10, points[0], edges[11], faces[1], edges[1], edges[9]),
            new SubdivisionEdge(11, points[2], edges[10], faces[2], edges[12], edges[3]),
            new SubdivisionEdge(12, points[0], edges[13], faces[2], edges[3], edges[11]),
            new SubdivisionEdge(13, points[3], edges[12], faces[3], edges[14], edges[4]),
            new SubdivisionEdge(14, points[0], edges[15], faces[3], edges[4], edges[13]),
            new SubdivisionEdge(15, points[4], edges[14], faces[4], edges[8], edges[6]),
        }, edges);


        final List<SubdivisionEdge> cycles = division.getZeroAreaCycles();
        assertEquals(0, cycles.size());
        return division;
    }

    /*
     *                 ----(0,+6)----
     *                /              \      
     *           e0↑ / ↓e1        e2↑ \ ↓e3
     *              /                  \   
     *             /         e10→       \
     *            / (-1,+2)------(+1,+2) \
     *           /    \     ←e11     /    \ 
     *          /      \            /      \
     *         /    e6↑ \ ↓e7  ↑e8 / e9↓    \
     *        /          \        /          \
     *       /            -(0, 0)-            \
     *      /                                  \ 
     *      |                e4→               |  
     *      (-5,-4)----------------------(+5,-4)
     *                      ←e5
     */

    @Test
    public void fromLinesTriforce() {
        createTriforce(true);
    }

    public static Subdivision createTriforce(boolean test) {
        final PointD[] points = {
            new PointD(-5, -4), new PointD(0, 6), new PointD(5, -4),
            new PointD(0, 0), new PointD(-1, 2), new PointD(1, 2)
        };
        final LineD[] lines = {
            new LineD(points[0], points[1]), new LineD(points[2], points[1]),
            new LineD(points[0], points[2]), new LineD(points[3], points[4]),
            new LineD(points[3], points[5]), new LineD(points[4], points[5])
        };

        final Subdivision division = Subdivision.fromLines(lines, 0);
        division.validate();
        if (!test) return division;

        assertArrayEquals(lines, division.toLines());
        final SubdivisionEdge[] edges = division.edges().values().toArray(new SubdivisionEdge[12]);
        final SubdivisionFace[] faces = division.faces().values().toArray(new SubdivisionFace[3]);

        final TreeMap<PointD, SubdivisionEdge> vertices = new TreeMap<>(division.vertices().comparator());
        vertices.put(points[0], edges[0]);
        vertices.put(points[2], edges[2]);
        vertices.put(points[3], edges[6]);
        vertices.put(points[4], edges[7]);
        vertices.put(points[5], edges[9]);
        vertices.put(points[1], edges[1]);
        assertEquals(vertices, division.vertices());

        assertArrayEquals(new SubdivisionEdge[] {
            new SubdivisionEdge(0, points[0], edges[1], faces[0], edges[3], edges[5]),
            new SubdivisionEdge(1, points[1], edges[0], faces[1], edges[4], edges[2]),
            new SubdivisionEdge(2, points[2], edges[3], faces[1], edges[1], edges[4]),
            new SubdivisionEdge(3, points[1], edges[2], faces[0], edges[5], edges[0]),
            new SubdivisionEdge(4, points[0], edges[5], faces[1], edges[2], edges[1]),
            new SubdivisionEdge(5, points[2], edges[4], faces[0], edges[0], edges[3]),
            new SubdivisionEdge(6, points[3], edges[7], faces[1], edges[10], edges[9]),
            new SubdivisionEdge(7, points[4], edges[6], faces[2], edges[8], edges[11]),
            new SubdivisionEdge(8, points[3], edges[9], faces[2], edges[11], edges[7]),
            new SubdivisionEdge(9, points[5], edges[8], faces[1], edges[6], edges[10]),
            new SubdivisionEdge(10, points[4], edges[11], faces[1], edges[9], edges[6]),
            new SubdivisionEdge(11, points[5], edges[10], faces[2], edges[7], edges[8]),
        }, edges);

        assertArrayEquals(new SubdivisionFace[] {
            new SubdivisionFace(division, 0, null, Collections.singletonList(edges[0])),
            new SubdivisionFace(division, 1, edges[1], Collections.singletonList(edges[6])),
            new SubdivisionFace(division, 2, edges[7], null),
        }, faces);

        PointD centroid = new PointD(0, -2 / 3.0);
        checkFace(edges[0], new PointD[] { points[0], points[1], points[2] }, -50, centroid);
        checkFace(edges[1], new PointD[] { points[1], points[0], points[2] }, +50, centroid);

        centroid = new PointD(0, 4 / 3.0);
        checkFace(edges[6], new PointD[] { points[3], points[4], points[5] }, -2, centroid);
        checkFace(edges[7], new PointD[] { points[4], points[3], points[5] }, +2, centroid);

        final List<SubdivisionEdge> cycles = division.getZeroAreaCycles();
        assertEquals(0, cycles.size());
        return division;
    }

    /*
     *              -----(0,+4)-----
     *             /      /  \      \
     *        e2↑ / ↓e3  /    \  e14↑\ ↓e15
     *           /      /      \      \
     *          /      /        \      \
     *         /       |        |       \
     *        /    e6↑ |↓e7 e10↑| ↓e11   \
     *       /         |        |         \
     *    (-6,0)    (-3,0)    (+3,0)    (+6,0)
     *       \         |        |         /
     *        \    e4↑ |↓e5  e8↑| ↓e9    /
     *         \       |        |       /
     *          \      \        /      /
     *           \      \      /      /
     *        e0↑ \ ↓e1  \    /  e12↑/ ↓e13
     *             \      \  /      / 
     *              -----(0,-4)-----
     */

    @Test
    public void fromLinesDiamond() {
        createLinesDiamond(true);
    }

    public static Subdivision createLinesDiamond(boolean test) {
        final PointD[] points = {
                new PointD(0, -4), new PointD(-6, 0), new PointD(-3, 0),
                new PointD(3, 0), new PointD(6, 0), new PointD(0, 4)
        };
        final LineD[] lines = {
                new LineD(points[0], points[1]), new LineD(points[1], points[5]),
                new LineD(points[0], points[2]), new LineD(points[2], points[5]),
                new LineD(points[0], points[3]), new LineD(points[3], points[5]),
                new LineD(points[0], points[4]), new LineD(points[4], points[5])
        };
        final Subdivision division = Subdivision.fromLines(lines, 0);
        division.validate();
        if (!test) return division;

        assertArrayEquals(lines, division.toLines());
        final SubdivisionEdge[] edges = division.edges().values().toArray(new SubdivisionEdge[16]);
        final SubdivisionFace[] faces = division.faces().values().toArray(new SubdivisionFace[4]);

        final TreeMap<PointD, SubdivisionEdge> vertices = new TreeMap<>(division.vertices().comparator());
        vertices.put(points[0], edges[0]);
        vertices.put(points[1], edges[1]);
        vertices.put(points[2], edges[5]);
        vertices.put(points[3], edges[9]);
        vertices.put(points[4], edges[13]);
        vertices.put(points[5], edges[3]);
        assertEquals(vertices, division.vertices());

        assertArrayEquals(new SubdivisionEdge[] {
                new SubdivisionEdge(0, points[0], edges[1], faces[0], edges[2], edges[13]),
                new SubdivisionEdge(1, points[1], edges[0], faces[1], edges[4], edges[3]),
                new SubdivisionEdge(2, points[1], edges[3], faces[0], edges[15], edges[0]),
                new SubdivisionEdge(3, points[5], edges[2], faces[1], edges[1], edges[6]),
                new SubdivisionEdge(4, points[0], edges[5], faces[1], edges[6], edges[1]),
                new SubdivisionEdge(5, points[2], edges[4], faces[2], edges[8], edges[7]),
                new SubdivisionEdge(6, points[2], edges[7], faces[1], edges[3], edges[4]),
                new SubdivisionEdge(7, points[5], edges[6], faces[2], edges[5], edges[10]),
                new SubdivisionEdge(8, points[0], edges[9], faces[2], edges[10], edges[5]),
                new SubdivisionEdge(9, points[3], edges[8], faces[3], edges[12], edges[11]),
                new SubdivisionEdge(10, points[3], edges[11], faces[2], edges[7], edges[8]),
                new SubdivisionEdge(11, points[5], edges[10], faces[3], edges[9], edges[14]),
                new SubdivisionEdge(12, points[0], edges[13], faces[3], edges[14], edges[9]),
                new SubdivisionEdge(13, points[4], edges[12], faces[0], edges[0], edges[15]),
                new SubdivisionEdge(14, points[4], edges[15], faces[3], edges[11], edges[12]),
                new SubdivisionEdge(15, points[5], edges[14], faces[0], edges[13], edges[2]),
        }, edges);

        assertArrayEquals(new SubdivisionFace[] {
                new SubdivisionFace(division, 0, null, Collections.singletonList(edges[0])),
                new SubdivisionFace(division, 1, edges[1], null),
                new SubdivisionFace(division, 2, edges[5], null),
                new SubdivisionFace(division, 3, edges[9], null),
        }, faces);

        checkFace(edges[0], new PointD[] { points[0], points[1], points[5], points[4] }, -48, PointD.EMPTY);
        checkFace(edges[1], new PointD[] { points[1], points[0], points[2], points[5] }, +12, new PointD(-3, 0));
        checkFace(edges[5], new PointD[] { points[2], points[0], points[3], points[5] }, +24, PointD.EMPTY);
        checkFace(edges[9], new PointD[] { points[3], points[0], points[4], points[5] }, +12, new PointD(3, 0));

        final List<SubdivisionEdge> cycles = division.getZeroAreaCycles();
        assertEquals(0, cycles.size());
        return division;
    }

    private static void checkFace(
        SubdivisionEdge edge, PointD[] polygon, double area, PointD centroid) {

        assertArrayEquals(polygon, edge.cyclePolygon());
        assertEquals(area, edge.cycleArea(), 0);
        assertEquals((area == 0), edge.isCycleAreaZero());
        if (area != 0) assertEquals(centroid, edge.cycleCentroid());
    }
}
