package org.kynosarges.tektosyne.subdivision;

import java.util.*;

/**
 * Contains the result of splitting a {@link SubdivisionEdge} into two parts.
 * Immutable class containing two or three {@link SubdivisionEdge} instances that describe the
 * result of splitting another instance  into two parts. If {@link SplitEdgeResult#createdEdge}
 * is valid then {@link SplitEdgeResult#isEdgeDeleted} is {@code false}, and vice versa.
 * 
 * @author Christoph Nahr
 * @version 6.2.0
 */
final class SplitEdgeResult {
    /**
     * The {@link SubdivisionEdge} with the same {@link SubdivisionEdge#origin} as the split instance.
     */
    final SubdivisionEdge originEdge;

    /**
     * The {@link SubdivisionEdge} with the same {@link SubdivisionEdge#destination} as the split instance.
     */
    final SubdivisionEdge destinationEdge;

    /**
     * The {@link SubdivisionEdge} that was newly created for one of the two parts
     * resulting from the split, if any.
     * Set to {@code null} if no {@link SubdivisionEdge} was created.
     */
    final SubdivisionEdge createdEdge;

    /**
     * Indicates whether the {@link SubdivisionEdge} to be split has been deleted
     * because both parts were duplicated by existing instances.
     */
    final boolean isEdgeDeleted;

    /**
     * Creates a {@link SplitEdgeResult} with the specified {@link SubdivisionEdge} instances.
     * @param originEdge the {@link SubdivisionEdge} with the same
     *                   {@link SubdivisionEdge#origin} as the split instance
     * @param destinationEdge the {@link SubdivisionEdge} with the same
     *                   {@link SubdivisionEdge#destination} as the split instance
     * @param createdEdge the {@link SubdivisionEdge} that was newly created for one of the
     *                   two parts resulting from the split, or {@code null} if none was created
     * @param isEdgeDeleted {@code true} if the {@link SubdivisionEdge} to be split has been deleted
     *                   because both parts were duplicated by existing instances, else {@code false}
     * @throws NullPointerException if {@code originEdge} or {@code destinationEdge} is {@code null}
     */
    SplitEdgeResult(SubdivisionEdge originEdge, SubdivisionEdge destinationEdge,
            SubdivisionEdge createdEdge, boolean isEdgeDeleted) {

        if (originEdge == null)
            throw new NullPointerException("originEdge");
        if (destinationEdge == null)
            throw new NullPointerException("destinationEdge");

        this.originEdge = originEdge;
        this.destinationEdge = destinationEdge;
        this.createdEdge = createdEdge;
        this.isEdgeDeleted = isEdgeDeleted;
    }

    /**
     * Updates the {@link SubdivisionFace} keys in the specified maps after the
     * specified {@link SubdivisionEdge} has been split.
     * Ensures that the partial mapping between original and intersected faces established
     * by {@link Subdivision#intersection} is kept up-to-date when edge splitting results in
     * a valid {@link #createdEdge}. Does nothing if {@link #createdEdge} is {@code null}.
     * 
     * @param edge the {@link SubdivisionEdge} whose splitting resulted in the {@link SplitEdgeResult}
     * @param edgeToFace1 maps the key of any existing {@link SubdivisionEdge} to the key of each
     *             incident {@link SubdivisionFace} in a first {@link Subdivision}
     * @param edgeToFace2 maps the key of any existing {@link SubdivisionEdge} to the key of each
     *             incident {@link SubdivisionFace} in a second {@link Subdivision}
     * @throws NullPointerException if {@code edgeToFace1} or {@code edgeToFace2} is {@code null}
     */
    void updateFaces(SubdivisionEdge edge,
            Map<Integer, Integer> edgeToFace1, Map<Integer, Integer> edgeToFace2) {

        if (createdEdge == null) return;

        Integer face = edgeToFace1.get(edge._key);
        if (face != null)
            edgeToFace1.put(createdEdge._key, face);

        face = edgeToFace2.get(edge._key);
        if (face != null)
            edgeToFace2.put(createdEdge._key, face);

        /*
         * As per Subdivision.splitEdgeAtVertex, the existing twin of the existing edge
         * was relinked to createdEdge whereas the existing edge receives the new twin.
         * So we must reverse existing and created edge for the twin mapping updates.
         */
        face = edgeToFace1.get(createdEdge._twin._key);
        if (face != null)
            edgeToFace1.put(edge._twin._key, face);

        face = edgeToFace2.get(createdEdge._twin._key);
        if (face != null)
            edgeToFace2.put(edge._twin._key, face);
    }
}
