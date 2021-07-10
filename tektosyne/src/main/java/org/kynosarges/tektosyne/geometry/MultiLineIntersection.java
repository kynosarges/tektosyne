package org.kynosarges.tektosyne.geometry;

import java.util.*;

/**
 * Provides algorithms to find all intersections between multiple line segments.
 * Provides two algorithms to find all intersections between multiple line segments,
 * specified as arrays of {@link LineD} instances:
 * <ul><li><p>
 * {@link MultiLineIntersection#find} provides a sweep line algorithm which is very
 * efficient if the number of intersections is much smaller than the number of lines.
 * </p></li><li><p>
 * {@link MultiLineIntersection#findSimple} provides a brute force algorithm which is
 * better suited for a small number of lines or a large number of intersections.
 * </p></li></ul>
 * Both algorithms sort their results lexicographically on the points of intersection, first
 * by increasing y-coordinates and then, in case of equality, by increasing x-coordinates.
 * The {@link MultiLinePoint} instance created for each point identifies all line segments
 * that contain the point. Please refer to the two methods for further details.
 * <p>
 * The sweep line algorithm implemented by {@link MultiLineIntersection#find} was first
 * described by J. L. Bentley and T. A. Ottmann, <em>Algorithms for reporting and counting
 * geometric intersections</em>, IEEE Transactions on Computers C-28 (1979), p.643-647.
 * An implementation outline is given by Mark de Berg et al., <em>Computational Geometry</em>
 * (3rd ed.), Springer-Verlag 2008, p.20-29, and a limited C++ implementation by Michael J. Laszlo,
 * <em>Computational Geometry and Computer Graphics in C++</em>, Prentice Hall 1996, p.173-181.</p>
 * <p>
 * Our implementation supports an unlimited number of line segments meeting at any intersection
 * point, and uses an improved sweep line comparer that raises the algorithm’s stability to the
 * same level as that of the brute force algorithm. A comment block at the top of the source code
 * file gives a detailed description of this comparer.</p>
 * 
 * @author Christoph Nahr
 * @version 6.1.0
 */
public final class MultiLineIntersection {
    /*
     * The Sweep Line Comparer (Status.compareLines)
     * ---------------------------------------------
     * A critical point in the sweep line algorithm is the design of the comparison function that
     * determines the order of line segments currently intersecting the moving sweep line. The
     * straightforward approach is to compute the intersection points and first order by those,
     * and then by line slopes when multiple lines intersect the sweep line at the same point.
     *
     * This approach has the obvious weakness that the "same" point is hard to identify, given the
     * precision limits of floating-point computation. Should the comparison method fail to sort
     * line segments correctly and consistently, the sweep line structure will become corrupted
     * and the algorithm will fail to output all intersections. One could use a comparison epsilon,
     * but that creates the risk of false positives when lines are near but do not (yet) cross.
     *
     * Laszlo’s solution is to compare line segments not on the sweep line itself, but slightly
     * above or below the sweep line. With an appropriate delta, the test point is never close to
     * an intersection and directly provides a consistent ordering. However, this method is also
     * susceptible to data corruption if the chosen delta is too large for very close event points,
     * or else too small for very narrow intersection angles.
     *
     * Our implementation takes a different approach. At each event point, we first remove all
     * ending and intersecting lines from the sweep line structure, then compute the intersections
     * of all remaining lines with the current sweep line, and then add (back) all starting and
     * intersecting lines. The intersection points of (re-)added lines are <em>not</em> computed
     * but simply copy the current event point location. This permits the use of exact coordinate
     * comparisons to reliably identify line intersections. The order of intersecting lines is
     * determined by their slopes, as usual.
     *
     * Our implementation performs about as well as Laszlo’s approach, but it is far more stable.
     * Testing shows the same perfect stability as for the brute force algorithm, even with large
     * input coordinates that reliably cause data corruption in the Laszlo implementation. The
     * {@link MultiLineIntersection#find} method still checks for evidence of data corruption and
     * throws an {@link IllegalStateException} upon detection, but this should not happen in the
     * current implementation. (The corruption tests do not impact performance.)
     */

    /**
     * Creates a {@link MultiLineIntersection} instance.
     * Private to prevent instantiation.
     */
    private MultiLineIntersection() { }

