package services.applicant;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

/**
 * Represents a path into the applicant JSON data. Stored as the path to data without the JSON
 * prefixes {@code applicant} or {@code metadata}.
 */
@AutoValue
public abstract class Path {
  private static final String JSON_PATH_START = "$.";
  private static final String APPLICANT_PREFIX = "applicant.";
  private static final String METADATA_PREFIX = "metadata.";
  private static final Splitter JSON_SPLITTER = Splitter.on('.');
  private static final Joiner JSON_JOINER = Joiner.on('.');

  public static Path create(String path) {
    if (path.startsWith(JSON_PATH_START)) {
      path = path.substring(JSON_PATH_START.length());
    }

    if (path.startsWith(APPLICANT_PREFIX)) {
      path = path.substring(APPLICANT_PREFIX.length());
    } else if (path.startsWith(METADATA_PREFIX)) {
      path = path.substring(METADATA_PREFIX.length());
    }

    return new AutoValue_Path(path);
  }

  /**
   * A single path in JSON notation, without the $. JsonPath prefix nor the applicant or metadata
   * prefixes.
   */
  public abstract String path();

  public String withApplicantPrefix() {
    return APPLICANT_PREFIX + path();
  }

  public String withMetadataPrefix() {
    return METADATA_PREFIX + path();
  }

  @Memoized
  public ImmutableList<String> segments() {
    System.out.println("In segments!");
    return ImmutableList.copyOf(JSON_SPLITTER.splitToList(path()));
  }

  /**
   * List of JSON annotation paths for each segment in this path. For example, a Path of
   * personality.favorites.color would return [personality, personality.favorites,
   * personality.favorites.color].
   */
  @Memoized
  public ImmutableList<Path> fullPathSegments() {
    ImmutableList.Builder<Path> builder = ImmutableList.builder();
    ImmutableList.Builder<String> path = ImmutableList.builder();
    // Thoughts: use lists, then create a path constructor for ImmutableList?
    segments()
        .forEach(
            segment -> {
              path.add(segment);
              builder.add(Path.create(JSON_JOINER.join(path.build())));
            });
    return builder.build();
  }

  public ImmutableList<Path> parentSegments() {
    ImmutableList.Builder<Path> builder = ImmutableList.builder();
    if (segments().isEmpty()) {
      return builder.build();
    }

    String path = segments().get(0);
    builder.add(Path.create(path));
    for (int i = 1; i < segments().size(); i++) {
      path += '.' + segments().get(i);
      builder.add(Path.create(path));
    }

    return builder.build();
  }
}
