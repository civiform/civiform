package app;

import org.junit.Before;
import org.junit.Test;

import static org.fluentlenium.core.filter.FilterConstructor.withText;

public class ApplicantProgramBrowserTest extends BaseBrowserTest {

  @Before
  public void setUp() {
    // Create a program with two blocks.

    String programName = "Mock program";
    loginAsAdmin();
    addNameQuestion("name", "applicant.name");
    addTextQuestion("color", "applicant.color");
    addAddressQuestion("Address", "applicant.address");

    addProgram(programName);

    addQuestionsToProgramFirstBlock(programName, "name", "color");
    addQuestionsToProgramNewBlock(programName, "address");
  }

  @Test
  public void submitABlock() {
    loginAsGuest();
    browser.$("button", withText("Apply")).click();
  }
}
