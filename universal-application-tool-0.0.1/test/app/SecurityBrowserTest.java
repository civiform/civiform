package app;

import static org.assertj.core.api.Assertions.assertThat;

import auth.Roles;
import controllers.routes;
import java.util.Optional;
import org.junit.Test;

public class SecurityBrowserTest extends WithBrowserBase {

  @Test
  public void homePage() {
    browser.goTo(BASE_URL);
    assertThat(browser.pageSource()).contains("Your new application is ready.");
  }

  @Test
  public void noCredLogin() {
    goTo(routes.HomeController.loginForm(Optional.empty()));
    browser.$("#guest").click();
    // should be redirected to root.
    assertThat(browser.url()).isEmpty();
    assertThat(browser.pageSource()).contains("Your new application is ready.");

    goTo(routes.HomeController.secureIndex());
    assertThat(browser.pageSource()).contains("You are logged in.");

    goTo(routes.ProfileController.myProfile());
    assertThat(browser.pageSource()).contains("GuestClient");
    assertThat(browser.pageSource()).contains("{\"created_time\":");
    assertThat(browser.pageSource()).contains(Roles.ROLE_APPLICANT.toString());

    goTo(controllers.admin.routes.AdminProgramController.index());
    assertThat(browser.pageSource()).contains("403");
  }

  @Test
  public void adminTestLogin() {
    goTo(routes.HomeController.loginForm(Optional.empty()));
    browser.$("#admin").click();
    // should be redirected to root.
    assertThat(browser.url()).isEmpty();
    assertThat(browser.pageSource()).contains("Your new application is ready.");

    goTo(routes.HomeController.secureIndex());
    assertThat(browser.pageSource()).contains("You are logged in.");

    goTo(routes.ProfileController.myProfile());
    assertThat(browser.pageSource()).contains("FakeAdminClient");
    assertThat(browser.pageSource()).contains("{\"created_time\":");
    assertThat(browser.pageSource()).contains(Roles.ROLE_UAT_ADMIN.toString());

    goTo(controllers.admin.routes.AdminProgramController.index());
    assertThat(browser.pageSource()).contains("Programs");
  }
}
