package services;

import static com.google.common.base.Preconditions.checkArgument;

import org.apache.commons.text.RandomStringGenerator;

public final class RandomStringUtils {
  /**
   * Creates a random string whose length is the number of characters specified.
   *
   * <p>Characters will be chosen from the set of Latin alphabetic characters (a-z, A-Z).
   *
   * @param length the length of random string to create
   * @return the random string
   * @throws IllegalArgumentException if {@code length < 0}
   */
  public static String randomAlphabetic(final int length) {
    checkArgument(length >= 0);
    final char[][] pairs = {{'a', 'z'}, {'A', 'Z'}};
    var generator = new RandomStringGenerator.Builder().withinRange(pairs).get();
    return generator.generate(length);
  }

  /**
   * Creates a random string whose length is the number of characters specified.
   *
   * <p>Characters will be chosen from the set of Latin alphabetic characters (a-z, A-Z) and the
   * digits 0-9.
   *
   * @param length the length of random string to create
   * @return the random string
   * @throws IllegalArgumentException if {@code length < 0}
   */
  public static String randomAlphanumeric(final int length) {
    checkArgument(length >= 0);
    final char[][] pairs = {{'a', 'z'}, {'A', 'Z'}, {'0', '9'}};
    var generator = new RandomStringGenerator.Builder().withinRange(pairs).get();
    return generator.generate(length);
  }
}
