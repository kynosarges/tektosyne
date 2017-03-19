package org.kynosarges.tektosyne.subdivision;

/**
 * Contains the result of an intersection between two {@link Subdivision} instances.
 * Immutable class containing the result of the {@link Subdivision#intersection} method
 * of the {@link Subdivision} class.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class SubdivisionIntersection {
    /**
     * The output {@link Subdivision} that is the intersection of both input instances.
     */
    public final Subdivision division;

    /**
     * Maps {@link #division} face keys to those of the first input {@link Subdivision}.
     * Always equal in size to the {@link Subdivision#faces} collection of {@link #division}.
     * Holds the original {@link SubdivisionFace} key of the first input {@link Subdivision}
     * at each index that equals the new key in the output {@link #division}.
     */
    public final int[] faceKeys1;

    /**
     * Maps {@link #division} face keys to those of the second input {@link Subdivision}.
     * Always equal in size to the {@link Subdivision#faces} collection of {@link #division}.
     * Holds the original {@link SubdivisionFace} key of the second input {@link Subdivision}
     * at each index that equals the new key in the output {@link #division}.
     */
    public final int[] faceKeys2;

    /**
     * Creates a {@link SubdivisionIntersection} with the specified output {@link Subdivision}
     * and mappings to the {@link SubdivisionFace} keys of both input instances.
     * @param division the {@link Subdivision} that is the intersection of both input instances
     * @param faceKeys1 maps {@code division} face keys to those of the first input {@link Subdivision}
     * @param faceKeys2 maps {@code division} face keys to those of the second input {@link Subdivision}
     * @throws IllegalArgumentException if {@code faceKeys1} or {@code faceKeys2} are not both
     *         equal in size to the {@link Subdivision#faces} collection of {@code division}
     * @throws NullPointerException if {@code division}, {@code faceKeys1}, or {@code faceKeys2} is {@code null}
     */
    SubdivisionIntersection(Subdivision division, int[] faceKeys1, int[] faceKeys2) {

        // implicit null checks for all arguments
        if (faceKeys1.length != division.faces().size())
            throw new IllegalArgumentException("faceKeys1.length != division.faces.size");
        if (faceKeys2.length != division.faces().size())
            throw new IllegalArgumentException("faceKeys2.length != division.faces.size");

        this.division = division;
        this.faceKeys1 = faceKeys1;
        this.faceKeys2 = faceKeys2;
    }
}
