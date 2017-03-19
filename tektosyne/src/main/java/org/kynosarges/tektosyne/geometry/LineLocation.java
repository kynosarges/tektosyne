package org.kynosarges.tektosyne.geometry;

import java.util.EnumSet;

/**
 * Specifies the location of a point relative to a directed line segment.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public enum LineLocation {
    /**
     * Specifies that the point is collinear with the line segment and located before its start
     * point on its infinite extension.
     */
    BEFORE,

    /**
     * Specifies that the point coincides with the start point.
     */
    START,

    /**
     * Specifies that the point is collinear with the line segment and located between its
     * start and end point, exclusively.
     */
    BETWEEN,

    /**
     * Specifies that the point coincides with the end point.
     */
    END,

    /**
     * Specifies that the point is collinear with the line segment and located after its end
     * point on its infinite extension.
     */
    AFTER,

    /**
     * Specifies that the point is not collinear with the line segment and located to the left
     * of its infinite extension, viewed from start point to end point.
     */
    LEFT,

    /**
     * Specifies that the point is not collinear with the line segment and located to the right
     * of its infinite extension, viewed from start point to end point.
     */
    RIGHT;

    // bit set for contains() detection
    private final static EnumSet<LineLocation> _containsSet = EnumSet.of(START, BETWEEN, END);

    /**
     * Determines whether the specified {@link LineLocation} indicates that a line segment contains a point.
     * Uses an {@link EnumSet} bit mask for efficient testing of {@link LineLocation} values.
     * 
     * @param location the {@link LineLocation} to examine (may be {@code null})
     * @return {@code true} if the {@link LineLocation} equals {@link #START}, {@link #BETWEEN},
     *         or {@link #END}, else {@code false}
     */
    public static boolean contains(LineLocation location) {
        return _containsSet.contains(location);
    }
}
