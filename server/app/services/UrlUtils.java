package services;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Contains helpers related to parsing and validating URLs. */
public final class UrlUtils {
  private UrlUtils() {}

  public static String checkIsRelativeUrl(String value) {
    if (!value.isBlank() && URI.create(value).isAbsolute()) {
      throw new RuntimeException("Invalid absolute URL. Only relative URLs are allowed.");
    }
    return value;
  }

  /**
   * Checks URL for basic validity. Among other things, this method requires that the URL starts
   * with a valid protocol such as "http" or "https". For example, "http://foo.gov" is valid.
   * "www.foo.gov" is not valid.
   *
   * @param url The URL to check
   * @return True if the URL is valid, false otherwise.
   */
  public static boolean isValid(String url) {
    try {
      // Ensure it's a valid URL and a valid URI
      new URL(url).toURI();
      return true;
    } catch (MalformedURLException | URISyntaxException e) {
      return false;
    }
  }

  public static Optional<String> safelyEncodeURL(String url) {
    if (!isValid(url)) {
      return Optional.empty();
    }

    URI uri;
    try {
      uri = new URL(url).toURI();
    } catch (MalformedURLException | URISyntaxException e) {
      // Since we check isValid() above, this should never happen
      return Optional.empty();
    }

    // Encoding "http://" breaks URLs. Deconstruct the URL.
    String scheme = uri.getScheme();
    String host = uri.getHost();
    String authority = uri.getAuthority();
    System.out.println("host: " + host);
    String path = uri.getPath() == null ? "" : uri.getPath();
    System.out.println("path: " + path);
    String query = uri.getQuery() == null ? "" : uri.getQuery();
    // uri.getQuery

    // This breaks URLs such as
    // "https://en.wikipedia.org/w/index.php?search=aobeusaeou&title=Special%3ASearch&ns0=1"
    String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

    /*
     * // Remove the scheme + "://"
     * String urlWithoutScheme = url.substring(scheme.length() + 3);
     *
     * System.out.println("ssandbekkhaug scheme: " + scheme);
     * System.out.println("ssandbekkhaug urlWithoutScheme: " + urlWithoutScheme);
     *
     * String encodedUrlWithoutScheme = URLEncoder.encode(urlWithoutScheme,
     * StandardCharsets.UTF_8);
     *
     * System.out.println("ssandbekkhaug encodedUrlWithoutScheme: " +
     * encodedUrlWithoutScheme);
     */

    // String reconstructedUrl = scheme + "://" + host + path + encodedQuery;
    URI reconstructedUri;
    try {
      reconstructedUri = new URI(scheme, authority, path, encodedQuery, uri.getFragment());
    } catch (URISyntaxException e) {
      return Optional.empty();
    }
    return Optional.of(reconstructedUri.toASCIIString());
  }
}
