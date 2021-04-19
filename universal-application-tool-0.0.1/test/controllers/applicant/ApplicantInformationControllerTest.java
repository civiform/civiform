package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;

public class ApplicantInformationControllerTest extends WithMockedApplicantProfiles {

  private Applicant currentApplicant;
  private ApplicantInformationController controller;

  @Before
  public void setup() {
    resourceCreator().clearDatabase();
    controller = instanceOf(ApplicantInformationController.class);
    currentApplicant = createApplicantWithMockedProfile();
  }

  @Test
  public void edit_differentApplicant_returnsUnauthorizedResult() {
    Result result =
        controller
            .edit(fakeRequest().build(), currentApplicant.id + 1)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void update_differentApplicant_returnsUnauthorizedResult() {
    Result result =
        controller
            .update(fakeRequest().build(), currentApplicant.id + 1)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void edit_usesHumanReadableLanguagesInsteadOfIsoTags() {
    Result result =
        controller
            .edit(addCSRFToken(fakeRequest()).build(), currentApplicant.id)
            .toCompletableFuture()
            .join();
    assertThat(contentAsString(result)).contains("English");
    assertThat(contentAsString(result)).contains("español");
  }
}
