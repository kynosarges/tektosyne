package org.kynosarges.tektosyne.geometry;

/**
 * Represents a rectangular region in two-dimensional space, using {@link Integer} coordinates.
 * {@link RectI} contains two immutable {@link PointI} locations, defining the opposite corners
 * of a rectangle. Use {@link RectD} to represent rectangles with {@link Double} coordinates.
 * 
 * @author Christoph Nahr
 * @version 6.3.0
 */
public final class RectI {
    /**
     * The smallest coordinates within the {@link RectI}.
     * Both dimensions are equal to or less than the corresponding {@link #max} dimensions.
     */
    public final PointI min;

    /**
     * The greatest coordinates within the {@link RectI}.
     * Both dimensions are equal to or greater than the corresponding {@link #min} dimensions.
     */
    public final PointI max;

    /**
     * An empty read-only {@link RectI}.
     * {@link #min} and {@link #max} are both set to {@link PointI#EMPTY}.
     */
    public final static RectI EMPTY = new RectI();

    /**
     * Creates a {@link RectI} with all coordinates set to zero.
     * {@link #min} and {@link #max} are both set to {@link PointI#EMPTY}.
     */
    public RectI() {
        this.min = PointI.EMPTY;
        this.max = PointI.EMPTY;
    }

    /**
     * Creates a {@link RectI} with the specified {@link Integer} coordinates.
     * @param minX the smallest x-coordinate within the {@link RectI}
     * @param minY the smallest y-coordinate within the {@link RectI}
     * @param maxX the greatest x-coordinate within the {@link RectI}
     * @param maxY the greatest y-coordinate within the {@link RectI}
     * @throws IllegalArgumentException if {@code maxX} is less than {@code minX},
     *                                  or {@code maxY} is less than {@code minY}
     */
    public RectI(int minX, int minY, int maxX, int maxY) {
        if (maxX < minX)
            throw new IllegalArgumentException("maxX < minX");
        if (maxY < minY)
            throw new IllegalArgumentException("maxY < minY");

        this.min = new PointI(minX, minY);
        this.max = new PointI(maxX, maxY);
    }

    /**
     * Creates a {@link RectI} with the specified {@link PointI} coordinates.
     * @param min the smallest coordinates within the {@link RectI}
     * @param max the greatest coordinates within the {@link RectI}
     * @throws IllegalArgumentException if any dimension of {@code max} is less than
     *                                  the corresponding dimension of {@code min}
     * @throws NullPointerException if {@code min} or {@code max} is {@code null}
     */
    public RectI(PointI min, PointI max) {
        if (max.x < min.x)
            throw new IllegalArgumentException("max.x < min.x");
        if (max.y < min.y)
            throw new IllegalArgumentException("max.y < min.y");

        this.min = min;
        this.max = max;
    }

    /**
     * Circumscribes a {@link RectI} around the specified {@link PointI} coordinates.
     * Sets both dimensions of {@link #min} to the smallest coordinates, and both dimensions
     * of {@link #max} to the greatest coordinates, found among any {@code points}.
     * 
     * @param points an array of {@link PointI} coordinates whose bounds to determine
     * @return the smallest {@link RectI} that contains all specified {@code points}
     * @throws NullPointerException if {@code points} is {@code null} or empty,
     *                              or contains any {@code null} elements
     */
    public static RectI circumscribe(PointI... points) {
        if (points == null || points.length == 0)
            throw new NullPointerException("points");

        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE;

        for (PointI point: points) {
            if (x0 > point.x) x0 = point.x;
            if (y0 > point.y) y0 = point.y;
            if (x1 < point.x) x1 = point.x;
            if (y1 < point.y) y1 = point.y;
        }

        return new RectI(x0, y0, x1, y1);
    }

    /**
     * Determines whether the {@link RectI} contains the specified {@link Integer} coordinates.
     * @param x the x-coordinate to examine
     * @param y the y-coordinate to examine
     * @return {@code true} if both {@code x} and {@code y} fall within the range of
     *         coordinates defined by {@link #min} and {@link #max}, else {@code false}
     */
    public boolean contains(int x, int y) {
        return (x >= min.x && y >= min.y &&
                x <= max.x && y <= max.y);
    }

    /**
     * Determines whether the {@link RectI} contains the specified {@link PointI} coordinates.
     * @param point the {@link PointI} to examine
     * @return {@code true} if both dimensions of {@code point} fall within the range of
     *         coordinates defined by {@link #min} and {@link #max}, else {@code false}
     * @throws NullPointerException if {@code point} is {@code null}
     */
    public boolean contains(PointI point) {
        return contains(point.x, point.y);
    }

    /**
     * Determines whether the {@link RectI} entirely contains the specified instance.
     * @param rect the {@link RectI} to examine
     * @return {@code true} if both {@link #min} and {@link #max} of the specified {@code rect}
     *         fall within the range coordinates defined by this instance, else {@code false}
     * @throws NullPointerException if {@code rect} is {@code null}
     */
    public boolean contains(RectI rect) {

        return (rect.min.x >= min.x && rect.min.y >= min.y &&
                rect.max.x <= max.x && rect.max.y <= max.y);
    }