    /**
     * Finds all intersections between the specified line segments, using a sweep line algorithm.
     * Moves a horizontal sweep line across the specified {@code lines}, testing only those elements
     * for intersection which are adjacent along the sweep line. The runtime is O((n + k) log n)
     * where k is the number of discovered intersections.
     * <p>
     * {@code find} is very efficient when there are no or few intersections, achieving its best
     * performance if the specified {@code lines} are horizontal parallels. However, the event point
     * schedule and sweep line structure impose a large overhead if there are many candidate lines
     * to consider. The worst case of O(n^2) intersections is much slower than the brute force
     * algorithm implemented by {@link #findSimple}.</p>
     * <p>
     * {@code find} always uses exact coordinate comparisons. Epsilon comparisons would corrupt
     * the search structures due to the merging of nearby event points. Call {@link #findSimple}
     * with an epsilon argument to use epsilon comparisons.</p>
     * 
     * @param lines a {@link LineD} array containing the line segments to intersect
     * @return a lexicographically sorted {@link MultiLinePoint} array describing
     *         all points of intersection between the specified {@code lines}
     * @throws IllegalArgumentException if {@code lines} contains a {@link LineD}
     *         whose {@link LineD#start} and {@link LineD#end} points are equal
     * @throws IllegalStateException if the search structure was corrupted
     * @throws NullPointerException if {@code lines} or any of its elements is {@code null}
     */
    public static MultiLinePoint[] find(LineD[] lines) {

        final Status status = new Status();
        final List<EventPoint> crossings = status.findCore(lines);
        return EventPoint.output(crossings);
    }

    /**
     * Finds all intersections between the specified line segments, using a brute force algorithm.
     * Performs a pairwise intersection of every {@code lines} element with every other element.
     * The runtime is therefore always O(n^2), regardless of the number of intersections found.
     * <p>
     * However, the constant factor is low and O(n^2) intersections are found in optimal time because
     * {@code findSimple} performs no additional work to avoid testing for possible intersections.
     * For a small number of {@code lines} (n &lt; 50), {@code findSimple} usually beats the sweep
     * line algorithm implemented by {@link #find} regardless of the number of intersections.</p>
     * 
     * @param lines a {@link LineD} array containing the line segments to intersect
     * @return a lexicographically sorted {@link MultiLinePoint} array describing
     *         all points of intersection between the specified {@code lines}
     * @throws NullPointerException if {@code lines} or any of its elements is {@code null}
     */
    public static MultiLinePoint[] findSimple(LineD[] lines) {
        if (lines == null)
            throw new NullPointerException("lines");

        final TreeMap<PointD, EventPoint> crossings = new TreeMap<>(PointDComparatorY::compareExact);

        for (int i = 0; i < lines.length - 1; i++)
            for (int j = i + 1; j < lines.length; j++) {
                final LineIntersection crossing = lines[i].intersect(lines[j]);

                if (crossing.exists()) {
                    final PointD p = crossing.shared;
                    final EventPoint e = EventPoint.tryAdd(crossings, p);
                    e.tryAddLines(i, crossing.first, j, crossing.second);
                }
            }

        return EventPoint.output(crossings.values());
    }

    /**
     * Finds all intersections between the specified line segments, using a brute force algorithm
     * with the specified epsilon for coordinate comparisons.
     * Identical with the basic {@link #findSimple(LineD[]) } overload, but uses the
     * specified {@code epsilon} to determine intersections between the specified
     * {@code lines} and to combine nearby intersections.
     * 
     * @param lines a {@link LineD} array containing the line segments to intersect
     * @param epsilon the maximum absolute difference at which coordinates should be considered equal
     * @return a lexicographically sorted {@link MultiLinePoint} array describing
     *         all points of intersection between the specified {@code lines}
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     * @throws NullPointerException if {@code lines} or any of its elements is {@code null}
     */
    public static MultiLinePoint[] findSimple(LineD[] lines, double epsilon) {
        if (lines == null)
            throw new NullPointerException("lines");
        if (epsilon < 0)
            throw new IllegalArgumentException("epsilon < 0");

        final TreeMap<PointD, EventPoint> crossings = new TreeMap<>(
                (a, b) -> PointDComparatorY.compareEpsilon(a, b, epsilon));

        for (int i = 0; i < lines.length - 1; i++)
            for (int j = i + 1; j < lines.length; j++) {
                final LineIntersection crossing = lines[i].intersect(lines[j], epsilon);

                if (crossing.exists()) {
                    final PointD p = crossing.shared;
                    final EventPoint e = EventPoint.tryAdd(crossings, p);
                    e.tryAddLines(i, crossing.first, j, crossing.second);
                }
            }

        return EventPoint.output(crossings.values());
    }

