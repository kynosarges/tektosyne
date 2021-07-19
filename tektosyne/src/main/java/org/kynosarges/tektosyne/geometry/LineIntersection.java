package org.kynosarges.tektosyne.geometry;

/**
 * Provides algorithms to find the intersection of two line segments.
 * Provides a static method for intersection detection and immutable instance data that
 * describes the detected intersection: the absolute and relative locations of the
 * intersection point and the spatial relationship between the intersected line segments.
 * 
 * @author Christoph Nahr
 * @version 6.3.0
 */
public final class LineIntersection {

    /**
     * The location of the {@link #shared} point relative to the first line segment.
     * Holds {@code null} if no intersection was found.
     */
    public final LineLocation first;

    /**
     * The location of the {@link #shared} point relative to the second line segment.
     * Holds {@code null} if no intersection was found.
     */
    public final LineLocation second;

    /**
     * The spatial relationship between the two line segments.
     * Guaranteed to never hold {@code null}.
     */
    public final LineRelation relation;

    /**
     * The {@link PointD} coordinates shared by the two line segments or their infinite extensions.
     * Holds {@code null} if no intersection was found.
     * <p>
     * Valid {@link #shared} coordinates are generally computed, but may be copied from the
     * start or end point of an intersecting line if {@link #first} or {@link #second} equals
     * {@link LineLocation#START} or {@link LineLocation#END}.</p>
     * <p>
     * If {@link #relation} equals {@link LineRelation#COLLINEAR}, {@link #shared} holds the
     * following special values. If the two line segments overlap, {@link #shared} is not computed
     * but set directly to the start or end point of the second line segment, whichever is contained
     * by the first line segment and is lexicographically smaller, according to {@link PointDComparatorY}.
     * Otherwise, {@link #shared} is set to {@code null}, even though the infinite extensions
     * of the two line segments share all their points.</p>
     */
    public final PointD shared;

    /**
     * Creates a {@link LineIntersection} with the specified spatial relationship between both lines.
     * {@link #first}, {@link #second}, and {@link #shared} remain at their default value of {@code null}.
     * Use this constructor for collinear or parallel line segments that share no points.
     * 
     * @param relation the spatial relationship between the two line segments
     * @throws NullPointerException if {@code relation} is {@code null}
     */
    private LineIntersection(LineRelation relation) {
        this(null, null, null, relation);
    }

    /**
     * Creates a {@link LineIntersection} with the specified shared location, relative locations,
     * and spatial relationship between both lines.
     * @param shared the {@link PointD} shared by the line segments or their infinite extensions
     * @param first the location of {@code shared} relative to the first line segment
     * @param second the location of {@code shared} relative to the second line segment
     * @param relation the spatial relationship between the two line segments
     * @throws NullPointerException if {@code relation} is {@code null}
     */
    private LineIntersection(PointD shared,
        LineLocation first, LineLocation second, LineRelation relation) {
        
        if (relation == null)
            throw new NullPointerException("relation");

        this.shared = shared;
        this.first = first;
        this.second = second;
        this.relation = relation;
    }
    
    /**
     * Determines whether both line segments contain the {@link #shared} coordinates.
     * Requires that both {@link #first} and {@link #second} equal one of the indicated
     * {@link LineLocation} values, but not necessarily the same value.
     * <p>
     * {@code exists} indicates whether the two line segments themselves intersect.
     * {@link #shared} may be a valid point even if {@code exists} returns {@code false},
     * indicating an intersection of the infinite extensions of either or both line segments.</p>
     * 
     * @return {@code true} if both {@link #first} and {@link #second} equal either
     *         {@link LineLocation#START}, {@link LineLocation#BETWEEN}, or {@link LineLocation#END},
     *         else {@code false}
     */
    public boolean exists() {
        return (LineLocation.contains(first) && LineLocation.contains(second));
    }

