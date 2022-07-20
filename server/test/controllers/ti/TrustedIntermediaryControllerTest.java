package controllers.ti;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.ProfileUtils;
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
import scala.App;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.fakeRequest;

public class TrustedIntermediaryControllerTest extends WithMockedProfiles {
  UserRepository repo;
  TrustedIntermediaryController tiController;
  ProfileFactory profileFactory;
  CiviFormProfileData data;


  @Before
  public void setup()
  {
    profileFactory = instanceOf(ProfileFactory.class);
    tiController = instanceOf(TrustedIntermediaryController.class);
    Applicant managedApplicant = createApplicant();
    createTIWithMockedProfile(managedApplicant);
    repo = instanceOf(UserRepository.class);
    data = profileFactory.createFakeTrustedIntermediary();
  }
  @Test(expected = NullPointerException.class)
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
    Result result = tiController.addApplicant(group.id, requestBuilder.build());
    assertThat(result.flash().get("error")).isEqualTo("providedDateOfBirth cannot be null");
  }

  @Test
  public void addApplicantTestWithAllInformation() {
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
              "sample2@fake.com",
              "dob",
              "2022-07-18")));
    TrustedIntermediaryGroup group = repo.listTrustedIntermediaryGroups().get(0);
    Result result = tiController.addApplicant(group.id, requestBuilder.build());
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    Optional<Applicant> testApplicant = repo.lookupApplicantByEmail("sample2@fake.com").toCompletableFuture().join();
    assertThat(testApplicant.get().getApplicantData().getDateOfBirth()).isEqualTo("2022-07-18");
  }

  @Test
  public void testUpdateDOBFunction() {
    Applicant managedApplicant = createApplicant();
    repo = instanceOf(UserRepository.class);
   createTIWithMockedProfile(managedApplicant);
    data = profileFactory.createFakeTrustedIntermediary();
    Http.RequestBuilder requestBuilder =
      addCSRFToken(
        Helpers.fakeRequest()
          .bodyForm(
            ImmutableMap.of(
              "dob",
              "2022-10-05")));
    TrustedIntermediaryGroup group = repo.listTrustedIntermediaryGroups().get(0);
    AddApplicantToTrustedIntermediaryGroupForm form = new AddApplicantToTrustedIntermediaryGroupForm();
    form.setEmailAddress("sample3@example.com");
    form.setFirstName("foo");
    form.setLastName("bar");
    form.setDob("2022-07-10");
    repo.createNewApplicantForTrustedIntermediaryGroup(form,group);
    Optional<Applicant> applicant = repo.lookupApplicantByEmail("sample3@example.com").toCompletableFuture().join();
    Result result = tiController.updateDateOfBirth(group.id+2,applicant.get().id,requestBuilder.build());
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    Optional<Applicant> finalApplicant = repo.lookupApplicant(applicant.get().id).toCompletableFuture().join();
    assertThat(finalApplicant.get().getApplicantData().getDateOfBirth().get().toString()).isEqualTo("2022-07-10");

  }

}
