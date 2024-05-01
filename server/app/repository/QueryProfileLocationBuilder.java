package repository;

import io.ebean.ProfileLocation;

/** Utility class for creating ProfileLocation objects. */
public final class QueryProfileLocationBuilder {
  private final String fileName;

  /**
   * Initializes the QueryUtils with a file name.
   *
   * @param fileName The file name to be used in ProfileLocation.
   */
  public QueryProfileLocationBuilder(String fileName) {
    this.fileName = fileName;
  }

  /**
   * Create a ProfileLocation based on the provided function name.
   *
   * @param functionName The name of the function.
   * @return ProfileLocation created using the provided file name and function name.
   */
  ProfileLocation create(String functionName) {
    return ProfileLocation.createAt(fileName + "." + functionName);
  }
}