    /**
     * Determines whether both line segments contain the {@link #shared} coordinates,
     * excluding the end points of at least one line segment.
     * Indicates whether the two line segments themselves intersect. Unlike {@link #exists}, at least
     * one line segment must be properly intersected, i.e. not just touched at one end point.
     * 
     * @return {@code true} if either {@link #first} or {@link #second} equals {@link LineLocation#BETWEEN},
     *         and the other location equals either {@link LineLocation#START}, {@link LineLocation#BETWEEN},
     *         or {@link LineLocation#END}, else {@code false}
     */
    public boolean existsBetween() {
        return ((LineLocation.contains(first) && second == LineLocation.BETWEEN) ||
                (first == LineLocation.BETWEEN && LineLocation.contains(second)));
    }

    /**
     * Finds the intersection of the specified line segments, using exact coordinate comparisons.
     * Adapted from the {@code Segments-Intersect} algorithm by Thomas H. Cormen et al.,
     * <em>Introduction to Algorithms</em> (3rd ed.), The MIT Press 2009, p.1018, for intersection
     * testing; and from the {@code SegSegInt} and {@code ParallelInt} algorithms by Joseph O’Rourke,
     * <em>Computational Geometry in C</em> (2nd ed.), Cambridge University Press 1998, p.224f,
     * for line relationships and shared coordinates.
     * <p>
     * Cormen’s intersection testing first examines the {@link PointD#crossProductLength} for each
     * triplet of specified points. If that is insufficient, O’Rourke’s algorithm then examines the
     * parameters of both line equations. This is mathematically redundant since O’Rourke’s algorithm
     * alone should produce all desired information, but the combination of both algorithms proved
     * much more resilient against misjudging line relationships due to floating-point inaccuracies.</p>
     * <p>
     * Although most comparisons in this overload are exact, cross-product testing is always
     * performed with a minimum epsilon of 1e-10. Moreover, {@code find} will return the result
     * of the epsilon overload with an epsilon of 2e-10 if cross-product testing contradicts line
     * equation testing. Subsequent contradictions result in further recursive calls, each time with
     * a doubled epsilon, until an intersection can be determined without contradictions.</p>
     * 
     * @param startA the {@link LineD#start} point of the first line segment
     * @param endA the {@link LineD#end} point of the first line segment
     * @param startB the {@link LineD#start} point of the second line segment
     * @param endB the {@link LineD#end} point of the second line segment
     * @return a {@link LineIntersection} instance that describes if and how the line segments
     *         from {@code startA} to {@code endA} and from {@code startB} to {@code endB} intersect
     * @throws NullPointerException if any {@link PointD} argument is {@code null}
     */
    public static LineIntersection find(PointD startA, PointD endA, PointD startB, PointD endB) {
        final double epsilon = 1e-10;
        LineLocation first, second;

        final double dxA = endA.x - startA.x, dyA = endA.y - startA.y;
        final double dxB = endB.x - startB.x, dyB = endB.y - startB.y;

        // compute cross-products for all end points
        final double d1 = (startA.x - startB.x) * dyB - (startA.y - startB.y) * dxB;
        final double d2 = (endA.x - startB.x) * dyB   - (endA.y - startB.y) * dxB;
        final double d3 = (startB.x - startA.x) * dyA - (startB.y - startA.y) * dxA;
        final double d4 = (endB.x - startA.x) * dyA   - (endB.y - startA.y) * dxA;

        assert(d1 == startB.crossProductLength(startA, endB));
        assert(d2 == startB.crossProductLength(endA, endB));
        assert(d3 == startA.crossProductLength(startB, endA));
        assert(d4 == startA.crossProductLength(endB, endA));

        /*
         * Some cross-products are zero: corresponding end point triplets are collinear.
         * 
         * The infinite lines intersect on the corresponding end points. The lines are collinear
         * exactly if all cross-products are zero; otherwise, the lines are divergent and we
         * need to check whether the finite line segments also intersect on the end points.
         * 
         * We always perform epsilon comparisons on cross-products, even in the exact overload,
         * because almost-zero cases are very frequent, especially for collinear lines.
         */
        if (Math.abs(d1) <= epsilon && Math.abs(d2) <= epsilon &&
            Math.abs(d3) <= epsilon && Math.abs(d4) <= epsilon) {

            // find lexicographically first point where segments overlap
            if (PointDComparatorY.compareExact(startB, endB) < 0) {
                first = locateCollinear(startA, endA, startB);
                if (LineLocation.contains(first))
                    return new LineIntersection(startB, first, LineLocation.START, LineRelation.COLLINEAR);

                first = locateCollinear(startA, endA, endB);
                if (LineLocation.contains(first))
                    return new LineIntersection(endB, first, LineLocation.END, LineRelation.COLLINEAR);
            } else {
                first = locateCollinear(startA, endA, endB);
                if (LineLocation.contains(first))
                    return new LineIntersection(endB, first, LineLocation.END, LineRelation.COLLINEAR);

                first = locateCollinear(startA, endA, startB);
                if (LineLocation.contains(first))
                    return new LineIntersection(startB, first, LineLocation.START, LineRelation.COLLINEAR);
            }

            // collinear line segments without overlapping points
            return new LineIntersection(LineRelation.COLLINEAR);
        }

        // check for divergent lines with end point intersection
        if (Math.abs(d1) <= epsilon) {
            second = locateCollinear(startB, endB, startA);
            return new LineIntersection(startA, LineLocation.START, second, LineRelation.DIVERGENT);
        }
        if (Math.abs(d2) <= epsilon) {
            second = locateCollinear(startB, endB, endA);
            return new LineIntersection(endA, LineLocation.END, second, LineRelation.DIVERGENT);
        }
        if (Math.abs(d3) <= epsilon) {
            first = locateCollinear(startA, endA, startB);
            return new LineIntersection(startB, first, LineLocation.START, LineRelation.DIVERGENT);
        }
        if (Math.abs(d4) <= epsilon) {
            first = locateCollinear(startA, endA, endB);
            return new LineIntersection(endB, first, LineLocation.END, LineRelation.DIVERGENT);
        }

        /*
         * All cross-products are non-zero: divergent or parallel lines.
         * 
         * The lines and segments might intersect, but not on any end point.
         * Compute parameters of both line equations to determine intersections.
         * Zero denominator indicates parallel lines (but not collinear, see above).
         */
        final double denom = dxB * dyA - dxA * dyB;
        if (Math.abs(denom) <= epsilon)
            return new LineIntersection(LineRelation.PARALLEL);

        /*
         * Compute position of intersection point relative to line segments, and also perform
         * sanity checks for floating-point inaccuracies. If a check fails, we cannot give a
         * reliable result at the current precision and must recurse with a greater epsilon.
         * 
         * Cross-products have pairwise opposite signs exactly if the corresponding line segment
         * straddles the infinite extension of the other line segment, implying a line equation
         * parameter between zero and one. Pairwise identical signs imply a parameter less than
         * zero or greater than one. Parameters cannot be exactly zero or one, as that indicates
         * end point intersections which were already ruled out by cross-product testing.
         */
        final double snum = startA.x * dyB - startA.y * dxB - startB.x * endB.y + startB.y * endB.x;
        final double s = snum / denom;

        if ((d1 < 0 && d2 < 0) || (d1 > 0 && d2 > 0)) {
            if (s < 0) first = LineLocation.BEFORE;
            else if (s > 1) first = LineLocation.AFTER;
            else return find(startA, endA, startB, endB, 2 * epsilon);
        } else {
            if (s > 0 && s < 1) first = LineLocation.BETWEEN;
            else return find(startA, endA, startB, endB, 2 * epsilon);
        }

        final double tnum = startB.y * dxA - startB.x * dyA + startA.x * endA.y - startA.y * endA.x;
        final double t = tnum / denom;

        if ((d3 < 0 && d4 < 0) || (d3 > 0 && d4 > 0)) {
            if (t < 0) second = LineLocation.BEFORE;
            else if (t > 1) second = LineLocation.AFTER;
            else return find(startA, endA, startB, endB, 2 * epsilon);
        } else {
            if (t > 0 && t < 1) second = LineLocation.BETWEEN;
            else return find(startA, endA, startB, endB, 2 * epsilon);
        }

        final PointD shared = new PointD(startA.x + s * dxA, startA.y + s * dyA);
        return new LineIntersection(shared, first, second, LineRelation.DIVERGENT);
    }

