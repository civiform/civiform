package helpers;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.StringJoiner;

/** Wrapper around {@link StringJoiner} with helpers. */
public final class BetterStringJoiner {
  private final StringJoiner joiner;

  /**
   * Creates a StringJoiner with a space delimiter. This is the same as doing {@code new
   * BetterStringJoiner(" "); }
   */
  public static BetterStringJoiner withSpaceDelimiter() {
    return new BetterStringJoiner(" ");
  }

  /**
   * Creates a StringJoiner with a space delimiter. This is the same as doing {@code new
   * BetterStringJoiner("\n"); }
   */
  public static BetterStringJoiner withNewlineDelimiter() {
    return new BetterStringJoiner("\n");
  }

  public BetterStringJoiner(CharSequence delimiter) {
    this.joiner = new StringJoiner(delimiter);
  }

  public BetterStringJoiner(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
    this.joiner = new StringJoiner(delimiter, prefix, suffix);
  }

  /** Add the string to the StringJoiner if the value is not null or blank */
  public BetterStringJoiner addIfNotBlank(String value) {
    if (isNotBlank(value)) {
      joiner.add(value);
    }
    return this;
  }

  // Only wrappers around the base joiner below here

  /** Add the string to the StringJoiner if the value is not null or blank */
  public BetterStringJoiner addIf(boolean condition, String value) {
    if (condition) {
      joiner.add(value);
    }
    return this;
  }

  /** Adds a copy of the given StringJoiner without prefix and suffix as the next element */
  public BetterStringJoiner add(CharSequence newElement) {
    joiner.add(newElement);
    return this;
  }

  /** Adds the contents of the given StringJoiner without prefix and suffix as the next element */
  public BetterStringJoiner merge(StringJoiner other) {
    joiner.merge(other);
    return this;
  }

  /**
   * Adds the contents of the given BetterStringJoiner without prefix and suffix as the next element
   */
  public BetterStringJoiner merge(BetterStringJoiner other) {
    joiner.merge(other.joiner);
    return this;
  }

  /** Returns the length of the String representation of this StringJoiner */
  public int length() {
    return joiner.length();
  }

  /**
   * Sets the sequence of characters to be used when determining the string representation of this
   * StringJoiner and no elements have been added yet
   */
  public BetterStringJoiner setEmptyValue(CharSequence emptyValue) {
    joiner.setEmptyValue(emptyValue);
    return this;
  }

  @Override
  public String toString() {
    return joiner.toString();
  }
}
