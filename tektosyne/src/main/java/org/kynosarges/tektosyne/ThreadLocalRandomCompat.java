package org.kynosarges.tektosyne;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Provide {@link java.util.concurrent.ThreadLocalRandom} compatibility for Java 1.7.
 */
public final class ThreadLocalRandomCompat {

  private static ThreadLocal<Random> RANDOM_INSTANCE = new ThreadLocal<Random>() {
    @Override
    public Random get() {
      return new SecureRandom();
    }
  };

  /**
   * Compatible replacement for {@link ThreadLocalRandom#current()}.
   */
  public static Random current() {
    return RANDOM_INSTANCE.get();
  }

}
