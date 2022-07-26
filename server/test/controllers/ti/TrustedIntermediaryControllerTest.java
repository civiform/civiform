package controllers.ti;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.SEE_OTHER;

import auth.CiviFormProfileData;
import auth.ProfileFactory;
import com.google.common.collect.ImmutableMap;
import controllers.WithMockedProfiles;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import java.util.Optional;
import models.Applicant;
import models.TrustedIntermediaryGroup;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import repository.UserRepository;

public class TrustedIntermediaryControllerTest extends WithMockedProfiles {
  UserRepository repo;
  TrustedIntermediaryController tiController;
  ProfileFactory profileFactory;
  CiviFormProfileData data;

  @Before
  public void setup() {
    profileFactory = instanceOf(ProfileFactory.class);
    tiController = instanceOf(TrustedIntermediaryController.class);
    Applicant managedApplicant = createApplicant();
    createTIWithMockedProfile(managedApplicant);
    repo = instanceOf(UserRepository.class);
    data = profileFactory.createFakeTrustedIntermediary();
  }

  @Test(expected = NullPointerException.class)
  public void addApplicantTestWithMissingDob() {
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
    Result result = tiController.addApplicant(group.id + 1, requestBuilder.build());
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    Optional<Applicant> testApplicant =
        repo.lookupApplicantByEmail("sample2@fake.com").toCompletableFuture().join();
    assertThat(testApplicant.get().getApplicantData().getDateOfBirth().get().toString())
        .isEqualTo("2022-07-18");
  }

  @Test
  public void testUpdateDOBFunctionWithExistingDob() {

    TrustedIntermediaryGroup group = repo.listTrustedIntermediaryGroups().get(0);
    AddApplicantToTrustedIntermediaryGroupForm form =
        new AddApplicantToTrustedIntermediaryGroupForm();
    form.setEmailAddress("sample3@example.com");
    form.setFirstName("foo");
    form.setLastName("bar");
    form.setDob("2022-07-10");
    repo.createNewApplicantForTrustedIntermediaryGroup(form, group);
    Optional<Applicant> applicant =
        repo.lookupApplicantByEmail("sample3@example.com").toCompletableFuture().join();
    Http.RequestBuilder requestBuilder =
        addCSRFToken(Helpers.fakeRequest().bodyForm(ImmutableMap.of("dob", "2022-10-05")));
    Result result = tiController.updateDateOfBirth(applicant.get().id, requestBuilder.build());
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    Optional<Applicant> finalApplicant =
        repo.lookupApplicant(applicant.get().id).toCompletableFuture().join();
    assertThat(finalApplicant.get().getApplicantData().getDateOfBirth().get().toString())
        .isEqualTo("2022-10-05");
  }
}
