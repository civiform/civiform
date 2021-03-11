package app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.withText;

import org.junit.Test;

public class ProgramAdministrationBrowserTest extends BaseBrowserTest {

  @Test
  public void managingAProgram() {
    String programName = "Reduced fee lunches";

    loginAsAdmin();
    addProgram(programName);
    manageExistingProgramQuestions(programName);

    String nameValue = "updated block name";
    String descriptionValue = "updated block description";

    fillInput("name", nameValue);
    fillTextArea("description", descriptionValue);
    browser.$("button", withText("Update Block")).click();

    assertThat(getInputValue("name")).isEqualTo(nameValue);

    // TODO: I cannot for the life of me figure out how to get just the value here.
    assertThat(textAreaContains("description", descriptionValue)).isTrue();
  }

  @Test
  public void addAndRemoveQuestionsToAProgram() {
    String questionName = "name question";
    String questionRemovedName = "removed question";
    String programName = "Reduced fee lunches";

    loginAsAdmin();
    addQuestion(questionName);
    addQuestion(questionRemovedName);
    addProgram(programName);
    addQuestionsToProgramFirstBlock(programName, questionName, questionRemovedName);

    assertThat(browser.$("#blockQuestions").$("li").textContents()).contains(questionName);
    assertThat(browser.$("#blockQuestions").$("li").textContents()).contains(questionRemovedName);
    assertThat(browser.$("#questionBlockQuestions").$("li").textContents())
        .doesNotContain(questionName);
    assertThat(browser.$("#questionBlockQuestions").$("li").textContents())
        .doesNotContain(questionRemovedName);

    removeQuestionsFromProgram(programName, questionRemovedName);

    assertThat(browser.$("#blockQuestions").$("li").textContents()).contains(questionName);
    assertThat(browser.$("#blockQuestions").$("li").textContents())
        .doesNotContain(questionRemovedName);
    assertThat(browser.$("#questionBlockQuestions").$("li").textContents())
        .doesNotContain(questionName);
    assertThat(browser.$("#questionBankQuestions").$("li").textContents())
        .contains(questionRemovedName);
  }

  @Test
  public void headerNavWorks() {
    loginAsAdmin();

    // Go to questions
    browser.$("nav").$("a", withText("Questions")).first().click();
    assertThat(browser.pageSource()).contains("All Questions");

    // Go to programs
    browser.$("nav").$("a", withText("Programs")).first().click();
    assertThat(browser.pageSource()).contains("All Programs");

    // Logout
    browser.$("nav").$("a", withText("Logout")).first().click();
    assertThat(browser.url()).contains("loginForm");
  }
}
