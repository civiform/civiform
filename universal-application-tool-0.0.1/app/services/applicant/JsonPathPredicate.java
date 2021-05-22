package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

/** Holds a JsonPath (https://github.com/json-path/JsonPath) path containing a query predicate. */
public class JsonPathPredicate {

  private final String pathPredicate;

  private JsonPathPredicate(String pathPredicate) {
    this.pathPredicate = checkNotNull(pathPredicate);
  }

  public static JsonPathPredicate create(String pathPredicate) {
    return new JsonPathPredicate(pathPredicate);
  }

  /** Get the string version of the JSON query path. */
  public String getPathPredicate() {
    return pathPredicate;
  }
}