    /**
     * Finds the intersection of the specified line segments,
     * given the specified epsilon for coordinate comparisons.
     * Identical with the exact {@link #find(PointD, PointD, PointD, PointD)} overload but starts
     * with the specified {@code epsilon} to compare coordinates and intermediate results.
     * {@code epsilon} is always raised to a minimum of 1e-10 because the algorithm is otherwise
     * too unstable, and would initiate multiple recursions with a greater epsilon anyway.
     * 
     * @param startA the {@link LineD#start} point of the first line segment
     * @param endA the {@link LineD#end} point of the first line segment
     * @param startB the {@link LineD#start} point of the second line segment
     * @param endB the {@link LineD#end} point of the second line segment
     * @param epsilon the maximum absolute difference at which coordinates and intermediate results
     *                should be considered equal. Always raised to a minimum of 1e-10.
     * @return a {@link LineIntersection} instance that describes if and how the line segments
     *         from {@code startA} to {@code endA} and from {@code startB} to {@code endB} intersect
     * @throws NullPointerException if any {@link PointD} argument is {@code null}
     */
    public static LineIntersection find(PointD startA, PointD endA, PointD startB, PointD endB, double epsilon) {
        if (epsilon < 1e-10) epsilon = 1e-10;
        LineLocation first, second;

        final double dxA = endA.x - startA.x, dyA = endA.y - startA.y;
        final double dxB = endB.x - startB.x, dyB = endB.y - startB.y;

        // compute cross-products for all end points
        final double d1 = (startA.x - startB.x) * dyB - (startA.y - startB.y) * dxB;
        final double d2 = (endA.x - startB.x) * dyB   - (endA.y - startB.y) * dxB;
        final double d3 = (startB.x - startA.x) * dyA - (startB.y - startA.y) * dxA;
        final double d4 = (endB.x - startA.x) * dyA   - (endB.y - startA.y) * dxA;

        assert(d1 == startB.crossProductLength(startA, endB));
        assert(d2 == startB.crossProductLength(endA, endB));
        assert(d3 == startA.crossProductLength(startB, endA));
        assert(d4 == startA.crossProductLength(endB, endA));

        // check for collinear (but not parallel) lines
        if (Math.abs(d1) <= epsilon && Math.abs(d2) <= epsilon &&
            Math.abs(d3) <= epsilon && Math.abs(d4) <= epsilon) {

            // find lexicographically first point where segments overlap
            if (PointDComparatorY.compareExact(startB, endB) < 0) {
                first = locateCollinear(startA, endA, startB, epsilon);
                if (LineLocation.contains(first))
                    return new LineIntersection(startB, first, LineLocation.START, LineRelation.COLLINEAR);

                first = locateCollinear(startA, endA, endB, epsilon);
                if (LineLocation.contains(first))
                    return new LineIntersection(endB, first, LineLocation.END, LineRelation.COLLINEAR);
            } else {
                first = locateCollinear(startA, endA, endB, epsilon);
                if (LineLocation.contains(first))
                    return new LineIntersection(endB, first, LineLocation.END, LineRelation.COLLINEAR);

                first = locateCollinear(startA, endA, startB, epsilon);
                if (LineLocation.contains(first))
                    return new LineIntersection(startB, first, LineLocation.START, LineRelation.COLLINEAR);
            }

            // collinear line segments without overlapping points
            return new LineIntersection(LineRelation.COLLINEAR);
        }

        // check for divergent lines with end point intersection
        if (Math.abs(d1) <= epsilon) {
            second = locateCollinear(startB, endB, startA, epsilon);
            return new LineIntersection(startA, LineLocation.START, second, LineRelation.DIVERGENT);
        }
        if (Math.abs(d2) <= epsilon) {
            second = locateCollinear(startB, endB, endA, epsilon);
            return new LineIntersection(endA, LineLocation.END, second, LineRelation.DIVERGENT);
        }
        if (Math.abs(d3) <= epsilon) {
            first = locateCollinear(startA, endA, startB, epsilon);
            return new LineIntersection(startB, first, LineLocation.START, LineRelation.DIVERGENT);
        }
        if (Math.abs(d4) <= epsilon) {
            first = locateCollinear(startA, endA, endB, epsilon);
            return new LineIntersection(endB, first, LineLocation.END, LineRelation.DIVERGENT);
        }

        // compute parameters of line equations
        final double denom = dxB * dyA - dxA * dyB;
        if (Math.abs(denom) <= epsilon)
            return new LineIntersection(LineRelation.PARALLEL);

        final double snum = startA.x * dyB - startA.y * dxB - startB.x * endB.y + startB.y * endB.x;
        final double s = snum / denom;

        if ((d1 < 0 && d2 < 0) || (d1 > 0 && d2 > 0)) {
            if (s < 0) first = LineLocation.BEFORE;
            else if (s > 1) first = LineLocation.AFTER;
            else return find(startA, endA, startB, endB, 2 * epsilon);
        } else {
            if (s > 0 && s < 1) first = LineLocation.BETWEEN;
            else return find(startA, endA, startB, endB, 2 * epsilon);
        }

        final double tnum = startB.y * dxA - startB.x * dyA + startA.x * endA.y - startA.y * endA.x;
        final double t = tnum / denom;

        if ((d3 < 0 && d4 < 0) || (d3 > 0 && d4 > 0)) {
            if (t < 0) second = LineLocation.BEFORE;
            else if (t > 1) second = LineLocation.AFTER;
            else return find(startA, endA, startB, endB, 2 * epsilon);
        } else {
            if (t > 0 && t < 1) second = LineLocation.BETWEEN;
            else return find(startA, endA, startB, endB, 2 * epsilon);
        }

        final PointD shared = new PointD(startA.x + s * dxA, startA.y + s * dyA);

        /*
         * Epsilon comparisons of cross products (or line equation parameters) might miss
         * epsilon-close end point intersections of very long line segments. We compensate by
         * directly comparing the computed intersection point against the four end points.
         */
        if (PointD.equals(startA, shared, epsilon))
            first = LineLocation.START;
        else if (PointD.equals(endA, shared, epsilon))
            first = LineLocation.END;

        if (PointD.equals(startB, shared, epsilon))
            second = LineLocation.START;
        else if (PointD.equals(endB, shared, epsilon))
            second = LineLocation.END;

        return new LineIntersection(shared, first, second, LineRelation.DIVERGENT);
    }

