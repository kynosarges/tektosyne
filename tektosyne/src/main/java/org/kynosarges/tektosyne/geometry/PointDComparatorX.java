package org.kynosarges.tektosyne.geometry;

import org.kynosarges.tektosyne.*;

/**
 * Provides methods that compare two {@link PointD} instances, preferring {@link PointD#x} coordinates.
 * Defines a lexicographic ordering for {@link PointD} instances, sorting first by
 * {@link PointD#x} and then by {@link PointD#y} coordinates. Use {@link PointDComparatorY}
 * to sort first by {@link PointD#y} coordinates.
 * <p>
 * Coordinate comparisons may be performed precisely or with a specified epsilon.
 * The actual comparisons are performed by two static methods, so you need to instantiate
 * this class only when required by a consumer.</p>
 *
 * @author Christoph Nahr
 * @version 6.0.1
 */
public final class PointDComparatorX extends PointDComparator {
    /**
     * The class fingerprint that is set to indicate serialization
     * compatibility with a previous version of the class.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Creates a {@link PointDComparatorX} with an epsilon of zero.
     */
    public PointDComparatorX() {
        super();
    }

    /**
     * Creates a {@link PointDComparatorX} with the specified epsilon.
     * @param epsilon the maximum absolute difference at which coordinates should be considered equal
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     */
    public PointDComparatorX(double epsilon) {
        super(epsilon);
    }

    /**
     * Compares two specified {@link PointD} instances and returns an indication of their
     * lexicographic ordering, given the current epsilon for coordinate comparisons.
     * Dispatches to {@link #compareExact} if epsilon is zero, else to {@link #compareEpsilon}.
     * 
     * @param a the first {@link PointD} to compare
     * @param b the second {@link PointD} to compare
     * @return a negative value, zero, or a positive value if {@code a} compares less than,
     *         equal to, or greater than {@code b}, respectively, given the current epsilon
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    @Override
    public int compare(PointD a, PointD b) {
        return (epsilon == 0 ?
                compareExact(a, b) :
                compareEpsilon(a, b, epsilon));
    }

    /**
     * Compares two specified {@link PointD} instances and returns an indication of their
     * lexicographic ordering, given the current epsilon for coordinate comparisons.
     * Slightly faster than {@link #compare} if the current epsilon is known to be positive.
     * 
     * @param a the first {@link PointD} to compare
     * @param b the second {@link PointD} to compare
     * @return a negative value, zero, or a positive value if {@code a} compares less than,
     *         equal to, or greater than {@code b}, respectively, given the current epsilon
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public int compareEpsilon(PointD a, PointD b) {
        return compareEpsilon(a, b, epsilon);
    }

    /**
     * Compares two specified {@link PointD} instances and returns an indication of their
     * lexicographic ordering, given the specified epsilon for coordinate comparisons.
     * Uses {@link MathUtils#compare} for comparisons.
     * 
     * @param a the first {@link PointD} to compare
     * @param b the second {@link PointD} to compare
     * @param epsilon the maximum absolute difference where coordinates are considered equal
     * @return a negative value, zero, or a positive value if {@code a} compares less than,
     *         equal to, or greater than {@code b}, respectively, given the specified {@code epsilon}
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public static int compareEpsilon(PointD a, PointD b, double epsilon) {
        if (a == b) return 0; // identical objects

        final int result = MathUtils.compare(a.x, b.x, epsilon);
        if (result != 0) return result;
        return MathUtils.compare(a.y, b.y, epsilon);
    }

    /**
     * Compares two specified {@link PointD} instances and returns an indication of their
     * lexicographic ordering, using exact coordinate comparisons.
     * @param a the first {@link PointD} to compare
     * @param b the second {@link PointD} to compare
     * @return a negative value, zero, or a positive value if {@code a} compares less than,
     *         equal to, or greater than {@code b}, respectively
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public static int compareExact(PointD a, PointD b) {
        if (a == b) return 0; // identical objects

        if (a.x < b.x) return -1; if (a.x > b.x) return +1;
        if (a.y < b.y) return -1; if (a.y > b.y) return +1;

        return 0;
    }

    /**
     * Gets the primary dimension of the specified {@link PointD}.
     * @param point the {@link PointD} whose primary dimension to return
     * @return the {@link PointD#x} component of {@code point}
     * @throws NullPointerException if {@code point} is {@code null}
     */
    @Override
    protected double getPrimary(PointD point) {
        return point.x;
    }

    /**
     * Gets the secondary dimension of the specified {@link PointD}.
     * @param point the {@link PointD} whose secondary dimension to return
     * @return the {@link PointD#y} component of {@code point}
     * @throws NullPointerException if {@code point} is {@code null}
     */
    @Override
    protected double getSecondary(PointD point) {
        return point.y;
    }
}
