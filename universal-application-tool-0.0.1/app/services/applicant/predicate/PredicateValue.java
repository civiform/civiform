package services.applicant.predicate;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PredicateValue {

  public static PredicateValue of(long value) {
    return create(String.valueOf(value));
  }

  public static PredicateValue of(String value) {
    // Escape the string value
    return create("'" + value + "'");
  }

  private static PredicateValue create(String value) {
    return new AutoValue_PredicateValue(value);
  }

  public abstract String value();
}
