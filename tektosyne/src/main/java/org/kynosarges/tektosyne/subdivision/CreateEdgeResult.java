package org.kynosarges.tektosyne.subdivision;

/**
 * Contains the result of optionally creating an edge in a {@link Subdivision}.
 * Immutable class containing intermediate results for internal edge creation in
 * the {@link Subdivision} class. See {@link AddEdgeResult} for public results
 * which represent a full update of the {@link Subdivision}.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
final class CreateEdgeResult {
    /**
     * The {@link SubdivisionEdge} from the specified start to end coordinates.
     * Never {@code null}. {@link #isEdgeCreated} indicates whether this
     * {@link SubdivisionEdge} and its {@link SubdivisionEdge#twin} were
     * newly created or already present in the {@link Subdivision}.
     */
    final SubdivisionEdge startEdge;

    /**
     * Indicates whether an edge was newly created.
     * {@code true} if {@link #startEdge} and its {@link SubdivisionEdge#twin}
     * were not found in the {@link Subdivision} and therefore created,
     * {@code false} if they were already present.
     */
    final boolean isEdgeCreated;

    /**
     * Creates a {@link CreateEdgeResult} with the specified {@link SubdivisionEdge} and creation flag.
     * @param startEdge the {@link SubdivisionEdge} from the specified start to end coordinates
     * @param isEdgeCreated {@code true} if {@link #startEdge} and its {@link SubdivisionEdge#twin}
     *                      were newly created, else {@code false}
     * @throws NullPointerException if {@code startEdge} is {@code null}
     */
    CreateEdgeResult(SubdivisionEdge startEdge, boolean isEdgeCreated) {
        if (startEdge == null)
            throw new NullPointerException("startEdge");

        this.startEdge = startEdge;
        this.isEdgeCreated = isEdgeCreated;
    }
}