    /**
     * Splits the specified line segments on the specified intersection points.
     * Returns a collection of {@link LineD} instances that are guaranteed not to intersect,
     * except at their {@link LineD#start} or {@link LineD#end} points. The specified
     * {@code crossings} are usually the result of {@link #find} or {@link #findSimple}
     * for the specified {@code lines}. 
     * <p>
     * {@code split} sets the {@link LineD#start} or {@link LineD#end} point of any {@link LineD}
     * that participates in a {@link LineLocation#START} or {@link LineLocation#END} intersection
     * to the {@link MultiLinePoint#shared} point of that intersection, so as to preserve
     * coordinate identities that were established by a positive comparison epsilon.</p>
     * 
     * @param lines a {@link LineD} array containing the line segments to split
     * @param crossings a {@link MultiLinePoint} array describing all points
     *                  of intersection between {@code lines}
     * @return a {@link LineD} array containing the line segments resulting from
     *         splitting all {@code lines} on the matching {@code crossings}
     * @throws ArrayIndexOutOfBoundsException if {@code crossings} contains any
     *         {@link MultiLinePoint.Line#index} values that are invalid for {@code lines}
     * @throws IllegalArgumentException if {@code crossings} contains any
     *         {@link MultiLinePoint.Line#location} other than {@link LineLocation#START},
     *         {@link LineLocation#BETWEEN}, or {@link LineLocation#END}
     * @throws NullPointerException if {@code lines} or {@code crossings} is {@code null}
     *                              or contains any {@code null} elements
     */
    public static LineD[] split(LineD[] lines, MultiLinePoint[] crossings) {
        if (lines == null)
            throw new NullPointerException("lines");
        if (crossings == null)
            throw new NullPointerException("crossings");

        // list of split segments, or null to use original line
        int count = lines.length;
        @SuppressWarnings({ "rawtypes", "unchecked" })
        final List<PointD>[] segmentPoints = new ArrayList[count];

        // process all epsilon-matched intersection points
        for (MultiLinePoint crossing: crossings)
            for (MultiLinePoint.Line line: crossing.lines) {
                final int index = line.index;
                List<PointD> points = segmentPoints[index];

                // initialize split segment list with start & end points
                if (points == null) {
                    points = new ArrayList<>(4);
                    points.add(lines[index].start);
                    points.add(lines[index].end);
                    segmentPoints[index] = points;
                }

                switch (line.location) {
                    case START:
                        // replace start point with epsilon-matched intersection
                        points.set(0, crossing.shared);
                        break;

                    case END:
                        // replace end point with epsilon-matched intersection
                        points.set(1, crossing.shared);
                        break;

                    case BETWEEN:
                        // add intersection point that defines new split segment
                        points.add(crossing.shared);
                        ++count;
                        break;

                    default:
                        throw new IllegalArgumentException("crossings contains invalid LineLocation");
                }
            }

        final SplitComparator comparer = new SplitComparator();
        final LineD[] segments = new LineD[count];
        int index = 0;

        for (int i = 0; i < lines.length; i++) {
            final List<PointD> points = segmentPoints[i];
            if (points == null) {
                // no intersections, store original line
                segments[index++] = lines[i];
            } else {
                assert(points.size() >= 2);

                // sort points by distance from start point
                comparer.setStart(points.get(0));
                points.sort(comparer);

                // convert sorted points to split line segments
                for (int j = 0; j < points.size() - 1; j++)
                    segments[index++] = new LineD(points.get(j), points.get(j+1));
            }
        }

        assert(index == count);
        return segments;
    }

