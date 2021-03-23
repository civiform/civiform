package services;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

/**
 * Represents a path into the applicant JSON data. Stored as the path to data without the JsonPath
 * prefix: $.
 */
@AutoValue
public abstract class Path {
  private static final char JSON_PATH_DIVIDER = '.';
  private static final String JSON_PATH_START = "$";
  private static final Splitter JSON_SPLITTER = Splitter.on(JSON_PATH_DIVIDER);
  private static final Joiner JSON_JOINER = Joiner.on(JSON_PATH_DIVIDER);

  public static Path empty() {
    return create(ImmutableList.of(JSON_PATH_START));
  }

  public static Path create(String path) {
    path = path.trim();
    if (path.isEmpty()) {
      return empty();
    }
    if (!path.startsWith(JSON_PATH_START)) {
      path = JSON_PATH_START + JSON_PATH_DIVIDER + path;
    }
    return create(ImmutableList.copyOf(JSON_SPLITTER.splitToList(path)));
  }

  private static Path create(ImmutableList<String> pathSegments) {
    return new AutoValue_Path(pathSegments);
  }

  /**
   * The list of path segments. A path {@code applicant.favorites.color} would return ["applicant",
   * "favorites", "color"].
   */
  public abstract ImmutableList<String> segments();

  public boolean isEmpty() {
    return segments().size() == 1 && segments().get(0).equals(JSON_PATH_START);
  }

  /** A single path in JSON notation, without the $. JsonPath prefix. */
  @Memoized
  public String path() {
    return JSON_JOINER.join(segments());
  }

  @Memoized
  @Override
  public String toString() {
    return path();
  }

  /**
   * The {@link Path} of the parent. For example, a path {@code applicant.favorites.color} would
   * return {@code applicant.favorites}.
   */
  @Memoized
  public Path parentPath() {
    if (isEmpty()) {
      return Path.empty();
    }
    return Path.create(segments().subList(0, segments().size() - 1));
  }

  /**
   * The last segment in this path. For example, a path {@code applicant.favorites.color} would
   * return "color".
   */
  @Memoized
  public String keyName() {
    if (segments().isEmpty()) {
      return "";
    }
    return segments().get(segments().size() - 1);
  }

  public abstract Builder toBuilder();

  public static Builder builder() {
    return new AutoValue_Path.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    abstract Builder setSegments(ImmutableList<String> segments);

    public abstract ImmutableList.Builder<String> segmentsBuilder();

    public abstract Path build();

    public Builder setPath(String path) {
      ImmutableList<String> newSegments =
          ImmutableList.<String>builder()
              .add(JSON_PATH_START)
              .addAll(JSON_SPLITTER.splitToList(path))
              .build();
      return setSegments(newSegments);
    }

    public Builder append(String segment) {
      segmentsBuilder().add(segment.trim());
      return this;
    }
  }
}
