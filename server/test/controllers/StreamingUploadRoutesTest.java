package controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.pac4j.play.java.Secure;
import parsers.StreamingMultipartBodyParser;
import play.mvc.BodyParser;
import play.mvc.With;
import support.RoutesFile;

/**
 * Fails if a route that uses a streaming upload body parser lacks the {@code deferBodyParsing}
 * modifier. See {@link FileUploadRouteAuthorizationTest}.
 */
public class StreamingUploadRoutesTest {

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
    List<String> lines = RoutesFile.readLines();
    ImmutableList.Builder<StreamingRoute> found = ImmutableList.builder();

    for (int i = 0; i < lines.size(); i++) {
      Optional<RoutesFile.Route> route = RoutesFile.parseRoute(lines.get(i));
      if (route.isEmpty()) {
        continue;
      }
      // The handler is "package.Class.method".
      String handler = route.get().handler();
      int lastDot = handler.lastIndexOf('.');
      if (lastDot < 0) {
        continue;
      }
      Method method = findMethod(handler.substring(0, lastDot), handler.substring(lastDot + 1));
      if (method == null || !usesStreamingParser(method)) {
        continue;
      }
      found.add(
          new StreamingRoute(
              lines.get(i).trim(), RoutesFile.hasModifier(lines, i, "deferBodyParsing"), method));
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
}
