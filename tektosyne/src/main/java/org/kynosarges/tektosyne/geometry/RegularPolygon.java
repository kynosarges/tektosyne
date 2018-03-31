package org.kynosarges.tektosyne.geometry;

import org.kynosarges.tektosyne.*;

/**
 * Represents a regular polygon.
 * Defines a regular polygon with three or more sides of a given length, and with one of the
 * orientations defined by {@link PolygonOrientation}. The vertex coordinates of all polygons
 * are symmetrical across the vertical axis, and those of polygons with an even number of sides
 * are also symmetrical across the horizontal axis.
 * <p>
 * Upon construction, {@link RegularPolygon} calculates the radii of the inscribed and
 * circumscribed circles, the coordinates of all vertices, and the minimum bounding rectangle.
 * All of these values are immutable once defined. Methods that seem to change the side length
 * of a given {@link RegularPolygon} return a new instance instead.</p>
 * 
 * @author Christoph Nahr
 * @version 6.1.0
 */
public class RegularPolygon {
    /**
     * The bounding {@link RectD} circumscribed around the {@link RegularPolygon}.
     * All coordinates are relative to the center of the {@link RegularPolygon}. The {@link RectD}
     * is horizontally centered on the {@link RegularPolygon}, and also vertically centered for
     * an even number of {@link #sides}. Both {@link RectD#min} coordinates are always negative,
     * and both {@link RectD#max} coordinates are always positive.
     */
    public final RectD bounds;

    /**
     * The maximum number of neighbors for the {@link RegularPolygon}.
     * Equals {@link #sides} if {@code #vertexNeighbors} is {@code false}, else twice
     * that number. Applies to regular grids of adjacent identical {@link RegularPolygon}
     * instances, such as the ones represented by {@link PolygonGrid}.
     */
    public final int connectivity;

    /**
     * Indicates whether index zero within {@link #connectivity} is topmost.
     * {@code true} if index zero within the {@link #connectivity} range corresponds to the
     * topmost edge or vertex of the {@link RegularPolygon}; {@code false} if this index
     * corresponds to the edge to the right of the topmost vertex.
     * <p>
     * {@link #hasTopIndex} is {@code true} if one of the following conditions holds:</p>
     * <ul>
     * <li>{@link #vertexNeighbors} is {@code true}</li>
     * <li>{@link #orientation} is {@link PolygonOrientation#ON_EDGE}, and {@link #sides} is even</li>
     * <li>{@link #orientation} is {@link PolygonOrientation#ON_VERTEX}, and {@link #sides} is odd</li>
     * </ul>
     */
    public final boolean hasTopIndex;

    /**
     * The radius of the circle inscribed within the {@link RegularPolygon}.
     * Always greater than zero and smaller than {@link #outerRadius}.
     */
    public final double innerRadius;

    /**
     * The length of each side of the {@link RegularPolygon}.
     * Always greater than zero.
     */
    public final double length;

    /**
     * The orientation of the {@link RegularPolygon}.
     * Never {@code null}.
     */
    public final PolygonOrientation orientation;

    /**
     * The radius of the circle circumscribed around the {@link RegularPolygon}.
     * Always greater than zero and greater than {@link #innerRadius}.
     */
    public final double outerRadius;

    /**
     * The number of sides of the {@link RegularPolygon}.
     * Always greater than or equal to three.
     */
    public final int sides;

    /**
     * Indicates whether {@link RegularPolygon} instances that share only a common vertex
     * are considered neighbors.
     * Applies to regular grids of adjacent identical {@link RegularPolygon} instances,
     * such as the ones represented by {@link PolygonGrid}. Always {@code false} if
     * {@link #sides} exceeds four, as inner angle of more than 90° prevent adjacent
     * instances from sharing a vertex without also sharing an edge.
     * <p>
     * {@link RegularPolygon} instances that share a common edge are always considered
     * neighbors. The maximum number of shared edges, and possibly vertices, equals
     * {@link #sides}. {@link #vertexNeighbors} and {@link #sides} together determine
     * {@link #connectivity} which in turn determines the index range used by
     * {@link #angleToIndex} and {@link #indexToAngle}.</p>
     */
    public final boolean vertexNeighbors;

    /**
     * The {@link PointD} coordinates of all vertices of the {@link RegularPolygon}.
     * Always contains {@link #sides} elements. Starts with the topmost vertex or with the
     * right-hand one of two topmost vertices, and continues clockwise.
     * <p>
     * All coordinates are relative to the center of the {@link RegularPolygon}.
     * The first element always has a negative {@link PointD#y} coordinate.</p>
     */
    public final PointD[] vertices;

