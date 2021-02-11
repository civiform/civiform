package services.program;

import com.google.auto.value.AutoValue;

/** String representation of a predicate with variables from the core data model. */
@AutoValue
abstract class Predicate {
  static Predicate create(String expression) {
    return new AutoValue_Predicate(expression);
  }

  /**
   * JsonPath compatible predicate expression as a string.
   *
   * <p>See <a href=https://github.com/json-path/JsonPath#inline-predicates>JsonPath Predicates</a>
   */
  abstract String expression();
}
