package org.kynosarges.tektosyne.geometry;

import org.kynosarges.tektosyne.*;

/**
 * Represents a location in two-dimensional space, using {@link Double} coordinates.
 * {@link PointD} contains two immutable non-negative {@link Double} dimensions,
 * representing either a two-dimensional location or a two-dimensional vector.
 * Use {@link PointI} to represent points with {@link Integer} dimensions.
 * 
 * @author Christoph Nahr
 * @version 6.1.0
 */
public final class PointD {

    /** The x-coordinate of the {@link PointD}. */
    public final double x;

    /** The y-coordinate of the {@link PointD}. */
    public final double y;

    /**
     * An empty read-only {@link PointD}.
     * Both {@link #x} and {@link #y} are set to zero.
     */
    public static final PointD EMPTY = new PointD();

    /**
     * Creates a {@link PointD} with the coordinate origin.
     * Both {@link #x} and {@link #y} are set to zero.
     */
    public PointD() {
        this.x = 0;
        this.y = 0;
    }

    /**
     * Creates a {@link PointD} with the specified {@link Double} coordinates.
     * @param x the x-coordinate of the {@link PointD}
     * @param y the y-coordinate of the {@link PointD} 
     */
    public PointD(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Adds the location of the specified {@link PointD} to this instance.
     * @param point the {@link PointD} whose location to add to this instance
     * @return a {@link PointD} whose {@link #x} and {@link #y} equal the addition of the
     *         corresponding dimensions of the specified {@code point} to this instance
     * @throws NullPointerException if {@code point} is {@code null}
     */
    public PointD add(PointD point) {
        return new PointD(x + point.x, y + point.y);
    }

    /**
     * Gets the polar angle of the vector represented by the {@link PointD}, in radians.
     * Returns the result of {@link Math#atan2} for {@link #y} and {@link #x}, 
     * within the interval [-{@link Math#PI}, +{@link Math#PI}].
     * 
     * @return the polar angle, in radians, of the vector represented by the {@link PointD},
     *         or zero if {@link #x} and {@link #y} both equal zero
     */
    public double angle() {
        return Math.atan2(y, x);
    }

    /**
     * Computes the angle between the vector represented by the {@link PointD} and the specified vector.
     * Returns the result of {@link Math#atan2} for the cross-product length and the scalar dot product
     * of the two vectors. The possible range of values is [-{@link Math#PI}, +{@link Math#PI}].
     * 
     * @param vector the {@link PointD} vector to compare with this instance
     * @return the angle, in radians, between this instance and the specified {@code vector}, in that order
     * @throws NullPointerException if {@code vector} is {@code null}
     */
    public double angleBetween(PointD vector) {

        final double y = this.x * vector.y - this.y * vector.x;
        final double x = this.x * vector.x + this.y * vector.y;

        return Math.atan2(y, x);
    }

    /**
     * Computes the angle between the vectors from the {@link PointD} to the specified locations.
     * Returns the result of {@link Math#atan2} for the cross-product length and the scalar dot product
     * of the two vectors. The possible range of values is [-{@link Math#PI}, +{@link Math#PI}].
     * 
     * @param a the {@link PointD} location where the first vector ends
     * @param b the {@link PointD} location where the second vector ends
     * @return the angle, in radians, between the vectors from this instance to {@code a}
     *         and from this instance to {@code b}, in that order
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public double angleBetween(PointD a, PointD b) {

        final double ax = a.x - this.x;
        final double ay = a.y - this.y;
        final double bx = b.x - this.x;
        final double by = b.y - this.y;

        final double y = ax * by - ay * bx;
        final double x = ax * bx + ay * by;

        return Math.atan2(y, x);
    }

    /**
     * Computes the length of the cross-product of the vector represented by the {@link PointD}
     * and the specified vector. The absolute value equals the area of the parallelogram
     * spanned by the {@link PointD} and the specified {@code vector}. The sign indicates their
     * spatial relationship, as described in {@link #crossProductLength(PointD, PointD)}.
     * 
     * @param vector the {@link PointD} vector to multiply with this instance
     * @return a {@link Double} value indicating the length of the cross-product
     *         of this instance and the specified {@code vector}, in that order
     * @throws NullPointerException if {@code vector} is {@code null}
     */
    public double crossProductLength(PointD vector) {
        return (x * vector.y - vector.x * y);
    }

    /**
     * Computes the length of the cross-product of the vectors from the {@link PointD}
     * to the specified locations. The absolute value equals the area of the parallelogram
     * spanned by the vectors from the {@link PointD} to {@code a} and {@code b}.
     * The sign indicates their spatial relationship of the two vectors, as follows:
     * <ul>
     * <li>Less than zero — The sequence from this instance to {@code a} and then {@code b}
     * constitutes a right-hand turn, assuming y-coordinates increase upward.</li>
     * <li>Zero — This instance, {@code a}, and {@code b} are collinear.</li>
     * <li>Greater than zero — The sequence from this instance to {@code a} and then {@code b}
     * constitutes a left-hand turn, assuming y-coordinates increase upward.</li>
     * </ul>
     * 
     * @param a the {@link PointD} location where the first vector ends
     * @param b the {@link PointD} location where the second vector ends
     * @return a {@link Double} value indicating the length of the cross-product of the vectors
     *         from this instance to {@code a} and from this instance to {@code b}, in that order
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public double crossProductLength(PointD a, PointD b) {
        return ((a.x - x) * (b.y - y) - (b.x - x) * (a.y - y));
    }

    /**
     * Compares two {@link PointD} instances for equality, given the specified epsilon.
     * @param a the first {@link PointD} to compare
     * @param b the second {@link PointD} to compare
     * @param epsilon the maximum absolute difference where the corresponding dimensions
     *                of {@code a} and {@code b} are considered equal
     * @return {@code true} if the absolute difference between both corresponding dimensions
     *         of {@code a} and {@code b} is no greater than {@code epsilon}, else {@code false}
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public static boolean equals(PointD a, PointD b, double epsilon) {
        if (epsilon < 0)
            throw new IllegalArgumentException("epsilon < 0");

        return (Math.abs(a.x - b.x) <= epsilon
             && Math.abs(a.y - b.y) <= epsilon);
    }

    /**
     * Converts the specified {@link Double} array to a {@link PointD} array.
     * The returned array has half as many elements as the specified {@code points}
     * and retains the same coordinate sequence. Expects input suitable for the JavaFX
     * {@link javafx.scene.shape.Polygon} and {@link javafx.scene.shape.Polyline} classes.
     * 
     * @param points an array containing the {@link #x} and {@link #y} components of
     *               {@link PointD} instances, stored in alternating index positions
     * @return the {@link PointD} array created from {@code points}
     * @throws IllegalArgumentException if {@code points} has an odd number of elements
     * @throws NullPointerException if {@code points} is {@code null}
     */
    public static PointD[] fromDoubles(double... points) {
        if (points.length % 2 != 0)
            throw new IllegalArgumentException("points.length % 2 != 0");

        final PointD[] output = new PointD[points.length / 2];

        for (int i = 0; i < output.length; i++)
            output[i] = new PointD(points[2 * i], points[2 * i + 1]);

        return output;
    }

    /**
     * Creates a {@link PointD} from the specified polar coordinates.
     * Returns {@link #EMPTY} if {@code length} equals zero, and inverts the signs
     * of {@link #x} and {@link #y} if {@code length} is less than zero.
     * 
     * @param length the distance from the coordinate origin to the {@link PointD}
     * @param angle the polar angle, in radians, of the {@link PointD}
     * @return a {@link PointD} whose {@link #length} and {@link #angle}
     *         equal the specified {@code length} and {@code angle}
     */
    public static PointD fromPolar(double length, double angle) {
        return new PointD(
                length * Math.cos(angle),
                length * Math.sin(angle));
    }

    /**
     * Determines if the {@link PointD} is collinear with the specified instances.
     * Returns {@code true} exactly if {@link #crossProductLength(PointD, PointD)}
     * is zero for {@code a} and {@code b}.
     * 
     * @param a the first {@link PointD} to examine
     * @param b the second {@link PointD} to examine
     * @return {@code true} if the {@link PointD} is collinear with {@code a} and {@code b}, else {@code false}
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public boolean isCollinear(PointD a, PointD b) {
        return (crossProductLength(a, b) == 0);
    }

    /**
     * Determines if the {@link PointD} is collinear with the specified instances,
     * given the specified epsilon.
     * Returns {@code true} exactly if {@link #crossProductLength(PointD, PointD) }
     * is no greater than {@code epsilon} for {@code a} and {@code b}.
     * 
     * @param a the first {@link PointD} to examine
     * @param b the second {@link PointD} to examine
     * @param epsilon the maximum absolute value at which the result of
     *                {@link #crossProductLength} should be considered zero
     * @return {@code true} if the {@link PointD} is collinear with {@code a} and {@code b}, else {@code false}
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public boolean isCollinear(PointD a, PointD b, double epsilon) {
        if (epsilon < 0)
            throw new IllegalArgumentException("epsilon < 0");

        return (Math.abs(crossProductLength(a, b)) <= epsilon);
    }

    /**
     * Gets the absolute length of the vector represented by the {@link PointD}.
     * Returns the square root of the sum of the squares of {@link #x} and {@link #y}.
     * 
     * @return the non-negative absolute length of the vector represented by the {@link PointD}
     */
    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    /**
     * Gets the squared absolute length of the vector represented by the {@link PointD}.
     * Returns the sum of the squares of {@link #x} and {@link #y}.
     * Use instead of {@link #length} if you only need the squared value.
     * 
     * @return a non-negative {@link Double} value that equals the square of {@link #length}
     */
    public double lengthSquared() {
        return (x * x + y * y);
    }

    /**
     * Moves the {@link PointD} by the specified distance in the specified direction.
     * Moves toward {@code target} if {@code distance} is positive, and away from {@code target}
     * if {@code distance} is negative. Returns the current instance if it equals {@code target}
     * or if {@code distance} is zero.
     * 
     * @param target the {@link PointD} location that indicates the direction of the move
     * @param distance the amount by which to move towards {@code target}
     * @return a {@link PointD} that equals this instance, moved toward or away from
     *         {@code target} by the specified {@code distance}
     * @throws NullPointerException if {@code target} is {@code null}
     */
    public PointD move(PointD target, double distance) {
        if (distance == 0) return this;
        final double dx = target.x - x, dy = target.y - y;

        if (dx == 0) {
            if (dy > 0)
                return new PointD(x, y + distance);
            else if (dy < 0)
                return new PointD(x, y - distance);
            else
                return this;
        }

        if (dy == 0) {
            if (dx > 0)
                return new PointD(x + distance, y);
            else
                return new PointD(x - distance, y);
        }

        final double length = Math.sqrt(dx * dx + dy * dy);
        return new PointD(
            x + distance * dx / length,
            y + distance * dy / length);
    }

    /**
     * Multiplies the vectors represented by the specified {@link PointD} and this instance.
     * Returns the sum of the pairwise products of both instances' {@link #x} and {@link #y}.
     * That sum equals {@link #lengthSquared} if the specified {@code vector} equals this instance.
     * 
     * @param vector the {@link PointD} to multiply with this instance
     * @return a {@link Double} value that represents the scalar dot product
     *         of the specified {@code vector} and this instance
     * @throws NullPointerException if {@code vector} is {@code null}
     */
    public double multiply(PointD vector) {
        return (x * vector.x + y * vector.y);
    }

    /**
     * Normalizes the vector represented by the {@link PointD}.
     * @return a {@link PointD} with the same {@link #angle} as this instance,
     *         and whose {@link #length} equals one
     */
    public PointD normalize() {
        final double angle = Math.atan2(y, x);
        return new PointD(Math.cos(angle), Math.sin(angle));
    }

    /**
     * Restricts the {@link PointD} to the specified location range.
     * @param minX the smallest permissible {@link #x}
     * @param minY the smallest permissible {@link #y}
     * @param maxX the greatest permissible {@link #x}
     * @param maxY the greatest permissible {@link #y}
     * @return a {@link PointD} whose {@link #x} and {@link #y} coordinates equal
     *         those of this instance, restricted to the specified location range
     */
    public PointD restrict(double minX, double minY, double maxX, double maxY) {
        double x = this.x, y = this.y;

        if (x < minX) x = minX; else if (x > maxX) x = maxX;
        if (y < minY) y = minY; else if (y > maxY) y = maxY;

        return new PointD(x, y);
    }

    /**
     * Converts the {@link PointD} to a {@link PointI} by rounding dimensions to the
     * nearest {@link Integer} values. Uses {@link Fortran#nint} for rounding.
     * 
     * @return a {@link PointI} whose {@link PointI#x} and {@link PointI#y}
     *         equal the corresponding dimensions of the {@link PointD},
     *         rounded to the nearest {@link Integer} values
     * @throws ArithmeticException if any dimension overflows {@link Integer}
     */
    public PointI round() {
        return new PointI(Fortran.nint(x), Fortran.nint(y));
    }

    /**
     * Subtracts the location of the specified {@link PointD} from this instance.
     * @param point the {@link PointD} location to subtract from this instance
     * @return a {@link PointD} whose {@link #x} and {@link #y} equal the subtraction
     *         of the corresponding dimensions of the specified {@code point} from this instance
     * @throws NullPointerException if {@code point} is {@code null}
     */
    public PointD subtract(PointD point) {
        return new PointD(x - point.x, y - point.y);
    }

    /**
     * Converts the specified {@link PointD} array to a {@link Double} array.
     * The returned array has twice as many elements as the specified {@code points}
     * and retains the same coordinate sequence. Produces output suitable for the JavaFX
     * {@link javafx.scene.shape.Polygon} and {@link javafx.scene.shape.Polyline} classes.
     * 
     * @param points the {@link PointD} array to convert
     * @return an array containing the {@link #x} and {@link #y} components
     *         of all {@code points}, stored in alternating index positions
     * @throws NullPointerException if {@code points} or any of its elements is {@code null}
     */
    public static double[] toDoubles(PointD... points) {
        final double[] output = new double[2 * points.length];

        for (int i = 0; i < points.length; i++) {
            output[2 * i] = points[i].x;
            output[2 * i + 1] = points[i].y;
        }

        return output;
    }

    /**
     * Converts the {@link PointD} to a {@link PointI} by truncating dimensions to the
     * nearest {@link Integer} values. Uses {@link Integer} casts for truncation.
     * 
     * @return a {@link PointI} whose {@link PointI#x} and {@link PointI#y}
     *         equal the corresponding dimensions of the {@link PointD},
     *         truncated to the nearest {@link Integer} values
     * @throws ArithmeticException if any dimension overflows {@link Integer}
     */
    public PointI toPointI() {
        return new PointI(
                MathUtils.toIntExact(x),
                MathUtils.toIntExact(y));
    }

    /**
     * Compares the specified {@link Object} to this {@link PointD} instance.
     * @param obj the {@link Object} to compare to this instance
     * @return {@code true} if {@code obj} is not {@code null} and a {@link PointD} instance whose 
     *         {@link #x} and {@link #y} equal those of this instance, else {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || !(obj instanceof PointD))
            return false;

        final PointD point = (PointD) obj;
        return (x == point.x && y == point.y);
    }

    /**
     * Returns a hash code for the {@link PointD}.
     * @return an {@link Integer} hash code for the {@link PointD}
     */
    @Override
    public int hashCode() {
        final long xHash = Double.doubleToLongBits(x);
        final long yHash = Double.doubleToLongBits(y);
        return (31 * (int) (xHash ^ (xHash >>> 32))
                   + (int) (yHash ^ (yHash >>> 32)));
    }

    /**
     * Returns a {@link String} representation of the {@link PointD}.
     * @return a {@link String} containing the values of {@link #x} and {@link #y}
     */
    @Override
    public String toString() {
        return String.format("PointD[x=%g, y=%g]", x, y);
    }
}