    /**
     * Determines the location of the specified {@link PointD} coordinates relative to the
     * specified line segment, assuming they are collinear and using exact coordinate comparisons.
     * Never returns {@code null}, {@link LineLocation#LEFT}, or {@link LineLocation#RIGHT}
     * due to the assumption of collinearity.
     * 
     * @param start the {@link LineD#start} point of the line segment
     * @param end the {@link LineD#end} point of the line segment
     * @param q the {@link PointD} coordinates to examine
     * @return a {@link LineLocation} value indicating the location of {@code q}
     *         relative to the line segment from {@code start} to {@code end}
     * @throws NullPointerException if any {@link PointD} argument is {@code null}
     */
    public static LineLocation locateCollinear(PointD start, PointD end, PointD q) {

        final double qx0 = q.x - start.x;
        final double qy0 = q.y - start.y;
        if (qx0 == 0 && qy0 == 0)
            return LineLocation.START;

        final double qx1 = q.x - end.x;
        final double qy1 = q.y - end.y;
        if (qx1 == 0 && qy1 == 0)
            return LineLocation.END;

        if (qx0 * qx1 <= 0 && qy0 * qy1 <= 0)
            return LineLocation.BETWEEN;

        final double dx = end.x - start.x;
        final double dy = end.y - start.y;
        if (dx * qx0 < 0 || dy * qy0 < 0)
            return LineLocation.BEFORE;
        else
            return LineLocation.AFTER;
    }