    /**
     * Creates a {@link RegularPolygon} with the specified side length, number of sides, and orientation.
     * {@link #vertexNeighbors} is set to {@code false}.
     * 
     * @param length the length of each side of the {@link RegularPolygon}
     * @param sides the number of sides of the {@link RegularPolygon}
     * @param orientation the orientation of the {@link RegularPolygon}
     * @throws IllegalArgumentException if {@code length} is equal to or less than zero,
     *         or {@code sides} is less than three, or {@code orientation} is unknown
     * @throws NullPointerException if {@code orientation} is {@code null}
     */
    public RegularPolygon(double length, int sides, PolygonOrientation orientation) {
        this(length, sides, orientation, false);
    }

    /**
     * Creates a {@link RegularPolygon} with the specified side length, number of sides, and orientation.
     * @param length the length of each side of the {@link RegularPolygon}
     * @param sides the number of sides of the {@link RegularPolygon}
     * @param orientation the orientation of the {@link RegularPolygon}
     * @param vertexNeighbors {@code true} if {@link RegularPolygon} instances that share only
     *                        a common vertex are considered neighbors, else {@code false}
     * @throws IllegalArgumentException if {@code length} is equal to or less than zero,
     *         or {@code sides} is less than three, or {@code vertexNeighbors} is {@code true}
     *         and {@code sides} is greater than four, or {@code orientation} is unknown
     * @throws NullPointerException if {@code orientation} is {@code null}
     */
    public RegularPolygon(double length, int sides,
        PolygonOrientation orientation, boolean vertexNeighbors) {

        if (length <= 0.0)
            throw new IllegalArgumentException("length <= 0");
        if (sides < 3)
            throw new IllegalArgumentException("sides < 3");
        if (orientation == null)
            throw new NullPointerException("orientation");
        if (vertexNeighbors && sides > 4)
            throw new IllegalArgumentException("vertexNeighbors && sides > 4");

        this.length = length;
        this.sides = sides;
        this.orientation = orientation;
        this.vertexNeighbors = vertexNeighbors;

        // compute maximum neighbors on edges and vertices
        this.connectivity = (vertexNeighbors ? 2 * sides : sides);

        // determine whether a top connection exists
        this.hasTopIndex = (vertexNeighbors || (sides % 2 == 0 ?
            orientation == PolygonOrientation.ON_EDGE :
            orientation == PolygonOrientation.ON_VERTEX));

        // compute angle of one segment between vertices
        double angle, segment = (2.0 * Math.PI) / sides;

        // compute radii of circumscribed and inscribed circles
        this.outerRadius = length / (2.0 * Math.sin(segment / 2.0));
        this.innerRadius = outerRadius * Math.cos(segment / 2.0);

        // compute angle of first vertex and check orientation
        switch (orientation) {
            case ON_EDGE:
                angle = (sides % 2 == 0 ? segment : 0.0);
                break;

            case ON_VERTEX:
                angle = (sides % 2 == 0 ? 0.0 : segment);
                break;
                
            default:
                throw new IllegalArgumentException("orientation");
        }

        // halve angle and rotate 90° counter-clockwise
        angle = (angle - Math.PI) / 2.0;

        // compute and store vertex coordinates around center
        this.vertices = new PointD[sides];
        for (int i = 0; i < sides; i++, angle += segment)
            this.vertices[i] = new PointD(
                outerRadius * Math.cos(angle), outerRadius * Math.sin(angle));

        // compute and store circumscribed rectangle
        this.bounds = RectD.circumscribe(vertices);
    }

