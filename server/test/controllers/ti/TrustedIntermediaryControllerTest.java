package controllers.ti;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import com.google.common.collect.ImmutableMap;
import controllers.WithMockedProfiles;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import models.Account;
import models.Applicant;
import models.TrustedIntermediaryGroup;
import org.junit.Before;
import org.junit.Test;
import org.testng.annotations.ExpectedExceptions;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.fakeRequest;

public class TrustedIntermediaryControllerTest extends WithMockedProfiles {
  UserRepository repo;
  TrustedIntermediaryGroup tiGroup;
  Http.Request request;
  Account tiAccount;
  TrustedIntermediaryController tiController;
  ProfileFactory profileFactory;
  CiviFormProfileData data;


  @Before
  public void setup()
  {
     tiAccount = createTIWithMockedProfile(createApplicant());
    profileFactory = instanceOf(ProfileFactory.class);
    data = profileFactory.createFakeTrustedIntermediary();
    repo = instanceOf(UserRepository.class);
    tiController = instanceOf(TrustedIntermediaryController.class);
    tiGroup = new TrustedIntermediaryGroup("org", "an org");
    tiGroup.save();
    request = fakeRequest().build();
  }
  @Test(expected = IllegalArgumentException.class)
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
    TrustedIntermediaryGroup group = repo.listTrustedIntermediaryGroups().get(0);
    Result result = tiController.addApplicant(group.id,requestBuilder.build());
    assertThat(result.flash().get("error")).isEqualTo("providedDateOfBirth cannot be null");
  }
}