    /**
     * Determines whether the {@link RectI} contains the specified {@link Integer} coordinates,
     * excluding the x- and y-coordinates of {@link #max}.
     * 
     * @param x the x-coordinate to examine
     * @param y the y-coordinate to examine
     * @return {@code true} if both {@code x} and {@code y} fall within the range of coordinates
     *         defined by {@link #min} (inclusive) and {@link #max} (exclusive), else {@code false}
     */
    public boolean containsOpen(int x, int y) {
        return (x >= min.x && y >= min.y &&
                x < max.x && y < max.y);
    }

    /**
     * Determines whether the {@link RectI} contains the specified {@link PointD} coordinates,
     * excluding the x- and y-coordinates of {@link #max}.
     * 
     * @param point the {@link PointI} to examine
     * @return {@code true} if both dimensions of {@code point} fall within the range of coordinates
     *         defined by {@link #min} (inclusive) and {@link #max} (exclusive), else {@code false}
     * @throws NullPointerException if {@code point} is {@code null}
     */
    public boolean containsOpen(PointI point) {
        return containsOpen(point.x, point.y);
    }

    /**
     * Finds the distance vector from the specified {@link PointI} coordinates
     * to the nearest edges of the {@link RectI}.
     * The returned coordinates are negative if the corresponding dimension of {@code q}
     * if less than that of {@link #min}, and positive if it is greater than {@link #max}.
     * 
     * @param q the {@link PointI} to examine
     * @return a {@link PointI} whose dimensions equal zero if they fall within
     *         the corresponding dimensions of {@link #min} and {@link #max},
     *         else the signed difference to the nearest edge coordinate
     * @throws ArithmeticException if the difference in any dimension overflows {@link Integer}
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public PointI distanceVector(PointI q) {

        final int x = (q.x < min.x ? Math.subtractExact(q.x, min.x) :
                       q.x > max.x ? Math.subtractExact(q.x, max.x) : 0);

        final int y = (q.y < min.y ? Math.subtractExact(q.y, min.y) :
                       q.y > max.y ? Math.subtractExact(q.y, max.y) : 0);

        return new PointI(x, y);
    }

    /**
     * Converts the specified {@link Integer} array to a {@link RectI} array.
     * The returned array has a quarter as many elements as the specified
     * {@code rects} and retains the same coordinate sequence.
     * 
     * @param rects an array containing the coordinates {@code min.x, min.y, max.x, max.y} of
     *              {@link RectI} instances, stored in successive index positions per {@link RectI}
     * @return the {@link RectI} array created from {@code rects}
     * @throws IllegalArgumentException if the length of {@code rects} is not divisible by four,
     *         or if any {@link #max} coordinate is less than the corresponding {@link #min} coordinate
     * @throws NullPointerException if {@code rects} is {@code null}
     */
    public static RectI[] fromInts(int... rects) {
        if (rects.length % 4 != 0)
            throw new IllegalArgumentException("rects.length % 4 != 0");

        final RectI[] output = new RectI[rects.length / 4];

        for (int i = 0; i < output.length; i++)
            output[i] = new RectI(rects[4 * i], rects[4 * i + 1],
                    rects[4 * i + 2], rects[4 * i + 3]);

        return output;
    }

    /**
     * Gets the height the {@link RectI}.
     * Never negative but may be zero. One less than the actual height if {@link RectI} coordinates
     * represent pixels. Coordinates are widened to {@link Long} to prevent {@link Integer} overflow.
     * 
     * @return the difference between the x-coordinates of {@link #max} and {@link #min}
     */
    public long height() {
        return ((long) max.y - min.y);
    }

    /**
     * Finds the intersection of the {@link RectI} with the specified instance.
     * @param rect the {@link RectI} to intersect with this instance
     * @return a {@link RectI} that contains the intersection of {@code rect} with this instance,
     *         if any intersection was found, else {@code null}
     * @throws NullPointerException if {@code rect} is {@code null}
     */
    public RectI intersect(RectI rect) {

        final int minX = Math.max(min.x, rect.min.x);
        final int minY = Math.max(min.y, rect.min.y);
        final int maxX = Math.min(max.x, rect.max.x);
        final int maxY = Math.min(max.y, rect.max.y);

        if (minX > maxX || minY > maxY)
            return null;
        
        return new RectI(minX, minY, maxX, maxY);
    }

    /**
     * Determines whether the {@link RectI} intersects with the specified instance.
     * @param rect the {@link RectI} to examine
     * @return {@code true} if {@code rect} shares any coordinates with this instance,
     *         else {@code false}
     * @throws NullPointerException if {@code rect} is {@code null}
     */
    public boolean intersectsWith(RectI rect) {

        return (rect.max.x >= min.x && rect.min.x <= max.x &&
                rect.max.y >= min.y && rect.min.y <= max.y);
    }

