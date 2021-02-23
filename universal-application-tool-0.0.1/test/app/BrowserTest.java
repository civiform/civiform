package app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;

import auth.Roles;
import com.google.common.collect.ImmutableMap;
import controllers.routes;
import java.util.Optional;
import models.Program;
import org.junit.Test;
import play.Application;
import play.api.mvc.Call;
import play.test.Helpers;
import play.test.TestBrowser;
import play.test.WithBrowser;

public class BrowserTest extends WithBrowser {

  private static final String LOCALHOST = "http://localhost:";
  private static final String BASE_URL = LOCALHOST + play.api.test.Helpers.testServerPort();

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

  @Test
  public void homePage() {
    browser.goTo(BASE_URL);
    assertThat(browser.pageSource()).contains("Your new application is ready.");
  }

  @Test
  public void applicantProgramList_selectApply_redirectsToEdit() {
    Program program = insertProgram("My Program");
    goTo(controllers.applicant.routes.ApplicantProgramsController.index(1L));
    assertThat(browser.pageSource()).contains("My Program");

    // Redirect when "apply" link is clicked.
    String applyLinkId = "#apply" + program.id;
    browser.$(applyLinkId).click();
    assertUrlEquals(controllers.applicant.routes.ApplicantProgramsController.edit(1L, program.id));
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
    assertTrue(browser.pageSource().contains("403"));
  }


  @Test
  public void adminTestLogin() {
    String baseUrl = "http://localhost:" + play.api.test.Helpers.testServerPort();
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
    assertTrue(browser.pageSource().contains("Programs"));
  }

  private void goTo(Call method) {
    browser.goTo(BASE_URL + method.url());
  }

  /** {@code browser.url()} does not have the leading "/" but route URLs do. */
  private void assertUrlEquals(Call method) {
    assertThat("/" + browser.url()).isEqualTo(method.url());
  }

  private static Program insertProgram(String name) {
    Program program = new Program(name, "description");
    program.save();
    return program;
  }
}
