package filters;

import com.google.common.collect.ImmutableList;
import play.mvc.Http.RequestHeader;

/**
 * Absolute paths for HTTP routes that may or may not involve a user session with a profile. These
 * can be safely skipped by filters that are creating a profile when one doesn't exist.
 */
final class OptionalProfileRoutes {

  public static ImmutableList<String> routes =
      ImmutableList.of("/", "/programs", "/applicants/programs");

  public static boolean anyMatch(RequestHeader requestHeader) {
    String path = requestHeader.path();
    return routes.stream().anyMatch(path::equals);
  }

  public static boolean noneMatch(RequestHeader requestHeader) {
    return !anyMatch(requestHeader);
  }
}
