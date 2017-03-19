package org.kynosarges.tektosyne.subdivision;

/**
 * Contains the result of adding an edge to a {@link Subdivision}.
 * Immutable class containing the result of the {@link Subdivision#addEdge} method
 * of the {@link Subdivision} class.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class AddEdgeResult {
    /**
     * One of the two added {@link SubdivisionEdge} instances, if any.
     * Set to a valid instance if {@link Subdivision#addEdge} succeeded, else to {@code null}.
     * A valid {@link SubdivisionEdge} is directed from the specified start to end coordinates.
     */
    public final SubdivisionEdge addedEdge;

    /**
     * The {@link SubdivisionFace#key} of the added {@link SubdivisionFace}, if any.
     * Set to a non-negative value if {@link Subdivision#addEdge} succeeded and a
     * {@link SubdivisionFace} was added along with {@link #addedEdge}, else to a negative value.
     */
    public final int addedFaceKey;

    /**
     * The {@link SubdivisionFace#key} of the changed {@link SubdivisionFace}, if any.
     * Set to a non-negative value if {@link Subdivision#addEdge} succeeded, else to a negative value.
     */
    public final int changedFaceKey;

    /**
     * Creates an {@link AddEdgeResult} with one specified added {@link SubdivisionEdge}
     * and added and changed {@link SubdivisionFace} keys.
     * Both {@code addedEdge} may be {@code null} and {@code changedFaceKey} may be negative
     * to indicate that no {@link SubdivisionEdge} was added. {@code addedFaceKey} must
     * also be negative in that case, or it may otherwise be negative to indicate that a
     * {@link SubdivisionEdge} was added but no {@link SubdivisionFace}.
     * 
     * @param addedEdge one of the two added {@link SubdivisionEdge} instances, if any
     * @param changedFaceKey the {@link SubdivisionFace#key} of the changed {@link SubdivisionFace}, if any
     * @param addedFaceKey the {@link SubdivisionFace#key} of the added {@link SubdivisionFace}, if any
     * @throws IllegalArgumentException if {@code addedEdge} is {@code null} and either
     *         {@code addedFaceKey} or {@code changedFaceKey} is non-negative, or if
     *         {@code addedEdge} is not {@code null} and {@code changedFaceKey} is negative
     */
    AddEdgeResult(SubdivisionEdge addedEdge, int changedFaceKey, int addedFaceKey) {
        if (addedEdge == null) {
            if (changedFaceKey >= 0)
                throw new IllegalArgumentException("addedEdge == null && changedFaceKey >= 0");
            if (addedFaceKey >= 0)
                throw new IllegalArgumentException("addedEdge == null && addedFaceKey >= 0");
        } else if (changedFaceKey < 0)
                throw new IllegalArgumentException("addedEdge != null && changedFaceKey < 0");

        this.addedEdge = addedEdge;
        this.changedFaceKey = changedFaceKey;
        this.addedFaceKey = addedFaceKey;
    }

    /**
     * Indicates whether an edge was added to the {@link Subdivision}.
     * @return {@code true} if {@link #changedFaceKey} is equal
     *         to or greater than zero, else {@code false}.
     */
    public boolean isEdgeAdded() {
        return (changedFaceKey >= 0);
    }
}
