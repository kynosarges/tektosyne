package org.kynosarges.tektosyne.subdivision;

import java.util.*;
import java.util.function.*;

import org.kynosarges.tektosyne.MathUtils;
import org.kynosarges.tektosyne.geometry.*;

/**
 * Represents a half-edge in a planar {@link Subdivision}.
 * Every {@link SubdivisionEdge} holds one end point of a full edge in a {@link Subdivision},
 * and a link to its {@link SubdivisionEdge#twin} holding the opposite end point. Together
 * these two half-edges for a complete {@link Subdivision} edge.
 * <p>
 * Every {@link SubdivisionEdge} is part of a cycle of half-edges that is connected by the
 * {@link SubdivisionEdge#next} and {@link SubdivisionEdge#previous} links. Assuming y-coordinates
 * increase upward, a clockwise cycle forms the inner boundary of the incident {@link SubdivisionFace},
 * and a counter-clockwise cycle forms its outer boundary. A {@link SubdivisionEdge} may form a cycle
 * with its own twin half-edge; such a zero-area cycle always forms an inner boundary.</p>
 * 
 * @author Christoph Nahr
 * @version 6.3.0
 */
public final class SubdivisionEdge {
    /**
     * The unique key of the {@link SubdivisionEdge} within its containing {@link Subdivision}.
     */
    int _key;
    /**
     * The {@link PointD} coordinates where the {@link SubdivisionEdge} begins.
     */
    PointD _origin;
    /**
     * The {@link SubdivisionFace} that is bounded by the {@link SubdivisionEdge}.
     */
    SubdivisionFace _face;
    /**
     * The next {@link SubdivisionEdge} that bounds the same {@link #face}.
     */
    SubdivisionEdge _next;
    /**
     * The previous {@link SubdivisionEdge} that bounds the same {@link #face}.
     */
    SubdivisionEdge _previous;
    /**
     * The {@link SubdivisionEdge} that is the twin of the current instance.
     */
    SubdivisionEdge _twin;

    /**
     * Creates a {@link SubdivisionEdge} with the specified unique key.
     * All other properties default to {@code null} and will have their final values
     * only when the containing {@link Subdivision} is fully constructed.
     * 
     * @param key the unique key of the {@link SubdivisionEdge} within its containing {@link Subdivision}
     */
    SubdivisionEdge(int key) {
        _key = key;
    }

    /**
     * Creates a {@link SubdivisionEdge} with the specified unique key, {@link PointD} origin,
     * incident {@link SubdivisionFace}, and twin, next, and previous {@link SubdivisionEdge}.
     * Intended for testing.
     * 
     * @param key the unique key of the {@link SubdivisionEdge} within its containing {@link Subdivision}
     * @param origin the {@link PointD} coordinates where the {@link SubdivisionEdge} begins
     * @param twin the {@link SubdivisionEdge} that is the twin of the current instance
     * @param face the {@link SubdivisionFace} that is bounded by the {@link SubdivisionEdge}
     * @param next the next {@link SubdivisionEdge} that bounds the same {@code face}
     * @param previous the previous {@link SubdivisionEdge} that bounds the same {@code face}
     */
    SubdivisionEdge(int key, PointD origin, SubdivisionEdge twin,
        SubdivisionFace face, SubdivisionEdge next, SubdivisionEdge previous) {

        _key = key;
        _origin = origin;
        _twin = twin;
        _face = face;
        _next = next;
        _previous = previous;
    }

    /**
     * Gets the unique key of the {@link SubdivisionEdge}.
     * Starts at zero for the first {@link SubdivisionEdge} in a {@link Subdivision},
     * and is incremented by one for each additional {@link SubdivisionEdge}. {@link #key}
     * thus reflects the order in which {@link SubdivisionEdge} instances were created.
     * Immutable unless {@link Subdivision#renumberFaces} is called on the {@link Subdivision}.
     * 
     * @return the unique key of the {@link SubdivisionEdge} within its containing {@link Subdivision}
     */
    public int key() {
        return _key;
    }

    /**
     * Gets the {@link PointD} coordinates where the {@link SubdivisionEdge} begins.
     * @return the {@link PointD} coordinates where the {@link SubdivisionEdge} begins
     */
    public PointD origin() {
        return _origin;
    }

