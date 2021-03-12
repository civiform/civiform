package app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.containingText;
import static org.fluentlenium.core.filter.FilterConstructor.withId;
import static org.fluentlenium.core.filter.FilterConstructor.withName;
import static org.fluentlenium.core.filter.FilterConstructor.withText;
import static play.test.Helpers.fakeApplication;

import controllers.routes;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Optional;
import models.Account;
import models.Applicant;
import models.Program;
import models.Question;
import org.fluentlenium.core.domain.FluentWebElement;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import play.Application;
import play.api.mvc.Call;
import play.db.ebean.EbeanConfig;
import play.test.WithBrowser;
import services.question.QuestionType;
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
    server.truncate(Applicant.class, Program.class, Question.class, Account.class);
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
    fillInput("name", name);
    fillTextArea("description", "Test description");
    browser.$("button", withText("Create")).click();

    // Check that program is added.
    assertThat(browser.pageSource()).contains(name);
  }

  /** Adds a test question through the admin flow. This requires the admin to be logged in. */
  protected void addQuestion(String questionName) {
    addQuestion(questionName, questionName.replace(" ", "."), QuestionType.TEXT);
  }

  protected void addTextQuestion(String questionName, String path) {
    addQuestion(questionName, path, QuestionType.TEXT);
  }

  protected void addNameQuestion(String questionName, String path) {
    addQuestion(questionName, path, QuestionType.NAME);
  }

  protected void addAddressQuestion(String questionName, String path) {
    addQuestion(questionName, path, QuestionType.ADDRESS);
  }

  /** Adds a question through the admin flow. This requires the admin to be logged in. */
  protected void addQuestion(String questionName, String path, QuestionType questionType) {
    // Go to admin question index and click "Create a new question".
    goTo(controllers.admin.routes.QuestionController.index());
    browser.$("a", withId("createQuestion")).first().click();

    // Fill out the question form and click submit.
    fillInput("questionName", questionName);
    fillTextArea("questionDescription", "question description");
    fillInput("questionPath", path);
    fillTextArea("questionText", "question text");
    selectAnOption("questionType", questionType.toString());

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
    browser.$("a", withId("manageQuestions")).first().click();
    assertThat(browser.pageSource()).contains(programName + " Questions");
  }

  /** Adds the questions with the given names to the first block in the given program. */
  protected void addQuestionsToProgramFirstBlock(String programName, String... questionNames) {
    manageExistingProgramQuestions(programName);
    addQuestionsToBlock(questionNames);
  }

  /** Adds the questions with the given names to a new block in the given program. */
  protected void addQuestionsToProgramNewBlock(String programName, String... questionNames) {
    manageExistingProgramQuestions(programName);
    browser.$("button", withText("Add Block")).click();
    addQuestionsToBlock(questionNames);
  }

  /**
   * Adds the given questions to the block currently shown. Depends on already being in the
   * block-editing context.
   */
  private void addQuestionsToBlock(String... questionNames) {
    // Add questions to the block.
    for (String questionName : questionNames) {
      browser
          .$("#questionBankQuestions")
          .$("label", withText(questionName))
          .$("input")
          .first()
          .click();
    }
    browser.$("button", containingText("Add to Block")).first().click();

    // Check that questions are added.
    for (String questionName : questionNames) {
      assertThat(browser.$("#blockQuestions").$("li").textContents()).contains(questionName);
      assertThat(browser.$("#questionBankQuestions").$("li").textContents())
          .doesNotContain(questionName);
    }
  }

  protected void removeQuestionsFromProgram(String programName, String... questions) {
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

  protected void fillInput(String name, String text) {
    browser.$("input", withName(name)).fill().with(text);
  }

  protected String getInputValue(String name) {
    return browser.$("input", withName(name)).first().value();
  }

  protected void fillTextArea(String name, String text) {
    WebElement element =
        browser.getDriver().findElement(By.cssSelector("textarea[name=" + name + "]"));
    element.clear();
    element.sendKeys(text);
  }

  protected String getTextAreaString(String name) {
    return browser
        .getDriver()
        .findElement(By.cssSelector("textarea[name=" + name + "]"))
        .toString();
  }

  protected boolean textAreaContains(String name, String value) {
    return getTextAreaString(name).contains(String.format("value=\"%s\"", value));
  }

  protected void selectAnOption(String selectName, String option) {
    FluentWebElement select = browser.$("select", withName(selectName)).first();
    select.fillSelect().withValue(option);
  }
}
