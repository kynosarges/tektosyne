package org.kynosarges.tektosyne.subdivision;

/**
 * Contains the result of removing an edge from a {@link Subdivision}.
 * Immutable class containing the result of the {@link Subdivision#removeEdge} method
 * of the {@link Subdivision} class.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class RemoveEdgeResult {
    /**
     * The {@link SubdivisionFace#key} of the changed {@link SubdivisionFace}, if any.
     * Set to a non-negative value if {@link Subdivision#removeEdge} succeeded,
     * else to a negative value.
     */
    public final int changedFaceKey;

    /**
     * The {@link SubdivisionFace#key} of the removed {@link SubdivisionFace}, if any.
     * Set to a non-negative value exactly if {@link Subdivision#removeEdge} succeeded,
     * and a {@link SubdivisionFace} was removed along with the edge.
     */
    public final int removedFaceKey;

    /**
     * Creates a {@link RemoveEdgeResult} with the specified changed and removed {@link SubdivisionFace} keys.
     * {@code changedFaceKey} may be negative to indicate that no edge was removed.
     * {@code removedFaceKey} must also be negative in that case, or it may otherwise
     * be negative to indicate that an edge was removed but no {@link SubdivisionFace}.
     * 
     * @param changedFaceKey the {@link SubdivisionFace#key} of the changed {@link SubdivisionFace}, if any
     * @param removedFaceKey the {@link SubdivisionFace#key} of the removed {@link SubdivisionFace}, if any
     * @throws IllegalArgumentException if {@code changedFaceKey} is negative
     *                                  and {@code removedFaceKey} is non-negative
     */
    RemoveEdgeResult(int changedFaceKey, int removedFaceKey) {
        if (changedFaceKey < 0 && removedFaceKey >= 0)
            throw new IllegalArgumentException("changedFaceKey < 0 && removedFaceKey >= 0");

        this.changedFaceKey = changedFaceKey;
        this.removedFaceKey = removedFaceKey;
    }

    /**
     * Indicates whether an edge was removed from the {@link Subdivision}.
     * @return {@code true} if {@link #changedFaceKey} is equal
     *         to or greater than zero, else {@code false}.
     */
    public boolean isEdgeRemoved() {
        return (changedFaceKey >= 0);
    }
}
