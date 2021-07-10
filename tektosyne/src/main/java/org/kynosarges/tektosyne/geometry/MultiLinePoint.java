package org.kynosarges.tektosyne.geometry;

import java.util.*;

/**
 * Contains the results of {@link MultiLineIntersection} algorithms.
 * Immutable class representing one valid intersection point found by the
 * {@link MultiLineIntersection} algorithms. The complete results are
 * stored as collections of zero or more {@link MultiLinePoint} instances.
 * <p>
 * {@link MultiLinePoint} holds the input {@link Line#index} of all intersecting
 * line segments, as well as the {@link Line#location} of the {@link #shared}
 * coordinates relative to each line segment.</p>
 * 
 * @author Christoph Nahr
 * @version 6.3.0
 */
public final class MultiLinePoint {
    /**
     * The line segments intersecting at the {@link #shared} coordinates.
     * Holds the indices of the line segments in the original input collection that
     * intersect at the {@link #shared} coordinates, as well as these coordinates’
     * {@link LineLocation} relative to the corresponding input line segment.
     * <p>
     * Always contains at least two elements. The element order is arbitrary,
     * depending on the generation sequence by {@link MultiLineIntersection}.</p>
     */
    public final Line[] lines;

    /**
     * The {@link PointD} coordinates shared by all intersected {@link #lines}.
     * Holds the coordinates where all {@link #lines} intersect. These coordinates
     * are always computed (rather than copied) if all {@link Line#location} values
     * equal {@link LineLocation#BETWEEN}.
     * <p>
     * Otherwise, the coordinates are either computed, or copied from the {@link LineD#start}
     * or {@link LineD#end} point of a {@link #lines} element whose {@link Line#location}
     * equals {@link LineLocation#START} or {@link LineLocation#END}, respectively.</p>
     */
    public final PointD shared;

    /**
     * Creates a {@link MultiLinePoint} with the specified {@link PointD} and {@link Line} instances.
     * @param shared the {@link PointD} coordinates shared by all {@code lines}
     * @param lines an array of {@link Line} instances representing all line segments
     *              intersecting at {@code shared}
     * @throws IllegalArgumentException if {@code lines} has less than two elements
     * @throws NullPointerException if {@code shared} or {@code lines} is {@code null}
     */
    MultiLinePoint(PointD shared, Line[] lines) {
        if (shared == null)
            throw new NullPointerException("shared");
        if (lines == null)
            throw new NullPointerException("lines");
        if (lines.length < 2)
            throw new IllegalArgumentException("lines.length < 2");

        this.shared = shared;
        this.lines = lines;
    }

    /**
     * Compares the specified {@link Object} to this {@link MultiLinePoint} instance.
     * @param obj the {@link Object} to compare to this instance
     * @return {@code true} if {@code obj} is not {@code null} and a {@link MultiLinePoint}
     *         instance whose {@link #shared} coordinates and {@link #lines} array equal those
     *         of this instance, else {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MultiLinePoint))
            return false;

        final MultiLinePoint other = (MultiLinePoint) obj;
        return (shared.equals(other.shared) &&
                Arrays.equals(lines, other.lines));
    }

    /**
     * Returns a hash code for the {@link MultiLinePoint}.
     * @return an {@link Integer} hash code for the {@link MultiLinePoint}
     */
    @Override
    public int hashCode() {
        return shared.hashCode();
    }

    /**
     * Returns a {@link String} representation of the {@link MultiLinePoint}.
     * @return a {@link String} containing the value of {@link #shared} and the length of {@link #lines}
     */
    @Override
    public String toString() {
        return String.format("MultiLinePoint[shared=%s, lines.length=%d]",
                shared, (lines == null ? 0 : lines.length));
    }

    /**
     * Represents one of the line segments intersecting at the {@link #shared} coordinates.
     * Immutable class that also stores the {@link LineLocation} of the {@link #shared}
     * coordinates relative to the indicated line segment.
     */
    public final static class Line {
        /**
         * The input index of a line segment intersecting at the {@link #shared} coordinates.
         * Holds a zero-based index relative to the original input collection, indicating
         * one of the line segments that intersect at the {@link #shared} coordinates.
         */
        public final int index;

        /**
         * The location of the {@link #shared} coordinates relative to the line segment at {@link #index}.
         * Holds a {@link LineLocation} value specifying the location of the {@link #shared}
         * coordinates relative to the input line segment indicated by {@link #index}.
         * <p>
         * Since all line segments of a {@link MultiLinePoint} are guaranteed to intersect at the
         * {@link #shared} coordinates, each {@link #location} is either {@link LineLocation#START},
         * {@link LineLocation#BETWEEN}, or {@link LineLocation#END}.</p>
         */
        public final LineLocation location;

        /**
         * Creates a {@link Line} with the specified input index and {@link LineLocation}.
         * @param index the input index of a line segment intersecting at {@link #shared}
         * @param location The location of {@link #shared} relative to the line segment at {@link #index}
         * @throws IllegalArgumentException if {@code index} is less than zero,
         *         or {@code location} does not equal {@link LineLocation#START},
         *         {@link LineLocation#BETWEEN}, or {@link LineLocation#END}
         */
        Line(int index, LineLocation location) {
            if (index < 0)
                throw new IllegalArgumentException("index < 0");
            if (!LineLocation.contains(location))
                throw new IllegalArgumentException("location != START, BETWEEN, END");

            this.index = index;
            this.location = location;
        }

        /**
         * Compares the specified {@link Object} to this {@link Line} instance.
         * @param obj the {@link Object} to compare to this instance
         * @return {@code true} if {@code obj} is not {@code null} and a {@link Line} instance whose
         *         {@link #index} and {@link #location} equal those of this instance, else {@code false}
         */
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Line))
                return false;

            final Line other = (Line) obj;
            return (index == other.index &&
                    location == other.location);
        }

        /**
         * Returns a hash code for the {@link Line}.
         * @return an {@link Integer} hash code for the {@link Line}
         */
        @Override
        public int hashCode() {
            return index;
        }

        /**
         * Returns a {@link String} representation of the {@link Line}.
         * @return a {@link String} containing the value of {@link #index} and {@link #location}
         */
        @Override
        public String toString() {
            return String.format("Line[index=%d, location=%s]", index, location);
        }
    }
}
