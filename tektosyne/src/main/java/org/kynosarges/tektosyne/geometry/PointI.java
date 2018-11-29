package org.kynosarges.tektosyne.geometry;

import org.kynosarges.tektosyne.*;

/**
 * Represents a location in two-dimensional space, using {@link Integer} coordinates.
 * {@link PointI} contains two immutable non-negative {@link Integer} dimensions,
 * representing either a two-dimensional location or a two-dimensional vector.
 * Use {@link PointD} to represent points with {@link Double} dimensions.
 * 
 * @author Christoph Nahr
 * @version 6.1.0
 */
public final class PointI {

    /** The x-coordinate of the {@link PointI}. */
    public final int x;

    /** The y-coordinate of the {@link PointI}. */
    public final int y;

    /**
     * An empty read-only {@link PointI}.
     * Both {@link #x} and {@link #y} are set to zero.
     */
    public static final PointI EMPTY = new PointI();

    /**
     * Creates a {@link PointI} with the coordinate origin.
     * Both {@link #x} and {@link #y} are set to zero.
     */
    public PointI() {
        this.x = 0;
        this.y = 0;
    }

    /**
     * Creates a {@link PointI} with the specified {@link Integer} coordinates.
     * @param x the x-coordinate of the {@link PointI}
     * @param y the y-coordinate of the {@link PointI} 
     */
    public PointI(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Adds the location of the specified {@link PointI} to this instance.
     * @param point the {@link PointI} whose location to add to this instance
     * @return a {@link PointI} whose {@link #x} and {@link #y} equal the addition of the
     *         corresponding dimensions of the specified {@code point} to this instance
     * @throws ArithmeticException if the sum in any dimension overflows {@link Integer}
     * @throws NullPointerException if {@code point} is {@code null}
     */
    public PointI add(PointI point) {
        return new PointI(
                MathCompat.addExact(x, point.x),
                MathCompat.addExact(y, point.y));
    }

    /**
     * Gets the polar angle of the vector represented by the {@link PointI}, in radians.
     * Returns the result of {@link Math#atan2} for {@link #y} and {@link #x}, 
     * within the interval [-{@link Math#PI}, +{@link Math#PI}].
     * 
     * @return the polar angle, in radians, of the vector represented by the {@link PointI},
     *         or zero if {@link #x} and {@link #y} both equal zero
     */
    public double angle() {
        return Math.atan2(y, x);
    }

    /**
     * Computes the angle between the vector represented by the {@link PointI} and the specified vector.
     * Returns the result of {@link Math#atan2} for the cross-product length and the scalar dot product
     * of the two vectors. The possible range of values is [-{@link Math#PI}, +{@link Math#PI}].
     * Coordinates are widened to {@link Double} to avoid {@link Integer} overflow.
     * 
     * @param vector the {@link PointI} vector to compare with this instance
     * @return the angle, in radians, between this instance and the specified {@code vector}, in that order
     * @throws NullPointerException if {@code vector} is {@code null}
     */
    public double angleBetween(PointI vector) {

        final double y = (double) this.x * vector.y - (double) this.y * vector.x;
        final double x = (double) this.x * vector.x + (double) this.y * vector.y;

        return Math.atan2(y, x);
    }

    /**
     * Computes the angle between the vectors from the {@link PointI} to the specified locations.
     * Returns the result of {@link Math#atan2} for the cross-product length and the scalar dot product
     * of the two vectors. The possible range of values is [-{@link Math#PI}, +{@link Math#PI}].
     * 
     * @param a the {@link PointI} location where the first vector ends
     * @param b the {@link PointI} location where the second vector ends
     * @return the angle, in radians, between the vectors from this instance to {@code a}
     *         and from this instance to {@code b}, in that order
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public double angleBetween(PointI a, PointI b) {

        final double ax = (double) a.x - this.x;
        final double ay = (double) a.y - this.y;
        final double bx = (double) b.x - this.x;
        final double by = (double) b.y - this.y;

        final double y = ax * by - ay * bx;
        final double x = ax * bx + ay * by;

        return Math.atan2(y, x);
    }

    /**
     * Computes the length of the cross-product of the vector represented by the {@link PointI}
     * and the specified vector. The absolute value equals the area of the parallelogram
     * spanned by the {@link PointI} and the specified {@code vector}. The sign indicates their
     * spatial relationship, as described in {@link #crossProductLength(PointI, PointI)}.
     * Coordinates are widened to {@link Long} to avoid {@link Integer} overflow.
     * 
     * @param vector the {@link PointI} vector to multiply with this instance
     * @return a {@link Long} value indicating the length of the cross-product of this instance
     *         and the specified {@code vector}, in that order
     * @throws NullPointerException if {@code vector} is {@code null}
     */
    public long crossProductLength(PointI vector) {
        return ((long) x * vector.y - (long) vector.x * y);
    }

    /**
     * Computes the length of the cross-product of the vectors from the {@link PointI}
     * to the specified locations. The absolute value equals the area of the parallelogram
     * spanned by the vectors from the {@link PointI} to {@code a} and {@code b}.
     * The sign indicates their spatial relationship of the two vectors, as follows:
     * <ul>
     * <li>Less than zero — The sequence from this instance to {@code a} and then {@code b}
     * constitutes a right-hand turn, assuming y-coordinates increase upward.</li>
     * <li>Zero — This instance, {@code a}, and {@code b} are collinear.</li>
     * <li>Greater than zero — The sequence from this instance to {@code a} and then {@code b}
     * constitutes a left-hand turn, assuming y-coordinates increase upward.</li>
     * </ul>
     * Coordinates are widened to {@link Long} to avoid {@link Integer} overflow.
     * 
     * @param a the {@link PointI} location where the first vector ends
     * @param b the {@link PointI} location where the second vector ends
     * @return a {@link Long} value indicating the length of the cross-product of the vectors
     *         from this instance to {@code a} and from this instance to {@code b}, in that order
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public long crossProductLength(PointI a, PointI b) {
        return ((long) a.x - x) * ((long) b.y - y) -
               ((long) b.x - x) * ((long) a.y - y);
    }

    /**
     * Converts the specified {@link Integer} array to a {@link PointI} array.
     * The returned array has half as many elements as the specified
     * {@code points} and retains the same coordinate sequence.
     * 
     * @param points an array containing the {@link #x} and {@link #y} components of
     *               {@link PointI} instances, stored in alternating index positions
     * @return the {@link PointI} array created from {@code points}
     * @throws IllegalArgumentException if {@code points} has an odd number of elements
     * @throws NullPointerException if {@code points} is {@code null}
     */
    public static PointI[] fromInts(int... points) {
        if (points.length % 2 != 0)
            throw new IllegalArgumentException("points.length % 2 != 0");

        final PointI[] output = new PointI[points.length / 2];

        for (int i = 0; i < output.length; i++)
            output[i] = new PointI(points[2 * i], points[2 * i + 1]);

        return output;
    }

    /**
     * Creates a {@link PointI} from the specified polar coordinates.
     * The calculated {@link #x} and {@link #y} coordinates are converted to the nearest
     * {@link Integer} values using {@link Fortran#nint} rounding. The resulting
     * {@link #length} and {@link #angle} may differ accordingly from the specified arguments.
     * 
     * @param length the distance from the coordinate origin to the {@link PointI}
     * @param angle the polar angle, in radians, of the {@link PointI}
     * @return a {@link PointI} whose {@link #length} and {@link #angle}
     *         approximately equal the specified {@code length} and {@code angle}
     * @throws ArithmeticException if any resulting Cartesian coordinate overflows {@link Integer}
     */
    public static PointI fromPolar(double length, double angle) {
        return new PointI(
            Fortran.nint(length * Math.cos(angle)),
            Fortran.nint(length * Math.sin(angle)));
    }

    /**
     * Determines if the {@link PointI} is collinear with the specified instances.
     * Returns {@code true} exactly if {@link #crossProductLength(PointI, PointI)}
     * is zero for {@code a} and {@code b}.
     * 
     * @param a the first {@link PointI} to examine
     * @param b the second {@link PointI} to examine
     * @return {@code true} if the {@link PointI} is collinear with {@code a} and {@code b},
     *         else {@code false}
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public boolean isCollinear(PointI a, PointI b) {
        return (crossProductLength(a, b) == 0);
    }

    /**
     * Gets the absolute length of the vector represented by the {@link PointI}.
     * Returns the square root of the sum of the squares of {@link #x} and {@link #y}.
     * 
     * @return the non-negative absolute length of the vector represented by the {@link PointI}
     */
    public double length() {
        return Math.sqrt((double) x * x + (double) y * y);
    }

    /**
     * Gets the squared absolute length of the vector represented by the {@link PointI}.
     * Returns the sum of the squares of {@link #x} and {@link #y}, widened to {@link Long} to avoid
     * {@link Integer} overflow. Use instead of {@link #length} if you only need the squared value.
     * 
     * @return a non-negative {@link Long} value that equals the square of {@link #length}
     */
    public long lengthSquared() {
        return ((long) x * x + (long) y * y);
    }

    /**
     * Multiplies the vectors represented by the specified {@link PointI} and this instance.
     * Returns the sum of the pairwise products of both instances' {@link #x} and {@link #y},
     * widened to {@link Long} to avoid {@link Integer} overflow. That sum equals
     * {@link #lengthSquared} if the specified {@code vector} equals this instance.
     * 
     * @param vector the {@link PointI} to multiply with this instance
     * @return a {@link Long} value that represents the scalar dot product
     *         of the specified {@code vector} and this instance
     * @throws NullPointerException if {@code vector} is {@code null}
     */
    public long multiply(PointI vector) {
        return ((long) x * vector.x + (long) y * vector.y);
    }

    /**
     * Restricts the {@link PointI} to the specified location range.
     * @param minX the smallest permissible {@link #x}
     * @param minY the smallest permissible {@link #y}
     * @param maxX the greatest permissible {@link #x}
     * @param maxY the greatest permissible {@link #y}
     * @return a {@link PointI} whose {@link #x} and {@link #y} coordinates equal
     *         those of this instance, restricted to the specified location range
     */
    public PointI restrict(int minX, int minY, int maxX, int maxY) {
        int x = this.x, y = this.y;

        if (x < minX) x = minX; else if (x > maxX) x = maxX;
        if (y < minY) y = minY; else if (y > maxY) y = maxY;

        return new PointI(x, y);
    }

    /**
     * Subtracts the location of the specified {@link PointI} from this instance.
     * @param point the {@link PointI} location to subtract from this instance
     * @return a {@link PointI} whose {@link #x} and {@link #y} equal the subtraction
     *         of the corresponding dimensions of the specified {@code point} from this instance
     * @throws ArithmeticException if the difference in any dimension overflows {@link Integer}
     * @throws NullPointerException if {@code point} is {@code null}
     */
    public PointI subtract(PointI point) {
        return new PointI(
                MathCompat.subtractExact(x, point.x),
                MathCompat.subtractExact(y, point.y));
    }

    /**
     * Converts the specified {@link PointI} array to an {@link Integer} array.
     * The returned array has twice as many elements as the specified
     * {@code points} and retains the same coordinate sequence.
     * 
     * @param points the {@link PointI} array to convert
     * @return an array containing the {@link #x} and {@link #y} components
     *         of all {@code points}, stored in alternating index positions
     * @throws NullPointerException if {@code points} or any of its elements is {@code null}
     */
    public static int[] toInts(PointI... points) {
        final int[] output = new int[2 * points.length];

        for (int i = 0; i < points.length; i++) {
            output[2 * i] = points[i].x;
            output[2 * i + 1] = points[i].y;
        }

        return output;
    }

    /**
     * Converts the {@link PointI} to a {@link PointD}.
     * @return a {@link PointD} whose {@link PointD#x} and {@link PointD#y}
     *         equal the corresponding dimensions of the {@link PointI}
     */
    public PointD toPointD() {
        return new PointD(x, y);
    }

    /**
     * Compares the specified {@link Object} to this {@link PointI} instance.
     * @param obj the {@link Object} to compare to this instance
     * @return {@code true} if {@code obj} is not {@code null} and a {@link PointI} instance whose 
     *         {@link #x} and {@link #y} equal those of this instance, else {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || !(obj instanceof PointI))
            return false;

        final PointI point = (PointI) obj;
        return (x == point.x && y == point.y);
    }

    /**
     * Returns a hash code for the {@link PointI}.
     * @return an {@link Integer} hash code for the {@link PointI}
     */
    @Override
    public int hashCode() {
        return (31 * x + y);
    }

    /**
     * Returns a {@link String} representation of the {@link PointI}.
     * @return a {@link String} containing the values of {@link #x} and {@link #y}
     */
    @Override
    public String toString() {
        return String.format("PointI[x=%d, y=%d]", x, y);
    }
}
