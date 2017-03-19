package org.kynosarges.tektosyne.subdivision;

/**
 * Specifies the possible results of the {@link SubdivisionFace#moveEdge} 
 * method of the {@link SubdivisionFace} class.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
enum MoveEdgeResult {
    /**
     * Neither the half-edge nor its twin equals {@link SubdivisionFace#outerEdge}
     * or any {@link SubdivisionFace#innerEdges} element; no properties were changed.
     */
    UNCHANGED,

    /**
     * The half-edge or its twin equals {@link SubdivisionFace#outerEdge},
     * and that property was changed to another {@link SubdivisionEdge}.
     */
    OUTER_CHANGED,

    /**
     * The half-edge or its twin equals an {@link SubdivisionFace#innerEdges} element,
     * and that element was changed to another {@link SubdivisionEdge}.
     */
    INNER_CHANGED,

    /**
     * The half-edge or its twin equals an {@link SubdivisionFace#innerEdges} element,
     * and that element was removed since its cycle contains no other half-edges.
     */
    INNER_REMOVED
}
