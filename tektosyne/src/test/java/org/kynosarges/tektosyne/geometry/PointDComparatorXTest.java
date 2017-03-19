package org.kynosarges.tektosyne.geometry;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;

/**
 * Provides unit tests for class {@link PointDComparatorX}.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class PointDComparatorXTest {

    private final PointDComparatorX comparer = new PointDComparatorX(0);
    private final PointDComparatorX comparer001 = new PointDComparatorX(0.01);
    private final PointDComparatorX comparer02 = new PointDComparatorX(0.2);
    private final PointDComparatorX comparer05 = new PointDComparatorX(0.5);

    private final static int POINTS_COUNT = 100;
    private final List<PointD> points = new ArrayList<>(POINTS_COUNT);
    private final NavigableSet<PointD> pointSet = new TreeSet<>(comparer);
    private final NavigableSet<PointD> pointSet02 = new TreeSet<>(comparer02);
    private final NavigableMap<PointD, String> pointMap = new TreeMap<>(comparer);
    private final NavigableMap<PointD, String> pointMap02 = new TreeMap<>(comparer02);

    @Test
    public void testCompare() {
        assertEquals(0, comparer.epsilon, 0);
        assertEquals(0, comparer.compare(new PointD(1, 2), new PointD(1, 2)));
        assertEquals(-1, comparer.compare(new PointD(1, 2), new PointD(2, 2)));
        assertEquals(-1, comparer.compare(new PointD(1, 2), new PointD(1, 3)));
        assertEquals(+1, comparer.compare(new PointD(1, 2), new PointD(0, 2)));
        assertEquals(+1, comparer.compare(new PointD(1, 2), new PointD(1, 1)));

        assertEquals(0, comparer001.epsilon, 0.01);
        assertEquals(-1, comparer001.compare(new PointD(1, 2), new PointD(1.1, 2)));
        assertEquals(-1, comparer001.compare(new PointD(1, 2), new PointD(1, 2.1)));
        assertEquals(+1, comparer001.compare(new PointD(1, 2), new PointD(0.9, 2)));
        assertEquals(+1, comparer001.compare(new PointD(1, 2), new PointD(1, 1.9)));

        assertEquals(0, comparer05.epsilon, 0.5);
        assertEquals(0, comparer05.compare(new PointD(1, 2), new PointD(1.1, 2)));
        assertEquals(0, comparer05.compare(new PointD(1, 2), new PointD(1, 2.1)));
        assertEquals(0, comparer05.compare(new PointD(1, 2), new PointD(0.9, 2)));
        assertEquals(0, comparer05.compare(new PointD(1, 2), new PointD(1, 1.9)));
    }

    @Test
    public void testCompareEpsilon() {
        assertEquals(0, comparer.epsilon, 0);
        assertEquals(0, comparer.compareEpsilon(new PointD(1, 2), new PointD(1, 2)));
        assertEquals(-1, comparer.compareEpsilon(new PointD(1, 2), new PointD(2, 2)));
        assertEquals(-1, comparer.compareEpsilon(new PointD(1, 2), new PointD(1, 3)));
        assertEquals(+1, comparer.compareEpsilon(new PointD(1, 2), new PointD(0, 2)));
        assertEquals(+1, comparer.compareEpsilon(new PointD(1, 2), new PointD(1, 1)));

        assertEquals(0, comparer001.epsilon, 0.01);
        assertEquals(-1, comparer001.compareEpsilon(new PointD(1, 2), new PointD(1.1, 2)));
        assertEquals(-1, comparer001.compareEpsilon(new PointD(1, 2), new PointD(1, 2.1)));
        assertEquals(+1, comparer001.compareEpsilon(new PointD(1, 2), new PointD(0.9, 2)));
        assertEquals(+1, comparer001.compareEpsilon(new PointD(1, 2), new PointD(1, 1.9)));

        assertEquals(0, comparer05.epsilon, 0.5);
        assertEquals(0, comparer05.compareEpsilon(new PointD(1, 2), new PointD(1.1, 2)));
        assertEquals(0, comparer05.compareEpsilon(new PointD(1, 2), new PointD(1, 2.1)));
        assertEquals(0, comparer05.compareEpsilon(new PointD(1, 2), new PointD(0.9, 2)));
        assertEquals(0, comparer05.compareEpsilon(new PointD(1, 2), new PointD(1, 1.9)));
    }

    @Test
    public void testCompareEpsilonStatic() {
        assertEquals(-1, PointDComparatorX.compareEpsilon(new PointD(1, 2), new PointD(1.1, 2), 0.01));
        assertEquals(-1, PointDComparatorX.compareEpsilon(new PointD(1, 2), new PointD(1, 2.1), 0.01));
        assertEquals(+1, PointDComparatorX.compareEpsilon(new PointD(1, 2), new PointD(0.9, 2), 0.01));
        assertEquals(+1, PointDComparatorX.compareEpsilon(new PointD(1, 2), new PointD(1, 1.9), 0.01));

        assertEquals(0, PointDComparatorX.compareEpsilon(new PointD(1, 2), new PointD(1.1, 2), 0.5));
        assertEquals(0, PointDComparatorX.compareEpsilon(new PointD(1, 2), new PointD(1, 2.1), 0.5));
        assertEquals(0, PointDComparatorX.compareEpsilon(new PointD(1, 2), new PointD(0.9, 2), 0.5));
        assertEquals(0, PointDComparatorX.compareEpsilon(new PointD(1, 2), new PointD(1, 1.9), 0.5));
    }

    @Test
    public void testCompareExact() {
        assertEquals(0, PointDComparatorX.compareExact(new PointD(1, 2), new PointD(1, 2)));
        assertEquals(-1, PointDComparatorX.compareExact(new PointD(1, 2), new PointD(2, 2)));
        assertEquals(-1, PointDComparatorX.compareExact(new PointD(1, 2), new PointD(1, 3)));
        assertEquals(+1, PointDComparatorX.compareExact(new PointD(1, 2), new PointD(0, 2)));
        assertEquals(+1, PointDComparatorX.compareExact(new PointD(1, 2), new PointD(1, 1)));

        assertEquals(-1, PointDComparatorX.compareExact(new PointD(1, 2), new PointD(2, 1)));
        assertEquals(+1, PointDComparatorX.compareExact(new PointD(2, 1), new PointD(1, 2)));
    }

    @Test
    public void testFindNearest() {
        createPoints(points);
        points.sort(comparer);

        for (int i = 0; i < POINTS_COUNT; i++) {
            PointD q = points.get(i);
            assertEquals(i, comparer.findNearest(points, q));

            q = new PointD(q.x + 0.1, q.y - 0.1);
            assertEquals(i, comparer.findNearest(points, q));
        }
    }

    @Test
    public void testFindNearestEpsilon() {
        createPoints(points);
        points.sort(comparer02);

        for (int i = 0; i < POINTS_COUNT; i++) {
            PointD q = points.get(i);
            assertEquals(i, comparer.findNearest(points, q));

            // equality given epsilon = 0.2
            q = new PointD(q.x + 0.1, q.y - 0.1);
            assertEquals(i, comparer.findNearest(points, q));

            // inequality given epsilon = 0.2
            q = new PointD(q.x - 0.4, q.y + 0.4);
            assertEquals(i, comparer.findNearest(points, q));
        }
    }

    @Test
    public void testFindNearestEpsilonOverlap() {
        points.clear();
        for (int i = 0; i < POINTS_COUNT; i++)
            points.add(new PointD((i % 10) / 10.0, i / 10));

        /*
         * Comparator classifies unequal points (according to equals and hashCode)
         * as equal since epsilon overlaps coordinate distances in both dimensions.
         * List.sort checks for this conditions and throws IllegalArgumentException.
         */
        try {
            points.sort(comparer02);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) { }
    }

    @Test
    public void testFindNearestSet() {
        createPoints(pointSet);

        for (PointD q: pointSet) {
            assertEquals(q, comparer.findNearest(pointSet, q));

            final PointD p = new PointD(q.x + 0.1, q.y - 0.1);
            assertEquals(q, comparer.findNearest(pointSet, p));
        }
    }

    @Test
    public void testFindNearestSetEpsilon() {
        createPoints(pointSet02);

        for (PointD q: pointSet02) {
            assertEquals(q, comparer02.findNearest(pointSet02, q));

            // equality given epsilon = 0.2
            PointD p = new PointD(q.x + 0.1, q.y - 0.1);
            assertEquals(q, comparer02.findNearest(pointSet02, p));

            // inequality given epsilon = 0.2
            p = new PointD(q.x - 0.4, q.y + 0.4);
            assertEquals(q, comparer02.findNearest(pointSet02, p));
        }
    }

    @Test
    public void testFindRange() {
        createPoints(pointMap);
        final NavigableSet<PointD> pointMapKeys = pointMap.navigableKeySet();

        RectD rect = RectD.EMPTY;
        NavigableSet<PointD> range = comparer.findRange(pointMap, rect).navigableKeySet();
        assertEquals(range, comparer.findRange(pointMapKeys, rect));
        assertEquals(1, range.size());
        assertTrue(range.contains(PointD.EMPTY));

        rect = new RectD(-5, -6, -3, -4);
        range = comparer.findRange(pointMap, rect).navigableKeySet();
        assertEquals(range, comparer.findRange(pointMapKeys, rect));
        assertTrue(range.isEmpty());

        rect = new RectD(0, 0, 9, 10);
        range = comparer.findRange(pointMap, rect).navigableKeySet();
        assertEquals(range, comparer.findRange(pointMapKeys, rect));
        assertEquals(pointMapKeys, range);

        rect = new RectD(2, 4, 3, 5);
        range = comparer.findRange(pointMap, rect).navigableKeySet();
        assertEquals(range, comparer.findRange(pointMapKeys, rect));
        assertEquals(4, range.size());
        assertTrue(range.contains(new PointD(2, 4)));
        assertTrue(range.contains(new PointD(3, 4)));
        assertTrue(range.contains(new PointD(2, 5)));
        assertTrue(range.contains(new PointD(3, 5)));
    }

    @Test
    public void testFindRangeEpsilon() {
        createPoints(pointMap02);
        final NavigableSet<PointD> pointMapKeys = pointMap02.navigableKeySet();

        RectD rect = RectD.EMPTY;
        NavigableSet<PointD> range = comparer02.findRange(pointMap02, rect).navigableKeySet();
        assertEquals(range, comparer02.findRange(pointMapKeys, rect));
        assertEquals(1, range.size());
        assertTrue(range.contains(PointD.EMPTY));

        rect = new RectD(-5, -6, -0.3, -0.3);
        range = comparer02.findRange(pointMap02, rect).navigableKeySet();
        assertEquals(range, comparer02.findRange(pointMapKeys, rect));
        assertTrue(range.isEmpty());

        rect = new RectD(0, 0, 9, 10);
        range = comparer02.findRange(pointMap02, rect).navigableKeySet();
        assertEquals(range, comparer02.findRange(pointMapKeys, rect));
        assertEquals(pointMapKeys, range);

        rect = new RectD(0.1, 0.1, 8.9, 9.9);
        range = comparer02.findRange(pointMap02, rect).navigableKeySet();
        assertEquals(range, comparer02.findRange(pointMapKeys, rect));
        assertEquals(pointMapKeys, range);

        rect = new RectD(2.1, 4.1, 2.9, 4.9);
        range = comparer02.findRange(pointMap02, rect).navigableKeySet();
        assertEquals(range, comparer02.findRange(pointMapKeys, rect));
        assertEquals(4, range.size());
        assertTrue(range.contains(new PointD(2, 4)));
        assertTrue(range.contains(new PointD(3, 4)));
        assertTrue(range.contains(new PointD(2, 5)));
        assertTrue(range.contains(new PointD(3, 5)));
    }

    private static void createPoints(List<PointD> list) {
        list.clear();
        for (int i = 0; i < POINTS_COUNT; i++)
            list.add(new PointD(i % 10, i / 10));
    }

    private static void createPoints(NavigableSet<PointD> set) {
        set.clear();
        for (int i = 0; i < POINTS_COUNT; i++)
            set.add(new PointD(i % 10, i / 10));
    }

    private static void createPoints(NavigableMap<PointD, String> map) {
        map.clear();
        for (int i = 0; i < POINTS_COUNT; i++) {
            final PointD key = new PointD(i % 10, i / 10);
            map.put(key, key.toString());
        }
    }
}
