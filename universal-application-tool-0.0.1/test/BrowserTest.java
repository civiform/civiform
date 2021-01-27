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
            // See FunctionalTest.java for explanation of this string.
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
}
