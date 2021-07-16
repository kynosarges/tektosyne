package org.kynosarges.tektosyne.geometry;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;

/**
 * Provides unit tests for class {@link MultiLineIntersection}.
 * @author Christoph Nahr
 * @version 6.3.1
 */
public class MultiLineIntersectionTest {

    @Test
    public void testCentral() {
        final LineD[] lines = new LineD[100];
        for (int i = 0; i < lines.length; i++)
            lines[i] = new LineD(i * 10, 0, 1000 - i * 10, 1000);

        final MultiLinePoint[] results = findBoth(lines);
        assertEquals(1, results.length);
        assertEquals(lines.length, results[0].lines.length);
        assertEquals(new PointD(500, 500), results[0].shared);

        for (MultiLinePoint.Line line: results[0].lines)
            assertEquals(LineLocation.BETWEEN, line.location);
    }

    @Test
    public void testCollinear() {
        LineD[] lines = new LineD[] {
            new LineD(0, 0, 3, 0), new LineD(1, 0, 4, 0),
            new LineD(0, 1, 0, 3), new LineD(0, 2, 0, 4),
            new LineD(3, 3, 1, 1), new LineD(2, 2, 4, 4),
        };

        MultiLinePoint[] results = findBoth(lines);
        assertEquals(3, results.length);

        LineD[] sharedEndLines = new LineD[] {
            new LineD(0, 0, 3, 0), new LineD(1, 0, 3, 0),
            new LineD(0, 1, 0, 3), new LineD(0, 2, 0, 3),
            new LineD(3, 3, 1, 1), new LineD(2, 2, 3, 3),
        };

        MultiLinePoint[] sharedEndResults = findBoth(sharedEndLines);
        compareResults(results, sharedEndResults);

        MultiLinePoint result = results[0];
        assertEquals(2, result.lines.length);
        assertEquals(new PointD(1, 0), result.shared);
        assertEquals(0, result.lines[0].index);
        assertEquals(LineLocation.BETWEEN, result.lines[0].location);
        assertEquals(1, result.lines[1].index);
        assertEquals(LineLocation.START, result.lines[1].location);

        result = results[1];
        assertEquals(2, result.lines.length);
        assertEquals(new PointD(0, 2), result.shared);
        assertEquals(2, result.lines[0].index);
        assertEquals(LineLocation.BETWEEN, result.lines[0].location);
        assertEquals(3, result.lines[1].index);
        assertEquals(LineLocation.START, result.lines[1].location);

        result = results[2];
        assertEquals(2, result.lines.length);
        assertEquals(new PointD(2, 2), result.shared);
        assertEquals(4, result.lines[0].index);
        assertEquals(LineLocation.BETWEEN, result.lines[0].location);
        assertEquals(5, result.lines[1].index);
        assertEquals(LineLocation.START, result.lines[1].location);
    }

    @Test
    public void testCongruent() {
        LineD[] lines = new LineD[] {
            new LineD(0, 0, 3, 0), new LineD(0, 0, 4, 0),
            new LineD(0, 1, 0, 3), new LineD(0, 4, 0, 1),
            new LineD(3, 3, 1, 1), new LineD(4, 4, 1, 1),
        };

        MultiLinePoint[] results = findBoth(lines);
        assertEquals(3, results.length);

        LineD[] sharedEndLines = new LineD[] {
            new LineD(0, 0, 3, 0), new LineD(0, 0, 3, 0),
            new LineD(0, 1, 0, 3), new LineD(0, 3, 0, 1),
            new LineD(3, 3, 1, 1), new LineD(3, 3, 1, 1),
        };

        MultiLinePoint[] sharedEndResults = findBoth(sharedEndLines);
        compareResults(results, sharedEndResults);

        MultiLinePoint result = results[0];
        assertEquals(2, result.lines.length);
        assertEquals(new PointD(0, 0), result.shared);
        assertEquals(0, result.lines[0].index);
        assertEquals(LineLocation.START, result.lines[0].location);
        assertEquals(1, result.lines[1].index);
        assertEquals(LineLocation.START, result.lines[1].location);

        result = results[1];
        assertEquals(2, result.lines.length);
        assertEquals(new PointD(0, 1), result.shared);
        assertEquals(2, result.lines[0].index);
        assertEquals(LineLocation.START, result.lines[0].location);
        assertEquals(3, result.lines[1].index);
        assertEquals(LineLocation.END, result.lines[1].location);

        result = results[2];
        assertEquals(2, result.lines.length);
        assertEquals(new PointD(1, 1), result.shared);
        assertEquals(4, result.lines[0].index);
        assertEquals(LineLocation.END, result.lines[0].location);
        assertEquals(5, result.lines[1].index);
        assertEquals(LineLocation.END, result.lines[1].location);
    }

