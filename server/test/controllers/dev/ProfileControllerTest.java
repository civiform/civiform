package controllers.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import controllers.WithMockedProfiles;
import models.ApplicantModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;

public class ProfileControllerTest extends WithMockedProfiles {
  private ApplicantModel applicant;
  private ProfileController controller;

  @Before
  public void setUp() throws Exception {
    resetDatabase();
    applicant = createApplicantWithMockedProfile();
    controller = instanceOf(ProfileController.class);
  }

  @Test
  public void testIndexWithNoProfile() {
    Http.Request request =
        addCSRFToken(fakeRequestBuilder().header(skipUserProfile, "true")).build();
    Result result = controller.index(request);
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).isEqualTo("No profile present");
  }

  @Test
  public void testIndexWithProfile() {
    Http.Request request =
        addCSRFToken(fakeRequestBuilder().header(skipUserProfile, "false")).build();
    Result result = controller.index(request);
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(String.format("\"id\" : \"%d\"", applicant.getAccount().id));
  }
}
