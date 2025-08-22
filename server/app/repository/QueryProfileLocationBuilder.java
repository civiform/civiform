package repository;

import io.ebean.ProfileLocation;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Utility class for creating ProfileLocation objects. */
public final class QueryProfileLocationBuilder {
  private final String fileName;
  private final ConcurrentMap<String, ProfileLocation> cache = new ConcurrentHashMap<>();

  /**
   * Initializes the QueryUtils with a file name.
   *
   * @param fileName The file name to be used in ProfileLocation.
   */
  public QueryProfileLocationBuilder(String fileName) {
    this.fileName = fileName;
  }

  /**
   * Create a ProfileLocation based on the provided function name, or get the ProfileLocation from
   * the cache if present.
   *
   * @param functionName The name of the function.
   * @return ProfileLocation created using the provided file name and function name.
   */
  ProfileLocation create(String functionName) {
    return cache.computeIfAbsent(functionName, fn -> ProfileLocation.create(fileName + "." + fn));
  }
}
