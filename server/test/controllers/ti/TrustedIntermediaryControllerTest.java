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
import models.Account;
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

  @Test
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
              "sample1@fake.com")));
    TrustedIntermediaryGroup group = repo.listTrustedIntermediaryGroups().get(0);
    Result result = tiController.addApplicant(group.id +2, requestBuilder.build());
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().get("error").get()).isEqualTo("Date Of Birth required.");
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
    Optional<Account> account = repo.lookupAccountByEmail("sample3@example.com");
    Http.RequestBuilder requestBuilder =
        addCSRFToken(Helpers.fakeRequest().bodyForm(ImmutableMap.of("dob", "2022-05-05")));
    Result result = tiController.updateDateOfBirth(account.get().id, requestBuilder.build());
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    Optional<Applicant> applicant = repo.lookupAccount(account.get().id).get().newestApplicant();
    assertThat(applicant.get().getApplicantData().getDateOfBirth().get().toString())
        .isEqualTo("2022-05-05");
  }
}
