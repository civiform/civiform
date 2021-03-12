package app;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.withName;
import static org.fluentlenium.core.filter.FilterConstructor.withText;

import org.junit.Before;
import org.junit.Test;

public class ApplicantProgramBrowserTest extends BaseBrowserTest {

  @Before
  public void setUp() {
    // Create a program with two blocks.

    loginAsAdmin();

    addNameQuestion("name", "applicant.name");
    addTextQuestion("color", "applicant.color");
    addAddressQuestion("address", "applicant.address");

    String programName = "Mock program";
    addProgram(programName);

    addQuestionsToProgramFirstBlock(programName, "name", "color");
    addQuestionsToProgramNewBlock(programName, "address");
  }

  @Test
  public void fillOutAndSubmitAProgramApplication() {
    // TODO(https://github.com/seattle-uat/universal-application-tool/issues/401): Create a page
    //  object for applicant pages (program list, program block edit).

    loginAsGuest();
    browser.$("a", withText("Apply")).click();

    // On first block of questions.
    // Error messages not present to begin with.
    assertThat(browser.$("span", withText("First name is required.")).present()).isFalse();
    assertThat(browser.$("span", withText("Last name is required.")).present()).isFalse();

    // Attempt to proceed to next block without filling in required fields.
    browser.$("button", withText("Save and continue")).click();

    // Validation errors: first block still displayed but with error messages.
    assertThat(browser.$("span", withText("First name is required.")).present()).isTrue();
    assertThat(browser.$("span", withText("Last name is required.")).present()).isTrue();

    // Fill in first block correctly.
    fillInput("applicant.name.first", "Finn");
    fillInput("applicant.name.last", "the Human");
    fillInput("applicant.color", "baby blue");

    browser.$("button", withText("Save and continue")).click();

    // Next block.
    assertThat(browser.$("input", withName("applicant.name.first")).present()).isFalse();

    fillInput("applicant.address.street", "123 A St");
    fillInput("applicant.address.city", "Seattle");
    fillInput("applicant.address.state", "WA");
    fillInput("applicant.address.zip", "99999");

    browser.$("button", withText("Save and continue")).click();

    // TODO(https://github.com/seattle-uat/universal-application-tool/issues/256): Expect review
    //  page when it is implemented.
    // All blocks complete. Back to list of programs.
    assertThat(browser.$("h1", withText("Programs")).present()).isTrue();
    assertThat(browser.$("h2", withText("Mock program")).present()).isTrue();
  }
}
