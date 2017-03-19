package org.kynosarges.tektosyne.geometry;

/**
 * Represents an extension in two-dimensional space, using {@link Integer} coordinates.
 * {@link SizeI} contains two immutable non-negative {@link Integer} dimensions.
 * Use {@link SizeD} to represent sizes with {@link Double} dimensions.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class SizeI {
    /**
     * The horizontal dimension of the {@link SizeI}. Never negative.
     */
    public final int width;

    /**
     * The vertical dimension of the {@link SizeI}. Never negative.
     */
    public final int height;

    /**
     * An empty read-only {@link SizeI}.
     * Both {@link #width} and {@link #height} are set to zero.
     */
    public static final SizeI EMPTY = new SizeI();

    /**
     * Creates a {@link SizeI} with zero extension.
     * Both {@link #width} and {@link #height} are set to zero.
     */
    public SizeI() {
        this.width = 0;
        this.height = 0;
    }

    /**
     * Creates a {@link SizeI} with the specified extension.
     * @param width the {@link #width} of the {@link SizeI}.
     * @param height the {@link #height} of the {@link SizeI}. 
     * @throws IllegalArgumentException if {@code width} or {@code height} is less than zero
     */
    public SizeI(int width, int height) {
        if (width < 0)
            throw new IllegalArgumentException("width < 0");
        if (height < 0)
            throw new IllegalArgumentException("height < 0");

        this.width = width;
        this.height = height;
    }

    /**
     * Adds the extension of the specified {@link SizeI} to this instance.
     * @param size the {@link SizeI} whose extension to add to this instance
     * @return a {@link SizeI} whose {@link #width} and {@link #height} equal the addition
     *         of the corresponding dimensions of the specified {@code size} to this instance
     * @throws ArithmeticException if the sum in any dimension overflows {@link Integer}
     * @throws NullPointerException if {@code size} is {@code null}
     */
    public SizeI add(SizeI size) {
        return new SizeI(
                Math.addExact(width, size.width),
                Math.addExact(height, size.height));
    }

    /**
     * Converts the specified {@link Integer} array to a {@link SizeI} array.
     * The returned array has half as many elements as the specified {@code sizes}
     * and retains the same dimension sequence.
     * 
     * @param sizes an array containing the {@link #width} and {@link #height} components
     *              of {@link SizeI} instances, stored in alternating index positions
     * @return the {@link SizeI} array created from {@code sizes}
     * @throws IllegalArgumentException if {@code sizes} has an odd number of elements,
     *         or if any {@link #width} or {@link #height} is less than zero
     * @throws NullPointerException if {@code sizes} is {@code null}
     */
    public static SizeI[] fromInts(int... sizes) {
        if (sizes.length % 2 != 0)
            throw new IllegalArgumentException("sizes.length % 2 != 0");

        final SizeI[] output = new SizeI[sizes.length / 2];

        for (int i = 0; i < output.length; i++)
            output[i] = new SizeI(sizes[2 * i], sizes[2 * i + 1]);

        return output;
    }

    /**
     * Determines whether the {@link SizeI} is empty.
     * @return {@code true} if both {@link #width} and {@link #height} equal zero, else {@code false}
     */
    public boolean isEmpty() {
        return (width == 0 && height == 0);
    }
    
    /**
     * Restricts the {@link SizeI} to the specified extension range.
     * @param minWidth the smallest permissible {@link #width}
     * @param minHeight the smallest permissible {@link #height}
     * @param maxWidth the greatest permissible {@link #width}
     * @param maxHeight the greatest permissible {@link #height}
     * @return a {@link SizeI} whose {@link #width} and {@link #height} equal those
     *         of this instance, restricted to the specified extension range
     * @throws IllegalArgumentException if {@code maxWidth} or {@code maxHeight} is less than zero
     */
    public SizeI restrict(int minWidth, int minHeight, int maxWidth, int maxHeight) {
        int width = this.width, height = this.height;

        if (width < minWidth) width = minWidth;
        else if (width > maxWidth) width = maxWidth;

        if (height < minHeight) height = minHeight;
        else if (height > maxHeight) height = maxHeight;

        return new SizeI(width, height);
    }

    /**
     * Subtracts the extension of the specified {@link SizeI} from this instance.
     * @param size the {@link SizeI} extension to subtract from this instance
     * @return a {@link SizeI} whose {@link #width} and {@link #height} equal the subtraction
     *         of the corresponding dimensions of the specified {@code size} from this instance
     * @throws IllegalArgumentException if the difference in any dimension is less than zero
     * @throws NullPointerException if {@code size} is {@code null}
     */
    public SizeI subtract(SizeI size) {
        return new SizeI(width - size.width, height - size.height);
    }

    /**
     * Converts the specified {@link SizeI} array to an {@link Integer} array.
     * The returned array has twice as many elements as the specified {@code sizes}
     * and retains the same dimension sequence.
     * 
     * @param sizes the {@link SizeI} array to convert
     * @return an array containing the {@link #width} and {@link #height} components
     *         of all {@code sizes}, stored in alternating index positions
     * @throws NullPointerException if {@code sizes} or any of its elements is {@code null}
     */
    public static int[] toInts(SizeI... sizes) {
        final int[] output = new int[2 * sizes.length];

        for (int i = 0; i < sizes.length; i++) {
            output[2 * i] = sizes[i].width;
            output[2 * i + 1] = sizes[i].height;
        }

        return output;
    }

    /**
     * Converts the {@link SizeI} to a {@link SizeD}.
     * @return a {@link SizeD} whose {@link SizeD#width} and {@link SizeD#height}
     *         equal the corresponding dimensions of the {@link SizeI}
     */
    public SizeD toSizeD() {
        return new SizeD(width, height);
    }

    /**
     * Compares the specified {@link Object} to this {@link SizeI} instance.
     * @param obj the {@link Object} to compare to this instance
     * @return {@code true} if {@code obj} is not {@code null} and a {@link SizeI} instance whose 
     *         {@link #width} and {@link #height} equal those of this instance, else {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || !(obj instanceof SizeI))
            return false;

        final SizeI size = (SizeI) obj;
        return (width == size.width && height == size.height);
    }

    /**
     * Returns a hash code for the {@link SizeI}.
     * @return an {@link Integer} hash code for the {@link SizeI}
     */
    @Override
    public int hashCode() {
        return 31 * width + height;
    }

    /**
     * Returns a {@link String} representation of the {@link SizeI}.
     * @return a {@link String} containing the values of {@link #width} and {@link #height}
     */
    @Override
    public String toString() {
        return String.format("SizeI[width=%d, height=%d]", width, height);
    }
}