    @Test
    public void testCongruentShared() {
        LineD[] lines = new LineD[] {
            new LineD(3, 3, 0, 0), new LineD(0, 0, 3, 3),
            new LineD(0, 1, 3, 3), new LineD(3, 3, 0, 1)
        };

        MultiLinePoint[] results = findBoth(lines);
        assertEquals(3, results.length);

        MultiLinePoint result = results[0];
        assertEquals(2, result.lines.length);
        assertEquals(new PointD(0, 0), result.shared);
        assertEquals(0, result.lines[0].index);
        assertEquals(LineLocation.END, result.lines[0].location);
        assertEquals(1, result.lines[1].index);
        assertEquals(LineLocation.START, result.lines[1].location);

        result = results[1];
        assertEquals(2, result.lines.length);
        assertEquals(new PointD(0, 1), result.shared);
        assertEquals(2, result.lines[0].index);
        assertEquals(LineLocation.START, result.lines[0].location);
        assertEquals(3, result.lines[1].index);
        assertEquals(LineLocation.END, result.lines[1].location);

        result = results[2];
        assertEquals(4, result.lines.length);
        assertEquals(new PointD(3, 3), result.shared);
        assertEquals(0, result.lines[0].index);
        assertEquals(LineLocation.START, result.lines[0].location);
        assertEquals(1, result.lines[1].index);
        assertEquals(LineLocation.END, result.lines[1].location);
        assertEquals(2, result.lines[2].index);
        assertEquals(LineLocation.END, result.lines[2].location);
        assertEquals(3, result.lines[3].index);
        assertEquals(LineLocation.START, result.lines[3].location);
    }

    @Test
    public void testEmpty() {
        LineD[] lines = new LineD[] { };
        MultiLinePoint[] results = findBoth(lines);
        assertEquals(0, results.length);

        lines = new LineD[] { new LineD(1, 2, 3, 4) };
        results = findBoth(lines);
        assertEquals(0, results.length);
    }

    @Test
    public void testStartEnd() {
        LineD[] lines = new LineD[] {
            new LineD(0, 0, 5, 0), new LineD(5, 0, 5, 5),
            new LineD(5, 5, 0, 5), new LineD(0, 5, 0, 0)
        };

        MultiLinePoint[] results = findBoth(lines);
        assertEquals(4, results.length);
        for (MultiLinePoint result: results)
            assertEquals(2, result.lines.length);

        MultiLinePoint result = results[0];
        assertEquals(new PointD(0, 0), result.shared);
        assertEquals(0, result.lines[0].index);
        assertEquals(LineLocation.START, result.lines[0].location);
        assertEquals(3, result.lines[1].index);
        assertEquals(LineLocation.END, result.lines[1].location);

        result = results[1];
        assertEquals(new PointD(5, 0), result.shared);
        assertEquals(0, result.lines[0].index);
        assertEquals(LineLocation.END, result.lines[0].location);
        assertEquals(1, result.lines[1].index);
        assertEquals(LineLocation.START, result.lines[1].location);

        result = results[2];
        assertEquals(new PointD(0, 5), result.shared);
        assertEquals(2, result.lines[0].index);
        assertEquals(LineLocation.END, result.lines[0].location);
        assertEquals(3, result.lines[1].index);
        assertEquals(LineLocation.START, result.lines[1].location);

        result = results[3];
        assertEquals(new PointD(5, 5), result.shared);
        assertEquals(1, result.lines[0].index);
        assertEquals(LineLocation.END, result.lines[0].location);
        assertEquals(2, result.lines[1].index);
        assertEquals(LineLocation.START, result.lines[1].location);
    }

    @Test
    public void testStartBetween() {
        LineD[] lines = new LineD[] {
            new LineD(0, 0, 6, 0), new LineD(5, 0, 5, 6),
            new LineD(5, 5, -1, 5), new LineD(0, 5, 0, -1)
        };

        MultiLinePoint[] results = findBoth(lines);
        assertEquals(4, results.length);
        for (MultiLinePoint result: results)
            assertEquals(2, result.lines.length);

        MultiLinePoint result = results[0];
        assertEquals(new PointD(0, 0), result.shared);
        assertEquals(0, result.lines[0].index);
        assertEquals(LineLocation.START, result.lines[0].location);
        assertEquals(3, result.lines[1].index);
        assertEquals(LineLocation.BETWEEN, result.lines[1].location);

        result = results[1];
        assertEquals(new PointD(5, 0), result.shared);
        assertEquals(0, result.lines[0].index);
        assertEquals(LineLocation.BETWEEN, result.lines[0].location);
        assertEquals(1, result.lines[1].index);
        assertEquals(LineLocation.START, result.lines[1].location);

        result = results[2];
        assertEquals(new PointD(0, 5), result.shared);
        assertEquals(2, result.lines[0].index);
        assertEquals(LineLocation.BETWEEN, result.lines[0].location);
        assertEquals(3, result.lines[1].index);
        assertEquals(LineLocation.START, result.lines[1].location);

        result = results[3];
        assertEquals(new PointD(5, 5), result.shared);
        assertEquals(1, result.lines[0].index);
        assertEquals(LineLocation.BETWEEN, result.lines[0].location);
        assertEquals(2, result.lines[1].index);
        assertEquals(LineLocation.START, result.lines[1].location);
    }

