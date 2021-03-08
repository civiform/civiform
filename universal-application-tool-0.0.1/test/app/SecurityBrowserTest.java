package app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import auth.Roles;
import controllers.routes;
import java.util.Optional;
import org.junit.Test;

public class SecurityBrowserTest extends BaseBrowserTest {

  @Test
  public void homePage_whenNotLoggedIn_redirectsToLoginForm() {
    goToRootUrl();
    assertUrlEquals(routes.HomeController.loginForm(Optional.empty()));
  }

  @Test
  public void homePage_whenLoggedInAsAdmin_redirectsToAdminProgramList() {
    loginAsAdmin();
    goToRootUrl();
    assertUrlEquals(controllers.admin.routes.AdminProgramController.index());
  }

  @Test
  public void homePage_whenLoggedInAsApplicant_redirectsToApplicantProgramList() {
    loginAsGuest();
    long applicantId = getApplicantId();

    goToRootUrl();

    assertUrlEquals(controllers.applicant.routes.ApplicantProgramsController.index(applicantId));
  }

  @Test
  public void noCredLogin() {
    loginAsGuest();
    // should be redirected to root.
    goTo(routes.HomeController.securePlayIndex());
    assertThat(browser.pageSource()).contains("You are logged in.");

    goTo(routes.ProfileController.myProfile());
    assertThat(browser.pageSource()).contains("GuestClient");
    assertThat(browser.pageSource()).contains("{\"created_time\":");
    assertThat(browser.pageSource()).contains(Roles.ROLE_APPLICANT.toString());

    goTo(controllers.admin.routes.AdminProgramController.index());
    assertTrue(browser.pageSource().contains("403"));
  }

  @Test
  public void adminTestLogin() {
    loginAsAdmin();

    goTo(routes.HomeController.securePlayIndex());
    assertThat(browser.pageSource()).contains("You are logged in.");

    goTo(routes.ProfileController.myProfile());
    assertThat(browser.pageSource()).contains("FakeAdminClient");
    assertThat(browser.pageSource()).contains("{\"created_time\":");
    assertThat(browser.pageSource()).contains(Roles.ROLE_UAT_ADMIN.toString());

    goTo(controllers.admin.routes.AdminProgramController.index());
    assertTrue(browser.pageSource().contains("Programs"));
    assertTrue(browser.pageSource().contains("Create new program"));
  }
}