    /**
     * Represents an event point encountered by any {@link MultiLineIntersection} algorithm.
     * Stores the immutable {@link #shared} coordinates at which an event occurs,
     * along with growing collections of the {@link #indices} of all intersecting input lines,
     * and their relative {@link EventPoint#locations} of the {@link #shared} coordinates.
     */
    private static final class EventPoint {
        /**
         * Creates an {@link EventPoint} with the specified shared coordinates.
         * @param shared the shared {@link PointD} coordinates of the {@link EventPoint}
         * @throws NullPointerException if {@code shared} is {@code null}
         */
        EventPoint(PointD shared) {
            if (shared == null)
                throw new NullPointerException("shared");

            this.shared = shared;
        }

        /**
         * The indices of all input lines that intersect at the {@link EventPoint}.
         * Each element holds the index of one of the input lines
         * that intersect at the {@link #shared} coordinates.
         */
        final List<Integer> indices = new ArrayList<>(2);

        /**
         * The locations of the {@link #shared} coordinates relative
         * to all intersecting input lines with the same {@link #indices}.
         */
        final List<LineLocation> locations = new ArrayList<>(2);

        /**
         * The shared {@link PointD} coordinates of the {@link EventPoint}.
         * Indicates the shared intersection point of all associated line {@link #indices}.
         */
        final PointD shared;

        /**
         * Adds the specified line event to the {@link EventPoint}.
         * Does not check whether {@link #indices} already contains {@code index}.
         * 
         * @param index the line index to add to {@link #indices}
         * @param location the {@link LineLocation} to add to {@link #locations}
         */
        void addLine(int index, LineLocation location) {
            indices.add(index);
            locations.add(location);
        }

        /**
         * Normalizes all {@link #locations} to match the corresponding {@link #indices},
         * according to the specified {@link LineD} instances.
         * Inverts any {@link LineLocation#START} or {@link LineLocation#END} values in
         * {@link #locations} whose corresponding {@link #indices} element indicates a
         * {@link LineD} in {@code lines} whose {@link LineD#start} and {@link LineD#end}
         * points have the opposite orientation.
         * <p>
         * Therefore, the {@link #locations} collection no longer reflects the sweep line
         * direction, but rather the point at which the corresponding {@link LineD} touches
         * the {@link #shared} coordinates. Call this method to prepare for output generation,
         * after the {@link EventPoint} has been removed from the {@link Status#schedule}.</p>
         * 
         * @param lines an array containing all input {@link LineD} segments indexed by {@link #indices}
         * @throws NullPointerException if {@code lines} is {@code null}
         */
        void normalize(LineD[] lines) {

            for (int i = 0; i < locations.size(); i++) {
                LineLocation location = locations.get(i);

                // check for start & end point events
                if (location == LineLocation.START || location == LineLocation.END) {
                    final LineD line = lines[indices.get(i)];

                    // correct orientation of inverted line
                    if (PointDComparatorY.compareExact(line.start, line.end) > 0) {
                        location = (location == LineLocation.START ?
                                LineLocation.END : LineLocation.START);
                        locations.set(i, location);
                    }
                }
            }
        }

        /**
         * Outputs the specified {@link EventPoint} collection to a {@link MultiLinePoint} array.
         * @param crossings an {@link EventPoint} collection that represent the complete results
         *                  of a {@link MultiLineIntersection} algorithm
         * @return an {@link MultiLinePoint} array containing the results of {@code crossings}
         * @throws NullPointerException if {@code crossings} is {@code null}
         */
        static MultiLinePoint[] output(Collection<EventPoint> crossings) {
            final MultiLinePoint[] output = new MultiLinePoint[crossings.size()];

            int i = 0;
            for (EventPoint e: crossings) {
                final int count = e.indices.size();
                assert(count == e.locations.size());

                final MultiLinePoint.Line[] lines = new MultiLinePoint.Line[count];
                for (int j = 0; j < count; j++)
                    lines[j] = new MultiLinePoint.Line(e.indices.get(j), e.locations.get(j));

                output[i++] = new MultiLinePoint(e.shared, lines);
            }

            return output;
        }

        /**
         * Adds an {@link EventPoint} for the specified {@link #shared} coordinates
         * to the specified {@link Map} if not already present.
         * 
         * @param map the {@link Map} that receives the {@link EventPoint}
         * @param p the {@link #shared} coordinates of the {@link EventPoint}
         * @return the existing {@link EventPoint} for {@code p} in {@code map} if already present,
         *         else the new {@link EventPoint} for {@code p} created and added to {@code map}
         */
        static EventPoint tryAdd(Map<PointD, EventPoint> map, PointD p) {
            final EventPoint e = new EventPoint(p);
            final EventPoint oldE = map.putIfAbsent(p, e);
            return (oldE != null ? oldE : e);
        }

