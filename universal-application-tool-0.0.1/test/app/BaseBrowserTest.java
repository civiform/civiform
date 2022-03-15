package app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.containingText;
import static org.fluentlenium.core.filter.FilterConstructor.withId;
import static org.fluentlenium.core.filter.FilterConstructor.withName;
import static org.fluentlenium.core.filter.FilterConstructor.withText;
import static play.test.Helpers.fakeApplication;

import controllers.routes;
import io.ebean.DB;
import io.ebean.Database;
import java.util.Optional;
import models.LifecycleStage;
import models.Models;
import models.Version;
import org.fluentlenium.core.domain.FluentList;
import org.fluentlenium.core.domain.FluentWebElement;
import org.junit.Before;
import org.openqa.selenium.By;
import play.Application;
import play.api.mvc.Call;
import play.test.WithBrowser;
import services.question.types.QuestionType;
import views.style.ReferenceClasses;

public class BaseBrowserTest extends WithBrowser {

  private static final String LOCALHOST = "http://localhost:";
  protected static final String BASE_URL = LOCALHOST + play.api.test.Helpers.testServerPort();
  private static final String APPLICANT_ROOT = "applicant";

  @Override
  protected Application provideApplication() {
    return fakeApplication();
  }

  @Before
  public void resetTables() {
    Database database = DB.getDefault();
    Models.truncate(database);
    Version newActiveVersion = new Version(LifecycleStage.ACTIVE);
    newActiveVersion.save();
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
    browser.$("#new-program-button").click();

    // Fill out name and description for program and submit.
    fillInput("name", name);
    fillTextArea("description", "Test description");
    browser.$("button", withText("Create")).click();

    // Check that program is added.
    assertThat(bodySource()).contains(name);
  }

  /** Adds a test question through the admin flow. This requires the admin to be logged in. */
  protected void addQuestion(String questionName) {
    addQuestion(questionName, APPLICANT_ROOT, QuestionType.TEXT);
  }

  protected void addTextQuestion(String questionName) {
    addQuestion(questionName, APPLICANT_ROOT, QuestionType.TEXT);
  }

  protected void addNameQuestion(String questionName) {
    addQuestion(questionName, APPLICANT_ROOT, QuestionType.NAME);
  }

  protected void addAddressQuestion(String questionName) {
    addQuestion(questionName, APPLICANT_ROOT, QuestionType.ADDRESS);
  }

  /** Adds a question through the admin flow. This requires the admin to be logged in. */
  protected void addQuestion(String questionName, String parentPath, QuestionType questionType) {
    // Go to admin question index and click "Create a new question".
    goTo(controllers.admin.routes.AdminQuestionController.index());
    browser.$(By.id("create-question-button")).first().click();

    String questionTypeButton = String.format("create-%s-question", questionType).toLowerCase();
    browser.$(By.id(questionTypeButton)).first().click();

    // Fill out the question form and click submit.
    fillInput("questionName", questionName);
    fillTextArea("questionDescription", "question description");
    selectAnOption("questionParentPath", parentPath);
    fillTextArea("questionText", "question text");

    // Check that the question form contains a Question Settings section.
    assertThat(browser.$(By.id("text-question-config")))
        .hasSize(questionType.equals(QuestionType.TEXT) ? 1 : 0);
    assertThat(browser.$(By.id("address-question-config")))
        .hasSize(questionType.equals(QuestionType.ADDRESS) ? 1 : 0);

    browser.$("button", withText("Create")).first().click();

    // Check that question is added.
    assertThat(bodySource()).contains(questionName);
  }

  /**
   * Navigates to the block management dashboard for the existing program with the given name.
   *
   * @param programName the name of the program to manage questions for.
   */
  protected void manageExistingProgramQuestions(String programName) {
    goTo(controllers.admin.routes.AdminProgramController.index());
    browser.$("div", containingText(programName)).$("a", containingText("Edit")).first().click();
    assertThat(bodySource()).contains("Edit program: " + programName);
    browser.$("a", withId("manage-questions-link")).first().click();
  }

