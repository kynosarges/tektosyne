package org.kynosarges.tektosyne.geometry;

import org.kynosarges.tektosyne.*;

/**
 * Represents an extension in two-dimensional space, using {@link Double} coordinates.
 * {@link SizeD} contains two immutable non-negative {@link Double} extensions.
 * Use {@link SizeI} to represent sizes with {@link Integer} extensions.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class SizeD {
    /**
     * The horizontal dimension of the {@link SizeD}. Never negative.
     */
    public final double width;

    /**
     * The vertical dimension of the {@link SizeD}. Never negative.
     */
    public final double height;

    /**
     * An empty read-only {@link SizeD}.
     * Both {@link #width} and {@link #height} are set to zero.
     */
    public static final SizeD EMPTY = new SizeD();

    /**
     * Creates a {@link SizeD} with zero extension.
     * Both {@link #width} and {@link #height} are set to zero.
     */
    public SizeD() {
        this.width = 0;
        this.height = 0;
    }

    /**
     * Creates a {@link SizeD} with the specified extension.
     * @param width the {@link #width} of the {@link SizeD}.
     * @param height the {@link #height} of the {@link SizeD}. 
     * @throws IllegalArgumentException if {@code width} or {@code height} is less than zero
     */
    public SizeD(double width, double height) {
        if (width < 0)
            throw new IllegalArgumentException("width < 0");
        if (height < 0)
            throw new IllegalArgumentException("height < 0");

        this.width = width;
        this.height = height;
    }

    /**
     * Adds the extension of the specified {@link SizeD} to this instance.
     * Returns {@link Double#POSITIVE_INFINITY} in any dimension that overflows {@link Double}.
     * 
     * @param size the {@link SizeD} whose extension to add to this instance
     * @return a {@link SizeD} whose {@link #width} and {@link #height} equal the addition
     *         of the corresponding dimensions of the specified {@code size} to this instance
     * @throws NullPointerException if {@code size} is {@code null}
     */
    public SizeD add(SizeD size) {
        return new SizeD(width + size.width, height + size.height);
    }

    /**
     * Compares two {@link SizeD} instances for equality, given the specified epsilon.
     * @param a the first {@link SizeD} to compare
     * @param b the second {@link SizeD} to compare
     * @param epsilon the maximum absolute difference where the corresponding dimensions
     *                of {@code a} and {@code b} are considered equal
     * @return {@code true} if the absolute difference between both corresponding dimensions
     *         of {@code a} and {@code b} is no greater than {@code epsilon}, else {@code false}
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public static boolean equals(SizeD a, SizeD b, double epsilon) {
        if (epsilon < 0)
            throw new IllegalArgumentException("epsilon < 0");

        return (Math.abs(a.width - b.width) <= epsilon
            && Math.abs(a.height - b.height) <= epsilon);
    }

    /**
     * Converts the specified {@link Double} array to a {@link SizeD} array.
     * The returned array has half as many elements as the specified {@code sizes}
     * and retains the same dimension sequence.
     * 
     * @param sizes an array containing the {@link #width} and {@link #height} components
     *              of {@link SizeD} instances, stored in alternating index positions
     * @return the {@link SizeD} array created from {@code sizes}
     * @throws IllegalArgumentException if {@code sizes} has an odd number of elements,
     *         or if any {@link #width} or {@link #height} is less than zero
     * @throws NullPointerException if {@code sizes} is {@code null}
     */
    public static SizeD[] fromDoubles(double... sizes) {
        if (sizes.length % 2 != 0)
            throw new IllegalArgumentException("sizes.length % 2 != 0");

        final SizeD[] output = new SizeD[sizes.length / 2];

        for (int i = 0; i < output.length; i++)
            output[i] = new SizeD(sizes[2 * i], sizes[2 * i + 1]);

        return output;
    }

    /**
     * Determines whether the {@link SizeD} is empty.
     * @return {@code true} if both {@link #width} and {@link #height} equal zero, else {@code false}
     */
    public boolean isEmpty() {
        return (width == 0 && height == 0);
    }

    /**
     * Restricts the {@link SizeD} to the specified extension range.
     * @param minWidth the smallest permissible {@link #width}
     * @param minHeight the smallest permissible {@link #height}
     * @param maxWidth the greatest permissible {@link #width}
     * @param maxHeight the greatest permissible {@link #height}
     * @return a {@link SizeD} whose {@link #width} and {@link #height} equal those
     *         of this instance, restricted to the specified extension range
     * @throws IllegalArgumentException if {@code maxWidth} or {@code maxHeight} is less than zero
     */
    public SizeD restrict(double minWidth, double minHeight, double maxWidth, double maxHeight) {
        double width = this.width, height = this.height;

        if (width < minWidth) width = minWidth;
        else if (width > maxWidth) width = maxWidth;

        if (height < minHeight) height = minHeight;
        else if (height > maxHeight) height = maxHeight;

        return new SizeD(width, height);
    }

    /**
     * Converts the {@link SizeD} to a {@link SizeI} by rounding dimensions to the
     * nearest {@link Integer} values. Uses {@link Fortran#nint} for rounding.
     * 
     * @return a {@link SizeI} whose {@link SizeI#width} and {@link SizeI#height}
     *         equal the corresponding dimensions of the {@link SizeD},
     *         rounded to the nearest {@link Integer} values
     * @throws ArithmeticException if any dimension overflows {@link Integer}
     */
    public SizeI round() {
        return new SizeI(Fortran.nint(width), Fortran.nint(height));
    }

    /**
     * Subtracts the extension of the specified {@link SizeD} from this instance.
     * @param size the {@link SizeD} extension to subtract from this instance
     * @return a {@link SizeD} whose {@link #width} and {@link #height} equal the subtraction
     *         of the corresponding dimensions of the specified {@code size} from this instance
     * @throws IllegalArgumentException if the difference in any dimension is less than zero
     * @throws NullPointerException if {@code size} is {@code null}
     */
    public SizeD subtract(SizeD size) {
        return new SizeD(width - size.width, height - size.height);
    }

    /**
     * Converts the specified {@link SizeD} array to a {@link Double} array.
     * The returned array has twice as many elements as the specified {@code sizes}
     * and retains the same dimension sequence.
     * 
     * @param sizes the {@link SizeD} array to convert
     * @return an array containing the {@link #width} and {@link #height} components
     *         of all {@code sizes}, stored in alternating index positions
     * @throws NullPointerException if {@code sizes} or any of its elements is {@code null}
     */
    public static double[] toDoubles(SizeD... sizes) {
        final double[] output = new double[2 * sizes.length];

        for (int i = 0; i < sizes.length; i++) {
            output[2 * i] = sizes[i].width;
            output[2 * i + 1] = sizes[i].height;
        }

        return output;
    }

    /**
     * Converts the {@link SizeD} to a {@link SizeI} by truncating dimensions to the
     * nearest {@link Integer} values. Uses {@link Integer} casts for truncation.
     * 
     * @return a {@link SizeI} whose {@link SizeI#width} and {@link SizeI#height}
     *         equal the corresponding dimensions of the {@link SizeD},
     *         truncated to the nearest {@link Integer} values
     * @throws ArithmeticException if any dimension overflows {@link Integer}
     */
    public SizeI toSizeI() {
        return new SizeI(
                MathUtils.toIntExact(width),
                MathUtils.toIntExact(height));
    }

    /**
     * Compares the specified {@link Object} to this {@link SizeD} instance.
     * @param obj the {@link Object} to compare to this instance
     * @return {@code true} if {@code obj} is not {@code null} and a {@link SizeD} instance whose 
     *         {@link #width} and {@link #height} equal those of this instance, else {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || !(obj instanceof SizeD))
            return false;

        final SizeD size = (SizeD) obj;
        return (width == size.width && height == size.height);
    }

    /**
     * Returns a hash code for the {@link SizeD}.
     * @return an {@link Integer} hash code for the {@link SizeD}
     */
    @Override
    public int hashCode() {
        final long widthHash = Double.doubleToLongBits(width);
        final long heightHash = Double.doubleToLongBits(height);
        return 31 * (int) (widthHash ^ (widthHash >>> 32))
                + (int) (heightHash ^ (heightHash >>> 32));
    }

    /**
     * Returns a {@link String} representation of the {@link SizeD}.
     * @return a {@link String} containing the values of {@link #width} and {@link #height}
     */
    @Override
    public String toString() {
        return String.format("SizeD[width=%g, height=%g]", width, height);
    }
}
