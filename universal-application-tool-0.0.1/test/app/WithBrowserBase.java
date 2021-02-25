package app;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeApplication;

import com.google.common.collect.ImmutableMap;
import org.junit.BeforeClass;
import play.Application;
import play.api.mvc.Call;
import play.test.Helpers;
import play.test.TestBrowser;
import play.test.WithBrowser;
import support.ResourceFabricator;

public class WithBrowserBase extends WithBrowser {

  private static final String LOCALHOST = "http://localhost:";

  protected static final String BASE_URL = LOCALHOST + play.api.test.Helpers.testServerPort();

  private ResourceFabricator resourceFabricator;

  @BeforeClass
  public void setupResources() {
    resourceFabricator = app.injector().instanceOf(ResourceFabricator.class);
  }

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

  @Override
  protected TestBrowser provideBrowser(int port) {
    return Helpers.testBrowser(port);
  }

  protected ResourceFabricator resourceFabricator() {
    return resourceFabricator;
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

  /**
   * Asserts that the current url is equal to the given route method. {@code browser.url()} does not
   * have the leading "/" but route URLs do.
   *
   * @param method the method to compare to, in reverse routing form
   */
  protected void assertUrlEquals(Call method) {
    assertThat("/" + browser.url()).isEqualTo(method.url());
  }
}
