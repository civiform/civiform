package services;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import services.applicant.question.Scalar;
import services.export.enums.ApiPathSegment;

/**
 * Represents a path into the applicant JSON data. Stored as the path to data without the JsonPath
 * prefix: $.
 */
@AutoValue
public abstract class Path {
  public static final String ARRAY_SUFFIX = "[]";
  private static final String JSON_PATH_START_TOKEN = "$";
  private static final Pattern ARRAY_INDEX_REGEX = Pattern.compile(".*(\\[(\\d*)])$");
  private static final int ARRAY_SUFFIX_GROUP = 1;
  private static final int ARRAY_INDEX_GROUP = 2;
  private static final char JSON_PATH_DIVIDER = '.';
  private static final String JSON_PATH_START = JSON_PATH_START_TOKEN + JSON_PATH_DIVIDER;
  private static final Splitter JSON_SPLITTER = Splitter.on(JSON_PATH_DIVIDER);
  private static final Joiner JSON_JOINER = Joiner.on(JSON_PATH_DIVIDER);

  public static Path empty() {
    return create(ImmutableList.of());
  }

  @JsonCreator
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

  /** Substitute "suffix" for "name_suffix" and return in a new path. */
  public static Path replaceSuffixPath(ImmutableList<String> segments) {
    List<String> modifiedSegments = new ArrayList<>(segments);
    String lastElement = segments.get(segments.size() - 1);
    if (lastElement.equals("name_suffix")) {
      modifiedSegments.remove(segments.size() - 1);
      modifiedSegments.add("suffix");
    }
    return new AutoValue_Path(modifiedSegments.stream().collect(ImmutableList.toImmutableList()));
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
   * Returns the JSON path compatible string representation of this path.
   *
   * <p>Example: {@code "applicant.children[2].favorite_color.text"}
   */
  @JsonValue
  @Memoized
  @Override
  public String toString() {
    return isEmpty() ? JSON_PATH_START_TOKEN : JSON_JOINER.join(segments());
  }

  /** Returns this path in JsonPath predicate format, which must start with \$. */
  @Memoized
  public String predicateFormat() {
    return JSON_PATH_START + toString();
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

  /** Returns a new path with the last segment replaced with the provided one. */
  public Path replacingLastSegment(String finalSegment) {
    return Path.create(
        ImmutableList.<String>builder()
            .addAll(segments().subList(0, segments().size() - 1))
            .add(finalSegment)
            .build());
  }

  /**
   * Append a {@link String} path to the {@link Path}.
   *
   * <p>If joining a {@link Scalar}, please use {@link Path#join(Scalar)} instead.
   *
   * @param path the path in String form to join with this path
   * @return this path joined with the provided path
   */
  public Path join(String path) {
    Path other = Path.create(path);
    return join(other);
  }

  /**
   * Append a {@link Scalar} to the {@link Path}.
   *
   * @param scalar the Scalar to join with this path
   * @return this path joined with the provided Scalar
   */
  public Path join(Scalar scalar) {
    Path other = Path.create(scalar.name());
    return join(other);
  }

  /**
   * Append a {@link ApiPathSegment} to the {@link Path}.
   *
   * @param apiPathSegment the ApiPathSegment to join with this path
   * @return this path joined with the provided ApiPathSegment
   */
  public Path join(ApiPathSegment apiPathSegment) {
    Path other = Path.create(apiPathSegment.name());
    return join(other);
  }

  /**
   * Append a {@link Path} to the {@link Path}.
   *
   * @param other the path to join with this path
   * @return this path joined with the provided path
   */
  public Path join(Path other) {
    return Path.create(
        ImmutableList.<String>builder().addAll(segments()).addAll(other.segments()).build());
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
   * Substitute "applicant" for "application" and return in a new path.
   *
   * @throws IllegalStateException if the first segment is not "applicant".
   */
  public Path asApplicationPath() {
    if (segments().isEmpty()
        || !segments().stream().findFirst().orElseThrow().equals("applicant")) {
      throw new IllegalStateException(
          "Only Paths referencing 'applicant' can be converted to application paths: " + this);
    }

    return Path.create(
        Stream.concat(Stream.of("application"), segments().stream().skip(1))
            .collect(ImmutableList.toImmutableList()));
  }

  /**
   * Returns a {@link Path} with repeated entities nested in an object under an `entities` property,
   * for use by the JSON API.
   *
   * <p>The path `one.two[3].four` is transformed to `one.two.entities[3].four`.
   *
   * @return the transformed {@link Path}
   */
  public Path asNestedEntitiesPath() {
    if (isEmpty()) {
      return this;
    }

    if (!isArrayElement()
        || (isArrayElement()
            && keyNameWithoutArrayIndex()
                .equals(Ascii.toLowerCase(ApiPathSegment.ENTITIES.name())))) {
      return parentPath().asNestedEntitiesPath().join(keyName());
    }

    return parentPath()
        .asNestedEntitiesPath()
        .join(keyNameWithoutArrayIndex())
        .join(ApiPathSegment.ENTITIES)
        .asArrayElement()
        .atIndex(arrayIndex());
  }

  /**
   * Checks whether this path is referring to an array element, e.g. {@code applicant.children[3]}.
   */
  public boolean isArrayElement() {
    return ARRAY_INDEX_REGEX.matcher(keyName()).find();
  }

  /** Returns this path as a path to an array element, e.g. {@code applicant.children[3]}. */
  public Path asArrayElement() {
    if (isArrayElement()) {
      return this;
    }
    return parentPath().join(keyName() + ARRAY_SUFFIX);
  }

  /**
   * Returns a path with a trailing array element reference stripped away. For example, {@code
   * applicant.children[2]} would return a path to {@code applicant.children}.
   *
   * <p>For paths to non-repeated entity collections, {@code IllegalStateException} is thrown.
   */
  public Path withoutArrayReference() {
    return parentPath().join(keyNameWithoutArrayIndex());
  }

  /**
   * A version of {@link #withoutArrayReference()} that doesn't throw an exception if there is not
   * an array reference.
   *
   * @return a path with the trailing array element reference stripped away.
   */
  public Path safeWithoutArrayReference() {
    return parentPath().join(stripArraySuffix(keyName()));
  }

  /**
   * Return the index of the last array element this path is referencing.
   *
   * <p>For example, a path of {@code "a.b[3].c[2].d[5]"} will return 5 because that is the array
   * index of the last array in the path.
   *
   * <p>For paths to non-array elements, {@code IllegalStateException} is thrown.
   */
  public int arrayIndex() {
    Matcher matcher = ARRAY_INDEX_REGEX.matcher(keyName());
    matcher.matches();

    try {
      return Integer.valueOf(matcher.group(ARRAY_INDEX_GROUP));

    } catch (IllegalStateException | NumberFormatException e) {
      throw new IllegalStateException(
          String.format("This path %s does not reference a repeated entity element.", this), e);
    }
  }

  /**
   * Return a new path referencing array element at the index.
   *
   * <p>For paths to non-array elements, {@code IllegalStateException} is thrown.
   */
  public Path atIndex(int index) {
    Matcher matcher = ARRAY_INDEX_REGEX.matcher(keyName());
    if (matcher.matches()) {
      return parentPath()
          .join(
              new StringBuilder(keyName())
                  .replace(
                      matcher.start(ARRAY_INDEX_GROUP),
                      matcher.end(ARRAY_INDEX_GROUP),
                      String.valueOf(index))
                  .toString());
    }
    throw new IllegalStateException(
        String.format("This path %s does not reference a repeated entity collection.", this));
  }

  /**
   * Returns true if this path starts with the other path, ignoring array index suffixes. that is,
   * "a.b[].c[].d" starts with "a.b.c".
   */
  public boolean startsWith(Path other) {
    ImmutableList<String> thisSegments =
        segments().stream().map(this::stripArraySuffix).collect(ImmutableList.toImmutableList());
    ImmutableList<String> otherSegments =
        other.segments().stream()
            .map(this::stripArraySuffix)
            .collect(ImmutableList.toImmutableList());

    // This can't start with something that is longer than it.
    if (otherSegments.size() > thisSegments.size()) {
      return false;
    }

    for (int i = 0; i < otherSegments.size(); i++) {
      if (!thisSegments.get(i).equals(otherSegments.get(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the path's key name without an array index suffix. e.g. {@code a.b[1].c[3]} returns
   * "c".
   *
   * <p>For paths to non-array elements, {@code IllegalStateException} is thrown.
   */
  private String keyNameWithoutArrayIndex() {
    return stripArraySuffix(keyName(), /* strict= */ true);
  }

  /**
   * Returns the path segment without an {@link #ARRAY_SUFFIX}.
   *
   * @param segment a path segment to strip of {@link #ARRAY_SUFFIX}
   * @param strict if true, throws an {@link IllegalArgumentException} if segment does not have
   *     anything to strip
   * @return segment, without an {@link #ARRAY_SUFFIX}
   */
  private String stripArraySuffix(String segment, boolean strict) {
    Matcher matcher = ARRAY_INDEX_REGEX.matcher(segment);
    if (matcher.matches()) {
      return new StringBuilder(segment)
          .replace(matcher.start(ARRAY_SUFFIX_GROUP), matcher.end(ARRAY_SUFFIX_GROUP), "")
          .toString();
    }

    if (strict) {
      throw new IllegalStateException(
          String.format("This path %s does not reference an array element.", this));
    }

    return segment;
  }

  /** Non-strict version of {@link #stripArraySuffix(String, boolean)} */
  private String stripArraySuffix(String segment) {
    return stripArraySuffix(segment, /* strict= */ false);
  }
}