    /**
     * Gets the {@link PointD} coordinates where the {@link SubdivisionEdge} ends.
     * @return the {@link #origin} of the {@link #twin} of the {@link SubdivisionEdge}
     * @throws NullPointerException if {@link #twin} has not yet been initialized
     */
    public PointD destination() {
        return _twin._origin;
    }

    /**
     * Gets the {@link SubdivisionFace} that is bounded by the {@link SubdivisionEdge}.
     * @return the {@link SubdivisionFace} that lies to the left of the {@link SubdivisionEdge},
     *         viewed from its {@link #origin} and assuming that y-coordinates increase upward
     */
    public SubdivisionFace face() {
        return _face;
    }

    /**
     * Gets the next {@link SubdivisionEdge} that bounds the same {@link #face}.
     * Returns the {@link #twin} of the current instance if no other {@link SubdivisionEdge}
     * begins at its {@link #destination}. Returns the nearest {@link SubdivisionEdge} in clockwise
     * direction, assuming y-coordinates increase upward, if multiple eligible instances exist.
     * 
     * @return the {@link SubdivisionEdge} that begins at the {@link #destination}
     *         of the current instance and bounds the same {@link #face}
     */
    public SubdivisionEdge next() {
        return _next;
    }

    /**
     * Gets the previous {@link SubdivisionEdge} that bounds the same {@link #face}.
     * Returns the {@link #twin} of the current instance if no other {@link SubdivisionEdge}
     * ends at its {@link #origin}. Returns the nearest {@link SubdivisionEdge} in counter-clockwise
     * direction, assuming y-coordinates increase upward, if multiple eligible instances exist.
     * 
     * @return the {@link SubdivisionEdge} that ends at the {@link #origin}
     *         of the current instance and bounds the same {@link #face}
     */
    public SubdivisionEdge previous() {
        return _previous;
    }

    /**
     * Gets the {@link SubdivisionEdge} that is the twin of the current instance.
     * A {@link SubdivisionEdge} and its {@link #twin} combine to form one full edge
     * in a {@link Subdivision}, corresponding to a single {@link LineD} instance.
     * 
     * @return the {@link SubdivisionEdge} that begins at the {@link #destination}
     *         and ends at the {@link #origin} of the current instance
     */
    public SubdivisionEdge twin() {
        return _twin;
    }

    /**
     * Gets the area of the polygon bounding the incident {@link #face}.
     * The absolute value of {@link #cycleArea} equals the area of {@link #cyclePolygon},
     * and is zero if all vertices are collinear or otherwise enclose no area. In that case,
     * {@link #cyclePolygon} forms an inner boundary of the incident {@link #face}.
     * <p>
     * The sign of a non-zero value indicates the orientation of its vertices: negative if the
     * vertices ordered clockwise, positive if they are ordered counter-clockwise, assuming
     * y-coordinates increase upward. Clockwise order indicates an inner boundary of the 
     * incident {@link #face}, and counter-clockwise order indicates the outer boundary.</p>
     * 
     * @return the area of {@link #cyclePolygon}, with a sign indicating its vertex orientation
     */
    public double cycleArea() {
        double area = 0;

        SubdivisionEdge edge = this;
        do {
            SubdivisionEdge next = edge._next;
            area += (edge._origin.x * next._origin.y - next._origin.x * edge._origin.y);
            edge = next;
        } while (edge != this);

        return area / 2.0;
    }

    /**
     * Gets the centroid of the polygon bounding the incident {@link #face}.
     * Undefined if {@link #cycleArea} is zero.
     * 
     * @return the centroid (center of gravity) of {@link #cyclePolygon}
     */
    public PointD cycleCentroid() {
        double area = 0, x = 0, y = 0;

        SubdivisionEdge edge = this;
        do {
            SubdivisionEdge next = edge._next;
            final double factor = edge._origin.x * next._origin.y - next._origin.x * edge._origin.y;

            area += factor;
            x += (edge._origin.x + next._origin.x) * factor;
            y += (edge._origin.y + next._origin.y) * factor;

            edge = next;
        } while (edge != this);

        area *= 3.0;
        return new PointD(x / area, y / area);
    }

    /**
     * Gets all {@link SubdivisionEdge} instances bounding the incident {@link #face}.
     * Begins with the current instance and follows the chain of {@link #next} links
     * until the sequence is complete, adding each encountered {@link SubdivisionEdge}.
     * 
     * @return a {@link List} of all {@link SubdivisionEdge} instances in the cycle
     *         that begins with this instance and continues along {@link #next} links
     */
    public List<SubdivisionEdge> cycleEdges() {
        final List<SubdivisionEdge> edges = new ArrayList<>();

        SubdivisionEdge edge = this;
        do {
            edges.add(edge);
            edge = edge._next;
        } while (edge != this);

        return edges;
    }

