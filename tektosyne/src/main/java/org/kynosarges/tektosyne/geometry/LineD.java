package org.kynosarges.tektosyne.geometry;

/**
 * Represents a directed line segment in two-dimensional space, using {@link Double} coordinates.
 * {@link LineD} contains two immutable {@link PointD} locations, indicating the {@link LineD#start}
 * and {@link LineD#end} points of a directed line segment. {@link LineD#angle} and {@link LineD#length}
 * are calculated on demand. Use {@link LineI} to represent lines with {@link Integer} coordinates.
 *
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class LineD {

    /** The start point of the {@link LineD}. */
    public final PointD start;

    /** The end point of the {@link LineD}. */
    public final PointD end;

    /**
     * An empty read-only {@link LineD}.
     * Both {@link #start} and {@link #end} are set to  {@link PointD#EMPTY}.
     */
    public static final LineD EMPTY = new LineD();

    /**
     * Creates a {@link LineD} that starts and ends at the coordinate origin.
     * Both {@link #start} and {@link #end} are set to {@link PointD#EMPTY}.
     */
    public LineD() {
        this.start = PointD.EMPTY;
        this.end = PointD.EMPTY;
    }

    /**
     * Creates a {@link LineD} with the specified {@link Double} coordinates.
     * @param startX the x-coordinate of the {@link #start} point
     * @param startY the y-coordinate of the {@link #start} point
     * @param endX the x-coordinate of the {@link #end} point
     * @param endY the y-coordinate of the {@link #end} point
     */
    public LineD(double startX, double startY, double endX, double endY) {
        this.start = new PointD(startX, startY);
        this.end = new PointD(endX, endY);
    }

    /**
     * Creates a {@link LineD} with the specified {@link PointD} coordinates.
     * @param start the {@link #start} point of the {@link LineD}
     * @param end the {@link #end} point of the {@link LineD}
     * @throws NullPointerException if {@code start} or {@code end} is {@code null}
     */
    public LineD(PointD start, PointD end) {
        if (start == null)
            throw new NullPointerException("start");
        if (end == null)
            throw new NullPointerException("end");

        this.start = start;
        this.end = end;
    }

    /**
     * Gets the angle of the {@link LineD}, in radians.
     * Returns the result of {@link Math#atan2} for the vertical and horizontal distances between
     * {@link #end} and {@link #start}, within the interval [-{@link Math#PI}, +{@link Math#PI}].
     * <p>
     * {@link #angle} equals zero if the {@link LineD} extends horizontally to the right,
     * and increases as the line turns clockwise (y-axis pointing down) or counter-clockwise
     * (y-axis pointing up).</p>
     * 
     * @return the angle, in radians, of the direction on the Cartesian plane in which
     *         the {@link LineD} is pointing, or zero if {@link #length} equals zero
     */
    public double angle() {

        final double x = end.x - start.x;
        final double y = end.y - start.y;

        return Math.atan2(y, x);
    }

    /**
     * Gets the squared distance between the {@link LineD} and the specified {@link PointD} coordinates.
     * Returns either the squared length of the perpendicular dropped from {@code q}
     * on the {@link LineD}, or the squared distance between {@code q} and {@link #start}
     * or {@link #end} if the perpendicular does not intersect the {@link LineD}.
     * 
     * @param q the {@link PointD} coordinates to examine
     * @return the squared distance between the {@link LineD} and {@code q}
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public double distanceSquared(PointD q) {
        if (q.equals(start) || q.equals(end))
            return 0;

        double x = start.x, y = start.y;
        final double ax = end.x - x, ay = end.y - y;

        // set (x,y) to nearest LineD point from q
        if (ax != 0 || ay != 0) {
            final double u = ((q.x - x) * ax + (q.y - y) * ay) / (ax * ax + ay * ay);
            if (u > 1) {
                x = end.x; y = end.y;
            } else if (u > 0) {
                x += u * ax; y += u * ay;
            }
        }

        x = q.x - x; y = q.y - y;
        return (x * x + y * y);
    }

    /**
     * Compares two {@link LineD} instances for equality, given the specified epsilon.
     * @param a the first {@link LineD} to compare
     * @param b the second {@link LineD} to compare
     * @param epsilon the maximum absolute difference where the corresponding coordinates
     *                of {@code a} and {@code b} are considered equal
     * @return {@code true} if the absolute difference between all corresponding coordinates
     *         of {@code a} and {@code b} is no greater than {@code epsilon}, else {@code false}
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public static boolean equals(LineD a, LineD b, double epsilon) {

        return (PointD.equals(a.start, b.start, epsilon) &&
                PointD.equals(a.end, b.end, epsilon));
    }

    /**
     * Finds the x-coordinate for the specified y-coordinate on the {@link LineD} or its infinite extension.
     * Returns the {@link PointD#x} coordinate of {@link #start} or {@link #end} if {@code y}
     * exactly equals the corresponding {@link PointD#y} coordinate, else a computed x-coordinate.
     * 
     * @param y the y-coordinate to examine
     * @return the x-coordinate for {@code y} on the {@link LineD} or its infinite extension,
     *         or {@link Double#MAX_VALUE} if the {@link LineD} is horizontal
     */
    public double findX(double y) {

        final double dy = end.y - start.y;
        if (dy == 0) return Double.MAX_VALUE;

        if (y == start.y) return start.x;
        if (y == end.y) return end.x;

        final double dx = end.x - start.x;
        return (start.x + (y - start.y) * dx / dy);
    }

    /**
     * Finds the y-coordinate for the specified x-coordinate on the {@link LineD} or its infinite extension.
     * Returns the {@link PointD#y} coordinate of {@link #start} or {@link #end} if {@code x}
     * exactly equals the corresponding {@link PointD#x} coordinate, else a computed y-coordinate.
     * 
     * @param x the x-coordinate to examine
     * @return the y-coordinate for {@code x} on the {@link LineD} or its infinite extension,
     *         or {@link Double#MAX_VALUE} if the {@link LineD} is vertical
     */
    public double findY(double x) {

        final double dx = end.x - start.x;
        if (dx == 0) return Double.MAX_VALUE;

        if (x == start.x) return start.y;
        if (x == end.x) return end.y;

        final double dy = end.y - start.y;
        return (start.y + (x - start.x) * dy / dx);
    }

    /**
     * Converts the specified {@link Double} array to a {@link LineD} array.
     * The returned array has a quarter as many elements as the specified
     * {@code lines} and retains the same coordinate sequence.
     * 
     * @param lines an array containing the coordinates {@code start.x, start.y, end.x, end.y} of
     *              {@link LineD} instances, stored in successive index positions per {@link LineD}
     * @return the {@link LineD} array created from {@code lines}
     * @throws IllegalArgumentException if the length of {@code lines} is not divisible by four
     * @throws NullPointerException if {@code lines} is {@code null}
     */
    public static LineD[] fromDoubles(double... lines) {
        if (lines.length % 4 != 0)
            throw new IllegalArgumentException("lines.length % 4 != 0");

        final LineD[] output = new LineD[lines.length / 4];

        for (int i = 0; i < output.length; i++)
            output[i] = new LineD(lines[4 * i], lines[4 * i + 1],
                    lines[4 * i + 2], lines[4 * i + 3]);

        return output;
    }

    /**
     * Converts the specified arrays of {@link PointD} coordinates and zero-based indices
     * into an array of equivalent {@link LineD} instances.
     * @param points an array of {@link PointD} coordinates representing {@link #start} or {@link #end} points
     * @param indices an array of {@link PointI} pairs of zero-based indices within {@code points}
     * @return an array of {@link LineD} instances created from the pairs of {@code points}
     *         coordinates indexed by each {@code indices} element
     * @throws ArrayIndexOutOfBoundsException if {@code indices} contains an index that is less than zero,
     *         or equal to or greater than the number of {@code points} elements
     * @throws NullPointerException if {@code points} or {@code indices} is {@code null}
     *                              or contains any {@code null} elements
     */
    public static LineD[] fromIndexPoints(PointD[] points, PointI[] indices) {
        if (points == null)
            throw new NullPointerException("points");

        final LineD[] lines = new LineD[indices.length];

        for (int i = 0; i < indices.length; i++)
            lines[i] = new LineD(points[indices[i].x], points[indices[i].y]);

        return lines;
    }

    /**
     * Finds the intersection between the {@link LineD} and a specified instance,
     * using exact coordinate comparisons.
     * Returns the result of {@link LineIntersection#find} for the {@link #start} and
     * {@link #end} points of this instance and the specified {@code line}, in that order.
     * 
     * @param line the {@link LineD} to intersect with this instance
     * @return a {@link LineIntersection} that describes if and how this instance
     *         intersects the specified {@code line}
     * @throws NullPointerException if {@code line} is {@code null}
     */
    public LineIntersection intersect(LineD line) {
        return LineIntersection.find(start, end, line.start, line.end);
    }

    /**
     * Finds the intersection between the {@link LineD} and a specified instance,
     * given the specified epsilon for coordinate comparisons.
     * Returns the result of {@link LineIntersection#find} for the {@link #start} and
     * {@link #end} points of this instance and the specified {@code line}, in that order,
     * and for the specified {@code epsilon}.
     * 
     * @param line the {@link LineD} to intersect with this instance
     * @param epsilon the maximum absolute difference at which coordinates and intermediate results
     *                should be considered equal. Always raised to a minium of 1e-10.
     * @return a {@link LineIntersection} that describes if and how this instance
     *         intersects the specified {@code line}
     * @throws NullPointerException if {@code line} is {@code null}
     */
    public LineIntersection intersect(LineD line, double epsilon) {
        return LineIntersection.find(start, end, line.start, line.end, epsilon);
    }

    /**
     * Finds the intersection between the {@link LineD} and the perpendicular dropped
     * from the specified {@link PointD} coordinates.
     * Returns {@link #start} if {@link #length} equals zero.
     * 
     * @param q the {@link PointD} coordinates from which to drop the perpendicular
     * @return the {@link PointD} location where the perpendicular dropped from {@code q}
     *         intersects the {@link LineD} or its infinite extension
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public PointD intersect(PointD q) {

        if (q.equals(start)) return start;
        if (q.equals(end)) return end;

        final double x = start.x, y = start.y;
        final double dx = end.x - x, dy = end.y - y;
        if (dx == 0 && dy == 0) return start;

        final double u = ((q.x - x) * dx + (q.y - y) * dy) / (dx * dx + dy * dy);
        return new PointD(x + u * dx, y + u * dy);
    }

    /**
     * Gets the inverse slope of the {@link LineD}.
     * Equals 1/{@link #slope}. May return a negative value, depending on {@link #angle}.
     * 
     * @return the quotient of the horizontal and vertical extensions of the {@link LineD},
     *         or {@link Double#MAX_VALUE} if the {@link LineD} is horizontal
     */
    public double inverseSlope() {

        final double dy = end.y - start.y;
        if (dy == 0) return Double.MAX_VALUE;

        final double dx = end.x - start.x;
        return (dx / dy);
    }

    /**
     * Gets the absolute length of the {@link LineD}.
     * Returns the square root of the sum of the squares of the horizontal
     * and vertical extensions of the {@link LineD}.
     * 
     * @return the non-negative absolute length of the {@link LineD}
     */
    public double length() {

        final double dx = end.x - start.x;
        final double dy = end.y - start.y;

        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Gets the squared absolute length of the {@link LineD}.
     * Returns the sum of the squares of the horizontal and vertical extensions of the
     * {@link LineD}. Use instead of {@link #length} if you only need the squared value.
     * 
     * @return a non-negative {@link Double} value that equals the square of {@link #length}
     */
    public double lengthSquared() {

        final double dx = end.x - start.x;
        final double dy = end.y - start.y;

        return (dx * dx + dy * dy);
    }

    /**
     * Determines the location of the specified {@link PointD} relative to the {@link LineD},
     * using exact coordinate comparisons.
     * Never returns {@code null}. The return values {@link LineLocation#LEFT} and
     * {@link LineLocation#RIGHT} assume that y-coordinates increase upward.
     * <p>
     * {@code locate} is based on the {@code classify} algorithm by Michael J. Laszlo,
     * <em>Computational Geometry and Computer Graphics in C++</em>, Prentice Hall 1996, p.76.</p>
     * 
     * @param q the {@link PointD} coordinates to examine
     * @return a {@link LineLocation} value indicating the location of {@code q} relative to the {@link LineD}
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public LineLocation locate(PointD q) {

        final double qx0 = q.x - start.x;
        final double qy0 = q.y - start.y;
        if (qx0 == 0 && qy0 == 0)
            return LineLocation.START;

        final double qx1 = q.x - end.x;
        final double qy1 = q.y - end.y;
        if (qx1 == 0 && qy1 == 0)
            return LineLocation.END;

        final double dx = end.x - start.x;
        final double dy = end.y - start.y;
        final double area = dx * qy0 - qx0 * dy;
        if (area > 0) return LineLocation.LEFT;
        if (area < 0) return LineLocation.RIGHT;

        if (qx0 * qx1 <= 0 && qy0 * qy1 <= 0)
            return LineLocation.BETWEEN;

        if (dx * qx0 < 0 || dy * qy0 < 0)
            return LineLocation.BEFORE;
        else
            return LineLocation.AFTER;
    }

    /**
     * Determines the location of the specified {@link PointD} relative to the {@link LineD},
     * given the specified epsilon for coordinate comparisons.
     * Never returns {@code null}. The return values {@link LineLocation#LEFT} and
     * {@link LineLocation#RIGHT} assume that y-coordinates increase upward.
     * 
     * @param q the {@link PointD} coordinates to examine
     * @param epsilon the maximum absolute difference at which coordinates
     *                and intermediate results should be considered equal
     * @return a {@link LineLocation} value indicating the location of {@code q} relative to the {@link LineD}
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public LineLocation locate(PointD q, double epsilon) {
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

        final double dx = end.x - start.x;
        final double dy = end.y - start.y;
        final double area = dx * qy0 - qx0 * dy;
        final double epsilon2 = epsilon * (Math.abs(dx) + Math.abs(dy));
        if (area > epsilon2) return LineLocation.LEFT;
        if (area < -epsilon2) return LineLocation.RIGHT;

        if ((qx0 * qx1 <= 0 || Math.abs(qx0) <= epsilon || Math.abs(qx1) <= epsilon) &&
            (qy0 * qy1 <= 0 || Math.abs(qy0) <= epsilon || Math.abs(qy1) <= epsilon))
            return LineLocation.BETWEEN;

        if (dx * qx0 < 0 || dy * qy0 < 0)
            return LineLocation.BEFORE;
        else
            return LineLocation.AFTER;
    }

    /**
     * Determines the location of the specified {@link PointD} relative to the {@link LineD},
     * assuming they are collinear and using exact coordinate comparisons.
     * Returns the result of {@link LineIntersection#locateCollinear} for the {@link #start}
     * and {@link #end} points of this instance and the specified {@code q}, in that order.
     * Never returns {@code null}, {@link LineLocation#LEFT}, or {@link LineLocation#RIGHT}
     * due to the assumption of collinearity.
     * 
     * @param q the {@link PointD} coordinates to examine
     * @return a {@link LineLocation} value indicating the location of {@code q} relative to the {@link LineD}
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public LineLocation locateCollinear(PointD q) {
        return LineIntersection.locateCollinear(start, end, q);
    }

    /**
     * Determines the location of the specified {@link PointD} relative to the {@link LineD},
     * assuming they are collinear and given the specified epsilon for coordinate comparisons.
     * Returns the result of {@link LineIntersection#locateCollinear} for the {@link #start}
     * and {@link #end} points of this instance and the specified {@code q} and {@code epsilon},
     * in that order. Never returns {@code null}, {@link LineLocation#LEFT}, or
     * {@link LineLocation#RIGHT} due to the assumption of collinearity.
     * 
     * @param q the {@link PointD} coordinates to examine
     * @param epsilon the maximum absolute difference at which coordinates
     *                and intermediate results should be considered equal
     * @return a {@link LineLocation} value indicating the location of {@code q} relative to the {@link LineD}
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public LineLocation locateCollinear(PointD q, double epsilon) {
        return LineIntersection.locateCollinear(start, end, q, epsilon);
    }

    /**
     * Reverses the direction of the {@link LineD}.
     * @return a {@link LineD} whose {@link #start} equals the {@link #end} of this instance, and vice versa
     */
    public LineD reverse() {
        return new LineD(end, start);
    }

    /**
     * Converts the {@link LineD} to a {@link LineI} by rounding coordinates to the
     * nearest {@link Integer} values. Uses {@link PointD#round} for rounding.
     * 
     * @return a {@link LineI} whose {@link LineI#start} and {@link LineI#end}
     *         points equal the corresponding points of the {@link LineD}, with
     *         each coordinate rounded to the nearest {@link Integer} value
     * @throws ArithmeticException if any coordinate overflows {@link Integer}
     */
    public LineI round() {
        return new LineI(start.round(), end.round());
    }

    /**
     * Gets the slope of the {@link LineD}.
     * Equals 1/{@link #inverseSlope}. May return a negative value, depending on {@link #angle}.
     * 
     * @return the quotient of the vertical and horizontal extensions of the {@link LineD},
     *         or {@link Double#MAX_VALUE} if the {@link LineD} is vertical
     */
    public double slope() {

        final double dx = end.x - start.x;
        if (dx == 0) return Double.MAX_VALUE;

        final double dy = end.y - start.y;
        return (dy / dx);
    }

    /**
     * Converts the specified {@link LineD} array to a {@link Double} array.
     * The returned array has four times as many elements as the specified
     * {@code lines} and retains the same coordinate sequence.
     * 
     * @param lines the {@link LineD} array to convert
     * @return an array containing the coordinates {@code start.x, start.y, end.x, end.y}
     *         for all {@code lines}, stored in successive index positions per {@link LineD}
     * @throws NullPointerException if {@code lines} or any of its elements is {@code null}
     */
    public static double[] toDoubles(LineD... lines) {
        final double[] output = new double[4 * lines.length];

        for (int i = 0; i < lines.length; i++) {
            output[4 * i] = lines[i].start.x;
            output[4 * i + 1] = lines[i].start.y;
            output[4 * i + 2] = lines[i].end.x;
            output[4 * i + 3] = lines[i].end.y;
        }

        return output;
    }

    /**
     * Converts the {@link LineD} to a {@link LineI} by truncating coordinates to the
     * nearest {@link Integer} values. Uses {@link Integer} casts for truncation.
     * 
     * @return a {@link LineI} whose {@link LineI#start} and {@link LineI#end}
     *         points equal the corresponding points of the {@link LineD}, with
     *         each coordinate truncated to the nearest {@link Integer} value
     * @throws ArithmeticException if any coordinate overflows {@link Integer}
     */
    public LineI toLineI() {
        return new LineI(start.toPointI(), end.toPointI());
    }

    /**
     * Gets the vector defined by the {@link LineD}.
     * Equals the {@link #end} point of the {@link LineD}, assuming its {@link #start}
     * point was shifted to the coordinate origin.
     * 
     * @return a {@link PointD} vector comprising the horizontal and vertical extensions of the {@link LineD}
     */
    public PointD vector() {
        return new PointD(end.x - start.x, end.y - start.y);
    }

    /**
     * Compares the specified {@link Object} to this {@link LineD} instance.
     * @param obj the {@link Object} to compare to this instance
     * @return {@code true} if {@code obj} is not {@code null} and a {@link LineD} instance whose 
     *         {@link #start} and {@link #end} equal those of this instance, else {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || !(obj instanceof LineD))
            return false;

        final LineD line = (LineD) obj;
        return (start.equals(line.start) && end.equals(line.end));
    }

    /**
     * Returns a hash code for the {@link LineD}.
     * @return an {@link Integer} hash code for the {@link LineD}
     */
    @Override
    public int hashCode() {
        return (31 * start.hashCode() + end.hashCode());
    }

    /**
     * Returns a {@link String} representation of the {@link LineD}.
     * @return a {@link String} containing the values of {@link #start} and {@link #end}
     */
    @Override
    public String toString() {
        return String.format("LineD[start.x=%g, start.y=%g, end.x=%g, end.y=%g]",
                start.x, start.y, end.x, end.y);
    }
}
