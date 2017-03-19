package org.kynosarges.tektosyne.geometry;

/**
 * Specifies the location of a point relative to a rectangle.
 * Immutable class containing a pair of {@link LineLocation} values indicating the
 * location of each point dimension relative to the corresponding rectangle edge.
 * For this purpose, the rectangle edges are interpreted as directed line segments 
 * starting at the minimum coordinate and ending at the maximum coordinate.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class RectLocation {
    /**
     * The relative location of the point along the x-coordinate.
     * Holds the {@link LineLocation} of the point relative to the rectangle’s top
     * (or bottom) edge, shifted to the point’s y-coordinate. Never {@code null},
     * {@link LineLocation#LEFT}, or {@link LineLocation#RIGHT}.
     */
    public final LineLocation edgeX;

    /**
     * The relative location of the point along the y-coordinate.
     * Holds the {@link LineLocation} of the point relative to the rectangle’s left
     * (or right) edge, shifted to the point’s x-coordinate. Never {@code null},
     * {@link LineLocation#LEFT}, or {@link LineLocation#RIGHT}.
     */
    public final LineLocation edgeY;

    /**
     * Creates a {@link RectLocation} with the specified dimensional locations.
     * @param edgeX the relative location of the point along the x-coordinate
     * @param edgeY the relative location of the point along the y-coordinate
     * @throws NullPointerException if {@code edgeX} or {@code edgeY} is {@code null}
     * @throws IllegalArgumentException if {@code edgeX} or {@code edgeY} is
     *         {@link LineLocation#LEFT} or {@link LineLocation#RIGHT}
     */
    public RectLocation(LineLocation edgeX, LineLocation edgeY) {
        if (edgeX == null)
            throw new NullPointerException("edgeX");
        if (edgeY == null)
            throw new NullPointerException("edgeY");

        if (edgeX == LineLocation.LEFT || edgeX == LineLocation.RIGHT)
            throw new IllegalArgumentException("edgeX == LEFT/RIGHT");
        if (edgeY == LineLocation.LEFT || edgeY == LineLocation.RIGHT)
            throw new IllegalArgumentException("edgeY == LEFT/RIGHT");

        this.edgeX = edgeX;
        this.edgeY = edgeY;
    }

    /**
     * Compares the specified {@link Object} to this {@link RectLocation} instance.
     * @param obj the {@link Object} to compare to this instance
     * @return {@code true} if {@code obj} is not {@code null} and a {@link RectLocation}
     *         instance whose {@link #edgeX} and {@link #edgeY} values equal those
     *         of this instance, else {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof RectLocation))
            return false;

        final RectLocation other = (RectLocation) obj;
        return (edgeX == other.edgeX && edgeY == other.edgeY);
    }

    /**
     * Returns a hash code for the {@link RectLocation}.
     * @return an {@link Integer} hash code for the {@link RectLocation}
     */
    @Override
    public int hashCode() {
        return (31 * edgeX.hashCode() + edgeY.hashCode());
    }

    /**
     * Returns a {@link String} representation of the {@link RectLocation}.
     * @return a {@link String} containing the values of {@link #edgeX} and {@link #edgeY}
     */
    @Override
    public String toString() {
        return String.format("RectLocation[edgeX=%s, edgeY=%s]", edgeX, edgeY);
    }
}
