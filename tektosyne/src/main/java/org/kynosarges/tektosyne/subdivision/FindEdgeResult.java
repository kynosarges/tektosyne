package org.kynosarges.tektosyne.subdivision;

/**
 * Contains the result of finding the {@link SubdivisionEdge} nearest to a query point.
 * Immutable class containing the results of two equivalent methods,
 * {@link Subdivision#findNearestEdge} of the {@link Subdivision} class and
 * {@link SubdivisionFace#findNearestEdge} of the {@link SubdivisionFace} class.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class FindEdgeResult {
    /**
     * The found {@link SubdivisionEdge}, if any.
     * The {@link SubdivisionEdge} on any boundary of the {@link SubdivisionFace} nearest to and
     * facing the query point, or {@code null} if the {@link SubdivisionFace} is completely unbounded.
     */
    public final SubdivisionEdge edge;

    /**
     * The distance to the found {@link SubdivisionEdge}, if any.
     * The distance between the query point and {@code edge}, if valid, else {@link Double#MAX_VALUE}.
     */
    public final double distance;

    /**
     * Creates a {@link FindEdgeResult} with the specified {@link SubdivisionEdge} and distance.
     * @param edge the found {@link SubdivisionEdge}, if any, else {@code null}
     * @param distance the distance to {@code edge}, if valid, else {@link Double#MAX_VALUE}
     */
    FindEdgeResult(SubdivisionEdge edge, double distance) {
        this.edge = edge;
        this.distance = distance;
    }
}