        /**
         * Adds the specified line events to the {@link EventPoint},
         * unless {@link #indices} already contains events for the same line.
         * Skips either or both argument pairs if {@link #indices} already
         * contains the corresponding element.
         * 
         * @param index1 the first element to add to {@link #indices}
         * @param location1 the first element to add to {@link #locations}
         * @param index2 the second element to add to {@link #indices}
         * @param location2 the second element to add to {@link #locations}
         */
        void tryAddLines(int index1, LineLocation location1, int index2, LineLocation location2) {

            if (!indices.contains(index1)) {
                indices.add(index1);
                locations.add(location1);
            }

            if (!indices.contains(index2)) {
                indices.add(index2);
                locations.add(location2);
            }
        }
    }

    /**
     * {@link Comparator} to sort the list of split segment points in {@link #split}.
     * We cannot use lexicographic or other single-coordinate comparisons because
     * epsilon matching might cause coordinate aberrations in the wrong direction.
     * So we compare the squared distances of both points from the start point.
     */
    private static final class SplitComparator implements Comparator<PointD> {

        private PointD start = PointD.EMPTY;

        /**
         * Sets the start {@link PointD} for comparisons.
         * @param start the start {@link PointD} for comparisons
         * @throws NullPointerException if {@code start} is {@code null}
         */
        void setStart(PointD start) {
            if (start == null)
                throw new NullPointerException("start");

            this.start = start;
        }

        /**
         * Compares two specified {@link PointD} instances and returns an indication of their
         * relative ordering, as defined by their Euclidean distance from the current start point.
         * 
         * @param a the first {@link PointD} to compare
         * @param b the second {@link PointD} to compare
         * @return a negative value, zero, or a positive value if {@code a} compares less than,
         *         equal to, or greater than {@code b}, respectively, given the current start point
         * @throws NullPointerException if {@code a} or {@code b} is {@code null}
         */
        @Override
        public int compare(PointD a, PointD b) {

            final double ax = a.x - start.x, ay = a.y - start.y;
            final double bx = b.x - start.x, by = b.y - start.y;
            
            final double d = (ax * ax + ay * ay) - (bx * bx + by * by);
            if (d < 0) return -1;
            if (d > 0) return +1;

            return 0;
        }
    }

    /**
     * Implements the sweep line algorithm run by {@link #find}.
     * {@link Status} and {@link EventPoint} provide all methods and data structures required
     * by the sweep line algorithm. Each call to {@link #find} creates a new {@link Status}.
     */
    private static final class Status {
        /**
         * The {@link EventPoint#shared} coordinates
         * of the current {@link EventPoint} on the sweep line.
         */
        private PointD cursor;

        /**
         * All intersections between {@link #lines} that were discovered so far.
         */
        private final List<EventPoint> crossings;

        /**
         * All input {@link LineD} segments whose intersections are to be found.
         */
        private LineD[] lines;

        /**
         * The position at which the corresponding {@link #lines} element intersected
         * the sweep line at the last event that was not an end point event.
         * Each element holds the x-coordinate where the {@link #lines} element
         * with the same index intersects the current sweep line.
         */
        private double[] positions;

        /**
         * All event points that were discovered so far but not yet processed,
         * sorted lexicographically by increasing y- and then x-coordinates.
         */
        private final TreeMap<PointD, EventPoint> schedule;

        /**
         * The indices of all {@link #lines} that intersect the horizontal sweep line,
         * sorted by increasing x-coordinates of the intersection, then by increasing slopes.
         * {@link #sweepLine} is <em>not</em> naturally sorted by {@link Integer} values, but
         * according to {@link #compareLines} as detailed at the top of {@link MultiLineIntersection}.
         */
        private final TreeSet<Integer> sweepLine;

        /**
         * The precomputed inverse slope of the corresponding {@link #lines} element.
         * Each element holds the precomputed {@link LineD#inverseSlope}
         * of the {@link #lines} element with the same index.
         */
        private double[] slopes;

