import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;

import auth.Roles;
import com.google.common.collect.ImmutableMap;
import controllers.routes;
import java.util.Optional;
import org.junit.Test;
import play.Application;
import play.test.Helpers;
import play.test.TestBrowser;
import play.test.WithBrowser;

public class BrowserTest extends WithBrowser {

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

  protected TestBrowser provideBrowser(int port) {
    return Helpers.testBrowser(port);
  }

  @Test
  public void test() {
    browser.goTo("http://localhost:" + play.api.test.Helpers.testServerPort());
    assertTrue(browser.pageSource().contains("Your new application is ready."));
  }

  @Test
  public void noCredLogin() {
    String baseUrl = "http://localhost:" + play.api.test.Helpers.testServerPort();
    browser.goTo(baseUrl + routes.HomeController.loginForm(Optional.empty()).url());
    browser.$("#guest").click();
    // should be redirected to root.
    assertEquals("", browser.url());
    assertTrue(browser.pageSource().contains("Your new application is ready."));
    browser.goTo(baseUrl + routes.HomeController.secureIndex().url());
    assertTrue(browser.pageSource().contains("You are logged in."));
    browser.goTo(baseUrl + routes.ProfileController.myProfile().url());
    assertTrue(browser.pageSource().contains("GuestClient"));
    assertTrue(browser.pageSource().contains("{\"created_time\":"));
    assertTrue(browser.pageSource().contains(Roles.ROLE_APPLICANT.toString()));
  }
}