    /**
     * Gets all {@link PointD} vertices of the polygon bounding the incident {@link #face}.
     * Represents the <em>outer</em> boundary of the incident {@link #face} if the vertices
     * contain a positive area and are ordered counter-clockwise, assuming y-coordinates
     * increase upward. Otherwise, represents one of the <em>inner</em> boundaries of {@link #face}.
     * 
     * @return an array containing the {@link #origin} of all {@link SubdivisionEdge} instances
     *         in the cycle that begins with this instance and continues along {@link #next} links
     */
    public PointD[] cyclePolygon() {
        // count half-edges in cycle
        int index = 0;
        SubdivisionEdge edge = this;
        do {
            ++index;
            edge = edge._next;
        } while (edge != this);

        // copy cycle vertices to array
        final PointD[] points = new PointD[index];
        for (index = 0; index < points.length; index++) {
            points[index] = edge._origin;
            edge = edge._next;
        }

        return points;
    }

    /**
     * Determines whether the area of the polygon bounding the incident {@link #face} is zero.
     * Returns {@code true} exactly if the twins of all {@link SubdivisionEdge} instances in the
     * current cycle bound the same {@link #face} as the current instance. This implies a cycle
     * that comprises only complete {@link #twin} pairs. Such a cycle cannot enclose any area,
     * as that would require some twins bounding a different {@link #face}.
     * <p>
     * {@link #cycleArea} should equal zero if {@link #isCycleAreaZero} returns {@code true}, but
     * this may not be the case due to floating-point inaccuracies. {@link #isCycleAreaZero} is
     * both faster and more precise than {@link #cycleArea} if the actual area is not required.</p>
     * 
     * @return {@code true} if {@link #cyclePolygon} encloses no area, else {@code false}
     */
    public boolean isCycleAreaZero() {
        SubdivisionEdge edge = this;
        do {
            if (edge._twin._face != _face)
                return false;

            edge = edge._next;
        } while (edge != this);

        return true;
    }

    /**
     * Gets all {@link SubdivisionEdge} instances with the same {@link #origin}.
     * Begins with the current instance and follows the chain of {@link #twin} and {@link #next}
     * links until the sequence is complete, adding each encountered {@link SubdivisionEdge}.
     * 
     * @return a {@link List} of all {@link SubdivisionEdge} instances with the same {@link #origin}
     */
    public List<SubdivisionEdge> originEdges() {
        final List<SubdivisionEdge> edges = new ArrayList<>();

        SubdivisionEdge edge = this;
        do {
            edges.add(edge);
            edge = edge._twin._next;
        } while (edge != this);
        
        return edges;
    }

