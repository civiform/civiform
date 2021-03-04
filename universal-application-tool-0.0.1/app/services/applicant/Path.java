package services.applicant;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;

/**
 * Represents a path into the applicant JSON data. Stored as the path to data without the JsonPath
 * prefix {@code $.}.
 */
@AutoValue
public abstract class Path {
  private static final char JSON_PATH_DIVIDER = '.';
  private static final String JSON_PATH_START = "$" + JSON_PATH_DIVIDER;
  private static final Splitter JSON_SPLITTER = Splitter.on(JSON_PATH_DIVIDER);
  private static final Joiner JSON_JOINER = Joiner.on(JSON_PATH_DIVIDER);

  public static Path create(String path) {
    if (path.startsWith(JSON_PATH_START)) {
      path = path.substring(JSON_PATH_START.length());
    }

    return new AutoValue_Path(path);
  }

  private static Path create(ImmutableList<String> pathSegments) {
    return new AutoValue_Path(JSON_JOINER.join(pathSegments));
  }

  /**
   * A single path in JSON notation, without the $. JsonPath prefix nor the applicant or metadata
   * prefixes.
   */
  public abstract String path();

  @Memoized
  public Path parentPath() {
    if (segments().isEmpty()) {
      return Path.create("");
    }
    return Path.create(segments().subList(0, segments().size() - 1));
  }

  @Memoized
  public String keyName() {
    if (segments().isEmpty()) {
      return "";
    }
    return segments().get(segments().size() - 1);
  }

  @Memoized
  public ImmutableList<String> segments() {
    if (path().isEmpty()) {
      return ImmutableList.of();
    }
    return ImmutableList.copyOf(JSON_SPLITTER.splitToList(path()));
  }

  /**
   * List of JSON annotation paths for each segment of the parent path. For example, a Path of
   * personality.favorites.color.blue would return [personality, personality.favorites,
   * personality.favorites.color].
   */
  @Memoized
  public ImmutableList<Path> parentPaths() {
    ArrayList<Path> parentPaths = new ArrayList<>();
    Path currentPath = parentPath();

    while (!currentPath.path().isEmpty()) {
      parentPaths.add(0, currentPath);
      currentPath = currentPath.parentPath();
    }

    return ImmutableList.copyOf(parentPaths);
  }
}
