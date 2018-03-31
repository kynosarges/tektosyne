package org.kynosarges.tektosyne.subdivision;

import org.kynosarges.tektosyne.geometry.PointD;

/**
 * Maps all elements of a planar {@link Subdivision} to arbitrary objects.
 * Provides an application-specific mapping of all {@link Subdivision#faces},
 * {@link Subdivision#edges}, and {@link Subdivision#vertices} of a planar
 * {@link Subdivision} to arbitrary objects.
 * <p>
 * All conversion methods have a default implementation that throws
 * {@link UnsupportedOperationException} so that clients may selectively
 * implement actual conversion methods for any desired element types.</p>
 * <p>
 * Since the {@link Subdivision} has no knowledge of any {@link SubdivisionFullMap}
 * instances that reference it, clients must manually update any such instances
 * whenever the underlying {@link Subdivision} changes.</p>
 *
 * @param <T> the type of all objects mapped to {@link Subdivision#faces}
 * @param <U> the type of all objects mapped to {@link Subdivision#edges}
 * @param <V> the type of all objects mapped to {@link Subdivision#vertices}
 * @author Christoph Nahr
 * @version 6.2.0
 */
public interface SubdivisionFullMap<T, U, V> extends SubdivisionMap<T> {
    /**
     * Converts the specified {@link SubdivisionEdge} into the associated <b>U</b> instance.
     * @param edge the {@link SubdivisionEdge} to convert
     * @return the <b>U</b> instance associated with {@code edge}
     * @throws IllegalArgumentException if {@link SubdivisionMap#source} does not contain {@code edge}
     * @throws UnsupportedOperationException if not overridden by an implementing class
     * @throws NullPointerException if {@code edge} is {@code null}
     */
    default U fromEdge(SubdivisionEdge edge) {
        throw new UnsupportedOperationException("fromEdge not implemented");
    }

    /**
     * Converts the specified <b>U</b> instance into the associated {@link SubdivisionEdge}.
     * @param value the <b>U</b> instance to convert
     * @return the {@link SubdivisionEdge} associated with {@code value}
     * @throws IllegalArgumentException if {@code value} does not map to any
     *                                  {@link SubdivisionEdge} within {@link #source}
     * @throws UnsupportedOperationException if not overridden by an implementing class
     */
    default SubdivisionEdge toEdge(U value) {
        throw new UnsupportedOperationException("toEdge not implemented");
    }

    /**
     * Converts the specified {@link PointD} vertex into the associated <b>V</b> instance.
     * @param vertex the {@link PointD} vertex to convert
     * @return the <b>V</b> instance associated with {@code vertex}
     * @throws IllegalArgumentException if {@link SubdivisionMap#source} does not contain {@code vertex}
     * @throws UnsupportedOperationException if not overridden by an implementing class
     * @throws NullPointerException if {@code vertex} is {@code null}
     */
    default V fromVertex(PointD vertex) {
        throw new UnsupportedOperationException("fromVertex not implemented");
    }

    /**
     * Converts the specified <b>V</b> instance into the associated {@link PointD} vertex.
     * @param value the <b>V</b> instance to convert
     * @return the {@link PointD} vertex associated with {@code value}
     * @throws IllegalArgumentException if {@code value} does not map to any
     *                                  {@link PointD} vertex within {@link #source}
     * @throws UnsupportedOperationException if not overridden by an implementing class
     */
    default PointD toVertex(V value) {
        throw new UnsupportedOperationException("toVertex not implemented");
    }
}