    /**
     * Converts the specified central angle to the index of the corresponding edge or vertex.
     * The specified {@code angle} is measured from the center of the {@link RegularPolygon},
     * and increases clockwise from the right-hand side of the x-axis.
     * <p>
     * If {@link #vertexNeighbors} is {@code false}, the returned index enumerates all edges
     * in clockwise direction. Counting starts at the topmost edge if {@link #hasTopIndex} is
     * {@code true}, and with the edge to the right of the topmost vertex otherwise.</p>
     * <p>
     * If {@link #vertexNeighbors} is {@code true}, the returned index enumerates all edges
     * and vertices in an alternating sequence. Counting starts with the topmost edge if
     * {@link #orientation} equals {@link PolygonOrientation#ON_EDGE} and with the topmost vertex
     * otherwise, continuing clockwise.</p>
     * <p>
     * Valid indices range from zero to {@link #connectivity} less one. The 360 degrees
     * of a full rotation around the central point are evenly divided among this range so that
     * each index corresponds to an equal arc. If {@link #vertexNeighbors} is {@code true},
     * the arcs that are mapped to edge indices cover only the central half of each edge. The 
     * arcs covering the outer parts are mapped to vertex indices instead.</p>
     * 
     * @param angle the central angle to convert, in degrees. Taken modulo 360°
     *              and may therefore lie outside the interval [0, 360)
     * @return the zero-based index of the edge or vertex at {@code angle}
     */
    public int angleToIndex(double angle) {
        final double segment = 360.0 / connectivity;
        if (hasTopIndex) angle += segment / 2.0;
        angle = Fortran.modulo(angle + 90.0, 360.0);
        return (int) (angle / segment);
    }

    /**
     * Creates a similar {@link RegularPolygon} that is circumscribed around the specified circle.
     * Returns the current instance if {@link #innerRadius} already equals {@code radius}.
     * 
     * @param radius the radius of the circle around which to circumscribe the {@link RegularPolygon}
     * @return a {@link RegularPolygon} whose {@link #innerRadius} equals {@code radius}
     *         and which is otherwise identical with the current instance
     * @throws IllegalArgumentException if {@code radius} is equal to or less than zero
     */
    public RegularPolygon circumscribe(double radius) {
        if (radius == innerRadius)
            return this;
        if (radius <= 0.0)
            throw new IllegalArgumentException("radius <= 0");

        final double newLength = 2.0 * radius * Math.tan(Math.PI / sides);
        return new RegularPolygon(newLength, sides, orientation, vertexNeighbors);
    }

    /**
     * Creates a similar {@link RegularPolygon} that is circumscribed around the specified rectangle.
     * Returns exact results for triangles and squares only. For other polygons, the returned
     * {@link RegularPolygon} is an approximation that includes some excess space around an
     * inscribed rectangle with the specified {@code width} and {@code height}.
     * 
     * @param width the width of the rectangle around which to circumscribe the {@link RegularPolygon}
     * @param height the width of the rectangle around which to circumscribe the {@link RegularPolygon}
     * @return a {@link RegularPolygon} whose {@link #bounds} completely cover both {@code width}
     *         and {@code height}, and which is otherwise identical with the current instance
     * @throws IllegalArgumentException if {@code width} or {@code height} is equal to or less than zero
     */
    public RegularPolygon circumscribe(double width, double height) {
        if (width <= 0.0)
            throw new IllegalArgumentException("width <= 0");
        if (height <= 0.0)
            throw new IllegalArgumentException("height <= 0");

        double newLength;
        if (sides == 3) {
            /*
             * Triangle: Width is always equal to or smaller than one edge.
             * The height of the triangle is at least the specified height
             * plus an extra bit, depending on the width of the rectangle.
             */
            final double angle = Math.PI / 3.0; // 60° angle
            final double triangleHeight = height + width * Math.tan(angle) / 2.0;
            final double heightLength = triangleHeight / Math.sin(angle);

            newLength = Math.max(width, heightLength);
        }
        else if (sides == 4) {
            /*
             * Square: Lying squares trivially cover an inscribed rectangle.
             * Standing squares have a diagonal that equals the sum of the
             * specified width and height, hence the side length.
             */
            if (orientation == PolygonOrientation.ON_EDGE)
                newLength = Math.max(width, height);
            else
                newLength = (width + height) / Math.sqrt(2.0);
        }
        else {
            /*
             * For any other polygons, we approximate the diameter of the
             * inscribed circle by the diagonal of the specified rectangle.
             * Then we circumscribe the polygon around this circle.
             */
            final double diameter = Math.sqrt(width * width + height * height);
            newLength = diameter * Math.tan(Math.PI / sides);
        }

        return new RegularPolygon(newLength, sides, orientation, vertexNeighbors);
    }

    /**
     * Converts the specified {@link Compass} direction to the corresponding edge or vertex index.
     * Returns the result of {@link Compass#degrees} for {@code compass}, less 90 degrees.
     * See {@link #angleToIndex} for an explanation of index values.
     * 
     * @param compass the {@link Compass} direction to convert
     * @return the zero-based index of the edge or vertex closest to {@code compass}
     * @throws NullPointerException if {@code compass} is {@code null}
     */
    public int compassToIndex(Compass compass) {
        return angleToIndex(compass.degrees() - 90);
    }

