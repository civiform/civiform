package services;

import java.net.URI;

/** Contains helpers related to parsing and validating URLs. */
public final class UrlUtils {
  private UrlUtils() {}

  public static String ensureRelativeUrlOrThrow(String value) {
    if (!value.isBlank() && URI.create(value).isAbsolute()) {
      throw new RuntimeException("Invalid absolute URL. Only relative URLs are allowed.");
    }
    return value;
  }
}
