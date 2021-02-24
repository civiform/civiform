package app;

import static org.assertj.core.api.Assertions.assertThat;

import play.api.mvc.Call;
import play.test.WithBrowser;

public class WithBrowserBase extends WithBrowser {

  private static final String LOCALHOST = "http://localhost:";

  protected static final String BASE_URL = LOCALHOST + play.api.test.Helpers.testServerPort();

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
