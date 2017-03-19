package org.kynosarges.tektosyne.geometry;

/**
 * Specifies the location of a point relative to an arbitrary polygon.
 * Specifies the possible return values of the {@link GeoUtils#pointInPolygon} algorithm.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public enum PolygonLocation {
    /** Specifies that the point is inside the polygon. */
    INSIDE,
    /** Specifies that the point is outside the polygon. */
    OUTSIDE,
    /** Specifies that the point coincides with an edge of the polygon. */
    EDGE,
    /** Specifies that the point coincides with a vertex of the polygon. */
    VERTEX
}