    /**
     * Finds the {@link SubdivisionEdge} with the same {@link #origin} and the
     * specified {@link #destination}, using exact coordinate comparisons.
     * This is an O(n) operation, where n is the number of {@link SubdivisionEdge}
     * instances originating from the current {@link #origin}.
     * 
     * @param destination the {@link #destination} of the {@link SubdivisionEdge}
     * @return the {@link SubdivisionEdge} with the same {@link #origin} as the current instance
     *         and the specified {@code destination}, or {@code null} if no match was found
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    public SubdivisionEdge findEdgeTo(PointD destination) {
        if (destination == null)
            throw new NullPointerException("destination");

        SubdivisionEdge edge = this;
        do {
            SubdivisionEdge twin = edge._twin;
            if (twin._origin.equals(destination))
                return edge;

            edge = twin._next;
        } while (edge != this);

        return null;
    }

    /**
     * Finds the {@link SubdivisionEdge} with the same {@link #origin} and the specified
     * {@link #destination}, given the specified epsilon for coordinate comparisons.
     * See {@link #findEdgeTo(PointD)} for details.
     * 
     * @param destination the {@link #destination} of the {@link SubdivisionEdge}
     * @param epsilon the maximum absolute difference at which coordinates are considered equal
     * @return the {@link SubdivisionEdge} with the same {@link #origin} as the current instance
     *         and the specified {@code destination}, or {@code null} if no match was found
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    public SubdivisionEdge findEdgeTo(PointD destination, double epsilon) {
        if (destination == null)
            throw new NullPointerException("destination");

        SubdivisionEdge edge = this;
        do {
            SubdivisionEdge twin = edge._twin;
            if (PointD.equals(twin._origin, destination, epsilon))
                return edge;

            edge = twin._next;
        } while (edge != this);

        return null;
    }

    /**
     * Finds the location of the specified {@link PointD} relative to the polygon
     * bounding the incident {@link #face}, using exact coordinate comparisons.
     * Performs a ray crossings algorithm with an asymptotic runtime of O(n).
     * This is equivalent to {@link GeoUtils#pointInPolygon} operating on the
     * {@link #origin} of all {@link SubdivisionEdge} instances in the cycle that
     * begins with this instances and continues along the chain of {@link #next} links.
     * 
     * @param q the {@link PointD} coordinates to locate
     * @return a {@link PolygonLocation} value indicating the location of {@code q}
     *         relative to {@link #cyclePolygon}
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    public PolygonLocation locate(PointD q) {
        if (q == null)
            throw new NullPointerException("q");

        // number of right & left crossings of edge & ray
        int rightCrossings = 0, leftCrossings = 0;

        // get starting point for first edge
        SubdivisionEdge edge = this;
        double x1 = edge._origin.x - q.x;
        double y1 = edge._origin.y - q.y;

        do {
            // get end point for current edge
            edge = edge._next;
            final double x0 = edge._origin.x - q.x;
            final double y0 = edge._origin.y - q.y;

            // check if q matches current vertex
            if (x0 == 0 && y0 == 0)
                return PolygonLocation.VERTEX;

            // check if current edge straddles x-axis
            final boolean rightStraddle = ((y0 > 0) != (y1 > 0));
            final boolean leftStraddle = ((y0 < 0) != (y1 < 0));

            // determine intersection of edge with x-axis
            if (rightStraddle || leftStraddle) {
                double x = (x0 * y1 - x1 * y0) / (y1 - y0);
                if (rightStraddle && x > 0) ++rightCrossings;
                if (leftStraddle && x < 0) ++leftCrossings;
            }

            // move starting point for next edge
            x1 = x0; y1 = y0;

        } while (edge != this);

        // q is on edge if crossings are of different parity
        if (rightCrossings % 2 != leftCrossings % 2)
            return PolygonLocation.EDGE;

        // q is inside for an odd number of crossings, else outside
        return (rightCrossings % 2 != 0 ?
            PolygonLocation.INSIDE : PolygonLocation.OUTSIDE);
    }

    /**
     * Finds the location of the specified {@link PointD} relative to the polygon bounding
     * the incident {@link #face},  given the specified epsilon for coordinate comparisons.
     * See {@link #locate(PointD)} for details.
     * 
     * @param q the {@link PointD} coordinates to locate
     * @param epsilon the maximum absolute difference at which coordinates should be considered equal
     * @return a {@link PolygonLocation} value indicating the location of {@code q}
     *         relative to {@link #cyclePolygon}
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    public PolygonLocation locate(PointD q, double epsilon) {
        if (q == null)
            throw new NullPointerException("q");
        if (epsilon <= 0)
            throw new IllegalArgumentException("epsilon <= 0");

        // number of right & left crossings of edge & ray
        int rightCrossings = 0, leftCrossings = 0;

        // get starting point for first edge
        SubdivisionEdge edge = this;
        double x1 = edge._origin.x - q.x;
        double y1 = edge._origin.y - q.y;
        int dy1 = MathUtils.compare(y1, 0, epsilon);

        do {
            // get end point for current edge
            edge = edge._next;
            final double x0 = edge._origin.x - q.x;
            final double y0 = edge._origin.y - q.y;

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
        } while (edge != this);

        // q is on edge if crossings are of different parity
        if (rightCrossings % 2 != leftCrossings % 2)
            return PolygonLocation.EDGE;

        // q is inside for an odd number of crossings, else outside
        return (rightCrossings % 2 != 0 ?
            PolygonLocation.INSIDE : PolygonLocation.OUTSIDE);
    }

    /**
     * Converts the {@link SubdivisionEdge} to a {@link LineD} with identical direction.
     * @return a {@link LineD} whose {@link LineD#start} is the {@link #origin} and whose
     *         {@link LineD#end} is the {@link #destination} of the {@link SubdivisionEdge}
     */
    public LineD toLine() {
        return new LineD(_origin, _twin._origin);
    }

