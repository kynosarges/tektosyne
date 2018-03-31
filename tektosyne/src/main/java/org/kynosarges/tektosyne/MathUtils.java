package org.kynosarges.tektosyne;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Provides general mathematical utility methods.
 * All <b>getAny...</b> methods use the {@link ThreadLocalRandom#current}
 * instance of {@link ThreadLocalRandom} for random number generation,
 * and are therefore both thread-safe and non-blocking.
 *
 * @author Christoph Nahr
 * @version 6.2.0
 */
public final class MathUtils {

    private final static ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    /**
     * Creates a {@link MathUtils} instance.
     * Private to prevent instantiation.
     */
    private MathUtils() { }

    /**
     * Compares two {@link Double} numbers numerically, given the specified epsilon.
     * @param a the first {@link Double} number to compare
     * @param b the second {@link Double} number to compare
     * @param epsilon the maximum absolute difference where {@code a} and {@code b} are considered equal
     * @return zero if the absolute difference between {@code a} and {@code b}
     *         is no greater than {@code epsilon}, otherwise a value less or greater
     *         than zero if {@code a} is less or greater than {@code b}, respectively
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     */
    public static int compare(double a, double b, double epsilon) {
        if (epsilon < 0)
            throw new IllegalArgumentException("epsilon < 0");

        final double delta = a - b;
        if (Math.abs(delta) <= epsilon)
            return 0;

        return (delta < 0 ? -1 : 1);
    }

    /**
     * Compares two {@link Float} numbers numerically, given the specified epsilon.
     * @param a the first {@link Float} number to compare
     * @param b the second {@link Float} number to compare
     * @param epsilon the maximum absolute difference where {@code a} and {@code b} are considered equal
     * @return zero if the absolute difference between {@code a} and {@code b}
     *         is no greater than {@code epsilon}, otherwise a value less or greater
     *         than zero if {@code a} is less or greater than {@code b}, respectively
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     */
    public static int compare(float a, float b, float epsilon) {
        if (epsilon < 0)
            throw new IllegalArgumentException("epsilon < 0");

        final float delta = a - b;
        if (Math.abs(delta) <= epsilon)
            return 0;

        return (delta < 0 ? -1 : 1);
    }

    /**
     * Compares two {@link Double} numbers for equality, given the specified epsilon.
     * @param a the first {@link Double} number to compare
     * @param b the second {@link Double} number to compare
     * @param epsilon the maximum absolute difference where {@code a} and {@code b} are considered equal
     * @return {@code true} if the absolute difference between {@code a} and {@code b}
     *         is no greater than {@code epsilon}, else {@code false}
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     */
    public static boolean equals(double a, double b, double epsilon) {
        if (epsilon < 0)
            throw new IllegalArgumentException("epsilon < 0");

        return (Math.abs(a - b) <= epsilon);
    }

    /**
     * Compares two {@link Float} numbers for equality, given the specified epsilon.
     * @param a the first {@link Float} number to compare
     * @param b the second {@link Float} number to compare
     * @param epsilon the maximum absolute difference where {@code a} and {@code b} are considered equal
     * @return {@code true} if the absolute difference between {@code a} and {@code b}
     *         is no greater than {@code epsilon}, else {@code false}
     * @throws IllegalArgumentException if {@code epsilon} is less than zero
     */
    public static boolean equals(float a, float b, float epsilon) {
        if (epsilon < 0)
            throw new IllegalArgumentException("epsilon < 0");

        return (Math.abs(a - b) <= epsilon);
    }

    /**
     * Gets a random element from the specified array.
     * Returns the {@code array} element at a random index between zero
     * (inclusive) and {@code length} (exclusive). This is an O(1) operation.
     * 
     * @param <T> the type of all elements in the array
     * @param array the array providing the elements
     * @return a random element from {@code array}
     * @throws IllegalArgumentException if {@code array} is empty
     * @throws NullPointerException if {@code array} is {@code null}
     */
    public static <T> T getAny(T[] array) {
        try {
            return array[RANDOM.nextInt(array.length)];
        }
        catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("empty array", e);
        }
    }

    /**
     * Gets a random element from the specified {@link Collection}.
     * Iterates through {@code collection} for a random number of steps, between zero
     * (inclusive) to {@link Collection#size} (exclusive). This is an O(n) operation.
     * 
     * @param <T> the type of all elements in the {@link Collection}
     * @param collection the {@link Collection} providing the elements
     * @return a random element from {@code collection}
     * @throws IllegalArgumentException if {@code collection} is empty
     * @throws NullPointerException if {@code collection} is {@code null}
     */
    public static <T> T getAny(Collection<T> collection) {
        int index = RANDOM.nextInt(collection.size());

        for(T t: collection)
            if (--index < 0) return t;

        throw new IllegalArgumentException("empty collection");
    }

    /**
     * Gets a random element from the specified {@link List}.
     * Returns the {@code list} element at a random index between zero (inclusive)
     * and {@link List#size} (exclusive). This is usually an O(1) operation.
     * 
     * @param <T> the type of all elements in the {@link List}
     * @param list the {@link List} providing the elements
     * @return a random element from {@code list}
     * @throws IllegalArgumentException if {@code list} is empty
     * @throws NullPointerException if {@code list} is {@code null}
     */
    public static <T> T getAny(List<T> list) {
        try {
            return list.get(RANDOM.nextInt(list.size()));
        }
        catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("empty list", e);
        }
    }

    /**
     * Determines whether the specified {@link Integer} number is prime.
     * Performs trial divisions of the specified {@code candidate} against
     * any number between two and the square root of {@code candidate}.
     * 
     * @param candidate the {@link Integer} number to examine
     * @return {@code true} if {@code candidate} is prime, else {@code false}
     * @throws IllegalArgumentException if {@code candidate} is not positive
     */
    public static boolean isPrime(int candidate) {
        if (candidate <= 0)
            throw new IllegalArgumentException("candidate <= 0");

        if ((candidate & 1) == 0)
            return (candidate == 2);

        final int root = (int) Math.sqrt(candidate);
        for (int i = 3; i <= root; i += 2)
            if ((candidate % i) == 0)
                return false;

        return true;
    }

    /**
     * Normalizes the specified array of non-negative {@link Double} numbers.
     * Divides all {@code array} elements by the sum of all elements (which is also returned),
     * thus normalizing their values to a partitioning of the standard interval [0, 1].
     * If the sum of all elements is zero, all elements are instead set to 1.0/{@code length}
     * where {@code length} is the number of {@code array} elements.
     * 
     * @param array the array of non-negative {@link Double} numbers to normalize
     * @return the sum of all {@code array} elements before normalization
     * @throws NullPointerException if {@code array} is {@code null}
     * @throws IllegalArgumentException if {@code array} contains a negative number
     */
    public static double normalize(double[] array) {
        if (array == null)
            throw new NullPointerException("array");

        double sum = 0;
        for (double element: array) {
            if (element < 0)
                throw new IllegalArgumentException("array element < 0");

            sum += element;
        }

        if (sum != 0) {
            for (int i = 0; i < array.length; i++)
                array[i] /= sum;
        } else {
            final double value = 1.0 / array.length;
            for (int i = 0; i < array.length; i++)
                array[i] = value;
        }

        return sum;
    }

    /**
     * Normalizes the specified array of non-negative {@link Float} numbers.
     * Divides all {@code array} elements by the sum of all elements (which is also returned),
     * thus normalizing their values to a partitioning of the standard interval [0, 1].
     * If the sum of all elements is zero, all elements are instead set to 1f/{@code length}
     * where {@code length} is the number of {@code array} elements.
     * 
     * @param array the array of non-negative {@link Float} numbers to normalize
     * @return the sum of all {@code array} elements before normalization
     * @throws NullPointerException if {@code array} is {@code null}
     * @throws IllegalArgumentException if {@code array} contains a negative number
     */
    public static float normalize(float[] array) {
        if (array == null)
            throw new NullPointerException("array");

        float sum = 0;
        for (float element: array) {
            if (element < 0)
                throw new IllegalArgumentException("array element < 0");

            sum += element;
        }

        if (sum != 0) {
            for (int i = 0; i < array.length; i++)
                array[i] /= sum;
        } else {
            final float value = 1f / array.length;
            for (int i = 0; i < array.length; i++)
                array[i] = value;
        }

        return sum;
    }

    /**
     * Restricts the specified {@link Double} number to the specified range.
     * @param a the {@link Double} number to restrict
     * @param min the smallest permissible {@link Double} value for {@code a}
     * @param max the greatest permissible {@link Double} value for {@code a}
     * @return {@code min} or {@code max} if {@code a} is less or greater than
     *         {@code min} or {@code max}, respectively, otherwise {@code a} itself
     */
    public static double restrict(double a, double min, double max) {
        return (a < min ? min : (a > max ? max : a));
    }

    /**
     * Restricts the specified {@link Float} number to the specified range.
     * @param a the {@link Float} number to restrict
     * @param min the smallest permissible {@link Float} value for {@code a}
     * @param max the greatest permissible {@link Float} value for {@code a}
     * @return {@code min} or {@code max} if {@code a} is less or greater than
     *         {@code min} or {@code max}, respectively, otherwise {@code a} itself
     */
    public static float restrict(float a, float min, float max) {
        return (a < min ? min : (a > max ? max : a));
    }

    /**
     * Restricts the specified {@link Integer} number to the specified range.
     * @param a the {@link Integer} number to restrict
     * @param min the smallest permissible {@link Integer} value for {@code a}
     * @param max the greatest permissible {@link Integer} value for {@code a}
     * @return {@code min} or {@code max} if {@code a} is less or greater than
     *         {@code min} or {@code max}, respectively, otherwise {@code a} itself
     */
    public static int restrict(int a, int min, int max) {
        return (a < min ? min : (a > max ? max : a));
    }

    /**
     * Restricts the specified {@link Long} number to the specified range.
     * @param a the {@link Long} number to restrict
     * @param min the smallest permissible {@link Long} value for {@code a}
     * @param max the greatest permissible {@link Long} value for {@code a}
     * @return {@code min} or {@code max} if {@code a} is less or greater than
     *         {@code min} or {@code max}, respectively, otherwise {@code a} itself
     */
    public static long restrict(long a, long min, long max) {
        return (a < min ? min : (a > max ? max : a));
    }

    /**
     * Casts the specified {@link Double} value to {@link Integer}, throwing an exception on overflow.
     * @param value the {@link Double} value to convert to {@link Integer}
     * @return the result of casting {@code value} to {@link Integer}
     * @throws ArithmeticException if {@code value} overflows {@link Integer}
     */
    public static int toIntExact(double value) {
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE)
            throw new ArithmeticException("value <> Integer: " + value);

        return (int) value;
    }

    /**
     * Casts the specified {@link Float} value to {@link Integer}, throwing an exception on overflow.
     * @param value the {@link Float} value to convert to {@link Integer}
     * @return the result of casting {@code value} to {@link Integer}
     * @throws ArithmeticException if {@code value} overflows {@link Integer}
     */
    public static int toIntExact(float value) {
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE)
            throw new ArithmeticException("value <> Integer: " + value);

        return (int) value;
    }

    /**
     * Casts the specified {@link Double} value to {@link Long}, throwing an exception on overflow.
     * @param value the {@link Double} value to convert to {@link Long}
     * @return the result of casting {@code value} to {@link Long}
     * @throws ArithmeticException if {@code value} overflows {@link Long}
     */
    public static long toLongExact(double value) {
        if (value < Long.MIN_VALUE || value > Long.MAX_VALUE)
            throw new ArithmeticException("value <> Long: " + value);

        return (long) value;
    }

    /**
     * Casts the specified {@link Float} value to {@link Long}, throwing an exception on overflow.
     * @param value the {@link Float} value to convert to {@link Long}
     * @return the result of casting {@code value} to {@link Long}
     * @throws ArithmeticException if {@code value} overflows {@link Long}
     */
    public static long toLongExact(float value) {
        if (value < Long.MIN_VALUE || value > Long.MAX_VALUE)
            throw new ArithmeticException("value <> Long: " + value);

        return (long) value;
    }
}
