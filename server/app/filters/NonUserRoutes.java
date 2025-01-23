package filters;

import com.google.common.collect.ImmutableList;
import play.mvc.Http.RequestHeader;

/**
 * Path prefixes and static paths for HTTP routes that may not involve a user session and can be
 * safely skipped by filters that act on the user session or account. The prefixes all should not
 * involve a session, and the programs path may or may not, but is included here to allow it not to.
 */
final class NonUserRoutes {
  public static ImmutableList<String> prefixes =
      ImmutableList.of(
          "/api/",
          "/assets/",
          "/dev/",
          "/favicon",
          "/apple-touch-icon",
          "/playIndex",
          "/metrics",
          "/logoutAllSessions");

  public static boolean anyMatch(RequestHeader requestHeader) {
    String path = requestHeader.path();
    return prefixes.stream().anyMatch(path::startsWith);
  }

  public static boolean noneMatch(RequestHeader requestHeader) {
    return !anyMatch(requestHeader);
  }
}
