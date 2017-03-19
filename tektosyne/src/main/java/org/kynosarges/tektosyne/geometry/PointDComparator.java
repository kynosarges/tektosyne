package org.kynosarges.tektosyne.geometry;

import java.util.*;

/**
 * Provides methods that compare two {@link PointD} instances,
 * given a specified epsilon for lexicographic coordinate comparisons.
 * Defines a {@link Comparator} that lexicographically compares {@link PointD} instances
 * with a given epsilon, applied to each dimension. {@link PointDComparatorX} and
 * {@link PointDComparatorY} provide implementations whose lexicographic ordering
 * prefers x- and y-coordinates, respectively.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public abstract class PointDComparator implements Comparator<PointD> {
    /**
     * The epsilon used for coordinate comparisons.
     * Defines the maximum absolute difference at which coordinates should be considered equal.
     * Zero indicates that exact coordinate comparisons should be used.
     * <p>
     * <b>Note:</b> {@link List#sort} throws {@link IllegalArgumentException} when supplied a
     * {@link PointDComparator} whose {@link #epsilon} overlaps the coordinates of distinct
     * {@link PointD} instances in both dimensions, resulting in the classification of
     * {@link PointD} instances as equal when their {@link PointD#equals} and {@link PointD#hashCode}
     * results signal inequality. ("Comparison method violates its general contract.")
     */
    public final double epsilon;

    /**
     * Creates a {@link PointDComparator} with the specified epsilon.
     * @param epsilon the maximum absolute difference at which coordinates should be considered equal
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     */
    public PointDComparator(double epsilon) {
        if (epsilon < 0)
            throw new IllegalArgumentException("epsilon < 0");

        this.epsilon = epsilon;
    }

    /**
     * Searches a sorted {@link PointD} list for the element nearest to the specified location,
     * given the current epsilon for coordinate comparisons.
     * First approximates the index position of {@code q} within {@code points} by a lexicographic
     * binary search, using {@link PointDComparator} methods with the current epsilon. The search
     * is then expanded to both increasing and decreasing index positions, using the Euclidean
     * distance of the first approximation, or of any subsequently found nearer element,
     * as the maximum search radius.
     * <p>
     * Once the vertical ({@link PointDComparatorY}) or horizontal ({@link PointDComparatorX})
     * distances of the tested {@code points} elements in both directions exceed the search
     * radius, {@code findNearest} returns the zero-based index of the element with the smallest
     * Euclidean distance to {@code q}.</p>
     * <p>
     * The worst-case runtime is O(ld n + n), where n is the total number of {@code points}.
     * However, the runtime for an evenly distributed point set is close to O(ld n) since
     * comparisons can be limited to a relatively narrow vertical or horizontal distance
     * around the initial approximation.</p>
     * 
     * @param points a {@link List} containing the {@link PointD} locations to search,
     *               sorted lexicographically using the current {@link PointDComparator}
     * @param q the {@link PointD} location to find in {@code points}
     * @return the zero-based index of any occurrence of {@code q} in {@code points}, if found;
     *         otherwise, the zero-based index of the {@code points} element with the smallest
     *         Euclidean distance to {@code q}
     * @throws NullPointerException if {@code points} or {@code q} is {@code null},
     *         or {@code points} is empty or contains any {@code null} elements
     */
    public int findNearest(List<PointD> points, PointD q) {
        if (points == null || points.isEmpty())
            throw new NullPointerException("points");

        final int last = points.size() - 1;
        if (last == 0) return 0;

        /*
         * We can derive the initial approximation either from a lexicographic binary search
         * or from a simple comparison of the query y-coordinate to the total vertical range.
         * In benchmarks, both work nearly equally well, but binary search has a slight edge.
         * Apparently the closer approximation more than compensates for the additional work.
         * The code shown below is prefers y-coordinates (PointDComparatorY).
         */
        /*
        // determine range of y-coordinates
        double y0 = points.get(0).y, y1 = points.get(last).y;
        assert(y1 >= y0);

        // approximate index of query y-coordinate
        int index;
        if (q.y <= y0)
            index = 0;
        else if (q.y >= y1)
            index = last;
        else {
            assert(y0 < y1);
            index = (int) (points.size() * (q.y - y0) / (y1 - y0));
            assert(index >= 0 && index < points.size());
        }
         */
        
        // use binary search for lexicographic approximation
        int index = Collections.binarySearch(points, q, this);

        /*
         * Return immediate binary search hit only if epsilon is zero.
         * Otherwise, we still need to search for nearer points in the vicinity,
         * as we might have found a non-nearest point within epsilon distance.
         */
        if (index < 0)
            index = Math.min(~index, last);
        else if (epsilon == 0)
            return index;

        // restrict search radius to first approximation
        PointD vector = points.get(index).subtract(q);
        double minDistance = vector.lengthSquared();
        if (minDistance == 0) return index;

        int minIndex = index;
        final double epsilon2 = 2 * epsilon;

        // expand search in both directions until radius exceeded
        boolean searchPlus = true, searchMinus = true;
        for (int search = 1; searchPlus || searchMinus; search++) {

            if (searchPlus) {
                int i = index + search;
                if (i > last)
                    searchPlus = false;
                else {
                    // check if we exceeded search radius
                    vector = points.get(i).subtract(q);
                    final double delta = Math.abs(getPrimary(vector)) - epsilon2;
                    if (delta * delta - epsilon2 > minDistance)
                        searchPlus = false;
                    else {
                        // check if we found smaller distance
                        final double distance = vector.lengthSquared();
                        if (minDistance > distance) {
                            if (distance == 0) return i;
                            minDistance = distance;
                            minIndex = i;
                        }
                    }
                }
            }

            if (searchMinus) {
                int i = index - search;
                if (i < 0)
                    searchMinus = false;
                else {
                    // check if we exceeded search radius
                    vector = points.get(i).subtract(q);
                    final double delta = Math.abs(getPrimary(vector)) - epsilon2;
                    if (delta * delta - epsilon2 > minDistance)
                        searchMinus = false;
                    else {
                        // check if we found smaller distance
                        final double distance = vector.lengthSquared();
                        if (minDistance > distance) {
                            if (distance == 0) return i;
                            minDistance = distance;
                            minIndex = i;
                        }
                    }
                }
            }
        }

        return minIndex;
    }

    /**
     * Searches a {@link NavigableSet} for the {@link PointD} element nearest to the
     * specified location, given the current epsilon for coordinate comparisons.
     * First approximates the vicinity of {@code q} within {@code points} using {@link
     * NavigableSet#headSet} and {@link NavigableSet#tailSet}. The search is then expanded
     * by both ascending and descending iteration, using the Euclidean distance of the first
     * approximation, or of any subsequently found nearer element, as the maximum search radius.
     * <p>
     * Once the vertical ({@link PointDComparatorY}) or horizontal ({@link PointDComparatorX})
     * distances of the tested {@code points} elements in both directions exceed the search radius,
     * {@code findNearest} returns the element with the smallest Euclidean distance to {@code q}.</p>
     * <p>
     * The actual runtime depends on the supplied implementation of {@link NavigableSet}.
     * For the algorithm to work, {@code points} must use the {@link PointDComparator} itself
     * as its {@link SortedSet#comparator}. This condition is accordingly checked.</p>
     * 
     * @param points a {@link NavigableSet} containing the {@link PointD} locations to search,
     *               sorted lexicographically using the current {@link PointDComparator}
     * @param q the {@link PointD} location to find in {@code points}
     * @return the specified {@code q} if found in {@code points}; otherwise, the
     *         {@code points} element with the smallest Euclidean distance to {@code q}
     * @throws IllegalArgumentException if the {@link SortedSet#comparator} of {@code points}
     *         differs from the current {@link PointDComparator} instance
     * @throws NullPointerException if {@code points} or {@code q} is {@code null},
     *         or {@code points} is empty or contains any {@code null} elements
     */
    public PointD findNearest(NavigableSet<PointD> points, PointD q) {
        if (points == null || points.isEmpty())
            throw new NullPointerException("points");
        if (points.comparator() != this)
            throw new IllegalArgumentException("points.comparator != this");

        if (points.size() == 1)
            return points.first();

        PointD minPoint = null;
        double minDistance = Double.MAX_VALUE;

        final NavigableSet<PointD> smallerSet = points.headSet(q, true);
        if (!smallerSet.isEmpty()) {
            final PointD smallerLast = smallerSet.last();
            if (compare(smallerLast, q) == 0)
                return smallerLast;

            minPoint = smallerLast;
            minDistance = minPoint.subtract(q).lengthSquared();
        }

        final NavigableSet<PointD> greaterSet = points.tailSet(q, true);
        if (!greaterSet.isEmpty()) {
            final PointD greaterFirst = greaterSet.first();
            if (compare(greaterFirst, q) == 0)
                return greaterFirst;
            
            if (minPoint == null) {
                minPoint = greaterFirst;
                minDistance = minPoint.subtract(q).lengthSquared();
            } else {
                double greaterDistance = greaterFirst.subtract(q).lengthSquared();
                if (minDistance > greaterDistance) {
                    minPoint = greaterFirst;
                    minDistance = greaterDistance;
                }
            }
        }

        assert(minPoint != null);
        assert(minDistance > 0);

        // expand search in valid directions until radius exceeded
        Iterator<PointD> smallerSearch = (smallerSet.isEmpty() ? null : smallerSet.descendingIterator());
        Iterator<PointD> greaterSearch = (greaterSet.isEmpty() ? null : greaterSet.iterator());

        final double epsilon2 = 2 * epsilon;
        while (smallerSearch != null || greaterSearch != null) {

            if (greaterSearch != null) {
                if (!greaterSearch.hasNext())
                    greaterSearch = null;
                else {
                    // check if we exceeded search radius
                    final PointD next = greaterSearch.next();
                    final PointD vector = next.subtract(q);
                    final double delta = Math.abs(getPrimary(vector)) - epsilon2;
                    if (delta * delta - epsilon2 > minDistance)
                        greaterSearch = null;
                    else {
                        // check if we found smaller distance
                        final double distance = vector.lengthSquared();
                        if (minDistance > distance) {
                            if (distance == 0) return next;
                            minDistance = distance;
                            minPoint = next;
                        }
                    }
                }
            }

            if (smallerSearch != null) {
                if (!smallerSearch.hasNext())
                    smallerSearch = null;
                else {
                    // check if we exceeded search radius
                    final PointD next = smallerSearch.next();
                    final PointD vector = next.subtract(q);
                    final double delta = Math.abs(getPrimary(vector)) - epsilon2;
                    if (delta * delta - epsilon2 > minDistance)
                        smallerSearch = null;
                    else {
                        // check if we found smaller distance
                        final double distance = vector.lengthSquared();
                        if (minDistance > distance) {
                            if (distance == 0) return next;
                            minDistance = distance;
                            minPoint = next;
                        }
                    }
                }
            }
        }

        return minPoint;
    }

    /**
     * Finds all entries in the specified {@link NavigableMap} whose {@link PointD} keys are
     * within the specified {@link RectD}, given the current epsilon for coordinate comparisons.
     * Always returns a new {@link NavigableMap} with the current {@link PointDComparator}.
     * 
     * @param <V> the type of all values in the {@link NavigableMap}
     * @param map the {@link NavigableMap} whose {@link PointD} keys to search
     * @param range a {@link RectD} indicating the coordinate range to find
     * @return a {@link NavigableMap} containing all {@code map} entries
     *         whose {@link PointD} keys are within {@code range}
     * @throws IllegalArgumentException if the {@link SortedSet#comparator} of {@code map}
     *         differs from the current {@link PointDComparator} instance
     * @throws NullPointerException if {@code map} or {@code range} is {@code null}
     */
    public <V> NavigableMap<PointD, V> findRange(NavigableMap<PointD, V> map, RectD range) {
        if (map.comparator() != this)
            throw new IllegalArgumentException("map.comparator != this");

        final NavigableMap<PointD, V> found = new TreeMap<>(this);
        final double minSec = getSecondary(range.min) - epsilon;
        final double maxSec = getSecondary(range.max) + epsilon;

        // add points within range (including borders)
        for (Map.Entry<PointD, V> entry: map.subMap(range.min, true, range.max, true).entrySet()) {
            final double sec = getSecondary(entry.getKey());
            if (sec >= minSec && sec <= maxSec)
                found.put(entry.getKey(), entry.getValue());
        }

        return found;
    }

    /**
     * Finds all {@link PointD} elements in the specified {@link NavigableSet} that are within
     * the specified {@link RectD}, given the current epsilon for coordinate comparisons.
     * Always returns a new {@link NavigableSet} with the current {@link PointDComparator}.
     * 
     * @param points the {@link NavigableSet} whose {@link PointD} elements to search
     * @param range a {@link RectD} indicating the coordinate range to find
     * @return a {@link NavigableSet} containing all {@code points} within {@code range}
     * @throws IllegalArgumentException if the {@link SortedSet#comparator} of {@code points}
     *         differs from the current {@link PointDComparator} instance
     * @throws NullPointerException if {@code points} or {@code range} is {@code null}
     */
    public NavigableSet<PointD> findRange(NavigableSet<PointD> points, RectD range) {
        if (points.comparator() != this)
            throw new IllegalArgumentException("points.comparator != this");

        final NavigableSet<PointD> found = new TreeSet<>(this);
        final double minSec = getSecondary(range.min) - epsilon;
        final double maxSec = getSecondary(range.max) + epsilon;

        // add points within range (including borders)
        for (PointD point: points.subSet(range.min, true, range.max, true)) {
            final double sec = getSecondary(point);
            if (sec >= minSec && sec <= maxSec)
                found.add(point);
        }

        return found;
    }

    /**
     * Gets the primary dimension of the specified {@link PointD}.
     * @param point the {@link PointD} whose primary dimension to return
     * @return the {@link PointD#x} or {@link PointD#y} component of {@code point}
     * @throws NullPointerException if {@code point} is {@code null}
     */
    protected abstract double getPrimary(PointD point);

    /**
     * Gets the secondary dimension of the specified {@link PointD}.
     * @param point the {@link PointD} whose secondary dimension to return
     * @return the {@link PointD#x} or {@link PointD#y} component of {@code point}
     * @throws NullPointerException if {@code point} is {@code null}
     */
    protected abstract double getSecondary(PointD point);
}
