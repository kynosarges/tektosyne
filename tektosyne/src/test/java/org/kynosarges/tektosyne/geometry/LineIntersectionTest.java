package org.kynosarges.tektosyne.geometry;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Provides unit tests for class {@link LineIntersection}.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class LineIntersectionTest {

    @Test
    public void testIntersect() {

        LineIntersection result = new LineD(0, 0, 0.9, 0.9).intersect(new LineD(1, 1, 2, 2));
        assertEquals(null, result.shared);
        assertEquals(null, result.first);
        assertEquals(null, result.second);
        assertEquals(LineRelation.COLLINEAR, result.relation);

        result = new LineD(0, 1, 2, 3).intersect(new LineD(1, 0, 3, 2));
        assertEquals(null, result.shared);
        assertEquals(null, result.first);
        assertEquals(null, result.second);
        assertEquals(LineRelation.PARALLEL, result.relation);

        result = new LineD(0, 0, 0.9, 0.9).intersect(new LineD(0, 2, 0.9, 1.1));
        assertTrue(PointD.equals(new PointD(1, 1), result.shared, 0.01));
        assertEquals(LineLocation.AFTER, result.first);
        assertEquals(LineLocation.AFTER, result.second);
        assertEquals(LineRelation.DIVERGENT, result.relation);

        result = new LineD(0.9, 0.9, 0, 0).intersect(new LineD(1.1, 0.9, 2, 0));
        assertTrue(PointD.equals(new PointD(1, 1), result.shared, 0.01));
        assertEquals(LineLocation.BEFORE, result.first);
        assertEquals(LineLocation.BEFORE, result.second);
        assertEquals(LineRelation.DIVERGENT, result.relation);
    }

    @Test
    public void testIntersectCollinear() {

        LineIntersection result = new LineD(1, 1, 0, 0).intersect(new LineD(1, 1, 0, 0));
        assertTrue(PointD.equals(new PointD(0, 0), result.shared, 0.01));
        assertEquals(LineLocation.END, result.first);
        assertEquals(LineLocation.END, result.second);
        assertEquals(LineRelation.COLLINEAR, result.relation);

        result = new LineD(0, 0, 1, 1).intersect(new LineD(1, 1, 2, 2));
        assertTrue(PointD.equals(new PointD(1, 1), result.shared, 0.01));
        assertEquals(LineLocation.END, result.first);
        assertEquals(LineLocation.START, result.second);
        assertEquals(LineRelation.COLLINEAR, result.relation);

        result = new LineD(1, 1, 0, 0).intersect(new LineD(2, 2, 1, 1));
        assertTrue(PointD.equals(new PointD(1, 1), result.shared, 0.01));
        assertEquals(LineLocation.START, result.first);
        assertEquals(LineLocation.END, result.second);
        assertEquals(LineRelation.COLLINEAR, result.relation);

        result = new LineD(0, 0, 2, 2).intersect(new LineD(3, 3, 1, 1));
        assertTrue(PointD.equals(new PointD(1, 1), result.shared, 0.01));
        assertEquals(LineLocation.BETWEEN, result.first);
        assertEquals(LineLocation.END, result.second);
        assertEquals(LineRelation.COLLINEAR, result.relation);
    }

    @Test
    public void testIntersectDivergent() {

        LineIntersection result = new LineD(0, 0, 1, 1).intersect(new LineD(1, 1, 2, 0));
        assertTrue(PointD.equals(new PointD(1, 1), result.shared, 0.01));
        assertEquals(LineLocation.END, result.first);
        assertEquals(LineLocation.START, result.second);
        assertEquals(LineRelation.DIVERGENT, result.relation);

        result = new LineD(1, 1, 0, 0).intersect(new LineD(2, 0, 1, 1));
        assertTrue(PointD.equals(new PointD(1, 1), result.shared, 0.01));
        assertEquals(LineLocation.START, result.first);
        assertEquals(LineLocation.END, result.second);
        assertEquals(LineRelation.DIVERGENT, result.relation);

        result = new LineD(0, 0, 2, 2).intersect(new LineD(1, 1, 2, 0));
        assertTrue(PointD.equals(new PointD(1, 1), result.shared, 0.01));
        assertEquals(LineLocation.BETWEEN, result.first);
        assertEquals(LineLocation.START, result.second);
        assertEquals(LineRelation.DIVERGENT, result.relation);

        result = new LineD(0, 0, 1, 1).intersect(new LineD(0, 2, 2, 0));
        assertTrue(PointD.equals(new PointD(1, 1), result.shared, 0.01));
        assertEquals(LineLocation.END, result.first);
        assertEquals(LineLocation.BETWEEN, result.second);
        assertEquals(LineRelation.DIVERGENT, result.relation);

        result = new LineD(0, 0, 2, 2).intersect(new LineD(0, 2, 2, 0));
        assertTrue(PointD.equals(new PointD(1, 1), result.shared, 0.01));
        assertEquals(LineLocation.BETWEEN, result.first);
        assertEquals(LineLocation.BETWEEN, result.second);
        assertEquals(LineRelation.DIVERGENT, result.relation);
    }

    @Test
    public void testIntersectEpsilon() {

        LineIntersection result = new LineD(0, 0, 0.9, 0.9).intersect(new LineD(1, 1, 2, 2), 0.01);
        assertEquals(null, result.shared);
        assertEquals(null, result.first);
        assertEquals(null, result.second);
        assertEquals(LineRelation.COLLINEAR, result.relation);

        result = new LineD(0, 0, 0.9, 0.9).intersect(new LineD(1, 1, 2, 2), 0.5);
        assertTrue(PointD.equals(new PointD(1, 1), result.shared, 0.01));
        assertEquals(LineLocation.END, result.first);
        assertEquals(LineLocation.START, result.second);
        assertEquals(LineRelation.COLLINEAR, result.relation);

        result = new LineD(0.9, 0.9, 0.1, 0.1).intersect(new LineD(1.1, 1.1, 0, 0), 0.5);
        assertTrue(PointD.equals(new PointD(0, 0), result.shared, 0.01));
        assertEquals(LineLocation.END, result.first);
        assertEquals(LineLocation.END, result.second);
        assertEquals(LineRelation.COLLINEAR, result.relation);

        result = new LineD(0, 0, 0.9, 0.9).intersect(new LineD(1, 1, 2, 0), 0.01);
        assertTrue(PointD.equals(new PointD(1, 1), result.shared, 0.01));
        assertEquals(LineLocation.AFTER, result.first);
        assertEquals(LineLocation.START, result.second);
        assertEquals(LineRelation.DIVERGENT, result.relation);

        result = new LineD(0, 0, 0.9, 0.9).intersect(new LineD(1, 1, 2, 0), 0.5);
        assertTrue(PointD.equals(new PointD(0.9, 0.9), result.shared, 0.01));
        assertEquals(LineLocation.END, result.first);
        assertEquals(LineLocation.START, result.second);
        assertEquals(LineRelation.DIVERGENT, result.relation);

        result = new LineD(0, 0, 2, 2).intersect(new LineD(0.9, 1.1, 2, 0), 0.01);
        assertTrue(PointD.equals(new PointD(1, 1), result.shared, 0.01));
        assertEquals(LineLocation.BETWEEN, result.first);
        assertEquals(LineLocation.BETWEEN, result.second);
        assertEquals(LineRelation.DIVERGENT, result.relation);

        result = new LineD(0, 0, 2, 2).intersect(new LineD(0.9, 1.1, 2, 0), 0.5);
        assertTrue(PointD.equals(new PointD(0.9, 1.1), result.shared, 0.01));
        assertEquals(LineLocation.BETWEEN, result.first);
        assertEquals(LineLocation.START, result.second);
        assertEquals(LineRelation.DIVERGENT, result.relation);
    }
}
