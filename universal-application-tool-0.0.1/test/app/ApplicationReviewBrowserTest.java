package app;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.containingText;
import static org.fluentlenium.core.filter.FilterConstructor.withText;

import controllers.admin.routes;
import org.junit.Before;
import org.junit.Test;

public class ApplicationReviewBrowserTest extends BaseBrowserTest {

  @Before
  public void setUp() {
    // Create a program with two blocks.

    loginAsAdmin();

    addNameQuestion("name");

    String programName = "Mock program";
    addProgram(programName);

    addQuestionsToProgramFirstBlock(programName, "name");

    logout();
    loginAsGuest();

    browser.$("a", withText("Apply")).click();
    fillInput("applicant.name.first", "Bob");
    fillInput("applicant.name.last", "the Builder");

    browser.$("button", withText("Save and continue")).click();

    logout();
  }

  @Test
  public void examineApplication() {
    loginAsAdmin();
    goTo(routes.AdminProgramController.index());
    assertThat(bodySource()).contains("Applications");

    browser.$("a", containingText("Applications")).click();
    assertThat(bodySource()).contains("the Builder, Bob");

    browser.$("a", containingText("View")).click();
    assertThat(bodySource()).contains("Block 1");
    assertThat(bodySource()).contains("Question 1");
    assertThat(bodySource()).contains("name");
    assertThat(bodySource()).contains("Bob");
    assertThat(bodySource()).contains("the Builder");
  }
}
