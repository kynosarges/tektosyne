package org.kynosarges.tektosyne.geometry;

import java.util.*;
import org.kynosarges.tektosyne.*;

/**
 * Provides standard algorithms and auxiliary methods for computational geometry.
 * All <b>random...</b> methods use the standard {@link Math#random} method for random
 * number generation and are therefore thread-safe, though access of that method by
 * multiple threads may lead to performance degradation.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class GeoUtils {
    /**
     * Creates a {@link GeoUtils} instance.
     * Private to prevent instantiation.
     */
    private GeoUtils() { }

    /**
     * Connects the specified {@link PointD} coordinates with {@link LineD} instances.
     * Returns an empty arry if {@code points} contains less than two elements. Otherwise,
     * returns an array with the same number of elements as {@code points} if {@code isClosed}
     * is {@code true}, else one element less.
     * <p>
     * {@code connectPoints} does not check for identical adjacent {@code points}, or for
     * congruent {@link LineD} instances. For example, if {@code points} contains two elements
     * and {@code isClosed} is {@code true}, the returned array will contain two {@link LineD}
     * instances with identical coordinates but opposite directions.</p>
     * 
     * @param isClosed {@code true} to create a {@link LineD} from the last to the first
     *                 {@code points} element, else {@code false}.
     * @param points the array of {@link PointD} coordinates to connect
     * @return an array of {@link LineD} instaces connecting all {@code points} in the given order
     * @throws NullPointerException if {@code points} is {@code null} or contains any {@code null} elements
     */
    public static LineD[] connectPoints(boolean isClosed, PointD... points) {
        if (points == null)
            throw new NullPointerException("points");
        if (points.length < 2)
            return new LineD[0];

        final LineD[] lines = new LineD[isClosed ? points.length : points.length - 1];

        for (int i = 0; i < points.length - 1; i++)
            lines[i] = new LineD(points[i], points[i + 1]);

        if (isClosed)
            lines[lines.length - 1] = new LineD(points[points.length - 1], points[0]);

        return lines;
    }
    
    /**
     * Finds the convex hull for the specified {@link PointD} coordinates.
     * If the specified {@code points} array contains only one or two elements, the result is
     * a new array containing the same elements. Any {@code points} elements that are coincident
     * or collinear with other elements are always removed from the returned array, however.
     * <p>
     * {@link convexHull} performs a Graham scan with an asymptotic runtime of O(n log n).
     * This Java implementation was adapted from the {@code Graham} algorithm by Joseph O’Rourke,
     * <em>Computational Geometry in C</em> (2nd ed.), Cambridge University Press 1998, p.72ff.</p>
     * 
     * @param points the array of {@link PointD} coordinates whose convex hull to find
     * @return the subset of {@code points} that represents the vertices of the convex hull
     * @throws NullPointerException if {@code points} is {@code null} or empty
     *                              or contains any {@code null} elements
     */
    public static PointD[] convexHull(PointD... points) {
        if (points == null || points.length == 0)
            throw new NullPointerException("points");

        // handle trivial edge cases
        switch (points.length) {
            case 1:
                return new PointD[] { points[0] };

            case 2:
                if (points[0] == points[1])
                    return new PointD[] { points[0] };
                else
                    return new PointD[] { points[0], points[1] };
        }

        /*
         * Set index n to lowest vertex. Unlike O’Rourke, we immediately mark duplicates
         * of the current lowest vertex for deletion. This eliminates some corner cases
         * of multiple duplicates that are missed by ConvexHullVertexComparator.compare.
         */
        final ConvexHullVertex[] p = new ConvexHullVertex[points.length];
        PointD pnv = points[0];
        p[0] = new ConvexHullVertex(pnv, 0);

        int i, n = 0;
        for (i = 1; i < p.length; i++) {
            final PointD piv = points[i];
            p[i] = new ConvexHullVertex(piv, i);

            final int result = PointDComparatorY.compareExact(piv, pnv);
            if (result < 0) {
                n = i; pnv = piv;
            } else if (result == 0)
                p[i].delete = true;
        }

        // move lowest vertex to index 0
        if (n > 0) {
            final ConvexHullVertex swap = p[0];
            p[0] = p[n]; p[n] = swap;
        }

        // sort and mark collinear/coincident vertices for deletion
        Arrays.sort(p, 1, p.length, new ConvexHullVertexComparator(p[0]));

        // delete marked vertices (n is remaining count)
        for (i = 0, n = 0; i < p.length; i++)
            if (!p[i].delete) p[n++] = p[i];

        // quit if only one unique vertex remains
        if (n == 1) return new PointD[] { p[0].vertex };

        // begin stack of convex hull vertices
        ConvexHullVertex top = p[1]; top.next = p[0];
        int hullCount = 2;

        // first two vertices are permanent, now examine others
        for (i = 2; i < n; ) {
            final ConvexHullVertex pi = p[i];

            if (top.next.vertex.crossProductLength(top.vertex, pi.vertex) > 0) {
                // push p[i] on stack
                pi.next = top;
                top = pi; ++i;
                ++hullCount;
            } else {
                // pop top from stack
                top = top.next;
                --hullCount;
            }
        }

        // convert vertex stack to point array
        final PointD[] hull = new PointD[hullCount];
        for (i = 0; i < hull.length; i++) {
            hull[i] = top.vertex;
            top = top.next;
        }
        assert(top == null);

        return hull;
    }

    /**
     * Converts the specified {@link Double} array to the specified geometric objects.
     * Dispatches to the {@code fromDoubles} method of {@link LineD}, {@link PointD},
     * {@link RectD}, or {@link SizeD}, as indicated by the generic type argument.
     * 
     * @param <T> the geometric type to convert to, which must be either
     *            {@link LineD}, {@link PointD}, {@link RectD}, or {@link SizeD}
     * @param type the {@link Class} object for <b>T</b>, required for method dispatch
     * @param items an array containing the {@link Double} components of <b>T</b>
     *              instances, stored in successive index positions per instance
     * @return the <b>T</b> array created from {@code items}
     * @throws IllegalArgumentException if <b>T</b> is not a valid geometric type,
     *         or if the length of {@code items} is not divisible by the number of {@link Double}
     *         components in <b>T</b>, or if any {@code items} are incompatible with <b>T</b>
     * @throws NullPointerException if {@code type} or {@code items} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] fromDoubles(Class<T> type, double... items) {
        if (type == null)
            throw new NullPointerException("type");
        
        if (type == LineD.class)
            return (T[]) LineD.fromDoubles(items);
        if (type == PointD.class)
            return (T[]) PointD.fromDoubles(items);
        if (type == RectD.class)
            return (T[]) RectD.fromDoubles(items);
        if (type == SizeD.class)
            return (T[]) SizeD.fromDoubles(items);

        throw new IllegalArgumentException("type != LineD, PointD, RectD, SizeD");
    }

    /**
     * Converts the specified {@link Integer} array to the specified geometric objects.
     * Dispatches to the {@code fromInts} method of {@link LineI}, {@link PointI},
     * {@link RectI}, or {@link SizeI}, as indicated by the generic type argument.
     * 
     * @param <T> the geometric type to convert to, which must be either
     *            {@link LineI}, {@link PointI}, {@link RectI}, or {@link SizeI}
     * @param type the {@link Class} object for <b>T</b>, required for method dispatch
     * @param items an array containing the {@link Integer} components of <b>T</b>
     *              instances, stored in successive index positions per instance
     * @return the <b>T</b> array created from {@code items}
     * @throws IllegalArgumentException if <b>T</b> is not a valid geometric type,
     *         or if the length of {@code items} is not divisible by the number of {@link Integer}
     *         components in <b>T</b>, or if any {@code items} are incompatible with <b>T</b>
     * @throws NullPointerException if {@code type} or {@code items} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] fromInts(Class<T> type, int... items) {
        if (type == null)
            throw new NullPointerException("type");
        
        if (type == LineI.class)
            return (T[]) LineI.fromInts(items);
        if (type == PointI.class)
            return (T[]) PointI.fromInts(items);
        if (type == RectI.class)
            return (T[]) RectI.fromInts(items);
        if (type == SizeI.class)
            return (T[]) SizeI.fromInts(items);

        throw new IllegalArgumentException("type != LineI, PointI, RectI, SizeI");
    }
    
    /**
     * Finds the {@link PointD} nearest to the specified coordinates in the specified collection.
     * Performs a linear search through {@code points} to find the element with the smallest
     * Euclidean distance to {@code q}. This is always an O(n) operation, where n is the total
     * number of {@code points}, unless an exact match for {@code q} is encountered.
     * <p>
     * If the specified {@code points} are already sorted lexicographically, {@link PointDComparator}
     * offers a much faster {@link PointDComparator#findNearest} method.</p>
     * 
     * @param points the {@link List} of {@link PointD} elements to search
     * @param q the {@link PointD} coordinates to locate
     * @return the zero-based index of the first occurrence of {@code q} in {@code points},
     *         if found; else the zero-based index of the {@code points} element with the
     *         smallest Euclidean distance to {@code q}
     * @throws NullPointerException if {@code points} or {@code q} is {@code null},
     *         or {@code points} is empty or contains any {@code null} elements
     */
    public static int nearestPoint(List<PointD> points, PointD q) {
        if (points == null || points.isEmpty())
            throw new NullPointerException("points");

        PointD vector = q.subtract(points.get(0));
        double minDistance = vector.lengthSquared();
        if (minDistance == 0) return 0;
        int minIndex = 0;

        for (int i = 1; i < points.size(); i++) {
            vector = q.subtract(points.get(i));
            double distance = vector.lengthSquared();

            if (minDistance > distance) {
                if (distance == 0) return i;
                minDistance = distance;
                minIndex = i;
            }
        }

        return minIndex;
    }

    /**
     * Finds the location of the specified {@link PointD} coordinates relative to the specified
     * arbitrary {@link PointD} polygon, using exact coordinate comparisons.
     * Never returns {@code null}. The specified {@code polygon} is implicitly assumed to be closed,
     * with an edge connecting its first and last vertex. Therefore, all vertices should be different.
     * <p>
     * {@link pointInPolygon} performs a ray crossings algorithm with an asymptotic runtime of O(n).
     * This Java implementation was adapted from the {@code InPoly1} algorithm by Joseph O’Rourke,
     * <em>Computational Geometry in C</em> (2nd ed.), Cambridge University Press 1998, p.244.</p>
     * 
     * @param q the {@link PointD} coordinates to locate
     * @param polygon an array of {@link PointD} coordinates defining the vertices of an arbitrary polygon
     * @return a {@link PolygonLocation} value indicating the location of {@code q} relative to {@code polygon}
     * @throws IllegalArgumentException if {@code polygon} has less than three elements
     * @throws NullPointerException if {@code q} or {@code polygon} is {@code null},
     *                              or any {@code polygon} element is {@code null}
     */
    public static PolygonLocation pointInPolygon(PointD q, PointD[] polygon) {
        if (polygon == null)
            throw new NullPointerException("polygon");
        if (polygon.length < 3)
            throw new IllegalArgumentException("polygon.length < 3");

        // number of right & left crossings of edge & ray
        int rightCrossings = 0, leftCrossings = 0;

        // last vertex is starting point for first edge
        final int lastIndex = polygon.length - 1;
        double x1 = polygon[lastIndex].x - q.x;
        double y1 = polygon[lastIndex].y - q.y;

        for (PointD vertex: polygon) {
            final double x0 = vertex.x - q.x;
            final double y0 = vertex.y - q.y;

            // check if q matches current vertex
            if (x0 == 0 && y0 == 0)
                return PolygonLocation.VERTEX;

            // check if current edge straddles x-axis
            final boolean rightStraddle = ((y0 > 0) != (y1 > 0));
            final boolean leftStraddle = ((y0 < 0) != (y1 < 0));

            // determine intersection of edge with x-axis
            if (rightStraddle || leftStraddle) {
                final double x = (x0 * y1 - x1 * y0) / (y1 - y0);
                if (rightStraddle && x > 0) ++rightCrossings;
                if (leftStraddle && x < 0) ++leftCrossings;
            }

            // move starting point for next edge
            x1 = x0; y1 = y0;
        }

        // q is on edge if crossings are of different parity
        if (rightCrossings % 2 != leftCrossings % 2)
            return PolygonLocation.EDGE;

        // q is inside for an odd number of crossings, else outside
        return (rightCrossings % 2 != 0 ?
            PolygonLocation.INSIDE : PolygonLocation.OUTSIDE);
    }

    /**
     * Finds the location of the specified {@link PointD} coordinates relative to the specified
     * arbitrary {@link PointD} polygon, given the specified epsilon for coordinate comparisons.
     * Never returns {@code null}. See exact overload for details.
     * 
     * @param q the {@link PointD} coordinates to locate
     * @param polygon an array of {@link PointD} coordinates defining the vertices of an arbitrary polygon
     * @param epsilon the maximum absolute difference at which two coordinates should be considered equal
     * @return a {@link PolygonLocation} value indicating the location of {@code q} relative to {@code polygon}
     * @throws IllegalArgumentException if {@code polygon} has less than three elements,
     *                                  or {@code epsilon} is less than or equal to zero
     * @throws NullPointerException if {@code q} or {@code polygon} is {@code null},
     *                              or any {@code polygon} element is {@code null}
     */
    public static PolygonLocation pointInPolygon(PointD q, PointD[] polygon, double epsilon) {
        if (polygon == null)
            throw new NullPointerException("polygon");
        if (polygon.length < 3)
            throw new IllegalArgumentException("polygon.length < 3");

        if (epsilon <= 0)
            throw new IllegalArgumentException("epsilon <= 0");

        // number of right & left crossings of edge & ray
        int rightCrossings = 0, leftCrossings = 0;

        // last vertex is starting point for first edge
        final int lastIndex = polygon.length - 1;
        double x1 = polygon[lastIndex].x - q.x;
        double y1 = polygon[lastIndex].y - q.y;
        int dy1 = MathUtils.compare(y1, 0, epsilon);

        for (PointD vertex: polygon) {
            final double x0 = vertex.x - q.x;
            final double y0 = vertex.y - q.y;

            final int dx0 = MathUtils.compare(x0, 0, epsilon);
            final int dy0 = MathUtils.compare(y0, 0, epsilon);

            // check if q matches current vertex
            if (dx0 == 0 && dy0 == 0)
                return PolygonLocation.VERTEX;

            // check if current edge straddles x-axis
            final boolean rightStraddle = ((dy0 > 0) != (dy1 > 0));
            final boolean leftStraddle = ((dy0 < 0) != (dy1 < 0));

            // determine intersection of edge with x-axis
            if (rightStraddle || leftStraddle) {
                final double x = (x0 * y1 - x1 * y0) / (y1 - y0);
                final int dx = MathUtils.compare(x, 0, epsilon);

                if (rightStraddle && dx > 0) ++rightCrossings;
                if (leftStraddle && dx < 0) ++leftCrossings;
            }

            // move starting point for next edge
            x1 = x0; y1 = y0; dy1 = dy0;
        }

        // q is on edge if crossings are of different parity
        if (rightCrossings % 2 != leftCrossings % 2)
            return PolygonLocation.EDGE;

        // q is inside for an odd number of crossings, else outside
        return (rightCrossings % 2 != 0 ?
            PolygonLocation.INSIDE : PolygonLocation.OUTSIDE);
    }

    /**
     * Computes the area of the specified arbitrary {@link PointD} polygon.
     * The specified {@code polygon} is implicitly assumed to be closed, with an edge
     * connecting its first and last vertex. Therefore, all vertices should be different.
     * Moreover, {@code polygon} must not self-intersect.
     * <p>
     * The absolute value of {@link #polygonArea} equals the area of {@code polygon}, and is
     * zero if all vertices are collinear or otherwise enclose no area. The sign of a non-zero
     * value indicates the orientation of its vertices: negative if the vertices are specified
     * in clockwise order, positive if they are specified in counter-clockwise order, assuming
     * y-coordinates increase upward.</p>
     * 
     * @param polygon an array of {@link PointD} coordinates defining the vertices of an arbitrary polygon
     * @return the area of {@code polygon}, with a sign that indicates the orientation of its vertices
     * @throws IllegalArgumentException if {@code polygon} has less than three elements
     * @throws NullPointerException if {@code polygon} is {@code null} or contains any {@code null} elements
     */
    public static double polygonArea(PointD... polygon) {
        if (polygon == null)
            throw new NullPointerException("polygon");
        if (polygon.length < 3)
            throw new IllegalArgumentException("polygon.length < 3");

        double area = 0;

        for (int i = polygon.length - 1, j = 0; j < polygon.length; i = j++)
            area += (polygon[i].x * polygon[j].y - polygon[j].x * polygon[i].y);

        return area / 2.0;
    }

    /**
     * Computes the centroid of the specified arbitrary {@link PointD} polygon.
     * The specified {@code polygon} is implicitly assumed to be closed, with an edge
     * connecting its first and last vertex. Therefore, all vertices should be different.
     * Moreover, {@code polygon} must not self-intersect and its vertices cannot be collinear,
     * i.e. {@link #polygonArea} cannot be zero.
     * 
     * @param polygon an array of {@link PointD} coordinates defining the vertices of an arbitrary polygon
     * @return the {@link PointD} coordinates of the centroid (center of gravity) of {@code polygon}
     * @throws IllegalArgumentException if {@code polygon} has less than three elements
     * @throws NullPointerException if {@code polygon} is {@code null} or contains any {@code null} elements
     */
    public static PointD polygonCentroid(PointD... polygon) {
        if (polygon == null)
            throw new NullPointerException("polygon");
        if (polygon.length < 3)
            throw new IllegalArgumentException("polygon.length < 3");

        double area = 0, x = 0, y = 0;

        for (int i = polygon.length - 1, j = 0; j < polygon.length; i = j++) {
            final double factor = polygon[i].x * polygon[j].y - polygon[j].x * polygon[i].y;

            area += factor;
            x += (polygon[i].x + polygon[j].x) * factor;
            y += (polygon[i].y + polygon[j].y) * factor;
        }

        area *= 3.0;
        return new PointD(x / area, y / area);
    }

    /**
     * Creates a random {@link LineD} within the specified bounding area.
     * @param x the smallest x-coordinate of the bounding area
     * @param y the smallest y-coordinate of the bounding area
     * @param width the horizontal extension of the bounding area
     * @param height the vertical extension of the bounding area
     * @return a {@link LineD} with random {@link LineD#start} and {@link LineD#end} points
     *         ranging from ({@code x}, {@code y}) to ({@code x + width}, {@code y + height})
     * @throws IllegalArgumentException if {@code width} or {@code height} is equal to or less than zero
     */
    public static LineD randomLine(double x, double y, double width, double height) {
        if (width <= 0)
            throw new IllegalArgumentException("width <= 0");
        if (height <= 0)
            throw new IllegalArgumentException("height <= 0");

        return new LineD(
            x + Math.random() * width,
            y + Math.random() * height,
            x + Math.random() * width,
            y + Math.random() * height);
    }

    /**
     * Creates a random {@link PointD} within the specified bounding area.
     * @param x the smallest x-coordinate of the bounding area
     * @param y the smallest y-coordinate of the bounding area
     * @param width the horizontal extension of the bounding area
     * @param height the vertical extension of the bounding area
     * @return a {@link PointD} with random {@link PointD#x} and {@link PointD#y} coordinates
     *         ranging from ({@code x}, {@code y}) to ({@code x + width}, {@code y + height})
     * @throws IllegalArgumentException if {@code width} or {@code height} is equal to or less than zero
     */
    public static PointD randomPoint(double x, double y, double width, double height) {
        if (width <= 0)
            throw new IllegalArgumentException("width <= 0");
        if (height <= 0)
            throw new IllegalArgumentException("height <= 0");

        return new PointD(
            x + Math.random() * width,
            y + Math.random() * height);
    }

    /**
     * Creates a set of random {@link PointD} coordinates within the specified bounding area.
     * The returned array is unsorted and may contain duplicate {@link PointD} coordinates.
     * 
     * @param count the number of {@link PointD} coordinates to create
     * @param bounds a {@link RectD} defining the the bounding area
     * @return an array of {@code count} randomly created {@link PointD} coordinates within {@code bounds}
     * @throws IllegalArgumentException if {@code count} is less than zero, or {@link RectD#width}
     *                                  or {@link RectD#height} of {@code bounds} is zero
     * @throws NullPointerException if {@code bounds} is {@code null}
     */
    public static PointD[] randomPoints(int count, RectD bounds) {
        if (count < 0)
            throw new IllegalArgumentException("count < 0");
        
        final double width = bounds.width(), height = bounds.height();
        if (width == 0)
            throw new IllegalArgumentException("bounds.width == 0");
        if (height == 0)
            throw new IllegalArgumentException("bounds.height == 0");

        final PointD[] points = new PointD[count];

        for (int i = 0; i < points.length; i++)
            points[i] = new PointD(
                bounds.min.x + Math.random() * width,
                bounds.min.y + Math.random() * height);

        return points;
    }

    /**
     * Creates a set of random {@link PointD} coordinates within the specified bounding area,
     * ensuring a specified pairwise minimum distance.
     * The returned array is sorted using the specified {@code comparer} and never contains
     * any duplicate {@link PointD} coordinates. This method may enter an endless loop if
     * {@code distance} is too great relative to {@code count} and {@code bounds}.
     * 
     * @param count the number of {@link PointD} coordinates to create
     * @param bounds a {@link RectD} defining the the bounding area
     * @param comparer the {@link PointDComparator} used to sort &amp; search the created array
     * @param distance the smallest Euclidean distance between any two {@link PointD} instances
     * @return an array of {@code count} randomly created {@link PointD} coordinates within {@code bounds}
     * @throws IllegalArgumentException if {@code count} is less than zero, or {@code distance} is equal to
     *         or less than zero, or {@link RectD#width} or {@link RectD#height} of {@code bounds} is zero
     * @throws NullPointerException if {@code bounds} or {@code comparer} is {@code null}
     */
    public static PointD[] randomPoints(int count,
        RectD bounds, PointDComparator comparer, double distance) {

        if (count < 0)
            throw new IllegalArgumentException("count < 0");
        if (comparer == null)
            throw new NullPointerException("comparer");
        if (distance <= 0)
            throw new IllegalArgumentException("distance <= 0");
        
        final double width = bounds.width(), height = bounds.height();
        if (width == 0)
            throw new IllegalArgumentException("bounds.width == 0");
        if (height == 0)
            throw new IllegalArgumentException("bounds.height == 0");

        // square distance for direct comparison with lengthSquared
        distance *= distance;
        PointD point;
        double length2;
        final List<PointD> points = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            do {
                point = new PointD(
                    bounds.min.x + Math.random() * width,
                    bounds.min.y + Math.random() * height);
                
                if (points.isEmpty()) break;

                final int index = comparer.findNearest(points, point);
                length2 = point.subtract(points.get(index)).lengthSquared();
            } while (length2 < distance);

            points.add(point);
            points.sort(comparer);
        }

        return points.toArray(new PointD[count]);
    }

    /**
     * Creates a random simple {@link PointD} polygon within the specified bounding area.
     * Moves in a full circle around the center of the specified bounding area, placing vertices
     * at random angles and radii within the area. Any two vertices are separated by a minimum
     * angular distance of six degrees. The resulting polygon is simple, i.e. covering a single
     * contiguous space without self-intersecting.
     * 
     * @param x the smallest x-coordinate of the bounding area
     * @param y the smallest y-coordinate of the bounding area
     * @param width the horizontal extension of the bounding area
     * @param height the vertical extension of the bounding area
     * @return an array of {@link PointD} vertices with random {@link PointD#x} and {@link PointD#y}
     *         coordinates ranging from ({@code x}, {@code y}) to ({@code x + width}, {@code y + height})
     * @throws IllegalArgumentException if {@code width} or {@code height} is equal to or less than zero
     */
    public static PointD[] randomPolygon(double x, double y, double width, double height) {
        if (width <= 0)
            throw new IllegalArgumentException("width < = 0");
        if (height <= 0)
            throw new IllegalArgumentException("height < = 0");

        // drawing range extending from center of drawing area
        final SizeD range = new SizeD(width / 2.0, height / 2.0);
        final PointD center = new PointD(x + range.width, y + range.height);

        // radius of circle circumscribed around drawing area
        final double radius = Math.sqrt(range.width * range.width + range.height * range.height);

        // create vertices as series of random polar coordinates
        final List<PointD> polygon = new ArrayList<>();
        for (double degrees = 0; degrees < 360; degrees += 6.0) {

            // random increment of angle for next vertex
            degrees += Math.random() * 110;
            if (degrees >= 360) break;
            final double radians = degrees * Angle.DEGREES_TO_RADIANS;

            // axis projections of circumscribed radius at current angle
            final double dx = Math.cos(radians) * radius;
            final double dy = Math.sin(radians) * radius;

            // shorten total radius where it extends beyond area
            final double dxLimit = range.width / Math.abs(dx);
            final double dyLimit = range.height / Math.abs(dy);
            double factor = Math.min(dxLimit, dyLimit);

            // additional random shortening to determine vertex
            factor *= Math.random();
            polygon.add(new PointD(center.x + dx * factor, center.y + dy * factor));
        }

        return polygon.toArray(new PointD[polygon.size()]);
    }

    /**
     * Converts the specified geometric objects to a {@link Double} array.
     * Dispatches to the {@code toDoubles} method of {@link LineD}, {@link PointD},
     * {@link RectD}, or {@link SizeD}, as indicated by the generic type argument.
     * 
     * @param <T> the geometric type to convert from, which must be either
     *            {@link LineD}, {@link PointD}, {@link RectD}, or {@link SizeD}
     * @param type the {@link Class} object for <b>T</b>, required for method dispatch
     * @param items the <b>T</b> array to convert
     * @return an array containing the {@link Double} components of all {@code items},
     *         stored in successive index positions per <b>T</b> instance
     * @throws IllegalArgumentException if <b>T</b> is not a valid geometric type
     * @throws NullPointerException if {@code type} or {@code items} is {@code null},
     *         or {@code items} contains any {@code null} elements
     */
    @SuppressWarnings("unchecked")
    public static <T> double[] toDoubles(Class<T> type, T... items) {
        if (type == null)
            throw new NullPointerException("type");
        
        if (type == LineD.class)
            return LineD.toDoubles((LineD[]) items);
        if (type == PointD.class)
            return PointD.toDoubles((PointD[]) items);
        if (type == RectD.class)
            return RectD.toDoubles((RectD[]) items);
        if (type == SizeD.class)
            return SizeD.toDoubles((SizeD[]) items);

        throw new IllegalArgumentException("type != LineD, PointD, RectD, SizeD");
    }

    /**
     * Converts the specified geometric objects to an {@link Integer} array.
     * Dispatches to the {@code toInts} method of {@link LineI}, {@link PointI},
     * {@link RectI}, or {@link SizeI}, as indicated by the generic type argument.
     * 
     * @param <T> the geometric type to convert from, which must be either
     *            {@link LineI}, {@link PointI}, {@link RectI}, or {@link SizeI}
     * @param type the {@link Class} object for <b>T</b>, required for method dispatch
     * @param items the <b>T</b> array to convert
     * @return an array containing the {@link Integer} components of all {@code items},
     *         stored in successive index positions per <b>T</b> instance
     * @throws IllegalArgumentException if <b>T</b> is not a valid geometric type
     * @throws NullPointerException if {@code type} or {@code items} is {@code null},
     *         or {@code items} contains any {@code null} elements
     */
    @SuppressWarnings("unchecked")
    public static <T> int[] toInts(Class<T> type, T... items) {
        if (type == null)
            throw new NullPointerException("type");
        
        if (type == LineI.class)
            return LineI.toInts((LineI[]) items);
        if (type == PointI.class)
            return PointI.toInts((PointI[]) items);
        if (type == RectI.class)
            return RectI.toInts((RectI[]) items);
        if (type == SizeI.class)
            return SizeI.toInts((SizeI[]) items);

        throw new IllegalArgumentException("type != LineI, PointI, RectI, SizeI");
    }

    /**
     * Represents a potential vertex of a {@link #convexHull} under construction.
     * Implements the {@code tPointStructure} and {@code tStackCell} structures by Joseph O’Rourke,
     * <em>Computational Geometry in C</em> (2nd ed.), Cambridge University Press 1998, p.78.
     */
    private static class ConvexHullVertex {
        /**
         * Creates a {@link ConvexHullVertex} with the specified coordinates and index
         * @param vertex the {@link PointD} coordinates of the {@link ConvexHullVertex}
         * @param index the unique index of the {@link ConvexHullVertex}
         * @throws NullPointerException if {@code vertex} is {@code null}
         */
        ConvexHullVertex(PointD vertex, int index) {
            if (vertex == null)
                throw new NullPointerException("vertex");

            this.vertex = vertex;
            this.index = index;
        }

        /**
         * {@code true} if the {@link ConvexHullVertex} is not part of the convex hull
         * and should be deleted, else {@code false}. The default is {@code false}.
         */
        boolean delete;

        /**
         * An {@link Integer} value that uniquely identifies the {@link ConvexHullVertex}
         * within the convex hull.
         */
        final int index;

        /**
         * The next {@link ConvexHullVertex} in the convex hull.
         * {@code null} if the convex hull ends with this {@link ConvexHullVertex},
         * or if the vertices have not yet been linked. The default is {@code null}.
         */
        ConvexHullVertex next;

        /**
         * The {@link PointD} coordinates of the {@link ConvexHullVertex}.
         */
        final PointD vertex;
    }

    /**
     * Compares two {@link ConvexHullVertex} instances for precedence.
     */
    private static class ConvexHullVertexComparator implements Comparator<ConvexHullVertex> {
        /**
         * Creates a {@link ConvexHullVertexComparator} with the specified initial vertex.
         * @param p0 the {@link ConvexHullVertex} that represents the first vertex
         *           in the convex hull under construction
         * @throws NullPointerException if {@code p0} is {@code null}
         */
        ConvexHullVertexComparator(ConvexHullVertex p0) {
            if (p0 == null)
                throw new NullPointerException("p0");

            this.p0v = p0.vertex;
        }

        /**
         * The first {@link ConvexHullVertex#vertex} in the convex hull under construction.
         */
        final PointD p0v;

        /**
         * Compares two specified {@link ConvexHullVertex} instances and returns an indication
         * of their precedence in the convex hull under construction.
         * Sets the {@link ConvexHullVertex#delete} flag on either {@code pi} or {@code pj}
         * if these two vertices are collinear or coincident.
         * <p>
         * {@code compare} implements the {@code Compare} algorithm by Joseph O’Rourke,
         * <em>Computational Geometry in C</em> (2nd ed.), Cambridge University Press 1998, p.82.
         * See there for an explanation of the established sorting order.</p>
         * <p>
         * Our implementation also compares {@code pi} and {@code pj} for reference equality
         * before doing anything else. This is necessary because sorting algorithms may supply
         * the same object for both parameters.</p>
         * 
         * @param pi the first {@link ConvexHullVertex} to compare
         * @param pj the second {@link ConvexHullVertex} to compare
         * @return a negative value, zero, or a positive value if {@code pi} compares less than,
         *         equal to, or greater than {@code pj}, respectively, given {@link #p0v}
         * @throws NullPointerException if {@code pi} or {@code pj} is {@code null}
         */
        @Override
        public int compare(ConvexHullVertex pi, ConvexHullVertex pj) {
            if (pi == pj) return 0;
            final PointD piv = pi.vertex, pjv = pj.vertex;

            // check if coordinate triplet constitutes a turn
            final double length = p0v.crossProductLength(piv, pjv);
            if (length > 0) return -1;
            else if (length < 0) return +1;

            // pi and pj are collinear with p0, delete one of them
            final double x = Math.abs(piv.x - p0v.x) - Math.abs(pjv.x - p0v.x);
            final double y = Math.abs(piv.y - p0v.y) - Math.abs(pjv.y - p0v.y);

            if (x < 0 || y < 0) {
                pi.delete = true;
                return -1;
            }
            if (x > 0 || y > 0) {
                pj.delete = true;
                return +1;
            }

            // pi and pj are coincident
            if (pi.index > pj.index)
                pj.delete = true;
            else
                pi.delete = true;

            return 0;
        }
    }
}
