package controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.List;
import org.junit.Test;
import support.RoutesFile;

/**
 * Fails if a route in conf/routes has the {@code nocsrf} modifier and is not on the allow-list
 * below, so a new route can't skip CSRF protection by accident. See
 * https://github.com/civiform/civiform/issues/13027.
 *
 * <p>If you add a route that really can't carry a CSRF token, add it to {@link #ALLOWED} with a
 * comment saying what protects it instead.
 *
 * <p>This only checks the nocsrf route modifier. Play's CSRF filter can also be relaxed in
 * application.conf under play.filters.csrf (routeModifiers, header.bypassHeaders, and the method
 * and contentType lists); none is set today. It also only reads conf/routes, not routes files
 * pulled in with a sub-router include ({@code ->}); there are none today.
 */
public class NoCsrfRoutesTest {

  /** Routes allowed to skip CSRF protection, as "METHOD /path handler". */
  private static final ImmutableSet<String> ALLOWED =
      ImmutableSet.of(
          // The OIDC callback is protected by the pac4j state parameter instead.
          "GET /callback/:client_name controllers.CallbackController.callback",
          "POST /callback/:client_name controllers.CallbackController.callback",
          // Dev-only seeding endpoints, called without a browser session. DevRoutesFilter returns a
          // 404 for them outside dev and staging.
          "POST /dev/seedQuestionsHeadless"
              + " controllers.dev.DevToolsController.seedQuestionsHeadless",
          "POST /dev/seedProgramsHeadless controllers.dev.DevToolsController.seedProgramsHeadless",
          "POST /dev/seedApplicationsHeadless"
              + " controllers.dev.DevToolsController.seedApplicationsHeadless",
          "POST /dev/seed/clearHeadless controllers.dev.DevToolsController.clearHeadless");

  @Test
  public void everyRouteWithoutCsrfProtection_isAllowListed() throws IOException {
    ImmutableSet<String> noCsrfRoutes = findNoCsrfRoutes(RoutesFile.readLines());

    assertThat(Sets.difference(noCsrfRoutes, ALLOWED))
        .as(
            "These routes in conf/routes have the nocsrf modifier, which turns off CSRF protection."
                + " Remove the modifier, or if the route can't carry a CSRF token, add it to"
                + " ALLOWED in this test with a comment saying what protects it instead.")
        .isEmpty();
  }

  @Test
  public void everyAllowListedRoute_stillHasNoCsrf() throws IOException {
    ImmutableSet<String> noCsrfRoutes = findNoCsrfRoutes(RoutesFile.readLines());

    assertThat(Sets.difference(ALLOWED, noCsrfRoutes))
        .as(
            "These ALLOWED entries don't match a nocsrf route in conf/routes. Remove them, so a new"
                + " route can't reuse an old exemption. If the routes are still there, the parsing"
                + " in this test may be broken.")
        .isEmpty();
  }

  /** Checks the parsing against the ways Play applies modifiers to routes. */
  @Test
  public void findNoCsrfRoutes_matchesHowPlayAppliesModifiers() {
    ImmutableSet<String> found =
        findNoCsrfRoutes(
            List.of(
                "+nocsrf",
                "GET /plain controllers.A.a()",
                // Modifiers apply to the route right below them, not to later routes.
                "GET /not-marked controllers.A.b()",
                "+ nocsrf",
                "POST /with-space controllers.A.c(request: Request)",
                "+ deferBodyParsing",
                "+nocsrf",
                "POST /stacked controllers.A.d()",
                // Play applies a modifier across comment lines.
                "+nocsrf",
                "# A comment between the modifier and the route",
                "PUT /comment-between controllers.A.e()",
                // But not across a blank line.
                "+nocsrf",
                "",
                "GET /blank-between controllers.A.f()",
                // Only the exact modifier name counts. Play does not split on commas.
                "+ nocsrf, deferBodyParsing",
                "GET /comma controllers.A.g()",
                "+ deferBodyParsing",
                "POST /other-modifier controllers.A.h()",
                // A '#' ends the modifier names and starts a comment.
                "+nocsrf# why",
                "POST /hash-comment controllers.A.l()",
                // Play ignores modifiers on a sub-router include, and it is not a route.
                "+nocsrf",
                "->  /api  api.Routes",
                // Play accepts indented lines, tabs between the columns and Windows line endings.
                "+nocsrf",
                "    POST    /indented    controllers.A.i()",
                "+nocsrf",
                "GET\t/tabbed\tcontrollers.A.j()",
                "  +nocsrf\r",
                "  GET /crlf controllers.A.k()\r"));

    assertThat(found)
        .containsExactlyInAnyOrder(
            "GET /plain controllers.A.a",
            "POST /with-space controllers.A.c",
            "POST /stacked controllers.A.d",
            "PUT /comment-between controllers.A.e",
            "POST /hash-comment controllers.A.l",
            "POST /indented controllers.A.i",
            "GET /tabbed controllers.A.j",
            "GET /crlf controllers.A.k");
  }

  /** Returns each route that has the nocsrf modifier. */
  private static ImmutableSet<String> findNoCsrfRoutes(List<String> lines) {
    ImmutableSet.Builder<String> found = ImmutableSet.builder();

    for (int i = 0; i < lines.size(); i++) {
      if (RoutesFile.hasModifier(lines, i, "nocsrf")) {
        RoutesFile.parseRoute(lines.get(i)).ifPresent(route -> found.add(route.toString()));
      }
    }
    return found.build();
  }
}
