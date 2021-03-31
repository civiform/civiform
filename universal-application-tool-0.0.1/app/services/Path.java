package services;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a path into the applicant JSON data. Stored as the path to data without the JsonPath
 * prefix: $.
 */
@AutoValue
public abstract class Path {
  public static final String ARRAY_SUFFIX = "[]";
  private static final String JSON_PATH_START_TOKEN = "$";
  private static final Pattern ARRAY_INDEX_REGEX = Pattern.compile(".*(\\[(\\d+)])$");
  private static final char JSON_PATH_DIVIDER = '.';
  private static final String JSON_PATH_START = JSON_PATH_START_TOKEN + JSON_PATH_DIVIDER;
  private static final Splitter JSON_SPLITTER = Splitter.on(JSON_PATH_DIVIDER);
  private static final Joiner JSON_JOINER = Joiner.on(JSON_PATH_DIVIDER);

  public static Path empty() {
    return create(ImmutableList.of());
  }

  public static Path create(String path) {
    path = path.trim();
    if (path.startsWith(JSON_PATH_START)) {
      path = path.substring(JSON_PATH_START.length());
    }
    if (path.isEmpty()) {
      return empty();
    }
    return create(ImmutableList.copyOf(JSON_SPLITTER.splitToList(path)));
  }

  private static Path create(ImmutableList<String> segments) {
    return new AutoValue_Path(
        segments.stream().map(String::toLowerCase).collect(ImmutableList.toImmutableList()));
  }

  /**
   * The list of path segments. A path {@code applicant.favorites.color} would return ["applicant",
   * "favorites", "color"].
   */
  public abstract ImmutableList<String> segments();

  @Memoized
  public boolean isEmpty() {
    return segments().isEmpty();
  }

  /**
   * A single path in JSON notation, without the $. JsonPath prefix.
   *
   * <p>TODO: get rid of this method. Methods should use {@link Path} or {@link #toString()}.
   */
  @Memoized
  public String path() {
    return toString();
  }

  /**
   * Returns the JSON path compatible string representation of this path.
   *
   * <p>Example: {@code "applicant.children[2].favorite_color.text"}
   */
  @Memoized
  @Override
  public String toString() {
    return isEmpty() ? JSON_PATH_START_TOKEN : JSON_JOINER.join(segments());
  }

  /**
   * The {@link Path} of the parent. For example, a path {@code applicant.favorite_color.text} would
   * return {@code applicant.favorite_color}.
   */
  @Memoized
  public Path parentPath() {
    if (segments().isEmpty()) {
      return Path.empty();
    }
    return Path.create(segments().subList(0, segments().size() - 1));
  }

  /**
   * Append a segment to the path.
   *
   * <p>TODO(#638): refactor things that use `toBuilder().append(seg).build()` with {@link
   * #join(String)};
   */
  public Path join(String segment) {
    return Path.create(ImmutableList.<String>builder().addAll(segments()).add(segment).build());
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

  /**
   * Checks whether this path is referring to an array element, e.g. {@code applicant.children[3]}.
   */
  public boolean isArrayElement() {
    return ARRAY_INDEX_REGEX.matcher(keyName()).find();
  }

  /**
   * Returns a path with a trailing array element reference stripped away. For example, {@code
   * applicant.children[2]} would return a path to {@code applicant.children}.
   *
   * <p>For paths to non-array elements, the path returned is the same as the original.
   */
  public Path withoutArrayReference() {
    return parentPath().join(keyNameWithoutArrayIndex());
  }

  /**
   * Return the index of the array element this path is referencing. If this path is not referencing
   * an array element, an empty optional is returned.
   */
  public Optional<Integer> arrayIndex() {
    Matcher matcher = ARRAY_INDEX_REGEX.matcher(keyName());
    if (matcher.matches()) {
      return Optional.of(Integer.valueOf(matcher.group(2)));
    }
    return Optional.empty();
  }

  /**
   * Return a new path referencing array element at the index.
   *
   * <p>If this path is not referencing an array element, then the path returned is the same as the
   * original.
   */
  public Path atIndex(int index) {
    Matcher matcher = ARRAY_INDEX_REGEX.matcher(keyName());
    String newKeyName = keyName();
    if (matcher.matches()) {
      newKeyName =
          new StringBuilder(keyName())
              .replace(matcher.start(2), matcher.end(2), String.valueOf(index))
              .toString();
    }
    return parentPath().join(newKeyName);
  }

  /**
   * Returns the path's key name without an array index suffix.
   *
   * <p>For paths to non-array elements, this is the same as {@link #keyName()}. For paths to array
   * elements {@code a.b[1].c[3]}, the key without the array index suffix is returned: "c".
   */
  public String keyNameWithoutArrayIndex() {
    Matcher matcher = ARRAY_INDEX_REGEX.matcher(keyName());
    String newKeyName = keyName();
    if (matcher.matches()) {
      newKeyName =
          new StringBuilder(keyName()).replace(matcher.start(1), matcher.end(1), "").toString();
    }
    return newKeyName;
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
      return setSegments(ImmutableList.copyOf(JSON_SPLITTER.splitToList(path)));
    }

    public Builder append(String segment) {
      segmentsBuilder().add(segment.trim());
      return this;
    }
  }
}
