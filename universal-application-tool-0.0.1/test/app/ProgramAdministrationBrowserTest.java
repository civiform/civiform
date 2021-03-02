package app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.withName;
import static org.fluentlenium.core.filter.FilterConstructor.withText;

import org.junit.Test;

public class ProgramAdministrationBrowserTest extends BaseBrowserTest {

  @Test
  public void managingAProgram() {
    String programName = "Reduced fee lunches";

    loginAsAdmin();
    addProgram(programName);
    manageExistingProgramQuestions(programName);

    browser.$("input", withName("name")).fill().with("updated block name");
    browser.$("input", withName("description")).fill().with("updated block description");
    browser.$("button", withText("Update Block")).click();

    assertThat(browser.$("input", withName("name")).values()).contains("updated block name");
    assertThat(browser.$("input", withName("description")).values())
        .contains("updated block description");
  }
}
