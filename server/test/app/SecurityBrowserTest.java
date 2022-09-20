package app;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeApplication;

import auth.Roles;
import com.google.common.collect.ImmutableMap;
import controllers.routes;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import play.Application;
import repository.UserRepository;
import services.WellKnownPaths;
import support.CfTestHelpers;
import org.fluentlenium.core.domain.FluentWebElement;

public class SecurityBrowserTest extends BaseBrowserTest {
  private static UserRepository userRepository;

  @Override
  protected Application provideApplication() {
    ImmutableMap<String, Object> config =
        new ImmutableMap.Builder<String, Object>()
            .putAll(CfTestHelpers.oidcConfig("oidc", 3380))
            .build();
    return fakeApplication(config);
  }

  @Before
  public void getApplicantRepository() {
    userRepository = app.injector().instanceOf(UserRepository.class);
  }

  private void loginWithSimulatedIdcs() {
    goTo(routes.LoginController.applicantLogin(Optional.empty()));
    // If we are not cookied, enter a username and password.
    // Otherwise, since the fake provider uses the "web" flow, we're automatically sent to the
    // redirect URI to merge logins.
    // TODO(#1770): Consider removing the below conditional entirely once full logout from the
    // fake OIDC provider is supported.
    if (browser.pageSource().contains("Enter any login")) {
      browser.$("[name='login']").click();
      browser.keyboard().sendKeys("username");
      browser.$("[name='password']").click();
      browser.keyboard().sendKeys("password");
      // Log in.
      browser.$(".login-submit").click();
      // Bypass consent screen.
      browser.$(".login-submit").click();
    }
  }

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
  public void homePage_whenLoggedInAsApplicantForFirstTime_redirectsToLanguageForm() {
    loginAsGuest();
    long applicantId = getApplicantId();

    goToRootUrl();

    assertUrlEquals(controllers.applicant.routes.ApplicantInformationController.edit(applicantId));
  }

  @Test
  public void noCredLogin() {
    loginAsGuest();
    // should be redirected to root.
    goTo(routes.HomeController.securePlayIndex());
    assertThat(browser.pageSource()).contains("You are logged in.");

    goTo(routes.ProfileController.myProfile());
    assertThat(browser.pageSource()).contains("GuestClient");
    assertThat(browser.pageSource()).contains(Roles.ROLE_APPLICANT.toString());

    goTo(controllers.admin.routes.AdminProgramController.index());
    assertThat(browser.pageSource()).contains("403");
  }

  @Test
  public void basicOidcLogin() {
    loginWithSimulatedIdcs();
    goTo(routes.HomeController.securePlayIndex());
    assertThat(browser.pageSource()).contains("You are logged in.");
    goTo(routes.ProfileController.myProfile());
    assertThat(browser.pageSource()).contains("OidcClient");
    assertThat(browser.pageSource()).contains("username@example.com");

    Applicant applicant = userRepository.lookupApplicant(getApplicantId()).toCompletableFuture().join().get();
    Optional<String> applicantName = applicant.getApplicantData().readString(WellKnownPaths.APPLICANT_FIRST_NAME);
    assertThat(applicantName).isPresent();
    assertThat(applicantName.get()).isEqualTo("username@example.com");

    assertThat(applicant.getApplicantData().readString(WellKnownPaths.APPLICANT_MIDDLE_NAME))
        .isEmpty();
    assertThat(applicant.getApplicantData().readString(WellKnownPaths.APPLICANT_LAST_NAME))
        .isEmpty();
  }

  @Test
  public void basicOidcProviderCentralLogout() {
    loginWithSimulatedIdcs();
    goTo(routes.HomeController.securePlayIndex());
    assertThat(browser.pageSource()).contains("You are logged in.");
    logout();
    assertThat(browser.url())
        .contains("session/end?post_logout_redirect_uri=http%3A%2F%2Flocalhost%3A19001%2F&client_id=foo")
        .as("redirects to login provider");
    assertThat(browser.pageSource().contains("Do you want to sign-out from")).as("Confirm logout from dev-oidc");
    FluentWebElement continueButton = browser.$("button").first();
    assertThat(continueButton.textContent()).contains("Yes");
    continueButton.click();
    assertThat(browser.url())
        .contains("loginForm");

    // Log in.
    goTo(routes.HomeController.index());
    assertThat(browser.pageSource()).contains("Don't have an account?");
    goTo(routes.LoginController.applicantLogin(Optional.empty()));
    // Verify we don't auto-login.
    assertThat(browser.pageSource())
        .contains("Enter any login");

  }
  @Test
  public void mergeLogins() {
    // First, log in as guest and get the applicant ID.
    loginAsGuest();
    goTo(routes.ProfileController.myProfile());
    assertThat(browser.pageSource()).contains("GuestClient");

    long applicantId = getApplicantId();

    // Then, login with IDCS and show that the applicant ID is the same.
    loginWithSimulatedIdcs();
    goTo(routes.ProfileController.myProfile());
    assertThat(browser.pageSource()).contains("OidcClient");
    assertThat(applicantId).isEqualTo(getApplicantId());

    logout();

    loginWithSimulatedIdcs();
    goTo(routes.ProfileController.myProfile());
    assertThat(browser.pageSource()).contains("OidcClient");
    assertThat(applicantId).isEqualTo(getApplicantId());
  }

  @Test
  public void adminTestLogin() {
    loginAsAdmin();

    goTo(routes.HomeController.securePlayIndex());
    assertThat(browser.pageSource()).contains("You are logged in.");

    goTo(routes.ProfileController.myProfile());
    assertThat(browser.pageSource()).contains("FakeAdminClient");
    assertThat(browser.pageSource()).contains(Roles.ROLE_CIVIFORM_ADMIN.toString());

    goTo(controllers.admin.routes.AdminProgramController.index());
    assertThat(browser.pageSource()).contains("Programs");
    assertThat(browser.pageSource()).contains("Create new program");
  }
}