    /**
     * Converts the {@link SubdivisionEdge} to a {@link LineD} with opposite direction.
     * @return a {@link LineD} whose {@link LineD#start} is the {@link #destination} and whose
     *         {@link LineD#end} is the {@link #origin} of the {@link SubdivisionEdge}
     */
    public LineD toLineReverse() {
        return new LineD(_twin._origin, _origin);
    }

    /**
     * Finds the insertion position of a new {@link SubdivisionEdge} to the specified
     * {@link #destination} within the current instance chain around {@link #origin}.
     * @param destination the {@link #destination} of the new {@link SubdivisionEdge}
     * @return an array of two {@link SubdivisionEdge} instances. Index zero holds the current instance
     *         that is {@link #next} from the {@link #twin} of the new instance. Index one holds
     *         the current instance whose {@link #twin} is {@link #previous} from the new instance.
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    SubdivisionEdge[] findEdgePosition(PointD destination) {
        SubdivisionEdge nextEdge = this;
        SubdivisionEdge previousEdge = this;

        // determine reference angle between new and existing edge
        PointD pivot = _twin._origin;
        final double firstAngle = _origin.angleBetween(destination, pivot);
        double angle;

        if (firstAngle > 0) {
            // positive angle: decrease until first negative angle found,
            // or until angle wraps around to greater than starting value
            do {
                previousEdge = nextEdge;
                nextEdge = nextEdge._twin._next;
                pivot = nextEdge._twin._origin;
                angle = _origin.angleBetween(destination, pivot);
            } while (angle > 0 && angle < firstAngle);
        } else {
            // negative angle: increase until first positive angle found,
            // or until angle wraps around to smaller than starting value
            do {
                nextEdge = previousEdge;
                previousEdge = previousEdge._previous._twin;
                pivot = previousEdge._twin._origin;
                angle = _origin.angleBetween(destination, pivot);
            } while (angle < 0 && angle > firstAngle);
        }

        return new SubdivisionEdge[] { nextEdge, previousEdge };
    }

    /**
     * Gets another {@link SubdivisionEdge} on the same boundary of the incident
     * {@link #face} that differs from the specified instance and its {@link #twin}.
     * Returns one of the following values:
     * <ul>
     * <li>{@link #next} if this instance equals {@code edge}, and {@link #next}
     * does not equal {@link #twin} of {@code edge};
     * or if this instance equals {@link #twin} of {@code edge}, and {@link #next}
     * does not equal {@code edge}.</li>
     * <li>{@link #previous} if this instance equals {@code edge}, and {@link #next}
     * equals {@link #twin} of {@code edge} but {@link #previous} does not;
     * or if this instance equals {@link #twin} of {@code edge}, and {@link #next}
     * equals {@code edge} but {@link #previous} does not.</li>
     * <li>{@code null} if this instance equals {@code edge}, and {@link #next}
     * and {@link #previous} both equal {@link #twin} of {@code edge};
     * or if this instance equals {@link #twin} of {@code edge}, and {@link #next}
     * and {@link #previous} both equal {@code edge}.</li>
     * <li>this instance if it equals neither {@code edge} nor its {@link #twin}.</li>
     * </ul>
     * 
     * @param edge the {@link SubdivisionEdge} to avoid
     * @return another {@link SubdivisionEdge} or {@code null}, as described above
     * @throws NullPointerException if {@code edge} is {@code null}
     */
    SubdivisionEdge getOtherCycleEdge(SubdivisionEdge edge) {
        if (edge == null)
            throw new NullPointerException("edge");

        if (this == edge) {
            if (_next != _twin) return _next;
            if (_previous != _twin) return _previous;
            return null;
        }

        if (this == edge._twin) {
            if (_next != edge) return _next;
            if (_previous != edge) return _previous;
            return null;
        }

        return this;
    }

