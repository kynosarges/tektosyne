package org.kynosarges.tektosyne;

/**
 * Provides functions defined by the Fortran 90 standard.
 * {@link Fortran} supplements the standard {@link Math} class with methods that mimic selected
 * Fortran 90 functions. Overloads for various numeric types are provided to avoid type casting.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class Fortran {
    /**
     * Creates a {@link Fortran} instance.
     * Private to prevent instantiation.
     */
    private Fortran() { }

    /**
     * Returns the whole {@link Double} number nearest the specified number, rounded towards zero.
     * Returns the result of the standard method {@link Math#floor} or {@link Math#ceil},
     * depending on the sign of {@code n}.
     * 
     * @param n the {@link Double} number to round
     * @return the whole {@link Double} number nearest {@code n} whose
     *         absolute value is less than or equal to {@code n}
     */
    public static double aint(double n) {
        return (n >= 0 ? Math.floor(n) : Math.ceil(n));
    }

    /**
     * Returns the whole {@link Float} number nearest the specified number, rounded towards zero.
     * Returns the result of the standard method {@link Math#floor} or {@link Math#ceil},
     * depending on the sign of {@code n}.
     * 
     * @param n the {@link Float} number to round
     * @return the whole {@link Float} number nearest {@code n} whose
     *         absolute value is less than or equal to {@code n}
     */
    public static float aint(float n) {
        return (float) (n >= 0 ? Math.floor(n) : Math.ceil(n));
    }

    /**
     * Returns the {@link Double} number nearest the specified number, using standard rounding.
     * Always rounds numbers midway between two integral values away from zero.
     * 
     * @param n the {@link Double} number to round
     * @return the whole {@link Double} number nearest to {@code n}
     */
    public static double anint(double n) {
        if (n > 0)
            return Math.floor(n + 0.5);
        else if (n < 0)
            return Math.ceil(n - 0.5);
        else
            return 0;
    }

    /**
     * Returns the {@link Float} number nearest the specified number, using standard rounding.
     * Always rounds numbers midway between two integral values away from zero.
     * 
     * @param n the {@link Float} number to round
     * @return the whole {@link Float} number nearest to {@code n}
     */
    public static float anint(float n) {
        if (n > 0)
            return (float) Math.floor(n + 0.5f);
        else if (n < 0)
            return (float) Math.ceil(n - 0.5f);
        else
            return 0;
    }

    /**
     * Returns the {@link Integer} nearest the specified {@link Double} number,
     * rounded towards positive infinity.
     * Returns the result of the standard {@link Math#ceil} method, cast to {@link Integer}.
     * 
     * @param n the {@link Double} number to round
     * @return the {@link Integer} number nearest to {@code n}
     * @throws ArithmeticException if the rounded result overflows {@link Integer}
     */
    public static int ceiling(double n) {
        return MathUtils.toIntExact(Math.ceil(n));
    }

    /**
     * Returns the {@link Integer} nearest the specified {@link Float} number,
     * rounded towards positive infinity.
     * Returns the result of the standard {@link Math#ceil} method, cast to {@link Integer}.
     * 
     * @param n the {@link Float} number to round
     * @return the {@link Integer} number nearest to {@code n}
     * @throws ArithmeticException if the rounded result overflows {@link Integer}
     */
    public static int ceiling(float n) {
        return MathUtils.toIntExact(Math.ceil(n));
    }

    /**
     * Returns the {@link Integer} nearest the specified {@link Double} number,
     * rounded towards negative infinity.
     * Returns the result of the standard {@link Math#floor} method, cast to {@link Integer}.
     * 
     * @param n the {@link Double} number to round
     * @return the {@link Integer} number nearest to {@code n}
     * @throws ArithmeticException if the rounded result overflows {@link Integer}
     */
    public static int floor(double n) {
        return MathUtils.toIntExact(Math.floor(n));
    }

    /**
     * Returns the {@link Integer} nearest the specified {@link Float} number,
     * rounded towards negative infinity.
     * Returns the result of the standard {@link Math#floor} method, cast to {@link Integer}.
     * 
     * @param n the {@link Float} number to round
     * @return the {@link Integer} number nearest to {@code n}
     * @throws ArithmeticException if the rounded result overflows {@link Integer}
     */
    public static int floor(float n) {
        return MathUtils.toIntExact(Math.floor(n));
    }

    /**
     * Returns the {@link Long} number nearest the specified {@link Double}, using standard rounding.
     * Equivalent to casting the result of {@link #anint(double)} to {@link Long}.
     * 
     * @param n the {@link Double} number to round
     * @return the {@link Long} number nearest to {@code n}
     * @throws ArithmeticException if the rounded result overflows {@link Long}
     */
    public static long knint(double n) {
        if (n > 0.0)
            return MathUtils.toLongExact(n + 0.5);
        else if (n < 0.0)
            return MathUtils.toLongExact(n - 0.5);
        else
            return 0;
    }

    /**
     * Returns the {@link Long} number nearest the specified {@link Float}, using standard rounding.
     * Equivalent to casting the result of {@link #anint(float)} to {@link Long}.
     * 
     * @param n the {@link Float} number to round
     * @return the {@link Long} number nearest to {@code n}
     * @throws ArithmeticException if the rounded result overflows {@link Long}
     */
    public static long knint(float n) {
        if (n > 0)
            return MathUtils.toLongExact(n + 0.5f);
        else if (n < 0)
            return MathUtils.toLongExact(n - 0.5f);
        else
            return 0;
    }

    /**
     * Returns the largest of the specified {@link Double} numbers.
     * Returns {@link Double#MIN_VALUE} if no arguments are supplied.
     * 
     * @param args the {@link Double} numbers to compare with each other.
     * @return the largest {@link Double} number found among {@code args}
     * @throws NullPointerException if {@code args} is {@code null}
     */
    public static double max(double... args) {
        double max = Double.MIN_VALUE;
        for (double n: args)
            if (n > max) max = n;

        return max;
    }

    /**
     * Returns the largest of the specified {@link Float} numbers.
     * Returns {@link Float#MIN_VALUE} if no arguments are supplied.
     * 
     * @param args the {@link Float} numbers to compare with each other.
     * @return the largest {@link Float} number found among {@code args}
     * @throws NullPointerException if {@code args} is {@code null}
     */
    public static float max(float... args) {
        float max = Float.MIN_VALUE;
        for (float n: args)
            if (n > max) max = n;

        return max;
    }

    /**
     * Returns the largest of the specified {@link Integer} numbers.
     * Returns {@link Integer#MIN_VALUE} if no arguments are supplied.
     * 
     * @param args the {@link Integer} numbers to compare with each other.
     * @return the largest {@link Integer} number found among {@code args}
     * @throws NullPointerException if {@code args} is {@code null}
     */
    public static int max(int... args) {
        int max = Integer.MIN_VALUE;
        for (int n: args)
            if (n > max) max = n;

        return max;
    }

    /**
     * Returns the largest of the specified {@link Long} numbers.
     * Returns {@link Long#MIN_VALUE} if no arguments are supplied.
     * 
     * @param args the {@link Long} numbers to compare with each other.
     * @return the largest {@link Long} number found among {@code args}
     * @throws NullPointerException if {@code args} is {@code null}
     */
    public static long max(long... args) {
        long max = Long.MIN_VALUE;
        for (long n: args)
            if (n > max) max = n;

        return max;
    }

    /**
     * Returns the smallest of the specified {@link Double} numbers.
     * Returns {@link Double#MAX_VALUE} if no arguments are supplied.
     * 
     * @param args the {@link Double} numbers to compare with each other.
     * @return the smallest {@link Double} number found among {@code args}
     * @throws NullPointerException if {@code args} is {@code null}
     */
    public static double min(double... args) {
        double min = Double.MAX_VALUE;
        for (double n: args)
            if (n < min) min = n;

        return min;
    }

    /**
     * Returns the smallest of the specified {@link Float} numbers.
     * Returns {@link Float#MAX_VALUE} if no arguments are supplied.
     * 
     * @param args the {@link Float} numbers to compare with each other.
     * @return the smallest {@link Float} number found among {@code args}
     * @throws NullPointerException if {@code args} is {@code null}
     */
    public static float min(float... args) {
        float min = Float.MAX_VALUE;
        for (float n: args)
            if (n < min) min = n;

        return min;
    }

    /**
     * Returns the smallest of the specified {@link Integer} numbers.
     * Returns {@link Integer#MAX_VALUE} if no arguments are supplied.
     * 
     * @param args the {@link Integer} numbers to compare with each other.
     * @return the smallest {@link Integer} number found among {@code args}
     * @throws NullPointerException if {@code args} is {@code null}
     */
    public static int min(int... args) {
        int min = Integer.MAX_VALUE;
        for (int n: args)
            if (n < min) min = n;

        return min;
    }

    /**
     * Returns the smallest of the specified {@link Long} numbers.
     * Returns {@link Long#MAX_VALUE} if no arguments are supplied.
     * 
     * @param args the {@link Long} numbers to compare with each other.
     * @return the smallest {@link Long} number found among {@code args}
     * @throws NullPointerException if {@code args} is {@code null}
     */
    public static long min(long... args) {
        long min = Long.MAX_VALUE;
        for (long n: args)
            if (n < min) min = n;

        return min;
    }

    /**
     * Returns the first {@link Double} number modulo the second number.
     * Provides the {@link Double} equivalent of the standard {@link Math#floorMod} method.
     * See there for details.
     * 
     * @param a the {@link Double} number indicating the dividend
     * @param p the {@link Double} number indicating the divisor
     * @return the {@link Double} number that equals {@code a} modulo {@code p}
     * @throws ArithmeticException if {@code p} is zero or the intermediate {@link #floor(double)}
     *                             result overflows {@link Integer}
     */
    public static double modulo(double a, double p) {
        if (p == 0)
            throw new ArithmeticException("p == 0");

        return a - floor(a / p) * p;
    }

    /**
     * Returns the first {@link Float} number modulo the second number.
     * Provides the {@link Float} equivalent of the standard {@link Math#floorMod} method.
     * See there for details.
     * 
     * @param a the {@link Float} number indicating the dividend
     * @param p the {@link Float} number indicating the divisor
     * @return the {@link Float} number that equals {@code a} modulo {@code p}
     * @throws ArithmeticException if {@code p} is zero or the intermediate {@link #floor(float)}
     *                             result overflows {@link Integer}
     */
    public static float modulo(float a, float p) {
        if (p == 0)
            throw new ArithmeticException("p == 0");

        return a - floor(a / p) * p;
    }

    /**
     * Returns the first {@link Integer} number modulo the second number.
     * Returns the result of the standard {@link Math#floorMod} method. See there for details.
     * 
     * @param a the {@link Integer} number indicating the dividend
     * @param p the {@link Integer} number indicating the divisor
     * @return the {@link Integer} number that equals {@code a} modulo {@code p}
     * @throws ArithmeticException if {@code p} is zero
     */
    public static int modulo(int a, int p) {
        return Math.floorMod(a, p);
    }

    /**
     * Returns the first {@link Long} number modulo the second number.
     * Returns the result of the standard {@link Math#floorMod} method. See there for details.
     * 
     * @param a the {@link Long} number indicating the dividend
     * @param p the {@link Long} number indicating the divisor
     * @return the {@link Long} number that equals {@code a} modulo {@code p}
     * @throws ArithmeticException if {@code p} is zero
     */
    public static long modulo(long a, long p) {
        return Math.floorMod(a, p);
    }

    /**
     * Returns the {@link Integer} number nearest the specified {@link Double}, using standard rounding.
     * Equivalent to casting the result of {@link #anint(double)} to {@link Integer}.
     * 
     * @param n the {@link Double} number to round
     * @return the {@link Integer} number nearest to {@code n}
     * @throws ArithmeticException if the rounded result overflows {@link Integer}
     */
    public static int nint(double n) {
        if (n > 0.0)
            return MathUtils.toIntExact(n + 0.5);
        else if (n < 0.0)
            return MathUtils.toIntExact(n - 0.5);
        else
            return 0;
    }

    /**
     * Returns the {@link Integer} number nearest the specified {@link Float}, using standard rounding.
     * Equivalent to casting the result of {@link #anint(float)} to {@link Integer}.
     * 
     * @param n the {@link Float} number to round
     * @return the {@link Integer} number nearest to {@code n}
     * @throws ArithmeticException if the rounded result overflows {@link Integer}
     */
    public static int nint(float n) {
        if (n > 0)
            return MathUtils.toIntExact(n + 0.5f);
        else if (n < 0)
            return MathUtils.toIntExact(n - 0.5f);
        else
            return 0;
    }

    /**
     * Returns the sum of the specified {@link Double} numbers.
     * Returns zero if no arguments are supplied.
     * 
     * @param args the {@link Double} numbers to sum up.
     * @return the {@link Double} number that is the sum of all {@code args}
     * @throws NullPointerException if {@code args} is {@code null}
     */
    public static double sum(double... args) {
        double sum = 0;
        for (double n: args)
            sum += n;

        return sum;
    }

    /**
     * Returns the sum of the specified {@link Float} numbers.
     * Returns zero if no arguments are supplied.
     * 
     * @param args the {@link Float} numbers to sum up.
     * @return the {@link Float} number that is the sum of all {@code args}
     * @throws NullPointerException if {@code args} is {@code null}
     */
    public static float sum(float... args) {
        float sum = 0;
        for (float n: args)
            sum += n;

        return sum;
    }

    /**
     * Returns the sum of the specified {@link Integer} numbers.
     * Returns zero if no arguments are supplied.
     * 
     * @param args the {@link Integer} numbers to sum up.
     * @return the {@link Integer} number that is the sum of all {@code args}
     * @throws ArithmeticException if the sum overflows {@link Integer} at any point
     * @throws NullPointerException if {@code args} is {@code null}
     */
    public static int sum(int... args) {
        int sum = 0;
        for (int n: args)
            sum = Math.addExact(sum, n);

        return sum;
    }

    /**
     * Returns the sum of the specified {@link Long} numbers.
     * Returns zero if no arguments are supplied.
     * 
     * @param args the {@link Long} numbers to sum up.
     * @return the {@link Long} number that is the sum of all {@code args}
     * @throws ArithmeticException if the sum overflows {@link Long} at any point
     * @throws NullPointerException if {@code args} is {@code null}
     */
    public static long sum(long... args) {
        long sum = 0;
        for (long n: args)
            sum = Math.addExact(sum, n);

        return sum;
    }
}
