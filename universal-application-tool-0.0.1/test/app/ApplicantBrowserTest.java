package app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.withText;

import org.junit.Test;

public class ApplicantBrowserTest extends BaseBrowserTest {

  // TODO(https://github.com/seattle-uat/universal-application-tool/issues/256): Expect a redirect
  // to end of program submission.
  @Test
  public void applicantProgramList_selectApply_noIncompleteBlocks_redirectsToProgramList() {
    addProgram("My Program");
    long id = loginAsApplicant();
    goTo(controllers.applicant.routes.ApplicantProgramsController.index(id));
    assertThat(browser.pageSource()).contains("My Program");

    // Redirect when "apply" link is clicked.
    browser.$(withText("Apply")).first().click();
    assertThat(browser.pageSource()).contains("Programs");
  }
}