    /**
     * Determines the location of the specified {@link PointD} coordinates relative to the
     * specified line segment, assuming they are collinear and given the specified epsilon
     * for coordinate comparisons.
     * Never returns {@code null}, {@link LineLocation#LEFT}, or {@link LineLocation#RIGHT}
     * due to the assumption of collinearity.
     * 
     * @param start the {@link LineD#start} point of the line segment
     * @param end the {@link LineD#end} point of the line segment
     * @param q the {@link PointD} coordinates to examine
     * @param epsilon the maximum absolute difference at which coordinates
     *                and intermediate results should be considered equal
     * @return a {@link LineLocation} value indicating the location of {@code q}
     *         relative to the line segment from {@code start} to {@code end}
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     * @throws NullPointerException if any {@link PointD} argument is {@code null}
     */
    public static LineLocation locateCollinear(PointD start, PointD end, PointD q, double epsilon) {
        if (epsilon < 0)
            throw new IllegalArgumentException("epsilon < 0");

        final double qx0 = q.x - start.x;
        final double qy0 = q.y - start.y;
        if (Math.abs(qx0) <= epsilon && Math.abs(qy0) <= epsilon)
            return LineLocation.START;

        final double qx1 = q.x - end.x;
        final double qy1 = q.y - end.y;
        if (Math.abs(qx1) <= epsilon && Math.abs(qy1) <= epsilon)
            return LineLocation.END;

        if ((qx0 * qx1 <= 0 || Math.abs(qx0) <= epsilon || Math.abs(qx1) <= epsilon) &&
            (qy0 * qy1 <= 0 || Math.abs(qy0) <= epsilon || Math.abs(qy1) <= epsilon))
            return LineLocation.BETWEEN;

        final double dx = end.x - start.x;
        final double dy = end.y - start.y;
        if (dx * qx0 < 0 || dy * qy0 < 0)
            return LineLocation.BEFORE;
        else
            return LineLocation.AFTER;
    }