    /**
     * Determines the location of the specified {@link PointI} relative to the {@link RectI}.
     * @param q the {@link PointI} coordinates to examine
     * @return a {@link RectLocation} indicating the location of {@code q}
     *         relative to the {@link RectD} in each dimension
     * @throws NullPointerException if {@code q} is {@code null}
     */
    public RectLocation locate(PointI q) {

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
     * Offsets the {@link RectI} by the specified {@link Integer} distances.
     * @param x the horizontal offset applied to the {@link RectI}
     * @param y the vertical offset applied to the {@link RectI}
     * @return a {@link RectI} whose {@link #min} and {@link #max} equal those of the current
     *         instance, with {@code x} and {@code y} added to all corresponding dimensions
     * @throws ArithmeticException if the new coordinates in any dimension overflow {@link Integer}
     */
    public RectI offset(int x, int y) {
        return new RectI(
                Math.addExact(min.x, x), Math.addExact(min.y, y),
                Math.addExact(max.x, x), Math.addExact(max.y, y));
    }

    /**
     * Offsets the {@link RectI} by the specified {@link PointI} vector.
     * @param vector the {@link PointI} offset applied to the {@link RectI}
     * @return a {@link RectI} whose {@link #min} and {@link #max} equal those
     *         of the current instance, with {@code vector} added to both points
     * @throws ArithmeticException if the new coordinates in any dimension overflow {@link Integer}
     * @throws NullPointerException if {@code vector} is {@code null}
     */
    public RectI offset(PointI vector) {
        return new RectI(min.add(vector), max.add(vector));
    }

    /**
     * Converts the specified {@link RectI} array to an {@link Integer} array.
     * The returned array has four times as many elements as the specified
     * {@code rects} and retains the same coordinate sequence.
     * 
     * @param rects the {@link RectI} array to convert
     * @return an array containing the coordinates {@code min.x, min.y, max.x, max.y}
     *         for all {@code rects}, stored in successive index positions per {@link RectI}
     * @throws NullPointerException if {@code rects} or any of its elements is {@code null}
     */
    public static int[] toInts(RectI... rects) {
        final int[] output = new int[4 * rects.length];

        for (int i = 0; i < rects.length; i++) {
            output[4 * i] = rects[i].min.x;
            output[4 * i + 1] = rects[i].min.y;
            output[4 * i + 2] = rects[i].max.x;
            output[4 * i + 3] = rects[i].max.y;
        }

        return output;
    }

    /**
     * Converts the {@link RectI} to a {@link RectD}.
     * @return a {@link RectD} whose {@link RectD#min} and {@link RectD#max}
     *         points equal the corresponding points of the {@link RectI}
     */
    public RectD toRectD() {
        return new RectD(min.toPointD(), max.toPointD());
    }
    
    /**
     * Finds the union of the {@link RectI} with the specified instance.
     * @param rect the {@link RectI} to combine with this instance
     * @return a {@link RectI} that contains the union of {@code rect} with this instance
     * @throws NullPointerException if {@code rect} is {@code null}
     */
    public RectI union(RectI rect) {

        final int minX = Math.min(min.x, rect.min.x);
        final int minY = Math.min(min.y, rect.min.y);
        final int maxX = Math.max(max.x, rect.max.x);
        final int maxY = Math.max(max.y, rect.max.y);

        return new RectI(minX, minY, maxX, maxY);
    }

    /**
     * Gets the width the {@link RectI}.
     * Never negative but may be zero. One less than the actual width if {@link RectI} coordinates
     * represent pixels. Coordinates are widened to {@link Long} to prevent {@link Integer} overflow.
     * 
     * @return the difference between the y-coordinates of {@link #max} and {@link #min}
     */
    public long width() {
        return ((long) max.x - min.x);
    }

    /**
     * Compares the specified {@link Object} to this {@link RectI} instance.
     * @param obj the {@link Object} to compare to this instance
     * @return {@code true} if {@code obj} is not {@code null} and a {@link RectI} instance whose 
     *         {@link #min} and {@link #max} equal those of this instance, else {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof RectI))
            return false;

        final RectI rect = (RectI) obj;
        return (min.equals(rect.min) && max.equals(rect.max));
    }

    /**
     * Returns a hash code for the {@link RectI}.
     * @return an {@link Integer} hash code for the {@link RectI}
     */
    @Override
    public int hashCode() {
        return (31 * min.hashCode() + max.hashCode());
    }

    /**
     * Returns a {@link String} representation of the {@link RectI}.
     * @return a {@link String} containing the values of {@link #min} and {@link #max}
     */
    @Override
    public String toString() {
        return String.format("RectI[min.x=%d, min.y=%d, max.x=%d, max.y=%d]",
                min.x, min.y, max.x, max.y);
    }
}
