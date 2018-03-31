package org.kynosarges.tektosyne.subdivision;

/**
 * Maps the faces of a planar {@link Subdivision} to arbitrary objects.
 * Provides an application-specific mapping of all {@link Subdivision#faces}
 * of a planar {@link Subdivision} to arbitrary objects. The use of an interface
 * allows clients to implement the most efficient mapping for their concrete
 * {@link Subdivision} structure and object type.
 * <p>
 * Since the {@link Subdivision} has no knowledge of any {@link SubdivisionMap}
 * instances that reference it, clients must manually update any such instances
 * whenever the underlying {@link Subdivision} changes.</p>
 * 
 * @param <T> the type of all objects mapped to {@link Subdivision#faces}
 * @author Christoph Nahr
 * @version 6.2.0
 */
public interface SubdivisionMap<T> {
    /**
     * Gets the {@link Subdivision} that contains all mapped {@link Subdivision#faces}.
     * Never {@code null}. Multiple {@link SubdivisionMap} implementations may refer to the
     * same {@link #source}, mapping its {@link Subdivision#faces} to different objects.
     * 
     * @return the {@link Subdivision} that contains all {@link Subdivision#faces} accepted
     *         and returned by {@link #fromFace} and {@link #toFace}, respectively
     */
    Subdivision source();

    /**
     * Gets the {@link Object} that defines all mapped <b>T</b> instances.
     * May be {@code null} if providing a container for all associated <b>T</b> instances
     * is impossible or inconvenient.
     * 
     * @return the {@link Object} that defines all <b>T</b> instances returned and accepted
     *         by {@link #fromFace} and {@link #toFace}, respectively
     */
    Object target();

    /**
     * Converts the specified {@link SubdivisionFace} into the associated <b>T</b> instance.
     * @param face the {@link SubdivisionFace} to convert
     * @return the <b>T</b> instance associated with {@code face}
     * @throws IllegalArgumentException if {@link #source} does not contain {@code face}
     * @throws UnsupportedOperationException if not overridden by an implementing class
     * @throws NullPointerException if {@code face} is {@code null}
     */
    default T fromFace(SubdivisionFace face) {
        throw new UnsupportedOperationException("fromFace not implemented");
    }

    /**
     * Converts the specified <b>T</b> instance into the associated {@link SubdivisionFace}.
     * @param value the <b>T</b> instance to convert
     * @return the {@link SubdivisionFace} associated with {@code value}
     * @throws IllegalArgumentException if {@code value} does not map to any
     *                                  {@link SubdivisionFace} within {@link #source}
     * @throws UnsupportedOperationException if not overridden by an implementing class
     */
    default SubdivisionFace toFace(T value) {
        throw new UnsupportedOperationException("toFace not implemented");
    }
}