        /**
         * Creates a new {@link Status}.
         */
        Status() {
            this.crossings = new ArrayList<>();
            this.schedule = new TreeMap<>(PointDComparatorY::compareExact);
            this.sweepLine = new TreeSet<>(this::compareLines);
        }

        /**
         * Finds all intersections between the specified {@link LineD} segments,
         * using a sweep line algorithm.
         * Creates the intermediate output which is further processed
         * by the public {@link MultiLineIntersection#find} method.
         * 
         * @param lines an array containing all {@link LineD} segments to intersect
         * @return a lexicographically sorted {@link List} containing the final {@link EventPoint}
         *         for every point of intersection between two or more {@code lines}.
         * @throws IllegalArgumentException if {@code lines} contains a {@link LineD} whose
         *                                  {@link LineD#start} and {@link LineD#end} points are equal
         * @throws IllegalStateException if the search structure was corrupted
         * @throws NullPointerException if {@code lines} or any of its elements is {@code null}
         */
        List<EventPoint> findCore(LineD[] lines) {
            buildSchedule(lines);

            while (!schedule.isEmpty())
                handle(schedule.pollFirstEntry().getValue());

            if (!sweepLine.isEmpty())
                throw new IllegalStateException("search structure corrupt");

            return crossings;
        }

        /**
         * Adds an intersection {@link EventPoint} to the {@link #schedule} if the
         * two specified {@link #lines} indices indicate a line crossing.
         * If the {@link #schedule} already contains an {@link EventPoint} for the computed
         * intersection point, {@link #addCrossing} adds the indicated lines to the existing
         * {@link EventPoint} if they are not already present. Otherwise, the intersection
         * is added only if the shared point occurs after the {@link #cursor}, either to a
         * new or an existing {@link EventPoint} with the {@link EventPoint#shared} point.
         * 
         * @param a the first {@link #lines} index to examine
         * @param b the second {@link #lines} index to examine
         * @param e the current {@link EventPoint} which receives a detected crossing
         *          that occurs exactly at the {@link #cursor}
         * @throws NullPointerException if {@code e} is {@code null}
         */
        private void addCrossing(int a, int b, EventPoint e) {
            if (e == null)
                throw new NullPointerException("e");

            final LineIntersection c = lines[a].intersect(lines[b]);

            // ignore crossings that involve only start or end points,
            // as those line events have been scheduled during initialization
            if ((c.first == LineLocation.BETWEEN && LineLocation.contains(c.second)) ||
                (LineLocation.contains(c.first) && c.second == LineLocation.BETWEEN)) {

                // quit if crossing occurs before cursor
                final PointD p = c.shared;
                final int result = PointDComparatorY.compareExact(cursor, p);
                if (result > 0) return;

                // update schedule if crossing occurs after cursor
                if (result < 0) e = EventPoint.tryAdd(schedule, p);

                // add crossing to current or scheduled event point
                e.tryAddLines(a, c.first, b, c.second);
            }
        }

        /**
         * Builds the {@link #schedule} and precomputes all {@link #slopes}.
         * @param lines an array containing all input {@link LineD} segments to intersect
         * @throws IllegalArgumentException if {@code lines} contains a {@link LineD} whose
         *                                  {@link LineD#start} and {@link LineD#end} points are equal
         * @throws NullPointerException if {@code lines} or any of its elements is {@code null}
         */
        private void buildSchedule(LineD[] lines) {
            this.lines = lines;
            this.positions = new double[lines.length];
            this.slopes = new double[lines.length];

            for (int i = 0; i < lines.length; i++) {
                LineD line = lines[i];

                final int direction = PointDComparatorY.compareExact(line.start, line.end);
                if (direction == 0)
                    throw new IllegalArgumentException("lines contains empty LineD");

                // start & end point events use lexicographic ordering
                if (direction > 0) line = line.reverse();
                slopes[i] = line.inverseSlope();

                // add start point event for current line
                EventPoint e = EventPoint.tryAdd(schedule, line.start);
                e.addLine(i, LineLocation.START);

                // add end point event for current line
                e = EventPoint.tryAdd(schedule, line.end);
                e.addLine(i, LineLocation.END);
            }
        }

