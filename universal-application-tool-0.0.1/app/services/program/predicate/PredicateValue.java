package services.program.predicate;

import com.google.auto.value.AutoValue;

/**
 * Represents the value on the right side of a JsonPath (https://github.com/json-path/JsonPath)
 * predicate expression. This value is usually a defined constant, such as a number, string, or
 * array.
 */
@AutoValue
public abstract class PredicateValue {

  public static PredicateValue of(long value) {
    return create(String.valueOf(value));
  }

  public static PredicateValue of(String value) {
    // Escape the string value
    return create("\"" + value + "\"");
  }

  private static PredicateValue create(String value) {
    return new AutoValue_PredicateValue(value);
  }

  public abstract String value();
}
