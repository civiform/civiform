package services.applicant;

import com.google.auto.value.AutoValue;

/**
 * Represents a path into the applicant JSON data. Stored as the path to data without the JSON
 * prefixes {@code applicant} or {@code metadata}.
 */
@AutoValue
public abstract class Path {
  private static final String JSON_PATH_START = "$.";
  private static final String APPLICANT_PREFIX = "applicant.";
  private static final String METADATA_PREFIX = "metadata.";

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
}
