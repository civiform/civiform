package controllers.ti;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.SEE_OTHER;

import auth.ProfileFactory;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import controllers.WithMockedProfiles;
import java.util.Optional;
import models.ApplicantModel;
import models.TrustedIntermediaryGroupModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import repository.AccountRepository;

public class TrustedIntermediaryControllerTest extends WithMockedProfiles {
  private AccountRepository repo;
  private TrustedIntermediaryController tiController;
  private ProfileFactory profileFactory;
  private ProfileUtils profileUtils;

  @Before
  public void setup() {
    profileFactory = instanceOf(ProfileFactory.class);
    tiController = instanceOf(TrustedIntermediaryController.class);
    ApplicantModel managedApplicant = createApplicant();
    createTIWithMockedProfile(managedApplicant);
    repo = instanceOf(AccountRepository.class);
    profileFactory.createFakeTrustedIntermediary();
    profileUtils = instanceOf(ProfileUtils.class);
  }

  @Test
  public void addApplicant_WithMissingDob() {
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
                        "sample1@fake.com",
                        "dob",
                        "")));
    Http.Request request = requestBuilder.build();
    TrustedIntermediaryGroupModel trustedIntermediaryGroup =
        repo.getTrustedIntermediaryGroup(profileUtils.currentUserProfile(request).get()).get();
    Result result = tiController.addApplicant(trustedIntermediaryGroup.id, request);
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().get("error").get().trim()).isEqualTo("Date of Birth required");
  }

  @Test
  public void addApplicant_WithAllInformation() {
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
    TrustedIntermediaryGroupModel group = repo.listTrustedIntermediaryGroups().get(0);
    Result result = tiController.addApplicant(group.id, requestBuilder.build());
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    Optional<ApplicantModel> testApplicant =
        repo.lookupApplicantByEmail("sample2@fake.com").toCompletableFuture().join();
    assertThat(testApplicant.get().getApplicantData().getDateOfBirth().get().toString())
        .isEqualTo("2022-07-18");
  }

  //  @Test
  //  public void testUpdateDOBFunctionWithExistingDob() throws ApplicantNotFoundException {
  //
  //    AddApplicantToTrustedIntermediaryGroupForm form =
  //        new AddApplicantToTrustedIntermediaryGroupForm();
  //    form.setEmailAddress("sample3@example.com");
  //    form.setFirstName("foo");
  //    form.setLastName("bar");
  //    form.setDob("2022-07-10");
  //
  //    Http.RequestBuilder requestBuilder =
  //        addCSRFToken(Helpers.fakeRequest().bodyForm(ImmutableMap.of("dob", "2022-05-05")));
  //    Http.Request request = requestBuilder.build();
  //    TrustedIntermediaryGroupModel trustedIntermediaryGroup =
  //        repo.getTrustedIntermediaryGroup(profileUtils.currentUserProfile(request).get()).get();
  //    repo.createNewApplicantForTrustedIntermediaryGroup(form, trustedIntermediaryGroup);
  //    Optional<AccountModel> account = repo.lookupAccountByEmail("sample3@example.com");
  //    Result result = tiController.updateDateOfBirth(account.get().id, request);
  //    assertThat(result.status()).isEqualTo(SEE_OTHER);
  //    Optional<ApplicantModel> applicant =
  //        repo.lookupAccountByEmail("sample3@example.com").get().newestApplicant();
  //    assertThat(applicant.get().getApplicantData().getDateOfBirth().get().toString())
  //        .isEqualTo("2022-05-05");
  //  }
}
