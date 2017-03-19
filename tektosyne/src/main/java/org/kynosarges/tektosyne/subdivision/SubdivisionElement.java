package org.kynosarges.tektosyne.subdivision;

import java.util.Objects;
import org.kynosarges.tektosyne.geometry.PointD;

/**
 * Represents one element of a planar {@link Subdivision}.
 * Immutable class containing either an {@link Subdivision#edges}, {@link Subdivision#faces},
 * {@link Subdivision#vertices} element of a planar {@link Subdivision}. A
 * {@link SubdivisionElementType} value indicates which element type is present.
 * 
 * @author Christpoh Nahr
 * @version 6.0.0
 */
public final class SubdivisionElement {
    /**
     * A {@link SubdivisionElement} of type {@link SubdivisionElementType#FACE}
     * whose {@link #face} is {@code null}.
     * By convention, {@link #NULL_FACE} represents the unbounded face of a planar
     * {@link Subdivision}. Use {@link #isUnboundedFace} to test for this condition,
     * as well as for the actual unbounded {@link SubdivisionFace}.
     */
    public static final SubdivisionElement NULL_FACE =
            new SubdivisionElement((SubdivisionFace) null);

    /**
     * The actual {@link Subdivision} element stored in the {@link SubdivisionElement}.
     * Contains either a {@link SubdivisionEdge}, a {@link SubdivisionFace}, or a {@link PointD}
     * vertex. Use {@link #edge}, {@link #face} and {@link #vertex} for type-safe access.
     */
    public final Object content;

    /**
     * The {@link SubdivisionElementType} of the {@link SubdivisionElement}.
     * Never {@code null}.
     */
    public final SubdivisionElementType type;

    /**
     * Creates a {@link SubdivisionElement} of type {@link SubdivisionElementType#EDGE}
     * with the specified {@link SubdivisionEdge}.
     * @param edge the {@link SubdivisionEdge} to store
     * @throws NullPointerException if {@code edge} is {@code null}
     */
    public SubdivisionElement(SubdivisionEdge edge) {
        if (edge == null)
            throw new NullPointerException("edge");

        content = edge;
        type = SubdivisionElementType.EDGE;
    }

    /**
     * Creates a {@link SubdivisionElement} of type {@link SubdivisionElementType#FACE}
     * with the specified {@link SubdivisionFace}.
     * {@code face} may be {@code null} to represent an unbounded face,
     * see {@link #NULL_FACE} and {@link #isUnboundedFace}.
     * 
     * @param face the {@link SubdivisionFace} to store
     */
    public SubdivisionElement(SubdivisionFace face) {
        content = face;
        type = SubdivisionElementType.FACE;
    }

    /**
     * Creates a {@link SubdivisionElement} of type {@link SubdivisionElementType#VERTEX}
     * with the specified {@link PointD} vertex.
     * @param vertex the {@link PointD} vertex to store
     * @throws NullPointerException if {@code vertex} is {@code null}
     */
    public SubdivisionElement(PointD vertex) {
        if (vertex == null)
            throw new NullPointerException("vertex");
        
        content = vertex;
        type = SubdivisionElementType.VERTEX;
    }

    /**
     * Gets the {@link SubdivisionEdge} stored in the {@link SubdivisionElement}.
     * Never returns {@code null}.
     * 
     * @return the {@link SubdivisionEdge} stored in the {@link SubdivisionElement}
     * @throws IllegalStateException if {@link #type} does not equal {@link SubdivisionElementType#EDGE}
     */
    public SubdivisionEdge edge() {
        if (type != SubdivisionElementType.EDGE)
            throw new IllegalStateException("type != EDGE");

        return (SubdivisionEdge) content;
    }

    /**
     * Gets the {@link SubdivisionFace} stored in the {@link SubdivisionElement}.
     * May return {@code null} to represent an unbounded face,
     * see {@link #NULL_FACE} and {@link #isUnboundedFace}.
     * 
     * @return the {@link SubdivisionFace} stored in the {@link SubdivisionElement}
     * @throws IllegalStateException if {@link #type} does not equal {@link SubdivisionElementType#FACE}
     */
    public SubdivisionFace face() {
        if (type != SubdivisionElementType.FACE)
            throw new IllegalStateException("type != FACE");

        return (SubdivisionFace) content;
    }

    /**
     * Indicates whether the {@link SubdivisionElement} represents an unbounded {@link SubdivisionFace}.
     * The unbounded face of a planar {@link Subdivision} may be represented by {@link #NULL_FACE}
     * or by a valid {@link SubdivisionFace} whose {@link SubdivisionFace#key} is zero.
     * {@link #isUnboundedFace} tests for both conditions.
     * 
     * @return {@code true} if {@link #type} is {@link SubdivisionElementType#FACE} and {@link #face}
     *         is {@code null} or its {@link SubdivisionFace#key} is zero, else {@code false}
     */
    public boolean isUnboundedFace() {
        return (type == SubdivisionElementType.FACE &&
            (content == null || ((SubdivisionFace) content)._key == 0));
    }

    /**
     * Gets the {@link PointD} vertex stored in the {@link SubdivisionElement}.
     * Never returns {@code null}.
     * 
     * @return the {@link PointD} vertex stored in the {@link SubdivisionElement}
     * @throws IllegalStateException if {@link #type} does not equal {@link SubdivisionElementType#VERTEX}
     */
    public PointD vertex() {
        if (type != SubdivisionElementType.VERTEX)
            throw new IllegalStateException("type != VERTEX");

        return (PointD) content;
    }

    /**
     * Compares the specified {@link Object} to this {@link SubdivisionElement} instance.
     * @param obj the {@link Object} to compare to this instance
     * @return {@code true} if {@code obj} is not {@code null} and a {@link SubdivisionElement} instance
     *         whose {@link #type} and {@link #content} equal those of this instance, else {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || !(obj instanceof SubdivisionElement))
            return false;

        final SubdivisionElement element = (SubdivisionElement) obj;
        return (type == element.type && Objects.equals(content, element.content));
    }

    /**
     * Returns a hash code for the {@link SubdivisionElement}.
     * @return an {@link Integer} hash code for the {@link SubdivisionElement}
     */
    @Override
    public int hashCode() {
        return (content != null ? content.hashCode() : type.ordinal());
    }

    /**
     * Returns a {@link String} representation of the {@link SubdivisionElement}.
     * @return a {@link String} containing the values of {@link #type} and {@link #content}
     */
    @Override
    public String toString() {
        return String.format("SubdivisionElement[type=%s, content=%s]",
            type.toString(), String.valueOf(content));
    }
}
