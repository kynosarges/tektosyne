package org.kynosarges.tektosyne.geometry;

/**
 * Specifies the spatial relationship between two line segments.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public enum LineRelation {
    /**
     * Specifies that the two line segments are parallel displacements of each other, and
     * therefore cannot share any points.
     */
    PARALLEL,

    /**
     * Specifies that the two line segments are part of the same infinite line, and therefore
     * may share some or all their points.
     */
    COLLINEAR,

    /**
     * Specifies that the two line segments have different angles, and therefore may share a
     * single point of intersection.
     */
    DIVERGENT
}
