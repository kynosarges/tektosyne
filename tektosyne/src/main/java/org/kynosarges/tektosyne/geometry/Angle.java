package org.kynosarges.tektosyne.geometry;

import org.kynosarges.tektosyne.*;

/**
 * Provides constants and methods to manipulate angles in radians and degrees.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class Angle {
    /**
     * Creates an {@link Angle} instance.
     * Private to prevent instantiation.
     */
    private Angle() { }

    /**
     * The factor that converts an angle from degrees to radians.
     * Holds the value {@link Math#PI} / 180.
     */
    public static final double DEGREES_TO_RADIANS = Math.PI / 180;

    /**
     * The factor that converts an angle from radians to degrees.
     * Holds the value 180 / {@link Math#PI}.
     */
    public static final double RADIANS_TO_DEGREES = 180 / Math.PI;

    /**
     * Converts the specified angle in degrees to the nearest {@link Compass} point.
     * @param degrees the angle in degrees to convert. This value is taken {@link Fortran#modulo}
     *                360 degrees, and may therefore lie outside the interval [0, 360).
     * @return the {@link Compass} point nearest the specified {@code degrees}
     */
    public static Compass degreesToCompass(double degrees) {
        degrees = Fortran.modulo(degrees + 22.5, 360);
        final int point = (int) (degrees / 45);
        return Compass.fromDegrees(point * 45);
    }

    /**
     * Finds the shortest distance between two specified normalized angles, in degrees.
     * Both specified angles must be normalized to the half-open interval [0, 360), e.g.
     * using {@link #normalizeDegrees}. If so, {@code distanceDegrees} returns the value
     * within the half-open interval (-180, 180] that solves the following equation:
     * {@code end} = {@link #normalizeDegrees}({@code start} +
     *               {@code distanceDegrees}({@code start}, {@code end}))
     * 
     * @param start the angle from which the distance is measured, in normalized degrees
     * @param end the angle to which the distance is measured, in normalized degrees
     * @return the shortest distance between {@code start} and {@code end}, in signed degrees
     */
    public static double distanceDegrees(double start, double end) {
        double dist = end - start;

        if (dist > 180)
            dist -= 360;
        else if (dist <= -180)
            dist += 360;

        return dist;
    }

    /**
     * Finds the shortest distance between two specified normalized angles, in radians.
     * Both specified angles must be normalized to the half-open interval [0, 2 {@link Math#PI}),
     * e.g. using {@link #normalizeRadians}. If so, {@code distanceRadians} returns the value within
     * the half-open interval (-{@link Math#PI}, {@link Math#PI}] that solves the following equation:
     * {@code end} = {@link #normalizeRadians}({@code start} +
     *               {@code distanceRadians}({@code start}, {@code end}))
     * 
     * @param start the angle from which the distance is measured, in normalized radians
     * @param end the angle to which the distance is measured, in normalized radians
     * @return the shortest distance between {@code start} and {@code end}, in signed radians
     */
    public static double distanceRadians(double start, double end) {
        double dist = end - start;

        if (dist > Math.PI)
            dist -= 2 * Math.PI;
        else if (dist <= -Math.PI)
            dist += 2 * Math.PI;

        return dist;
    }

    /**
     * Normalizes the specified angle in degrees to the interval [0, 360).
     * @param degrees the angle in degrees to normalize
     * @return the specified {@code degrees} normalized to the half-open interval [0, 360)
     */
    public static double normalizeDegrees(double degrees) {
        degrees %= 360;
        if (degrees < 0) degrees += 360;
        return degrees;
    }

    /**
     * Normalizes the specified angle in degrees to the interval [0, 360),
     * after rounding to the nearest {@link Integer}.
     * Uses {@link Fortran#nint} to round {@code degrees} before normalization.
     * The result is guaranteed to be an {@link Integer} value within [0, 359].
     * 
     * @param degrees the angle in degrees to normalize
     * @return the specified {@code degrees} rounded to the nearest {@link Integer}
     *         and normalized to the half-open interval [0, 360)
     * @throws ArithmeticException if {@code degrees} overflows {@link Integer}
     */
    public static int normalizeRoundedDegrees(double degrees) {
        int angle = Fortran.nint(degrees);
        angle %= 360;
        if (angle < 0) angle += 360;
        return angle;
    }

    /**
     * Normalizes the specified angle in radians to the interval [0, 2 {@link Math#PI}).
     * @param radians the angle in radians to normalize
     * @return the specified {@code radians} normalized to the half-open interval [0, 2 {@link Math#PI})
     */
    public static double normalizeRadians(double radians) {
        radians %= 2 * Math.PI;
        if (radians < 0) radians += 2 * Math.PI;
        return radians;
    }
}
