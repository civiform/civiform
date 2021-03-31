package app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.withId;
import static org.fluentlenium.core.filter.FilterConstructor.withText;

import org.junit.Test;

public class ProgramAdministrationBrowserTest extends BaseBrowserTest {

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

  @Test
  public void managingAProgram() {
    String questionName = "name question";
    String questionRemovedName = "removed question";
    String programName = "Reduced fee lunches";

    loginAsAdmin();
    addProgram(programName);
    addQuestion(questionName);
    addQuestion(questionRemovedName);
    manageExistingProgramQuestions(programName);

    String nameValue = "updated block name";
    String descriptionValue = "updated block description";

    // update block name and description.
    fillInput("name", nameValue);
    fillTextArea("description", descriptionValue);
    browser.$("button", withText("Update Block")).click();

    // assert that block was successfully updated.
    assertThat(getInputValue("name")).isEqualTo(nameValue);
    assertThat(getTextAreaValue("description")).isEqualTo(descriptionValue);

    // add questions to block 1.
    addQuestionsToProgramFirstBlock(programName, questionName, questionRemovedName);

    // Remove question from block 1.
    removeQuestionsFromProgram(programName, questionRemovedName);
    assertThat(browser.$("#block-questions-form button").textContents()).contains(questionName);
    assertThat(browser.$("#question-bank-questions button").textContents())
        .doesNotContain(questionName);

    // add block to program
    browser.$("button", withId("add-block-button")).first().click();
    assertThat(getInputValue("name")).isEqualTo("Block 2");
    assertThat(getTextAreaValue("description")).isNotEmpty();

    // add question to block 2
    addQuestionsToBlock(questionRemovedName);

    // delete block 2
    browser.$("button", withId("delete-block-button")).first().click();
    assertThat(getInputValue("name")).isEqualTo(nameValue);
    assertThat(getTextAreaValue("description")).isEqualTo(descriptionValue);

    assertThat(browser.$("#block-questions-form button").textContents()).contains(questionName);
    assertThat(browser.$("#question-bank-questions button").textContents())
        .contains(questionRemovedName);
  }
}
