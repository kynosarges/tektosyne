package org.kynosarges.tektosyne.geometry;

/**
 * Represents one edge in the Voronoi diagram found by the {@link Voronoi} algorithm.
 * Immutable class holding one element in the {@link VoronoiResults#voronoiEdges}
 * collection of the {@link VoronoiResults} class. Contains the indices of the two
 * {@link VoronoiResults#voronoiVertices} that terminate the edge, as well as the indices
 * of the two {@link VoronoiResults#generatorSites} that are bisected by the edge.
 * This allows constructing the Voronoi region corresponding to each generator site.
 * 
 * @author Christoph Nahr
 * @version 6.3.0
 */
public final class VoronoiEdge {
    /**
     * The index of the first of the two {@link VoronoiResults#generatorSites}
     * that are bisected by the {@link VoronoiEdge}.
     */
    public final int site1;

    /**
     * The index of the second of the two {@link VoronoiResults#generatorSites}
     * that are bisected by the {@link VoronoiEdge}.
     */
    public final int site2;

    /**
     * The index of the first of the two {@link VoronoiResults#voronoiVertices}
     * that are connected by the {@link VoronoiEdge}.
     */
    public final int vertex1;

    /**
     * The index of the second of the two {@link VoronoiResults#voronoiVertices}
     * that are connected by the {@link VoronoiEdge}.
     */
    public final int vertex2;

    /**
     * Creates a {@link VoronoiEdge} with the specified index pairs of
     * {@link VoronoiResults#generatorSites} and {@link VoronoiResults#voronoiVertices}.
     * 
     * @param site1 the index of the first of the two {@link VoronoiResults#generatorSites}
     *              that are bisected by the {@link VoronoiEdge}
     * @param site2 the index of the second of the two {@link VoronoiResults#generatorSites}
     *              that are bisected by the {@link VoronoiEdge}
     * @param vertex1 the index of the first of the two {@link VoronoiResults#voronoiVertices}
     *                that are connected by the {@link VoronoiEdge}
     * @param vertex2 the index of the second of the two {@link VoronoiResults#voronoiVertices}
     *                that are connected by the {@link VoronoiEdge}
     * @throws IllegalArgumentException if any argument is less than zero
     */
    VoronoiEdge(int site1, int site2, int vertex1, int vertex2) {
        if (site1 < 0)
            throw new IllegalArgumentException("site1 < 0");
        if (site2 < 0)
            throw new IllegalArgumentException("site2 < 0");
        if (vertex1 < 0)
            throw new IllegalArgumentException("vertex1 < 0");
        if (vertex2 < 0)
            throw new IllegalArgumentException("vertex2 < 0");

        this.site1 = site1;
        this.site2 = site2;
        this.vertex1 = vertex1;
        this.vertex2 = vertex2;
    }

    /**
     * Compares the specified {@link Object} to this {@link VoronoiEdge} instance.
     * @param obj the {@link Object} to compare to this instance
     * @return {@code true} if {@code obj} is not {@code null} and a {@link VoronoiEdge} instance
     *         whose {@link #site1}, {@link #site2}, {@link #vertex1}, and {@link #vertex2} values
     *         equal those of this instance, else {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof VoronoiEdge))
            return false;

        final VoronoiEdge edge = (VoronoiEdge) obj;
        return (site1 == edge.site1 && site2 == edge.site2 &&
                vertex1 == edge.vertex1 && vertex2 == edge.vertex2);
    }

    /**
     * Returns a hash code for the {@link VoronoiEdge}.
     * @return an {@link Integer} hash code for the {@link VoronoiEdge}
     */
    @Override
    public int hashCode() {
        return (31 * (31 * (31 * site1 + site2) + vertex1) + vertex2);
    }

    /**
     * Returns a {@link String} representation of the {@link VoronoiEdge}.
     * @return a {@link String} containing the values of {@link #site1},
     *         {@link #site2}, {@link #vertex1}, and {@link #vertex2}
     */
    @Override
    public String toString() {
        return String.format(
                "VoronoiEdge[site1=%d, site2=%d, vertex1=%d, vertex2=%d]",
                site1, site2, vertex1, vertex2);
    }
}
