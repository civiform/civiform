package app;

import org.junit.Before;
import org.junit.Test;

public class ApplicantProgramBrowserTest extends BaseBrowserTest {

  @Before
  public void setUp() {
    String programName = "Mock program";
    String questionName1 = "name";
    String questionName2 = "color";
    String questionName3 = "address";

    // Set up a program with two blocks.
    loginAsAdmin();
    addQuestion(questionName1);
    addQuestion(questionName2);
    addQuestion(questionName3);

    addProgram(programName);
    manageExistingProgramQuestions(programName);


  }

  @Test
  public void submitABlock() {

  }
}
