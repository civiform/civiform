package actions;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RouteExtractor interrogates the current request URI path and route pattern allowing for a simple
 * way to get path parameters within a custom Action
 *
 * <p>The Play Framework does not currently expose URI path parameters within custom Actions. This
 * is a simple workaround originally provided on the related Github Issue on the Play Framework. <a
 * href="https://github.com/playframework/playframework/issues/2983#issuecomment-456375489">...</a>.
 *
 * <p>This is modified from the original. Created by Akinniranye James Ayodele on 8/12/16 12:56 PM.
 * Project Andromeda Version 0.1
 */
public final class RouteExtractor {
  private final String routePattern;
  private final String path;
  private final ImmutableMap<String, String> routeParameters;

  // Example: something like /$id<[^/]+>
  private static final String INDEX_PATTERN = "\\$(.+?)<\\[\\^/\\]\\+>";

  /**
   * @param routePattern Get the routePattern from an instance of {@link play.mvc.Http.Request} like
   *     this {@code request.attrs().get(Router.Attrs.HANDLER_DEF).path()}
   *     <p>Example string: {@code /foo/$id<[^/]+>/edit/$james<[^/]+>}
   * @param path The URI path value.
   *     <p>Get it from an instance of {@link play.mvc.Http.Request} {@code request.path()}
   */
  public RouteExtractor(String routePattern, String path) {

    this.routePattern = checkNotNull(routePattern);
    this.path = checkNotNull(path);
    this.routeParameters = extract();
  }

  /** Create a map containing all the matching path parameters and associated values */
  private ImmutableMap<String, String> extract() {
    Pattern pattern = Pattern.compile(this.replaceRoutePatternWithGroup());
    Matcher matcher = pattern.matcher(this.path);
    ImmutableMap.Builder<String, String> results = ImmutableMap.builder();

    if (matcher.find()) {
      this.extractPositions().forEach((key, value) -> results.put(value, matcher.group(key + 1)));
    }

    return results.build();
  }

  private String replaceRoutePatternWithGroup() {
    Pattern pattern = Pattern.compile(INDEX_PATTERN);
    Matcher matcher = pattern.matcher(this.routePattern);
    return matcher.replaceAll("([^/]+)");
  }

  private Map<Integer, String> extractPositions() {
    Pattern pattern = Pattern.compile(INDEX_PATTERN);
    Matcher matcher = pattern.matcher(this.routePattern);
    Map<Integer, String> results = new HashMap<>();

    int index = 0;
    while (matcher.find()) {
      results.put(index++, matcher.group(1));
    }
    return results;
  }

  /**
   * @param key Path parameter key name as defined in the routes file.
   * @return the given key's value converted into a long format.
   */
  public long getLongValue(String key) {
    if (!routeParameters.containsKey(key)) {
      throw new RuntimeException(String.format("Could not find '%s' in route '%s'", key, path));
    }

    try {
      return Long.parseLong(routeParameters.get(key));
    } catch (NumberFormatException ex) {
      throw new RuntimeException(
          String.format("Could parse value from '%s' in route '%s'", key, path), ex);
    }
  }

  /**
   * @return the number of parameters found
   */
  @VisibleForTesting
  int size() {
    return routeParameters.size();
  }
}
