package org.kynosarges.tektosyne.geometry;

/**
 * Represents a directed line segment in two-dimensional space, using {@link Integer} coordinates.
 * {@link LineI} contains two immutable {@link PointI} locations, indicating the {@link LineI#start}
 * and {@link LineI#end} points of a directed line segment. {@link LineI#angle} and {@link LineI#length}
 * are calculated on demand. Use {@link LineD} to represent lines with {@link Double} coordinates.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class LineI {

    /** The start point of the {@link LineI}. */
    public final PointI start;

    /** The end point of the {@link LineI}. */
    public final PointI end;

    /**
     * An empty read-only {@link LineI}.
     * Both {@link #start} and {@link #end} are set to  {@link PointI#EMPTY}.
     */
    public static final LineI EMPTY = new LineI();

    /**
     * Creates a {@link LineI} that starts and ends at the coordinate origin.
     * Both {@link #start} and {@link #end} are set to {@link PointI#EMPTY}.
     */
    public LineI() {
        this.start = PointI.EMPTY;
        this.end = PointI.EMPTY;
    }

    /**
     * Creates a {@link LineI} with the specified {@link Integer} coordinates.
     * @param startX the x-coordinate of the {@link #start} point
     * @param startY the y-coordinate of the {@link #start} point
     * @param endX the x-coordinate of the {@link #end} point
     * @param endY the y-coordinate of the {@link #end} point
     */
    public LineI(int startX, int startY, int endX, int endY) {
        this.start = new PointI(startX, startY);
        this.end = new PointI(endX, endY);
    }

    /**
     * Creates a {@link LineI} with the specified {@link PointI} coordinates.
     * @param start the {@link #start} point of the {@link LineI}
     * @param end the {@link #end} point of the {@link LineI}
     * @throws NullPointerException if {@code start} or {@code end} is {@code null}
     */
    public LineI(PointI start, PointI end) {
        if (start == null)
            throw new NullPointerException("start");
        if (end == null)
            throw new NullPointerException("end");

        this.start = start;
        this.end = end;
    }

    /**
     * Gets the angle of the {@link LineI}, in radians.
     * Returns the result of {@link Math#atan2} for the vertical and horizontal distances between
     * {@link #end} and {@link #start}, within the interval [-{@link Math#PI}, +{@link Math#PI}].
     * <p>
     * {@link #angle} equals zero if the {@link LineI} extends horizontally to the right,
     * and increases as the line turns clockwise (y-axis pointing down) or counter-clockwise
     * (y-axis pointing up).</p>
     * 
     * @return the angle, in radians, of the direction on the Cartesian plane in which
     *         the {@link LineI} is pointing, or zero if {@link #length} equals zero
     */
    public double angle() {

        final double x = (double) end.x - start.x;
        final double y = (double) end.y - start.y;

        return Math.atan2(y, x);
    }

    /**
     * Gets the squared distance between the {@link LineI} and the specified {@link PointI} coordinates.
     * Returns either the squared length of the perpendicular dropped from {@code q}
     * on the {@link LineI}, or the squared distance between {@code q} and {@link #start}
     * or {@link #end} if the perpendicular does not intersect the {@link LineI}.
     * 
     * @param q the {@link PointI} coordinates to examine
     * @return the squared distance between the {@link LineI} and {@code q}
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public double distanceSquared(PointI q) {
        if (q.equals(start) || q.equals(end))
            return 0;

        double x = start.x, y = start.y;
        final double ax = end.x - x, ay = end.y - y;

        // set (x,y) to nearest LineI point from q
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
     * Finds the x-coordinate for the specified y-coordinate on the {@link LineI} or its infinite extension.
     * Returns the {@link PointI#x} coordinate of {@link #start} or {@link #end} if {@code y}
     * exactly equals the corresponding {@link PointI#y} coordinate, else a computed x-coordinate.
     * 
     * @param y the y-coordinate to examine
     * @return the x-coordinate for {@code y} on the {@link LineI} or its infinite extension,
     *         or {@link Double#MAX_VALUE} if the {@link LineI} is horizontal
     */
    public double findX(double y) {

        final double dy = (double) end.y - start.y;
        if (dy == 0) return Double.MAX_VALUE;

        if (y == start.y) return start.x;
        if (y == end.y) return end.x;

        final double dx = (double) end.x - start.x;
        return (start.x + (y - start.y) * dx / dy);
    }

    /**
     * Finds the y-coordinate for the specified x-coordinate on the {@link LineI} or its infinite extension.
     * Returns the {@link PointI#y} coordinate of {@link #start} or {@link #end} if {@code x}
     * exactly equals the corresponding {@link PointI#x} coordinate, else a computed y-coordinate.
     * 
     * @param x the x-coordinate to examine
     * @return the y-coordinate for {@code x} on the {@link LineI} or its infinite extension,
     *         or {@link Double#MAX_VALUE} if the {@link LineI} is vertical
     */
    public double findY(double x) {

        final double dx = (double) end.x - start.x;
        if (dx == 0) return Double.MAX_VALUE;

        if (x == start.x) return start.y;
        if (x == end.x) return end.y;

        final double dy = (double) end.y - start.y;
        return (start.y + (x - start.x) * dy / dx);
    }

    /**
     * Converts the specified {@link Integer} array to a {@link LineI} array.
     * The returned array has a quarter as many elements as the specified
     * {@code lines} and retains the same coordinate sequence.
     * 
     * @param lines an array containing the coordinates {@code start.x, start.y, end.x, end.y} of
     *              {@link LineI} instances, stored in successive index positions per {@link LineI}
     * @return the {@link LineI} array created from {@code lines}
     * @throws IllegalArgumentException if the length of {@code lines} is not divisible by four
     * @throws NullPointerException if {@code lines} is {@code null}
     */
    public static LineI[] fromInts(int... lines) {
        if (lines.length % 4 != 0)
            throw new IllegalArgumentException("lines.length % 4 != 0");

        final LineI[] output = new LineI[lines.length / 4];

        for (int i = 0; i < output.length; i++)
            output[i] = new LineI(lines[4 * i], lines[4 * i + 1],
                    lines[4 * i + 2], lines[4 * i + 3]);

        return output;
    }

    /**
     * Finds the intersection between the {@link LineI} and a specified instance.
     * Returns the result of {@link LineIntersection#find} for the {@link #start} and
     * {@link #end} points of this instance and the specified {@code line}, in that order.
     * 
     * @param line the {@link LineI} to intersect with this instance
     * @return a {@link LineIntersection} that describes if and how this instance
     *         intersects the specified {@code line}
     * @throws NullPointerException if {@code line} is {@code null}
     */
    public LineIntersection intersect(LineI line) {
        return LineIntersection.find(
                start.toPointD(), end.toPointD(),
                line.start.toPointD(), line.end.toPointD());
    }

    /**
     * Finds the intersection between the {@link LineI} and the perpendicular dropped
     * from the specified {@link PointI} coordinates.
     * Returns {@link #start} if {@link #length} equals zero.
     * 
     * @param q the {@link PointI} coordinates from which to drop the perpendicular
     * @return the {@link PointD} location where the perpendicular dropped from {@code q}
     *         intersects the {@link LineI} or its infinite extension
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public PointD intersect(PointI q) {

        if (q.equals(start)) return start.toPointD();
        if (q.equals(end)) return end.toPointD();

        final double x = start.x, y = start.y;
        final double dx = end.x - x, dy = end.y - y;
        if (dx == 0 && dy == 0) return start.toPointD();

        final double u = ((q.x - x) * dx + (q.y - y) * dy) / (dx * dx + dy * dy);
        return new PointD(x + u * dx, y + u * dy);
    }

    /**
     * Gets the inverse slope of the {@link LineI}.
     * Equals 1/{@link #slope}. May return a negative value, depending on {@link #angle}.
     * 
     * @return the quotient of the horizontal and vertical extensions of the {@link LineI},
     *         or {@link Double#MAX_VALUE} if the {@link LineI} is horizontal
     */
    public double inverseSlope() {

        final double dy = (double) end.y - start.y;
        if (dy == 0) return Double.MAX_VALUE;

        final double dx = (double) end.x - start.x;
        return (dx / dy);
    }

    /**
     * Gets the absolute length of the {@link LineI}.
     * Returns the square root of the sum of the squares of the horizontal
     * and vertical extensions of the {@link LineI}.
     * 
     * @return the non-negative absolute length of the {@link LineI}
     */
    public double length() {

        final double dx = (double) end.x - start.x;
        final double dy = (double) end.y - start.y;

        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Gets the squared absolute length of the {@link LineI}.
     * Returns the sum of the squares of the horizontal and vertical extensions
     * of the {@link LineI}, widened to {@link Long} to avoid {@link Integer} overflow.
     * Use instead of {@link #length} if you only need the squared value.
     * 
     * @return a non-negative {@link Long} value that equals the square of {@link #length}
     */
    public long lengthSquared() {

        final long dx = (long) end.x - start.x;
        final long dy = (long) end.y - start.y;

        return (dx * dx + dy * dy);
    }

    /**
     * Determines the location of the specified {@link PointI} relative to the {@link LineI}.
     * Never returns {@code null}. The return values {@link LineLocation#LEFT} and
     * {@link LineLocation#RIGHT} assume that y-coordinates increase upward.
     * <p>
     * {@code locate} is based on the {@code classify} algorithm by Michael J. Laszlo,
     * <em>Computational Geometry and Computer Graphics in C++</em>, Prentice Hall 1996, p.76.</p>
     * 
     * @param q the {@link PointI} coordinates to examine
     * @return a {@link LineLocation} value indicating the location of {@code q} relative to the {@link LineI}
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public LineLocation locate(PointI q) {

        final double qx0 = (double) q.x - start.x;
        final double qy0 = (double) q.y - start.y;
        if (qx0 == 0 && qy0 == 0)
            return LineLocation.START;

        final double qx1 = (double) q.x - end.x;
        final double qy1 = (double) q.y - end.y;
        if (qx1 == 0 && qy1 == 0)
            return LineLocation.END;

        final double dx = (double) end.x - start.x;
        final double dy = (double) end.y - start.y;
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
     * Determines the location of the specified {@link PointI} relative to the {@link LineI},
     * assuming they are collinear.
     * Returns the result of {@link LineIntersection#locateCollinear} for the {@link #start}
     * and {@link #end} points of this instance and the specified {@code q}, in that order.
     * Never returns {@code null}, {@link LineLocation#LEFT}, or {@link LineLocation#RIGHT}
     * due to the assumption of collinearity.
     * 
     * @param q the {@link PointI} coordinates to examine
     * @return a {@link LineLocation} value indicating the location of {@code q} relative to the {@link LineI}
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public LineLocation locateCollinear(PointI q) {
        return LineIntersection.locateCollinear(
                start.toPointD(), end.toPointD(), q.toPointD());
    }

    /**
     * Reverses the direction of the {@link LineI}.
     * @return a {@link LineI} whose {@link #start} equals the {@link #end} of this instance, and vice versa
     */
    public LineI reverse() {
        return new LineI(end, start);
    }

    /**
     * Gets the slope of the {@link LineI}.
     * Equals 1/{@link #inverseSlope}. May return a negative value, depending on {@link #angle}.
     * 
     * @return the quotient of the vertical and horizontal extensions of the {@link LineI},
     *         or {@link Double#MAX_VALUE} if the {@link LineI} is vertical
     */
    public double slope() {

        final double dx = (double) end.x - start.x;
        if (dx == 0) return Double.MAX_VALUE;

        final double dy = (double) end.y - start.y;
        return (dy / dx);
    }

    /**
     * Converts the specified {@link LineI} array to an {@link Integer} array.
     * The returned array has four times as many elements as the specified
     * {@code lines} and retains the same coordinate sequence.
     * 
     * @param lines the {@link LineI} array to convert
     * @return an array containing the coordinates {@code start.x, start.y, end.x, end.y}
     *         for all {@code lines}, stored in successive index positions per {@link LineI}
     * @throws NullPointerException if {@code lines} or any of its elements is {@code null}
     */
    public static int[] toInts(LineI... lines) {
        final int[] output = new int[4 * lines.length];

        for (int i = 0; i < lines.length; i++) {
            output[4 * i] = lines[i].start.x;
            output[4 * i + 1] = lines[i].start.y;
            output[4 * i + 2] = lines[i].end.x;
            output[4 * i + 3] = lines[i].end.y;
        }

        return output;
    }

    /**
     * Converts the {@link LineI} to a {@link LineD}.
     * @return a {@link LineD} whose {@link LineD#start} and {@link LineD#end}
     *         points equal the corresponding points of the {@link LineI}
     */
    public LineD toLineD() {
        return new LineD(start.toPointD(), end.toPointD());
    }

    /**
     * Gets the vector defined by the {@link LineI}.
     * Equals the {@link #end} point of the {@link LineI}, assuming its {@link #start}
     * point was shifted to the coordinate origin.
     * 
     * @return a {@link PointI} vector comprising the horizontal and vertical extensions of the {@link LineI}
     * @throws ArithmeticException if the difference in any dimension overflows {@link Integer}
     */
    public PointI vector() {
        return new PointI(
                Math.subtractExact(end.x, start.x),
                Math.subtractExact(end.y, start.y));
    }

    /**
     * Compares the specified {@link Object} to this {@link LineI} instance.
     * @param obj the {@link Object} to compare to this instance
     * @return {@code true} if {@code obj} is not {@code null} and a {@link LineI} instance whose 
     *         {@link #start} and {@link #end} equal those of this instance, else {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || !(obj instanceof LineI))
            return false;

        final LineI line = (LineI) obj;
        return (start.equals(line.start) && end.equals(line.end));
    }

    /**
     * Returns a hash code for the {@link LineI}.
     * @return an {@link Integer} hash code for the {@link LineI}
     */
    @Override
    public int hashCode() {
        return (31 * start.hashCode() + end.hashCode());
    }

    /**
     * Returns a {@link String} representation of the {@link LineI}.
     * @return a {@link String} containing the values of {@link #start} and {@link #end}
     */
    @Override
    public String toString() {
        return String.format("LineI[start.x=%d, start.y=%d, end.x=%d, end.y=%d]",
                start.x, start.y, end.x, end.y);
    }
}