  protected void publishExistingProgram(String programName) {
    goTo(controllers.admin.routes.AdminProgramController.index());
    browser
        .$("div", containingText(programName))
        .$("button", containingText("Publish"))
        .first()
        .click();
  }

  /** Adds the questions with the given names to the first block in the given program. */
  protected void addQuestionsToProgramFirstBlock(String programName, String... questionNames) {
    manageExistingProgramQuestions(programName);
    addQuestionsToBlock(questionNames);
  }

  /** Adds the questions with the given names to a new block in the given program. */
  protected void addQuestionsToProgramNewBlock(String programName, String... questionNames) {
    manageExistingProgramQuestions(programName);

    browser.$("button", withText("Add Screen")).click();
    addQuestionsToBlock(questionNames);
  }

  /**
   * Adds the given questions to the block currently shown. Depends on already being in the
   * block-editing context.
   */
  protected void addQuestionsToBlock(String... questionNames) {
    for (String questionName : questionNames) {
      // Verify initial state
      FluentList<FluentWebElement> questionBank =
          browser.$(By.className(ReferenceClasses.ADD_QUESTION_BUTTON));
      FluentList<FluentWebElement> blockQuestions =
          browser.$(By.className(ReferenceClasses.REMOVE_QUESTION_BUTTON));

      assertThat(questionBank.texts()).contains(questionName);
      assertThat(blockQuestions.texts()).doesNotContain(questionName);

      // Add question.
      browser
          .$(By.className(ReferenceClasses.ADD_QUESTION_BUTTON), withText(questionName))
          .first()
          .click();

      // Verify end state.
      questionBank = browser.$(By.className(ReferenceClasses.ADD_QUESTION_BUTTON));
      blockQuestions = browser.$(By.className(ReferenceClasses.REMOVE_QUESTION_BUTTON));
      assertThat(questionBank.texts()).doesNotContain(questionName);
      assertThat(blockQuestions.texts()).contains(questionName);
    }
  }

  protected void removeQuestionsFromProgram(String programName, String... questions) {
    manageExistingProgramQuestions(programName);
    removeQuesitonsFromBlock(questions);
  }

  protected void removeQuesitonsFromBlock(String... questionNames) {
    for (String questionName : questionNames) {
      // Verify initial state
      FluentList<FluentWebElement> questionBank =
          browser.$(By.className(ReferenceClasses.ADD_QUESTION_BUTTON));
      FluentList<FluentWebElement> blockQuestions =
          browser.$(By.className(ReferenceClasses.REMOVE_QUESTION_BUTTON));
      assertThat(questionBank.textContents()).doesNotContain(questionName);
      assertThat(blockQuestions.textContents()).contains(questionName);

      // Remove question.
      browser
          .$(By.className(ReferenceClasses.REMOVE_QUESTION_BUTTON), withText(questionName))
          .first()
          .click();

      // Verify end state.
      questionBank = browser.$(By.className(ReferenceClasses.ADD_QUESTION_BUTTON));
      blockQuestions = browser.$(By.className(ReferenceClasses.REMOVE_QUESTION_BUTTON));
      assertThat(questionBank.textContents()).contains(questionName);
      assertThat(blockQuestions.textContents()).doesNotContain(questionName);
    }
  }

  protected String bodySource() {
    return browser.$("body").first().html();
  }

  protected void fillInput(String name, String text) {
    browser.$("input", withName(name)).fill().with(text);
  }

  protected String getInputValue(String name) {
    return browser.$("input", withName(name)).first().value();
  }

  protected void fillTextArea(String name, String text) {
    browser.$("textarea", withName(name)).fill().with(text);
  }

  protected String getTextAreaValue(String name) {
    return browser.getDriver().findElement(By.cssSelector("textarea[name=" + name + "]")).getText();
  }

  protected void selectAnOption(String selectName, String option) {
    FluentWebElement select = browser.$("select", withName(selectName)).first();
    select.fillSelect().withValue(option);
  }
}
