package services.applicant.predicate;

import com.google.auto.value.AutoValue;

/** Holds a JsonPath (https://github.com/json-path/JsonPath) path containing a query predicate. */
@AutoValue
public abstract class JsonPathPredicate {

  /** String representation of the JsonPath query string. */
  public abstract String pathPredicate();

  public static JsonPathPredicate create(String pathPredicate) {
    return new AutoValue_JsonPathPredicate(pathPredicate);
  }
}
