package filters;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import play.mvc.Http;

/**
 * Path prefixes for HTTP routes that do not involve a user session and can be safely skipped by
 * filters that act on the user session or account.
 */
enum NonUserRoutePrefixes {
  API("/api/"),
  ASSETS("/assets/"),
  DEV("/dev/"),
  FAVICON("/favicon"),
  PLAY_INDEX("/playIndex"),
  METRICS("/metrics");

  public static ImmutableList<String> ALL =
      Arrays.stream(NonUserRoutePrefixes.values())
          .map(NonUserRoutePrefixes::getPrefix)
          .collect(ImmutableList.toImmutableList());

  public static boolean anyMatch(Http.RequestHeader requestHeader) {
    return ALL.stream().anyMatch(prefix -> requestHeader.path().startsWith(prefix));
  }

  public static boolean noneMatch(Http.RequestHeader requestHeader) {
    return !anyMatch(requestHeader);
  }

  private final String prefix;

  NonUserRoutePrefixes(String prefix) {
    this.prefix = prefix;
  }

  public String getPrefix() {
    return prefix;
  }
}
