package org.kynosarges.tektosyne.geometry;

/**
 * Specifies the eight major compass directions.
 * Specifies the four cardinal compass points, and the four ordinal points halfway between them.
 * Each direction is associated with its compass angle in degrees, starting with zero for
 * {@link #NORTH} and continuing clockwise in 45 degree increments.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public enum Compass {

    /** Specifies zero degrees. */
    NORTH(0),
    /** Specifies 45 degrees. */
    NORTH_EAST(45),
    /** Specifies 90 degrees. */
    EAST(90),
    /** Specifies 135 degrees. */
    SOUTH_EAST(135),
    /** Specifies 180 degrees. */
    SOUTH(180),
    /** Specifies 225 degrees. */
    SOUTH_WEST(225),
    /** Specifies 270 degrees. */
    WEST(270),
    /** Specifies 315 degrees. */
    NORTH_WEST(315);

    private final int _degrees;

    /**
     * Creates a {@link Compass} point with the specified angle.
     * @param degrees the angle in degrees, within the interval [0, 359]
     */
    Compass(int degrees) {
        _degrees = degrees;
    }

    /**
     * Gets the angle associated with the {@link Compass} point.
     * @return the angle in degrees, within the interval [0, 359]
     */
    public int degrees() {
        return _degrees;
    }

    /**
     * Gets the {@link Compass} point at the specified angle.
     * Returns the closest {@link Compass} point in counter-clockwise direction
     * if {@code degrees} does not exactly match an existing point.
     * 
     * @param degrees the angle in degrees whose {@link Compass} point to find
     * @return the {@link Compass} point at the specified {@code degrees}
     * @throws ArrayIndexOutOfBoundsException if {@code degrees} is outside [0, 359]
     */
    public static Compass fromDegrees(int degrees) {
        return Compass.values()[Math.floorDiv(degrees, 45)];
    }
}
