package filters;

import com.google.common.collect.ImmutableList;
import play.mvc.Http;

/**
 * Path prefixes and static paths for HTTP routes that may not involve a user session and can be
 * safely skipped by filters that act on the user session or account. The prefixes all should not
 * involve a session, and the programs path may or may not, but is included here to allow it not to.
 */
final class NonUserRoutes {
  public static ImmutableList<String> prefixes =
      ImmutableList.of(
          "/api/", "/assets/", "/dev/", "/favicon", "/apple-touch-icon", "/playIndex", "/metrics");

  public static ImmutableList<String> staticRoutes = ImmutableList.of("/", "/programs");

  public static boolean anyMatch(Http.RequestHeader requestHeader) {
    // Because both / and /programs are prefixes to routes where we DO want a user present,
    // we check for these paths specifically as well.
    String path = requestHeader.path();
    return prefixes.stream().anyMatch(path::startsWith)
        || staticRoutes.stream().anyMatch(path::equals);
  }

  public static boolean noneMatch(Http.RequestHeader requestHeader) {
    return !anyMatch(requestHeader);
  }
}
