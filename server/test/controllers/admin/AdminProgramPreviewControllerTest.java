package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.mvc.Http.Status.SEE_OTHER;

import controllers.WithMockedProfiles;
import models.Account;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import play.test.Helpers;

public class AdminProgramPreviewControllerTest extends WithMockedProfiles {

  private AdminProgramPreviewController controller;

  @Before
  public void setup() {
    controller = instanceOf(AdminProgramPreviewController.class);
  }

  @Test
  public void preview_redirectsToProgramReviewPage() {
    Account adminAccount = createGlobalAdminWithMockedProfile();
    long programId = 0;
    Result result = controller.preview(Helpers.fakeRequest().build(), programId);
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            controllers.applicant.routes.ApplicantProgramReviewController.review(
                    adminAccount.ownedApplicantIds().get(0), programId)
                .url());
  }

  @Test
  public void preview_withNoProfileThrowsException() {
    assertThatThrownBy(() -> controller.preview(Helpers.fakeRequest().build(), /*p rogramId =*/ 0))
        .isInstanceOf(RuntimeException.class);
  }
}
