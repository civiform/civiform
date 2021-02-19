import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;

import com.google.common.collect.ImmutableMap;
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
            "jdbc:tc:postgresql:9.6.8:///databasename"));
  }

  protected TestBrowser provideBrowser(int port) {
    return Helpers.testBrowser(port);
  }

  /**
   * add your integration test here in this example we just check if the welcome page is being shown
   */
  @Test
  public void test() {
    browser.goTo("http://localhost:" + play.api.test.Helpers.testServerPort());
    assertTrue(browser.pageSource().contains("Your new application is ready."));
  }

  @Test
  public void login() {
    String baseUrl = "http://localhost:" + play.api.test.Helpers.testServerPort();
    browser.goTo(baseUrl + "/loginForm");
    browser.$("#uname").click();
    browser.keyboard().sendKeys("test");
    browser.$("#pwd").click();
    browser.keyboard().sendKeys("test");
    browser.$("#login").click();
    // should be redirected to root.
    assertEquals("", browser.url());
    assertTrue(browser.pageSource().contains("Your new application is ready."));
    browser.goTo(baseUrl + "/secure");
    assertTrue(browser.pageSource().contains("You are logged in."));
    browser.goTo(baseUrl + "/users/me");
    assertTrue(browser.pageSource().contains("FormClient"));
  }

  @Test
  public void noCredLogin() {
    String baseUrl = "http://localhost:" + play.api.test.Helpers.testServerPort();
    browser.goTo(baseUrl + "/loginForm");
    browser.$("#guest").click();
    // should be redirected to root.
    assertEquals("", browser.url());
    assertTrue(browser.pageSource().contains("Your new application is ready."));
    browser.goTo(baseUrl + "/secure");
    assertTrue(browser.pageSource().contains("You are logged in."));
    browser.goTo(baseUrl + "/users/me");
    assertTrue(browser.pageSource().contains("GuestClient"));
  }
}