    /**
     * Determines whether the specified {@link #destination} is compatible
     * with the present vertex chain around {@link #origin}.
     * Always returns {@code true} if the {@link SubdivisionEdge} is the only incident instance
     * at its {@link #origin}, or if the specified {@code destination} equals the current one.
     * <p>
     * Otherwise, returns {@code true} exactly if rotating the {@link SubdivisionEdge} to
     * {@code destination}, in the direction that minimizes angular distance, would not
     * traverse any neighboring instance in the present vertex chain around {@link #origin}.</p>
     * 
     * @param destination the {@link PointD} coordinates of the new {@link #destination}
     * @return {@code true} if {@code destination} is compatible with the present
     *         vertex chain around {@link #origin}, else {@code false}
     * @throws NullPointerException if {@code destination} is {@code false}
     */
    boolean isCompatibleDestination(PointD destination) {
        if (destination == null)
            throw new NullPointerException("destination");

        // succeed if only one edge or same destination
        if (_next == _twin) return true;
        final PointD pivot = _twin._origin;
        if (pivot.equals(destination))
            return true;

        // compute angles to destination and previous edge
        final double pivotAngle = _origin.angleBetween(pivot, destination);
        double prevAngle = _origin.angleBetween(pivot, _previous._origin);

        // compute angle to next edge, if different
        final SubdivisionEdge next = _twin._next._twin;
        double nextAngle = (_previous == next ? prevAngle :
                _origin.angleBetween(pivot, next._origin));

        // adjust signs of neighboring angles
        if (prevAngle > 0) prevAngle -= 2 * Math.PI;
        if (nextAngle < 0) nextAngle += 2 * Math.PI;

        if (pivotAngle < 0) {
            if (prevAngle < 0)
                return (pivotAngle > prevAngle);
            else {
                assert(nextAngle < 0);
                return (pivotAngle > nextAngle);
            }
        } else {
            if (prevAngle > 0)
                return (pivotAngle < prevAngle);
            else {
                assert(nextAngle > 0);
                return (pivotAngle < nextAngle);
            }
        }
    }

    /**
     * Sets the {@link #face} of the {@link SubdivisionEdge} and all other instances
     * in the same cycle to the specified {@link SubdivisionFace}.
     * @param face the new {@link #face} for all {@link SubdivisionEdge} instances in the cycle
     * @throws NullPointerException if {@code face} is {@code null}
     */
    void setAllFaces(SubdivisionFace face) {
        if (face == null)
            throw new NullPointerException("face");

        SubdivisionEdge edge = this;
        do {
            edge._face = face;
            edge = edge._next;
        } while (edge != this);
    }

    /**
     * Compares the specified {@link Object} to this {@link SubdivisionEdge} instance.
     * Compares only the keys of {@link #twin}, {@link #face}, {@link #next}, and {@link #previous}
     * to avoid infinite recursions in half-edge cycles. Intended for testing, as any two
     * {@link SubdivisionEdge} instances in the same {@link Subdivision} are never equal.
     * 
     * @param obj the {@link Object} to compare to this instance
     * @return {@code true} if {@code obj} is not {@code null} and a {@link SubdivisionEdge}
     *         instance whose {@link #key}, {@link #origin}, and linked keys equal those
     *         of this instance, else {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof SubdivisionEdge))
            return false;

        final SubdivisionEdge edge = (SubdivisionEdge) obj;
        return (_key == edge._key &&
                Objects.equals(_origin, edge._origin) &&
                _twin._key == edge._twin._key &&
                _face._key == edge._face._key &&
                _next._key == edge._next._key &&
                _previous._key == edge._previous._key);
    }

    /**
     * Returns a hash code for the {@link SubdivisionEdge}.
     * Returns {@link #key} which is guaranteed to be unique within the containing {@link Subdivision}.
     * 
     * @return an {@link Integer} hash code for the {@link SubdivisionEdge}
     */
    @Override
    public int hashCode() {
        return _key;
    }

    /**
     * Returns a {@link String} representation of the {@link SubdivisionEdge}.
     * Objects that are {@code null} are represented with a key value of -1.
     * 
     * @return a {@link String} containing the values of {@link #key} and {@link #origin}, and
     *         the keys of {@link #twin}, {@link #face}, {@link #next}, and {@link #previous}
     */
    @Override
    public String toString() {
        final ToIntFunction<SubdivisionEdge> edgeKey = (e -> e == null ? -1 : e._key);
        final ToIntFunction<SubdivisionFace> faceKey = (e -> e == null ? -1 : e._key);

        return String.format(
                "SubdivisionEdge[key=%d, origin=%s, twin=%d, face=%d, next=%d, previous=%d]",
                _key, String.valueOf(_origin),
                edgeKey.applyAsInt(_twin), faceKey.applyAsInt(_face),
                edgeKey.applyAsInt(_next), edgeKey.applyAsInt(_previous));
    }
}