    /**
     * Returns the {@link LineD#start} or {@link LineD#end} point of either specified
     * {@link LineD} for a matching {@link #first} or {@link #second} location.
     * Call to obtain exact coordinates when {@link #first} or {@link #second} indicates
     * an exact match to avoid approximately computed {@link #shared} coordinates.
     * 
     * @param a the {@link LineD} to which {@link #first} applies
     * @param b the {@link LineD} to which {@link #second} applies
     * @return the {@link LineD#start} or {@link LineD#end} point of {@code a} or {@code b}
     *         if {@link #first} or {@link #second} equals {@link LineLocation#START} or
     *         {@link LineLocation#END}, respectively, else {@link #shared}
     */
    public PointD startOrEnd(LineD a, LineD b) {

        if (first == LineLocation.START)  return a.start;
        if (first == LineLocation.END)    return a.end;
        if (second == LineLocation.START) return b.start;
        if (second == LineLocation.END)   return b.end;

        return shared;
    }

    /**
     * Compares the specified {@link Object} to this {@link LineIntersection} instance.
     * @param obj the {@link Object} to compare to this instance
     * @return {@code true} if {@code obj} is not {@code null} and a {@link LineIntersection}
     *         instance whose {@link #first}, {@link #second}, {@link #relation}, and
     *         {@link #shared} fields equal those of this instance, else {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof LineIntersection))
            return false;

        final LineIntersection other = (LineIntersection) obj;
        return (first == other.first && second == other.second &&
            relation == other.relation && shared.equals(other.shared));
    }

    /**
     * Returns a hash code for the {@link LineI}.
     * Returns the {@link PointD#hashCode} for {@link #shared} if valid, else zero.
     * 
     * @return an {@link Integer} hash code for the {@link LineI}
     */
    @Override
    public int hashCode() {
        return (shared == null ? 0 : shared.hashCode());
    }

    /**
     * Returns a {@link String} representation of the {@link LineIntersection}.
     * @return a {@link String} containing the values of {@link #shared},
     *         {@link #first}, {@link #second}, and {@link #relation}
     */
    @Override
    public String toString() {
        return String.format("LineIntersection[first=%s, second=%s, relation=%s, shared=%s]",
                (first == null ? "null" : first.toString()),
                (second == null ? "null": second.toString()),
                (relation == null ? "null": relation.toString()),
                (shared == null ? "null" : shared.toString()));
    }
}
