package app;

import static org.assertj.core.api.Assertions.assertThat;

import models.Program;
import org.junit.Test;

public class ApplicantBrowserTest extends WithBrowserBase {

  @Test
  public void applicantProgramList_selectApply_redirectsToEdit() {
    Program program = resourceFabricator().insertProgram("My Program");
    goTo(controllers.applicant.routes.ApplicantProgramsController.index(1L));
    assertThat(browser.pageSource()).contains("My Program");

    // Redirect when "apply" link is clicked.
    String applyLinkId = "#apply" + program.id;
    browser.$(applyLinkId).click();
    assertUrlEquals(controllers.applicant.routes.ApplicantProgramsController.edit(1L, program.id));
  }
}
