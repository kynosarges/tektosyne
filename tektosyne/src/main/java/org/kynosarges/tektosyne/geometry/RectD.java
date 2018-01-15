package org.kynosarges.tektosyne.geometry;

import java.util.*;
import org.kynosarges.tektosyne.*;

/**
 * Represents a rectangular region in two-dimensional space, using {@link Double} coordinates.
 * {@link RectI} contains two immutable {@link PointD} locations, defining the opposite corners
 * of a rectangle. Use {@link RectI} to represent rectangles with {@link Integer} coordinates.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class RectD {
    /**
     * The smallest coordinates within the {@link RectD}.
     * Both dimensions are equal to or less than the corresponding {@link #max} dimensions.
     */
    public final PointD min;

    /**
     * The greatest coordinates within the {@link RectD}.
     * Both dimensions are equal to or greater than the corresponding {@link #min} dimensions.
     */
    public final PointD max;

    /**
     * An empty read-only {@link RectD}.
     * {@link #min} and {@link #max} are both set to {@link PointD#EMPTY}.
     */
    public final static RectD EMPTY = new RectD();

    /**
     * Creates a {@link RectD} with all coordinates set to zero.
     * {@link #min} and {@link #max} are both set to {@link PointD#EMPTY}.
     */
    public RectD() {
        this.min = PointD.EMPTY;
        this.max = PointD.EMPTY;
    }

    /**
     * Creates a {@link RectD} with the specified {@link Double} coordinates.
     * @param minX the smallest x-coordinate within the {@link RectD}
     * @param minY the smallest y-coordinate within the {@link RectD}
     * @param maxX the greatest x-coordinate within the {@link RectD}
     * @param maxY the greatest y-coordinate within the {@link RectD}
     * @throws IllegalArgumentException if {@code maxX} is less than {@code minX},
     *                                  or {@code maxY} is less than {@code minY}
     */
    public RectD(double minX, double minY, double maxX, double maxY) {
        if (maxX < minX)
            throw new IllegalArgumentException("maxX < minX");
        if (maxY < minY)
            throw new IllegalArgumentException("maxY < minY");

        this.min = new PointD(minX, minY);
        this.max = new PointD(maxX, maxY);
    }

    /**
     * Creates a {@link RectD} with the specified {@link PointD} coordinates.
     * @param min the smallest coordinates within the {@link RectD}
     * @param max the greatest coordinates within the {@link RectD}
     * @throws IllegalArgumentException if any dimension of {@code max} is less than
     *                                  the corresponding dimension of {@code min}
     * @throws NullPointerException if {@code min} or {@code max} is {@code null}
     */
    public RectD(PointD min, PointD max) {
        if (max.x < min.x)
            throw new IllegalArgumentException("max.x < min.x");
        if (max.y < min.y)
            throw new IllegalArgumentException("max.y < min.y");

        this.min = min;
        this.max = max;
    }

    /**
     * Gets the {@link PointD} at the center of the {@link RectD}.
     * Returns a {@link PointD} that is offset from {@link #min}
     * by half the distance from {@link #min} to {@link #max}.
     * 
     * @return the {@link PointD} at the center of the {@link RectD}
     */
    public PointD center() {
        return new PointD(
                min.x + (max.x - min.x) / 2,
                min.y + (max.y - min.y) /2);
    }

    /**
     * Circumscribes a {@link RectI} around the {@link RectD}.
     * The {@link RectI} extends from the {@link Fortran#floor(double)} of
     * {@link #min} to the {@link Fortran#ceiling(double)} of {@link #max}.
     * 
     * @return a {@link RectI} that entirely covers the {@link RectD}
     * @throws ArithmeticException if any coordinate overflows {@link Integer}
     */
    public RectI circumscribe() {
        return new RectI(
            Fortran.floor(min.x), Fortran.floor(min.y),
            Fortran.ceiling(max.x), Fortran.ceiling(max.y));
    }

    /**
     * Circumscribes a {@link RectD} around the specified {@link PointD} coordinates.
     * Sets both dimensions of {@link #min} to the smallest coordinates, and both dimensions
     * of {@link #max} to the greatest coordinates, found among any {@code points}.
     * 
     * @param points an array of {@link PointD} coordinates whose bounds to determine
     * @return the smallest {@link RectD} that contains all specified {@code points}
     * @throws NullPointerException if {@code points} is {@code null} or empty,
     *                              or contains any {@code null} elements
     */
    public static RectD circumscribe(PointD... points) {
        if (points == null || points.length == 0)
            throw new NullPointerException("points");

        double x0 = Double.POSITIVE_INFINITY, y0 = Double.POSITIVE_INFINITY;
        double x1 = Double.NEGATIVE_INFINITY, y1 = Double.NEGATIVE_INFINITY;

        for (PointD point: points) {
            if (x0 > point.x) x0 = point.x;
            if (y0 > point.y) y0 = point.y;
            if (x1 < point.x) x1 = point.x;
            if (y1 < point.y) y1 = point.y;
        }

        return new RectD(x0, y0, x1, y1);
    }

    /**
     * Determines whether the {@link RectD} contains the specified {@link Double} coordinates.
     * @param x the x-coordinate to examine
     * @param y the y-coordinate to examine
     * @return {@code true} if both {@code x} and {@code y} fall within the range of
     *         coordinates defined by {@link #min} and {@link #max}, else {@code false}
     */
    public boolean contains(double x, double y) {
        return (x >= min.x && y >= min.y &&
                x <= max.x && y <= max.y);
    }

    /**
     * Determines whether the {@link RectD} contains the specified {@link PointD} coordinates.
     * @param point the {@link PointD} to examine
     * @return {@code true} if both dimensions of {@code point} fall within the range of
     *         coordinates defined by {@link #min} and {@link #max}, else {@code false}
     * @throws NullPointerException if {@code point} is {@code null}
     */
    public boolean contains(PointD point) {
        return contains(point.x, point.y);
    }

    /**
     * Determines whether the {@link RectD} contains the specified {@link Double} coordinates,
     * excluding the x- and y-coordinates of {@link #max}.
     * 
     * @param x the x-coordinate to examine
     * @param y the y-coordinate to examine
     * @return {@code true} if both {@code x} and {@code y} fall within the range of coordinates
     *         defined by {@link #min} (inclusive) and {@link #max} (exclusive), else {@code false}
     */
    public boolean containsOpen(double x, double y) {
        return (x >= min.x && y >= min.y &&
                x < max.x && y < max.y);
    }

    /**
     * Determines whether the {@link RectD} contains the specified {@link PointD} coordinates,
     * excluding the x- and y-coordinates of {@link #max}.
     * 
     * @param point the {@link PointD} to examine
     * @return {@code true} if both dimensions of {@code point} fall within the range of coordinates
     *         defined by {@link #min} (inclusive) and {@link #max} (exclusive), else {@code false}
     * @throws NullPointerException if {@code point} is {@code null}
     */
    public boolean containsOpen(PointD point) {
        return containsOpen(point.x, point.y);
    }

    /**
     * Determines whether the {@link RectD} entirely contains the specified instance.
     * @param rect the {@link RectD} to examine
     * @return {@code true} if both {@link #min} and {@link #max} of the specified {@code rect}
     *         fall within the range coordinates defined by this instance, else {@code false}
     * @throws NullPointerException if {@code rect} is {@code null}
     */
    public boolean contains(RectD rect) {

        return (rect.min.x >= min.x && rect.min.y >= min.y &&
                rect.max.x <= max.x && rect.max.y <= max.y);
    }

    /**
     * Finds the distance vector from the specified {@link PointD} coordinates
     * to the nearest edges of the {@link RectD}.
     * The returned coordinates are negative if the corresponding dimension of {@code q}
     * if less than that of {@link #min}, and positive if it is greater than {@link #max}.
     * 
     * @param q the {@link PointD} to examine
     * @return a {@link PointD} whose dimensions equal zero if they fall within
     *         the corresponding dimensions of {@link #min} and {@link #max},
     *         else the signed difference to the nearest edge coordinate
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public PointD distanceVector(PointD q) {

        final double x = (q.x < min.x ? q.x - min.x :
                          q.x > max.x ? q.x - max.x : 0);

        final double y = (q.y < min.y ? q.y - min.y :
                          q.y > max.y ? q.y - max.y : 0);

        return new PointD(x, y);
    }

    /**
     * Compares two {@link RectD} instances for equality, given the specified epsilon.
     * @param a the first {@link RectD} to compare
     * @param b the second {@link RectD} to compare
     * @param epsilon the maximum absolute difference where the corresponding coordinates
     *                of {@code a} and {@code b} are considered equal
     * @return {@code true} if the absolute difference between all corresponding coordinates
     *         of {@code a} and {@code b} is no greater than {@code epsilon}, else {@code false}
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public static boolean equals(RectD a, RectD b, double epsilon) {

        return (PointD.equals(a.min, b.min, epsilon) &&
                PointD.equals(a.max, b.max, epsilon));
    }

    /**
     * Converts the specified {@link Double} array to a {@link RectD} array.
     * The returned array has a quarter as many elements as the specified
     * {@code rects} and retains the same coordinate sequence.
     * 
     * @param rects an array containing the coordinates {@code min.x, min.y, max.x, max.y} of
     *              {@link RectD} instances, stored in successive index positions per {@link RectD}
     * @return the {@link RectD} array created from {@code rects}
     * @throws IllegalArgumentException if the length of {@code rects} is not divisible by four,
     *         or if any {@link #max} coordinate is less than the corresponding {@link #min} coordinate
     * @throws NullPointerException if {@code rects} is {@code null}
     */
    public static RectD[] fromDoubles(double... rects) {
        if (rects.length % 4 != 0)
            throw new IllegalArgumentException("rects.length % 4 != 0");

        final RectD[] output = new RectD[rects.length / 4];

        for (int i = 0; i < output.length; i++)
            output[i] = new RectD(rects[4 * i], rects[4 * i + 1],
                    rects[4 * i + 2], rects[4 * i + 3]);

        return output;
    }

    /**
     * Gets the height the {@link RectD}. Never negative but may be zero.
     * @return the difference between the x-coordinates of {@link #max} and {@link #min}
     */
    public double height() {
        return (max.y - min.y);
    }

    /**
     * Finds the intersection of the {@link RectD} with the specified {@link LineD}.
     * Performs the Liang-Barsky line clipping algorithm. This Java implementation
     * was adapted from the C implementation by Daniel White, published at <a
     * href="http://www.skytopia.com/project/articles/compsci/clipping.html">Skytopia</a>.
     * 
     * @param line the {@link LineD} to intersect with the {@link RectD}
     * @return a {@link LineD} that contains the intersection of {@code line} with the
     *         {@link RectD}, if any intersection was found, else {@code null}
     * @throws NullPointerException if {@code line} is {@code null}
     */
    public LineD intersect(LineD line) {

        final double x0 = line.start.x, y0 = line.start.y;
        final double dx = line.end.x - x0, dy = line.end.y - y0;
        double t0 = 0, t1 = 1, p = 0, q = 0;

        // traverse all four rectangle borders
        for (int border = 0; border < 4; border++) {
            switch (border) {
                case 0: p = -dx; q = x0 - min.x; break;
                case 1: p = +dx; q = max.x - x0; break;
                case 2: p = -dy; q = y0 - min.y; break;
                case 3: p = +dy; q = max.y - y0; break;
            }

            if (p == 0) {
                // parallel line outside of rectangle
                if (q < 0) return null;
            } else {
                double r = q / p;
                if (p < 0) {
                    if (r > t1) return null;
                    if (r > t0) t0 = r;
                } else {
                    if (r < t0) return null;
                    if (r < t1) t1 = r;
                }
            }
        }

        return new LineD(
            x0 + t0 * dx, y0 + t0 * dy,
            x0 + t1 * dx, y0 + t1 * dy);
    }

    /**
     * Intersects the {@link RectD} with the specified arbitrary {@link PointD} polygon.
     * Performs the Sutherland–Hodgman polygon clipping algorithm, optimized for an axis-aligned
     * {@link RectD} as the clipping polygon. At intersection points, the border coordinates of the
     * {@link RectD} are copied rather than computed, allowing exact floating-point comparisons.
     * <p>
     * The specified {@code polygon} and the returned intersection are implicitly assumed to be closed,
     * with an edge connecting first and last vertex. Therefore, all vertices should be different.</p>
     * <p>
     * Unless the specified {@code polygon} is convex, the returned intersection may represent
     * multiple polygons, connected across the borders of {@link RectD}.</p>
     * 
     * @param polygon an array of {@link PointD} coordinates describing the vertices
     *                of the polygon to intersect with the {@link RectD}
     * @return the intersection of {@code polygon} with the {@link RectD},
     *         if an intersection was found, else {@code null}
     * @throws NullPointerException if {@code polygon} is {@code null} or empty,
     *                              or contains any {@code null} elements
     */
    public PointD[] intersect(PointD[] polygon) {
        if (polygon == null || polygon.length == 0)
            throw new NullPointerException("polygon");

        // input/output storage for intermediate polygons
        int outputLength = polygon.length;
        PointD[] inputVertices = new PointD[3 * outputLength];
        PointD[] outputVertices = new PointD[3 * outputLength];
        System.arraycopy(polygon, 0, outputVertices, 0, outputLength);

        double q = 0;
        boolean startInside = false, endInside = false;

        // traverse all four rectangle borders
        for (int border = 0; border < 4; border++) {
            switch (border) {
                case 0: q = min.x; break;
                case 1: q = max.x; break;
                case 2: q = min.y; break;
                case 3: q = max.y; break;
            }

            // last output is new input for current border
            PointD[] swap = inputVertices;
            inputVertices = outputVertices;
            outputVertices = swap;
            int inputLength = outputLength;
            outputLength = 0;

            // check all polygon edges against infinite border
            PointD start = inputVertices[inputLength - 1];
            for (int i = 0; i < inputLength; i++) {
                PointD end = inputVertices[i];

                switch (border) {
                    case 0: startInside = (start.x >= q); endInside = (end.x >= q); break;
                    case 1: startInside = (start.x <= q); endInside = (end.x <= q); break;
                    case 2: startInside = (start.y >= q); endInside = (end.y >= q); break;
                    case 3: startInside = (start.y <= q); endInside = (end.y <= q); break;
                }

                // store intersection point if border crossed
                if (startInside != endInside) {
                    double x, y, dx = end.x - start.x, dy = end.y - start.y;
                    if (border < 2) {
                        x = q;
                        y = (x == end.x ? end.y : start.y + (x - start.x) * dy / dx);
                    } else {
                        y = q;
                        x = (y == end.y ? end.x : start.x + (y - start.y) * dx / dy);
                    }
                    outputVertices[outputLength++] = new PointD(x, y);
                }

                // also store end point if inside rectangle
                if (endInside) outputVertices[outputLength++] = end;
                start = end;
            }

            if (outputLength == 0) return null;
        }

        return Arrays.copyOfRange(outputVertices, 0, outputLength);
    }

    /**
     * Finds the intersection of the {@link RectD} with the specified instance.
     * @param rect the {@link RectD} to intersect with this instance
     * @return a {@link RectD} that contains the intersection of {@code rect} with this instance,
     *         if any intersection was found, else {@code null}
     * @throws NullPointerException if {@code rect} is {@code null}
     */
    public RectD intersect(RectD rect) {

        final double minX = Math.max(min.x, rect.min.x);
        final double minY = Math.max(min.y, rect.min.y);
        final double maxX = Math.min(max.x, rect.max.x);
        final double maxY = Math.min(max.y, rect.max.y);

        if (minX > maxX || minY > maxY)
            return null;
        
        return new RectD(minX, minY, maxX, maxY);
    }

    /**
     * Determines whether the {@link RectD} intersects with the specified {@link LineD}.
     * Performs the same Liang-Barsky line clipping algorithm as {@link #intersect(LineD)}
     * but without computing the intersecting line segment.
     * 
     * @param line the {@link LineD} to examine
     * @return {@code true} if {@code line} intersects the {@link RectD}, else {@code false}
     * @throws NullPointerException if {@code line} is {@code null}
     */
    public boolean intersectsWith(LineD line) {

        final double x0 = line.start.x, y0 = line.start.y;
        final double dx = line.end.x - x0, dy = line.end.y - y0;
        double t0 = 0, t1 = 1, p = 0, q = 0;

        // traverse all four rectangle borders
        for (int border = 0; border < 4; border++) {
            switch (border) {
                case 0: p = -dx; q = x0 - min.x; break;
                case 1: p = +dx; q = max.x - x0; break;
                case 2: p = -dy; q = y0 - min.y; break;
                case 3: p = +dy; q = max.y - y0; break;
            }

            if (p == 0) {
                // parallel line outside of rectangle
                if (q < 0) return false;
            } else {
                double r = q / p;
                if (p < 0) {
                    if (r > t1) return false;
                    else if (r > t0) t0 = r;
                } else {
                    if (r < t0) return false;
                    else if (r < t1) t1 = r;
                }
            }
        }

        return true;
    }

    /**
     * Determines whether the {@link RectD} intersects with the specified instance.
     * @param rect the {@link RectD} to examine
     * @return {@code true} if {@code rect} shares any coordinates with this instance,
     *         else {@code false}
     * @throws NullPointerException if {@code rect} is {@code null}
     */
    public boolean intersectsWith(RectD rect) {

        return (rect.max.x >= min.x && rect.min.x <= max.x &&
                rect.max.y >= min.y && rect.min.y <= max.y);
    }

    /**
     * Determines the location of the specified {@link PointD} relative to the {@link RectD},
     * using exact coordinate comparisons.
     * @param q the {@link PointD} coordinates to examine
     * @return a {@link RectLocation} indicating the location of {@code q}
     *         relative to the {@link RectD} in each dimension
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public RectLocation locate(PointD q) {

        final LineLocation x =
                (q.x < min.x ? LineLocation.BEFORE :
                (q.x == min.x ? LineLocation.START :
                (q.x < max.x ? LineLocation.BETWEEN :
                (q.x == max.x ? LineLocation.END : LineLocation.AFTER))));

        final LineLocation y =
                (q.y < min.y ? LineLocation.BEFORE :
                (q.y == min.y ? LineLocation.START :
                (q.y < max.y ? LineLocation.BETWEEN :
                (q.y == max.y ? LineLocation.END : LineLocation.AFTER))));

        return new RectLocation(x, y);
    }

    /**
     * Determines the location of the specified {@link PointD} relative to the {@link RectD},
     * given the specified epsilon for coordinate comparisons.
     * @param q the {@link PointD} coordinates to examine
     * @param epsilon the maximum absolute difference at which coordinates should be considered equal
     * @return a {@link RectLocation} indicating the location of {@code q}
     *         relative to the {@link RectD} in each dimension
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public RectLocation locate(PointD q, double epsilon) {

        final LineLocation x =
                (MathUtils.equals(q.x, min.x, epsilon) ? LineLocation.START :
                (MathUtils.equals(q.x, max.x, epsilon) ? LineLocation.END :
                (q.x < min.x ? LineLocation.BEFORE :
                (q.x < max.x ? LineLocation.BETWEEN : LineLocation.AFTER))));

        final LineLocation y =
                (MathUtils.equals(q.y, min.y, epsilon) ? LineLocation.START :
                (MathUtils.equals(q.y, max.y, epsilon) ? LineLocation.END :
                (q.y < min.y ? LineLocation.BEFORE :
                (q.y < max.y ? LineLocation.BETWEEN : LineLocation.AFTER))));

        return new RectLocation(x, y);
    }

    /**
     * Offsets the {@link RectD} by the specified {@link Double} distances.
     * @param x the horizontal offset applied to the {@link RectD}
     * @param y the vertical offset applied to the {@link RectD}
     * @return a {@link RectD} whose {@link #min} and {@link #max} equal those of the current
     *         instance, with {@code x} and {@code y} added to all corresponding dimensions
     */
    public RectD offset(double x, double y) {
        return new RectD(min.x + x, min.y + y, max.x + x, max.y + y);
    }

    /**
     * Offsets the {@link RectD} by the specified {@link PointD} vector.
     * @param vector the {@link PointD} offset applied to the {@link RectD}
     * @return a {@link RectD} whose {@link #min} and {@link #max} equal those
     *         of the current instance, with {@code vector} added to both points
     * @throws NullPointerException if {@code vector} is {@code null}
     */
    public RectD offset(PointD vector) {
        return new RectD(min.add(vector), max.add(vector));
    }

    /**
     * Converts the {@link RectD} to a {@link RectI} by rounding coordinates to the
     * nearest {@link Integer} values. Uses {@link PointD#round} for rounding.
     * 
     * @return a {@link RectI} whose {@link RectI#min} and {@link RectI#max}
     *         points equal the corresponding points of the {@link RectD}, with
     *         each coordinate rounded to the nearest {@link Integer} value
     * @throws ArithmeticException if any coordinate overflows {@link Integer}
     */
    public RectI round() {
        return new RectI(min.round(), max.round());
    }

    /**
     * Converts the specified {@link RectD} array to a {@link Double} array.
     * The returned array has four times as many elements as the specified
     * {@code rects} and retains the same coordinate sequence.
     * 
     * @param rects the {@link RectD} array to convert
     * @return an array containing the coordinates {@code min.x, min.y, max.x, max.y}
     *         for all {@code rects}, stored in successive index positions per {@link RectD}
     * @throws NullPointerException if {@code rects} or any of its elements is {@code null}
     */
    public static double[] toDoubles(RectD... rects) {
        final double[] output = new double[4 * rects.length];

        for (int i = 0; i < rects.length; i++) {
            output[4 * i] = rects[i].min.x;
            output[4 * i + 1] = rects[i].min.y;
            output[4 * i + 2] = rects[i].max.x;
            output[4 * i + 3] = rects[i].max.y;
        }

        return output;
    }

    /**
     * Converts the {@link RectD} to a {@link RectI} by truncating coordinates to the
     * nearest {@link Integer} values. Uses {@link Integer} casts for truncation.
     * 
     * @return a {@link RectI} whose {@link RectI#min} and {@link RectI#max}
     *         points equal the corresponding points of the {@link RectD}, with
     *         each coordinate truncated to the nearest {@link Integer} value
     * @throws ArithmeticException if any coordinate overflows {@link Integer}
     */
    public RectI toRectI() {
        return new RectI(min.toPointI(), max.toPointI());
    }
    
    /**
     * Finds the union of the {@link RectD} with the specified instance.
     * @param rect the {@link RectD} to combine with this instance
     * @return a {@link RectD} that contains the union of {@code rect} with this instance
     * @throws NullPointerException if {@code rect} is {@code null}
     */
    public RectD union(RectD rect) {

        final double minX = Math.min(min.x, rect.min.x);
        final double minY = Math.min(min.y, rect.min.y);
        final double maxX = Math.max(max.x, rect.max.x);
        final double maxY = Math.max(max.y, rect.max.y);

        return new RectD(minX, minY, maxX, maxY);
    }

    /**
     * Gets the width the {@link RectD}. Never negative but may be zero.
     * @return the difference between the y-coordinates of {@link #max} and {@link #min}
     */
    public double width() {
        return (max.x - min.x);
    }

    /**
     * Compares the specified {@link Object} to this {@link RectD} instance.
     * @param obj the {@link Object} to compare to this instance
     * @return {@code true} if {@code obj} is not {@code null} and a {@link RectD} instance whose 
     *         {@link #min} and {@link #max} equal those of this instance, else {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || !(obj instanceof RectD))
            return false;

        final RectD rect = (RectD) obj;
        return (min.equals(rect.min) && max.equals(rect.max));
    }

    /**
     * Returns a hash code for the {@link RectD}.
     * @return an {@link Integer} hash code for the {@link RectD}
     */
    @Override
    public int hashCode() {
        return (31 * min.hashCode() + max.hashCode());
    }

    /**
     * Returns a {@link String} representation of the {@link RectD}.
     * @return a {@link String} containing the values of {@link #min} and {@link #max}
     */
    @Override
    public String toString() {
        return String.format("RectD[min.x=%g, min.y=%g, max.x=%g, max.y=%g]",
                min.x, min.y, max.x, max.y);
    }
}
