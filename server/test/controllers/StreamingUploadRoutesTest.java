package controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import org.pac4j.play.java.Secure;
import parsers.StreamingMultipartBodyParser;
import play.mvc.BodyParser;
import play.mvc.With;

/**
 * Fails if a route that uses a streaming upload body parser lacks the {@code deferBodyParsing}
 * modifier. See {@link FileUploadRouteAuthorizationTest}.
 */
public class StreamingUploadRoutesTest {

  private static final Pattern ROUTE_LINE =
      Pattern.compile("^(GET|POST|PUT|PATCH|DELETE)\\s+\\S+\\s+([\\w.]+)\\.(\\w+)\\(");

  private record StreamingRoute(String routeLine, boolean defersBodyParsing, Method method) {}

  @Test
  public void everyRouteUsingAStreamingParser_defersBodyParsing() throws Exception {
    ImmutableList<StreamingRoute> routes = findStreamingRoutes();

    // Guard against this test passing because it found nothing.
    assertThat(routes).hasSizeGreaterThanOrEqualTo(4);
    for (StreamingRoute route : routes) {
      assertThat(route.defersBodyParsing())
          .as(
              "Route uses a streaming upload body parser, so it needs the deferBodyParsing"
                  + " modifier in conf/routes (a line starting with '+' directly above it): %s",
              route.routeLine())
          .isTrue();
    }
  }

  @Test
  public void everyRouteUsingAStreamingParser_runsSecureBeforeOtherActions() throws Exception {
    for (StreamingRoute route : findStreamingRoutes()) {
      List<Class<? extends Annotation>> order = new ArrayList<>();
      for (Annotation annotation : route.method().getAnnotations()) {
        order.add(annotation.annotationType());
      }

      assertThat(order)
          .as("%s needs @Secure", route.routeLine())
          .contains(Secure.class)
          .as("%s needs at least one @With action for its feature flag check", route.routeLine())
          .contains(With.class);
      assertThat(order.indexOf(Secure.class))
          .as(
              "@Secure must come before @With so the signed in user is available: %s",
              route.routeLine())
          .isLessThan(order.indexOf(With.class));
    }
  }

  private static ImmutableList<StreamingRoute> findStreamingRoutes() throws Exception {
    List<String> lines = Files.readAllLines(Path.of("conf", "routes"));
    ImmutableList.Builder<StreamingRoute> found = ImmutableList.builder();

    for (int i = 0; i < lines.size(); i++) {
      Matcher matcher = ROUTE_LINE.matcher(lines.get(i));
      if (!matcher.find()) {
        continue;
      }
      Method method = findMethod(matcher.group(2), matcher.group(3));
      if (method == null || !usesStreamingParser(method)) {
        continue;
      }
      found.add(new StreamingRoute(lines.get(i).trim(), hasDeferModifier(lines, i), method));
    }
    return found.build();
  }

  private static Method findMethod(String className, String methodName) {
    try {
      for (Method method : Class.forName(className).getDeclaredMethods()) {
        if (method.getName().equals(methodName)) {
          return method;
        }
      }
    } catch (ClassNotFoundException e) {
      // Not a controller class we can inspect, such as a generated or external route.
    }
    return null;
  }

  private static boolean usesStreamingParser(Method method) {
    BodyParser.Of parser = method.getAnnotation(BodyParser.Of.class);
    return parser != null && StreamingMultipartBodyParser.class.isAssignableFrom(parser.value());
  }

  /** Route modifiers are the lines starting with '+' directly above a route. */
  private static boolean hasDeferModifier(List<String> lines, int routeIndex) throws IOException {
    for (int i = routeIndex - 1; i >= 0 && lines.get(i).trim().startsWith("+"); i--) {
      if (List.of(lines.get(i).trim().substring(1).trim().split("\\s+"))
          .contains("deferBodyParsing")) {
        return true;
      }
    }
    return false;
  }
}
