package org.kynosarges.tektosyne.geometry;

/**
 * Specifies the possible orientations of a {@link RegularPolygon}.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public enum PolygonOrientation {
    /** Specifies that the {@link RegularPolygon} is lying on an edge. */
    ON_EDGE,
    /** Specifies that the {@link RegularPolygon} is standing on a vertex. */
    ON_VERTEX
}
