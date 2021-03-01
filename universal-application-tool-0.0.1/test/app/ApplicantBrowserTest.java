package app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.withText;

import org.junit.Test;

public class ApplicantBrowserTest extends BaseBrowserTest {

  @Test
  public void applicantProgramList_selectApply_redirectsToEdit() {
    addProgram("My Program");
    long id = loginAsApplicant();
    goTo(controllers.applicant.routes.ApplicantProgramsController.index(id));
    assertThat(browser.pageSource()).contains("My Program");

    // Redirect when "apply" link is clicked.
    browser.$(withText("Apply")).first().click();
    // TODO: This should test the page content when the page is created.
    assertThat(browser.pageSource()).contains("Applicant " + id + " chose program");
  }
}
