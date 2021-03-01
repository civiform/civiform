package app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ApplicantBrowserTest extends BaseBrowserTest {

  @Test
  public void applicantProgramList_selectApply_redirectsToEdit() {
    addProgram("My Program");
    long id = loginAsApplicant();
    goTo(controllers.applicant.routes.ApplicantProgramsController.index(id));
    assertThat(browser.pageSource()).contains("My Program");

    // Redirect when "apply" link is clicked.
    browser.$("#apply1").click();
    assertUrlEquals(controllers.applicant.routes.ApplicantProgramsController.edit(id, 1L));
  }
}