        /**
         * Compares two specified {@link #lines} indices and returns
         * an indication of their {@link #sweepLine} ordering.
         * See the comment at the top of {@link MultiLineIntersection} for details.
         * 
         * @param a the first {@link #lines} index to compare
         * @param b the second {@link #lines} index to compare
         * @return a negative value, zero, or a positive value if {@code a} compares less than,
         *         equal to, or greater than {@code b}, respectively
         */
        private int compareLines(int a, int b) {
            if (a == b) return 0;

            // sort on last intersection if possible
            final double ax = positions[a], bx = positions[b];
            if (ax < bx) return -1;
            if (ax > bx) return +1;

            // else sort on slope above sweep line
            final double aSlope = slopes[a], bSlope = slopes[b];
            if (aSlope < bSlope) return -1;
            if (aSlope > bSlope) return +1;

            return (a - b);
        }

        /**
         * Handles the specified {@link EventPoint} just removed from the {@link #schedule}.
         * Always updates the {@link #sweepLine}, and possibly also the
         * {@link #schedule} via {@link #addCrossing}.
         * 
         * @param e the {@link EventPoint} to handle
         * @throws IllegalStateException if the search structure was corrupted
         * @throws NullPointerException if {@code e} is {@code null}
         */
        private void handle(EventPoint e) {
            cursor = e.shared;
            boolean adding = false;
            assert(e.indices.size() == e.locations.size());
            Integer prevLine = null, nextLine = null;

            // remove end point & crossing nodes
            for (int i = 0; i < e.locations.size(); i++) {
                final int index = e.indices.get(i);

                switch (e.locations.get(i)) {
                    case START:
                        adding = true;
                        break;

                    case END:
                        if (!sweepLine.remove(index))
                            throw new IllegalStateException("search structure corrupt");

                        // remember surrounding lines, if any
                        prevLine = sweepLine.lower(index);
                        nextLine = sweepLine.higher(index);
                        break;

                    case BETWEEN:
                        if (!sweepLine.remove(index))
                            throw new IllegalStateException("search structure corrupt");

                        adding = true;
                        break;

                    default:
                        throw new IllegalStateException("e contains invalid LineLocation");
                }
            }
            
            if (!adding) {
                // intersect remaining neighbors of removed lines
                if (prevLine != null && nextLine != null)
                    addCrossing(prevLine, nextLine, e);

                final List<Integer> indices = e.indices;
                if (indices.size() < 2) return;

                /*
                 * Record intersection event.
                 *
                 * The sweep line algorithm would normally record TWO intersections for
                 * overlapping lines that share the same lexicographic end point: one for the
                 * start point, and one for the end point. So when we encounter an event that
                 * contains only end points, we must check that its line segments arrive from at
                 * least two different directions, and only then record an intersection.
                 */
                final double slope = slopes[indices.get(0)];
                for (int i = 1; i < indices.size(); i++)
                    if (slope != slopes[indices.get(i)]) {
                        e.normalize(lines);
                        crossings.add(e);
                        break;
                    }

                return;
            }

            // update remaining sweep line to prepare for insertion
            for (int index: sweepLine) {
                final double slope = slopes[index];

                if (slope != Double.MAX_VALUE) {
                    final PointD start = lines[index].start;
                    positions[index] = slope * (cursor.y - start.y) + start.x;
                }
            }

            // (re-)insert start point & crossing nodes
            prevLine = nextLine = null;
            boolean storeNeighbors = true;
            for (int i = 0; i < e.locations.size(); i++)
                if (e.locations.get(i) != LineLocation.END) {

                    final int index = e.indices.get(i);
                    positions[index] = cursor.x;
                    sweepLine.add(index);

                    // remember surrounding lines, if any
                    if (storeNeighbors) {
                        prevLine = sweepLine.lower(index);
                        nextLine = sweepLine.higher(index);
                        storeNeighbors = false;
                    }
                }

            // intersect outermost added lines with existing neighbors
            if (prevLine != null)
                addCrossing(prevLine, sweepLine.higher(prevLine), e);
            if (nextLine != null)
                addCrossing(sweepLine.lower(nextLine), nextLine, e);

            // record intersection event
            if (e.indices.size() > 1) {
                e.normalize(lines);
                crossings.add(e);
            }
        }
    }
}
