package support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads conf/routes for tests that check how routes are set up. */
public final class RoutesFile {

  private static final Pattern ROUTE_LINE =
      Pattern.compile("^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\\s+(\\S+)\\s+([^\\s(]+)");

  private RoutesFile() {}

  /** A route. {@code handler} is the controller method without its parameters. */
  public record Route(String method, String path, String handler) {
    /** Returns "METHOD /path handler". */
    @Override
    public String toString() {
      return method + " " + path + " " + handler;
    }
  }

  /** Returns the lines of conf/routes. Tests run from the server directory. */
  public static List<String> readLines() throws IOException {
    return Files.readAllLines(Path.of("conf", "routes"));
  }

  /**
   * Parses a line of conf/routes if it is a route. Sub-router includes ({@code ->} lines) are not
   * routes and return empty. Play accepts indented lines, so leading and trailing whitespace is
   * ignored.
   */
  public static Optional<Route> parseRoute(String line) {
    String trimmed = line.trim();
    Matcher route = ROUTE_LINE.matcher(trimmed);
    if (route.find()) {
      return Optional.of(new Route(route.group(1), route.group(2), route.group(3)));
    }
    return Optional.empty();
  }

  /**
   * Returns whether the route on line {@code routeIndex} of conf/routes has the given modifier.
   *
   * <p>Modifiers are the lines starting with '+' above a route, such as {@code +nocsrf} or {@code +
   * deferBodyParsing}. Play also applies them across comment lines, but a blank line or another
   * route ends them. Names are separated by whitespace and end at a '#', which starts a comment.
   * Play does not split on commas, so {@code + nocsrf, other} does not add {@code nocsrf}. Play
   * ignores modifiers above a sub-router include.
   *
   * <p>These rules were checked against RoutesFileParser in Play 3.0.12. Recheck them when
   * upgrading Play.
   */
  public static boolean hasModifier(List<String> routesLines, int routeIndex, String modifier) {
    for (int i = routeIndex - 1; i >= 0; i--) {
      String line = routesLines.get(i).trim();
      if (line.startsWith("#")) {
        continue;
      }
      if (!line.startsWith("+")) {
        return false;
      }
      String names = line.substring(1).split("#", 2)[0].trim();
      if (List.of(names.split("\\s+")).contains(modifier)) {
        return true;
      }
    }
    return false;
  }
}
