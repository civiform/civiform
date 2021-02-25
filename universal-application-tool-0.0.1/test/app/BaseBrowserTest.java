package app;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeApplication;

import com.google.common.collect.ImmutableMap;
import controllers.routes;
import java.util.Optional;
import play.Application;
import play.api.mvc.Call;
import play.mvc.Http;
import play.test.Helpers;
import play.test.WithBrowser;

public class BaseBrowserTest extends WithBrowser {

  private static final String LOCALHOST = "http://localhost:";

  protected static final String BASE_URL = LOCALHOST + play.api.test.Helpers.testServerPort();

  @Override
  protected Application provideApplication() {
    return fakeApplication(
        ImmutableMap.of(
            "db.default.driver",
            "org.testcontainers.jdbc.ContainerDatabaseDriver",
            "db.default.url",
            // See WithPostgresContainer.java for explanation of this string.
            "jdbc:tc:postgresql:12.5:///databasename",
            "play.evolutions.db.default.enabled",
            "true"));
  }

  /**
   * Redirect to the given route, using reverse routing:
   * https://www.playframework.com/documentation/2.8.x/JavaRouting#Reverse-routing
   *
   * @param method the method to call, using reverse routing
   */
  protected void goTo(Call method) {
    browser.goTo(BASE_URL + method.url());
  }

  protected void goTo(Call method, Http.RequestBuilder builder) {
    browser.goTo(BASE_URL + "/" + method.relativeTo(builder.build()));
  }

  /**
   * Asserts that the current url is equal to the given route method. {@code browser.url()} does not
   * have the leading "/" but route URLs do.
   *
   * @param method the method to compare to, in reverse routing form
   */
  protected void assertUrlEquals(Call method) {
    assertThat("/" + browser.url()).isEqualTo(method.url());
  }

  protected void loginAsAdmin() {
    goTo(routes.HomeController.loginForm(Optional.empty()));
    browser.$("#admin").click();
  }

  /**
   * Add a program through the admin flow.
   *
   * @param name a name for the new program
   */
  protected void addProgram(String name) {
    loginAsAdmin();
    Http.RequestBuilder requestBuilder =
        Helpers.fakeRequest()
            .method("post")
            .bodyForm(ImmutableMap.of("name", name, "description", "A new program"));
    System.out.println(
        controllers.admin.routes.AdminProgramController.create()
            .relativeTo(requestBuilder.build()));
    goTo(controllers.admin.routes.AdminProgramController.create(), requestBuilder);
  }
}
