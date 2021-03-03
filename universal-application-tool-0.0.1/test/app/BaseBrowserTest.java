package app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.containingText;
import static org.fluentlenium.core.filter.FilterConstructor.withName;
import static org.fluentlenium.core.filter.FilterConstructor.withText;
import static play.test.Helpers.fakeApplication;

import controllers.routes;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Optional;
import models.Applicant;
import models.Program;
import models.Question;
import org.junit.Before;
import play.Application;
import play.api.mvc.Call;
import play.db.ebean.EbeanConfig;
import play.test.WithBrowser;
import support.TestConstants;

public class BaseBrowserTest extends WithBrowser {

  private static final String LOCALHOST = "http://localhost:";
  protected static final String BASE_URL = LOCALHOST + play.api.test.Helpers.testServerPort();

  @Override
  protected Application provideApplication() {
    return fakeApplication(TestConstants.TEST_DATABASE_CONFIG);
  }

  @Before
  public void truncateTables() {
    EbeanConfig config = app.injector().instanceOf(EbeanConfig.class);
    EbeanServer server = Ebean.getServer(config.defaultServer());
    server.truncate(Applicant.class, Program.class, Question.class);
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

  protected void goToRootUrl() {
    browser.goTo(BASE_URL);
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

  protected void logout() {
    goTo(org.pac4j.play.routes.LogoutController.logout());
  }

  protected void loginAsAdmin() {
    goTo(routes.HomeController.loginForm(Optional.empty()));
    browser.$("#admin").click();
  }

  /** Log in as a guest (applicant) and return the applicant ID for the user. */
  protected void loginAsGuest() {
    goTo(routes.HomeController.loginForm(Optional.empty()));
    browser.$("#guest").click();
  }

  protected long getApplicantId() {
    goTo(routes.ProfileController.myProfile());
    Optional<String> stringId =
        browser.$("#applicant-id").attributes("data-applicant-id").stream().findFirst();
    assertThat(stringId).isNotEmpty();
    return Long.valueOf(stringId.get());
  }

  /**
   * Add a program through the admin flow. This requires that an admin is logged in.
   *
   * @param name a name for the new program
   */
  protected void addProgram(String name) {
    // Go to admin program index and click "New Program".
    goTo(controllers.admin.routes.AdminProgramController.index());
    browser.$("#new-program").click();

    // Fill out name and description for program and submit.
    browser.$("input", withName("name")).fill().with(name);
    browser.$("input", withName("description")).fill().with("Test description");
    browser.$("button", withText("Create")).click();

    // Check that program is added.
    assertThat(browser.pageSource()).contains(name);
  }

  /** Add a question through the admin flow. This requires the admin is logged in. */
  protected void addQuestion(String questionName) {
    // Go to admin question index and click "Create a new question".
    goTo(controllers.admin.routes.QuestionController.index("table"));
    browser.$("a", withText("Create a new question")).first().click();

    // Fill out the question form and click submit.
    browser.$("input", withName("questionName")).fill().with(questionName);
    browser.$("input", withName("questionDescription")).fill().with("question description");
    browser.$("input", withName("questionPath")).fill().with(questionName.replace(" ", "."));
    browser.$("textarea", withName("questionText")).fill().with("question text");
    browser.$("button", withText("Create")).first().click();

    // Check that question is added.
    assertThat(browser.pageSource()).contains(questionName);
  }

  /**
   * Navigates to the block management dashboard for the existing program with the given name.
   *
   * @param programName the name of the program to manage questions for.
   */
  protected void manageExistingProgramQuestions(String programName) {
    goTo(controllers.admin.routes.AdminProgramController.index());
    browser.$("div", containingText(programName)).$("a", containingText("Edit")).first().click();
    assertThat(browser.pageSource()).contains("Edit program: " + programName);
    browser.$("a", withText("Manage Questions")).first().click();
    assertThat(browser.pageSource()).contains(programName + " Questions");
  }

  protected void addQuestionsToProgram(String programName, String... questions) {
    manageExistingProgramQuestions(programName);

    // Add questions to the block.
    for (String question : questions) {
      browser.$("#questionBankQuestions").$("label", withText(question)).$("input").first().click();
    }
    browser.$("button", containingText("Add to Block")).first().click();

    // Check that questions are added.
    for (String question : questions) {
      assertThat(browser.$("#blockQuestions").$("li").textContents()).contains(question);
      assertThat(browser.$("#questionBankQuestions").$("li").textContents())
          .doesNotContain(question);
    }
  }

  protected void removeQuestionsToProgram(String programName, String... questions) {
    manageExistingProgramQuestions(programName);

    // Remove questions to the block.
    for (String question : questions) {
      browser.$("#blockQuestions").$("label", withText(question)).$("input").first().click();
    }
    browser.$("button", containingText("Remove questions")).first().click();

    // Check that questions are removed.
    for (String question : questions) {
      assertThat(browser.$("#blockQuestions").$("li").textContents()).doesNotContain(question);
      assertThat(browser.$("#questionBankQuestions").$("li").textContents()).contains(question);
    }
  }
}