    /**
     * Converts the specified edge or vertex index to the corresponding central angle.
     * Always returns a value within [0, 360) which is measured from the center of the 
     * {@link RegularPolygon} and increases clockwise from the right-hand side of the x-axis.
     * This value represents the angle from the central point to the indicated vertex,
     * or to the middle of the indicated edge.
     * <p>
     * If {@link #vertexNeighbors} is {@code false}, the specified {@code index} enumerates
     * all edges in clockwise direction. Counting starts at the topmost edge if {@link #hasTopIndex}
     * is {@code true}, and with the edge to the right of the topmost vertex otherwise.</p>
     * <p>
     * If {@link #vertexNeighbors} is {@code true}, the specified {@code index} enumerates
     * all edges and vertices in an alternating sequence. Counting starts with the topmost edge
     * for {@link PolygonOrientation#ON_EDGE} orientation and with the topmost vertex otherwise,
     * continuing clockwise.</p>
     * 
     * @param index the zero-based index of an edge or vertex. Taken modulo {@link #connectivity}
     *              and may therefore be negative or greater than the maximum index
     * @return the central angle, in degrees, of the edge or vertex with the specified {@code index}
     */
    public double indexToAngle(int index) {
        final double segment = 360.0 / connectivity;
        double angle = Fortran.modulo(index, connectivity) * segment;
        if (!hasTopIndex) angle += segment / 2.0;
        return Fortran.modulo(angle - 90.0, 360.0);
    }

    /**
     * Converts the specified edge or vertex index to the corresponding {@link Compass} direction.
     * First adds 90° to the result of {@link #indexToAngle} for the specified {@code index},
     * and then returns the result of {@link Angle#degreesToCompass} for that angle.
     * See {@link #indexToAngle} for an explanation of index values.
     * 
     * @param index the zero-based index of an edge or vertex. Taken modulo {@link #connectivity}
     *              and may therefore be negative or greater than the maximum index
     * @return the {@link Compass} direction closest to the edge or vertex with the specified {@code index}
     */
    public Compass indexToCompass(int index) {
        final double degrees = indexToAngle(index) + 90;
        return Angle.degreesToCompass(degrees);
    }

    /**
     * Creates a similar {@link RegularPolygon} that is inflated by the specified radius.
     * Returns the current instance if {@code delta} equals zero. Otherwise, adds {@code delta}
     * (which may be negative) to {@link #outerRadius}. The new {@link #length} changes by the
     * same ratio as the new {@link #outerRadius}.
     * 
     * @param delta the amount by which to inflate the {@link #outerRadius} of the {@link RegularPolygon}
     * @return a {@link RegularPolygon} whose {@link #outerRadius} is inflated by {@code delta}
     *         and which is otherwise identical with the current instance
     * @throws IllegalArgumentException if {@code delta} is equal to or less than the negative
     *                                  value of {@link #outerRadius}
     */
    public RegularPolygon inflate(double delta) {
        if (delta == 0.0) return this;
        if (delta <= -outerRadius)
            throw new IllegalArgumentException("delta <= -outerRadius");

        final double newLength = length * (outerRadius + delta) / outerRadius;
        return new RegularPolygon(newLength, sides, orientation, vertexNeighbors);
    }

    /**
     * Creates a similar {@link RegularPolygon} that is inscribed in the specified circle.
     * Returns the current instance if {@link #outerRadius} already equals {@code radius}.
     * 
     * @param radius the radius of the circle in which to inscribe the {@link RegularPolygon}
     * @return a {@link RegularPolygon} whose {@link #outerRadius} equals {@code radius}
     *         and which is otherwise identical with the current instance
     * @throws IllegalArgumentException if {@code radius} is equal to or less than zero
     */
    public RegularPolygon inscribe(double radius) {
        if (radius == outerRadius)
            return this;
        if (radius <= 0.0)
            throw new IllegalArgumentException("radius <= 0");

        final double newLength = 2.0 * radius * Math.sin(Math.PI / sides);
        return new RegularPolygon(newLength, sides, orientation, vertexNeighbors);
    }

