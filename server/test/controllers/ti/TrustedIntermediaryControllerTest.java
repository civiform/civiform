package controllers.ti;

import com.google.common.collect.ImmutableMap;
import controllers.WithMockedProfiles;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import models.Account;
import models.Applicant;
import models.TrustedIntermediaryGroup;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.fakeRequest;

public class TrustedIntermediaryControllerTest extends WithMockedProfiles {
  UserRepository repo;
  TrustedIntermediaryGroup tiGroup;
  Http.Request request;
  Applicant managedApplicant = createApplicant();
  TrustedIntermediaryController tiController;


  @Before
  public void setup()
  {
    repo = instanceOf(UserRepository.class);
    tiController = instanceOf(TrustedIntermediaryController.class);
    tiGroup = new TrustedIntermediaryGroup("org", "an org");
    tiGroup.save();
    request = fakeRequest().build();
    createTIWithMockedProfile(managedApplicant);

  }
  @Test
  public void addApplicantTestWithMissingDOB()
  {
    Http.RequestBuilder requestBuilder =
      addCSRFToken(
        Helpers.fakeRequest()
          .bodyForm(
            ImmutableMap.of(
              "firstName",
              "first",
              "middleName",
              "middle",
              "lastName",
              "last",
              "emailAddress",
    "sample@fake.com")));


    Result result = tiController.addApplicant(managedApplicant.id,requestBuilder.build());
    assertThat(result.status()).isEqualTo(SEE_OTHER);


  }
}
