package controllers.ti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.fakeRequest;
import static support.CfTestHelpers.requestBuilderWithSettings;

import auth.ProfileFactory;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import controllers.WithMockedProfiles;
import java.util.Optional;
import models.AccountModel;
import models.ApplicantModel;
import models.TrustedIntermediaryGroupModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import repository.AccountRepository;
import services.applicant.ApplicantData;
import services.applicant.exception.ApplicantNotFoundException;

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
  public void testUpdateClientInfo_ThrowsApplicantNotFoundException()
      throws ApplicantNotFoundException {
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
                        "sample3@fake.com",
                        "dob",
                        "2022-07-18")));

    TrustedIntermediaryGroupModel trustedIntermediaryGroup =
        repo.getTrustedIntermediaryGroup(
                profileUtils.currentUserProfile(requestBuilder.build()).get())
            .get();
    Result result = tiController.addApplicant(trustedIntermediaryGroup.id, requestBuilder.build());
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    Optional<ApplicantModel> testApplicant =
        repo.lookupApplicantByEmail("sample3@fake.com").toCompletableFuture().join();
    AccountModel account = testApplicant.get().getAccount();
    TrustedIntermediaryGroupModel tiGroup2 =
        repo.createNewTrustedIntermediaryGroup("testGroup", "Unit Testing Group");
    account.setManagedByGroup(tiGroup2);
    account.save();

    // TODO (#5503): Remove when we remove the feature flag
    assertThat(
            testApplicant.get().getApplicantData().getDateOfBirthAtWellKnownPath().get().toString())
        .isEqualTo("2022-07-18");
    assertThat(testApplicant.get().getDateOfBirth().get().toString()).isEqualTo("2022-07-18");
    Http.RequestBuilder requestBuilder2 =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "clientFirst",
                        "middleName",
                        "clientMiddle",
                        "lastName",
                        "clientLast",
                        "dob",
                        "2022-07-07",
                        "emailAddress",
                        "emailControllerSam",
                        "tiNote",
                        "unitTest",
                        "phoneNumber",
                        "4259879090")));
    Http.Request request2 = requestBuilder2.build();
    assertThatThrownBy(() -> tiController.updateClientInfo(account.id, request2))
        .isInstanceOf(ApplicantNotFoundException.class)
        .hasMessage("Applicant not found for ID " + account.id);
  }

  @Test
  public void testEditClient_ReturnsNotFound() {
    resetDatabase();
    AccountModel account = createApplicantWithMockedProfile().getAccount();
    account.setEmailAddress("test@ReturnsNotfound");
    account.save();
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
    Result result = tiController.editClient(account.id, request);
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void testUpdateClientInfo_AllFieldsUpdated() throws ApplicantNotFoundException {
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
                        "testUpdate@fake.com",
                        "dob",
                        "2022-07-18")));

    TrustedIntermediaryGroupModel trustedIntermediaryGroup =
        repo.getTrustedIntermediaryGroup(
                profileUtils.currentUserProfile(requestBuilder.build()).get())
            .get();
    Result result = tiController.addApplicant(trustedIntermediaryGroup.id, requestBuilder.build());
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    Optional<ApplicantModel> testApplicant =
        repo.lookupApplicantByEmail("testUpdate@fake.com").toCompletableFuture().join();
    AccountModel account = testApplicant.get().getAccount();
    account.setManagedByGroup(trustedIntermediaryGroup);
    account.save();

    ApplicantModel applicant = testApplicant.get();
    ApplicantData data = applicant.getApplicantData();
    // TODO (#5503): Remove when we remove the feature flag
    assertThat(data.getDateOfBirthAtWellKnownPath().get().toString()).isEqualTo("2022-07-18");
    assertThat(data.getApplicantFirstNameAtWellKnownPath().get()).isEqualTo("first");
    assertThat(data.getApplicantMiddleNameAtWellKnownPath().get()).isEqualTo("middle");
    assertThat(data.getApplicantLastNameAtWellKnownPath().get()).isEqualTo("last");

    assertThat(applicant.getFirstName().get()).isEqualTo("first");
    assertThat(applicant.getMiddleName().get()).isEqualTo("middle");
    assertThat(applicant.getLastName().get()).isEqualTo("last");
    assertThat(applicant.getAccount().getEmailAddress()).isEqualTo("testUpdate@fake.com");
    assertThat(applicant.getEmailAddress().get()).isEqualTo("testUpdate@fake.com");
    assertThat(applicant.getDateOfBirth().get().toString()).isEqualTo("2022-07-18");
    Http.RequestBuilder requestBuilder2 =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "clientFirst",
                        "middleName",
                        "clientMiddle",
                        "lastName",
                        "clientLast",
                        "dob",
                        "2022-07-07",
                        "emailAddress",
                        "emailControllerSam",
                        "tiNote",
                        "unitTest",
                        "phoneNumber",
                        "4259879090")));
    Http.Request request2 = requestBuilder2.build();
    Result result2 = tiController.updateClientInfo(account.id, request2);

    assertThat(result2.status()).isEqualTo(SEE_OTHER);

    assertThat(repo.lookupAccountByEmail("testUpdate@fake.com")).isEmpty();
    // assert email address
    assertThat(repo.lookupAccountByEmail("emailControllerSam")).isNotEmpty();
    AccountModel accountFinal = repo.lookupAccountByEmail("emailControllerSam").get();
    // assert ti notes
    assertThat(accountFinal.getTiNote()).isEqualTo("unitTest");

    applicant = accountFinal.newestApplicant().get();
    // TODO (#5503): Remove when we remove the feature flag
    data = applicant.getApplicantData();
    assertThat(data.getDateOfBirthAtWellKnownPath().get().toString()).isEqualTo("2022-07-07");
    assertThat(data.getApplicantFirstNameAtWellKnownPath().get()).isEqualTo("clientFirst");
    assertThat(data.getApplicantMiddleNameAtWellKnownPath().get()).isEqualTo("clientMiddle");
    assertThat(data.getApplicantLastNameAtWellKnownPath().get()).isEqualTo("clientLast");
    assertThat(data.getPhoneNumberAtWellKnownPath().get()).isEqualTo("4259879090");

    assertThat(applicant.getFirstName().get()).isEqualTo("clientFirst");
    assertThat(applicant.getMiddleName().get()).isEqualTo("clientMiddle");
    assertThat(applicant.getLastName().get()).isEqualTo("clientLast");
    assertThat(applicant.getPhoneNumber().get()).isEqualTo("4259879090");
    assertThat(applicant.getAccount().getEmailAddress()).isEqualTo("emailControllerSam");
    assertThat(applicant.getEmailAddress().get()).isEqualTo("emailControllerSam");
    assertThat(applicant.getDateOfBirth().get().toString()).isEqualTo("2022-07-07");
  }

  @Test
  public void testEditClientCall() {
    AccountModel account = setupForEditUpdateClient("test33@test.com");
    Http.Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result = tiController.editClient(account.id, request);
    assertThat(result.status()).isEqualTo(OK);
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

    TrustedIntermediaryGroupModel trustedIntermediaryGroup =
        repo.getTrustedIntermediaryGroup(
                profileUtils.currentUserProfile(requestBuilder.build()).get())
            .get();
    Result result = tiController.addApplicant(trustedIntermediaryGroup.id, requestBuilder.build());
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    ApplicantModel applicant =
        repo.lookupApplicantByEmail("sample2@fake.com").toCompletableFuture().join().get();
    ApplicantData data = applicant.getApplicantData();
    // TODO (#5503): Remove when we remove the feature flag
    assertThat(data.getDateOfBirthAtWellKnownPath().get().toString()).isEqualTo("2022-07-18");
    assertThat(data.getApplicantFirstNameAtWellKnownPath().get()).isEqualTo("first");
    assertThat(data.getApplicantMiddleNameAtWellKnownPath().get()).isEqualTo("middle");
    assertThat(data.getApplicantLastNameAtWellKnownPath().get()).isEqualTo("last");

    assertThat(applicant.getFirstName().get()).isEqualTo("first");
    assertThat(applicant.getMiddleName().get()).isEqualTo("middle");
    assertThat(applicant.getLastName().get()).isEqualTo("last");
    assertThat(applicant.getAccount().getEmailAddress()).isEqualTo("sample2@fake.com");
    assertThat(applicant.getEmailAddress().get()).isEqualTo("sample2@fake.com");
    assertThat(applicant.getDateOfBirth().get().toString()).isEqualTo("2022-07-18");
  }

  private AccountModel setupForEditUpdateClient(String email) {
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
                        email,
                        "dob",
                        "2022-07-18")));

    TrustedIntermediaryGroupModel trustedIntermediaryGroup =
        repo.getTrustedIntermediaryGroup(
                profileUtils.currentUserProfile(requestBuilder.build()).get())
            .get();
    Result result = tiController.addApplicant(trustedIntermediaryGroup.id, requestBuilder.build());
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    Optional<ApplicantModel> testApplicant =
        repo.lookupApplicantByEmail(email).toCompletableFuture().join();
    assertThat(testApplicant.get().getDateOfBirth().get().toString()).isEqualTo("2022-07-18");
    AccountModel account = repo.lookupAccountByEmail(email).get();
    return account;
  }
}