    /**
     * Creates a similar {@link RegularPolygon} that is inscribed in the specified rectangle.
     * Returns the current instance if the extensions of {@link #bounds} already equal both
     * {@code width} and {@code height}.
     * 
     * @param width the width of the rectangle in which to inscribe the {@link RegularPolygon}
     * @param height the width of the rectangle in which to inscribe the {@link RegularPolygon}
     * @return a {@link RegularPolygon} whose {@link #bounds} exactly match either {@code width}
     *         or {@code height} without exceeding the other dimension, and which is otherwise
     *         identical with the current instance
     * @throws IllegalArgumentException if {@code width} or {@code height} is equal to or less than zero
     */
    public RegularPolygon inscribe(double width, double height) {
        if (width == bounds.width() && height == bounds.height())
            return this;

        if (width <= 0.0)
            throw new IllegalArgumentException("width <= 0");
        if (height <= 0.0)
            throw new IllegalArgumentException("height <= 0");

        // compute angle of one segment between vertices
        double newLength, halfSegment = Math.PI / sides;

        if (sides % 4 == 0) {
            /*
             * All edges of the circumscribed rectangle face either edges or vertices
             * of the inscribed polygon. If edges, we use the diameter of the inscribed
             * circle to determine the side length; otherwise, that of the circumcircle.
             */
            final double diameter = Math.min(width, height);

            if (orientation == PolygonOrientation.ON_EDGE)
                newLength = diameter * Math.tan(halfSegment);
            else
                newLength = diameter * Math.sin(halfSegment);
        }
        else if (sides % 2 == 0) {
            /*
             * One pair of edges of the circumscribed rectangle face edges of the
             * inscribed polygon, and the other pair face vertices. We compute the
             * side length resulting from the inscribed circle for one pair and from
             * the circumcircle for the other, and then choose the smaller length.
             */
            double innerDiameter, outerDiameter;

            if (orientation == PolygonOrientation.ON_EDGE) {
                innerDiameter = height; outerDiameter = width;
            } else {
                innerDiameter = width; outerDiameter = height;
            }

            final double outerLength = outerDiameter * Math.sin(halfSegment);
            final double innerLength = innerDiameter * Math.tan(halfSegment);

            newLength = Math.min(innerLength, outerLength);
        }
        else {
            /*
             * RegularPolygon is symmetrical horizontally but not vertically. We base
             * all calculations on the circumcircle and derive the distance between
             * top and bottom vertex from the height of the rectangle, and twice the
             * distance to the rightmost vertex from the width of the rectangle.
             */
            double topAngle, segment = 2.0 * halfSegment;
            int rightIndex = (sides / 4);

            if (orientation == PolygonOrientation.ON_EDGE) {
                topAngle = 0.0;
                if ((sides - 1) % 4 != 0) ++rightIndex;
            } else
                topAngle = segment;

            topAngle = (topAngle - Math.PI) / 2.0;
            final double radiusFactor = Math.sin(halfSegment);

            final double rightAngle = topAngle + rightIndex * segment;
            final double widthLength = width * radiusFactor / Math.cos(rightAngle);

            final int halfSide = sides / 2; // intentional truncation!
            final double bottomAngle = topAngle + halfSide * segment;
            final double heightLength = 2.0 * height * radiusFactor /
                    (Math.sin(bottomAngle) - Math.sin(topAngle));

            newLength = Math.min(widthLength, heightLength);
        }

        return new RegularPolygon(newLength, sides, orientation, vertexNeighbors);
    }

    /**
     * Determines the index of the edge or vertex opposite to that with the specified index.
     * See {@link #indexToAngle} for an explanation of index values.
     * 
     * @param index the zero-based index of an edge or vertex. Taken modulo {@link #connectivity}
     *              and may therefore be negative or greater than the maximum index
     * @return the zero-based index of the edge or vertex opposite {@code index}
     * @throws IllegalStateException if {@link #connectivity} is odd, as opposing indices
     *                               exist only if the total number of indices is even
     */
    public int opposingIndex(int index) {
        if (connectivity % 2 != 0)
            throw new IllegalStateException("connectivity % 2 != 0");

        return Fortran.modulo(index + connectivity / 2, connectivity);
    }

    /**
     * Creates a similar {@link RegularPolygon} with the specified side length.
     * Returns the current instance if {@link #length} already equals the specified {@code length}.
     * 
     * @param length the new {@link #length} for the {@link RegularPolygon}
     * @return a {@link RegularPolygon} whose {@link #length} equals {@code length}
     *         and which is otherwise identical with the current instance
     * @throws IllegalArgumentException if {@code length} is equal to or less than zero
     */
    public RegularPolygon resize(double length) {
        if (length == length) return this;
        if (length <= 0.0)
            throw new IllegalArgumentException("length <= 0");

        return new RegularPolygon(length, sides, orientation, vertexNeighbors);
    }
}
