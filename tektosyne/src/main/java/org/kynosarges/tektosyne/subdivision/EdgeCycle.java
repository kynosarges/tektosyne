package org.kynosarges.tektosyne.subdivision;

/**
 * Represents a cycle of half-edges that constitutes a {@link SubdivisionFace}. 
 * There is one {@link EdgeCycle} for each chain of {@link SubdivisionEdge#next} links that forms
 * the inner or outer boundary of a single {@link SubdivisionFace}. Multiple {@link EdgeCycle}
 * instances may also be linked to indicate one or more “holes” within the same outer boundary.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
final class EdgeCycle {
    /**
     * Creates an {@link EdgeCycle} with the specified incident {@link SubdivisionEdge}.
     * @param edge a {@link SubdivisionEdge} that is part of the {@link EdgeCycle}
     * @throws NullPointerException if {@code edge} is {@code null}
     */
    EdgeCycle(SubdivisionEdge edge) {
        if (edge == null)
            throw new NullPointerException("edge");

        firstEdge = edge;
    }

    /**
     * A {@link SubdivisionEdge} that is part of the {@link EdgeCycle}.
     * Follow the chain of {@link SubdivisionEdge#next} links starting with {@link #firstEdge}
     * to visit all other {@link SubdivisionEdge} instances in the {@link EdgeCycle}.
     */
    SubdivisionEdge firstEdge;

    /**
     * Another {@link EdgeCycle} that is an inner cycle, and either contained within
     * or neighboring the current instance.
     * Either {@code null} or an inner cycle. If the current instance is an outer cycle,
     * {@link #next} is a “hole” contained within that outer cycle. Otherwise, the current
     * instance and {@link #next} are both neighboring “holes,” either within the same outer
     * cycle that begins the chain of {@link #next} links, or else within the unbounded face.
     */
    EdgeCycle next;

    /**
     * Adds the {@link EdgeCycle} data to the specified {@link SubdivisionFace}.
     * Updates the {@link SubdivisionEdge} link of the specified {@code face} indicated
     * by {@code isOuter}, and the {@link SubdivisionFace} links of all half-edges
     * in the chain that starts with {@link #firstEdge}.
     * 
     * @param face the {@link SubdivisionFace} that receives the {@link EdgeCycle} data
     * @param isOuter {@code true} if the {@link EdgeCycle} represents an outer boundary,
     *                {@code false} if it represents an inner boundary
     * @throws NullPointerException if {@code face} is {@code null}
     */
    void addToFace(SubdivisionFace face, boolean isOuter) {

        // set edge pointer of face
        if (isOuter) {
            assert(face._outerEdge == null);
            face._outerEdge = firstEdge;
        } else
            face.addInnerEdge(firstEdge);

        // set face pointers of all incident edges
        SubdivisionEdge edge = firstEdge;
        do {
            assert(edge._face == null);
            edge._face = face;
            edge = edge._next;
        } while (edge != firstEdge);
    }
}