    @Test
    public void testEndBetween() {
        LineD[] lines = new LineD[] {
            new LineD(6, 0, 0, 0), new LineD(5, 6, 5, 0),
            new LineD(-1, 5, 5, 5), new LineD(0, -1, 0, 5)
        };

        MultiLinePoint[] results = findBoth(lines);
        assertEquals(4, results.length);
        for (MultiLinePoint result: results)
            assertEquals(2, result.lines.length);

        MultiLinePoint result = results[0];
        assertEquals(new PointD(0, 0), result.shared);
        assertEquals(0, result.lines[0].index);
        assertEquals(LineLocation.END, result.lines[0].location);
        assertEquals(3, result.lines[1].index);
        assertEquals(LineLocation.BETWEEN, result.lines[1].location);

        result = results[1];
        assertEquals(new PointD(5, 0), result.shared);
        assertEquals(0, result.lines[0].index);
        assertEquals(LineLocation.BETWEEN, result.lines[0].location);
        assertEquals(1, result.lines[1].index);
        assertEquals(LineLocation.END, result.lines[1].location);

        result = results[2];
        assertEquals(new PointD(0, 5), result.shared);
        assertEquals(2, result.lines[0].index);
        assertEquals(LineLocation.BETWEEN, result.lines[0].location);
        assertEquals(3, result.lines[1].index);
        assertEquals(LineLocation.END, result.lines[1].location);

        result = results[3];
        assertEquals(new PointD(5, 5), result.shared);
        assertEquals(1, result.lines[0].index);
        assertEquals(LineLocation.BETWEEN, result.lines[0].location);
        assertEquals(2, result.lines[1].index);
        assertEquals(LineLocation.END, result.lines[1].location);
    }

    @Test
    public void testEpsilon() {
        LineD[] lines = new LineD[] { new LineD(0, 2, 5, 2), new LineD(3, 2.1, 5, 4) };
        MultiLinePoint[] results = findBoth(lines);
        assertEquals(0, results.length);
        results = MultiLineIntersection.findSimple(lines, 1.0);
        assertEquals(1, results.length);

        MultiLinePoint result = results[0];
        assertTrue(PointD.equals(new PointD(3, 2), result.shared, 1.0));
        assertEquals(2, result.lines.length);
        assertEquals(0, result.lines[0].index);
        assertEquals(LineLocation.BETWEEN, result.lines[0].location);
        assertEquals(1, result.lines[1].index);
        assertEquals(LineLocation.START, result.lines[1].location);

        lines = new LineD[] { new LineD(3, 1, 1, 1), new LineD(1, 1.1, 3, 3), new LineD(1, 0.9, 3, -2) };
        results = findBoth(lines);
        assertEquals(0, results.length);
        results = MultiLineIntersection.findSimple(lines, 1.0);
        assertEquals(1, results.length);

        result = results[0];
        assertTrue(PointD.equals(new PointD(1, 1), result.shared, 1.0));
        assertEquals(3, result.lines.length);
        assertEquals(0, result.lines[0].index);
        assertEquals(LineLocation.END, result.lines[0].location);
        assertEquals(1, result.lines[1].index);
        assertEquals(LineLocation.START, result.lines[1].location);
        assertEquals(2, result.lines[2].index);
        assertEquals(LineLocation.START, result.lines[1].location);
    }

    @Test
    public void testRandom() {
        final int range = 10000;
        final LineD[] lines = new LineD[100];

        for (int i = 0; i < lines.length; i++)
            lines[i] = GeoUtils.randomLine(0, 0, range, range);

        findBoth(lines);
    }

    private MultiLinePoint[] findBoth(LineD[] lines) {
        final MultiLinePoint[] brute = MultiLineIntersection.findSimple(lines);
        final MultiLinePoint[] sweep = MultiLineIntersection.find(lines);
        compareResults(brute, sweep);
        MultiLineIntersection.split(lines, brute);
        return brute;
    }

    private void compareResults(MultiLinePoint[] brute, MultiLinePoint[] sweep) {
        assertEquals(brute.length, sweep.length);
        for (int i = 0; i < brute.length; i++)
            compareResultPoints(brute[i], sweep[i]);
    }

    private void compareResultPoints(MultiLinePoint brute, MultiLinePoint sweep) {
        assertTrue(PointD.equals(brute.shared, sweep.shared, 1e-6));
        Arrays.sort(brute.lines, Comparator.comparingInt(p -> p.index));
        Arrays.sort(sweep.lines, Comparator.comparingInt(p -> p.index));
        assertArrayEquals(brute.lines, sweep.lines);
    }
}
