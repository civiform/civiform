package controllers.ti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
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
    ApplicantModel managedApplicant = createApplicantWithMockedProfile();
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
                        "clientFirst",
                        "middleName",
                        "middle",
                        "lastName",
                        "ClientLast",
                        "dob",
                        "2022-07-18",
                        "emailAddress",
                        "sample3@fake.com",
                        "tiNote",
                        "unitTest",
                        "phoneNumber",
                        "4259879090")));

    TrustedIntermediaryGroupModel trustedIntermediaryGroup =
        repo.getTrustedIntermediaryGroup(
                profileUtils.currentUserProfile(requestBuilder.build()).get())
            .get();
    Result result = tiController.addClient(trustedIntermediaryGroup.id, requestBuilder.build());
    assertThat(result.status()).isEqualTo(OK);
    Optional<ApplicantModel> testApplicant =
        repo.lookupApplicantByEmail("sample3@fake.com").toCompletableFuture().join();
    AccountModel account = testApplicant.get().getAccount();
    TrustedIntermediaryGroupModel tiGroup2 =
        repo.createNewTrustedIntermediaryGroup("testGroup", "Unit Testing Group");
    account.setManagedByGroup(tiGroup2);
    account.save();

    assertThat(testApplicant.get().getApplicantData().getDateOfBirth().get().toString())
        .isEqualTo("2022-07-18");
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
    assertThatThrownBy(() -> tiController.editClient(account.id, request2))
        .isInstanceOf(ApplicantNotFoundException.class)
        .hasMessage("Applicant not found for ID " + account.id);
  }

  @Test
  public void testShowEditClientForm_ReturnsNotFound() throws ApplicantNotFoundException {
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
    Result result = tiController.showAddClientForm(account.id, request);
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void testEditClient_AllFieldsUpdated() throws ApplicantNotFoundException {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            Helpers.fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "clientFirst",
                        "middleName",
                        "clientMiddle",
                        "lastName",
                        "clientLast",
                        "dob",
                        "2022-07-18",
                        "emailAddress",
                        "testUpdate@fake.com",
                        "tiNote",
                        "unitTest",
                        "phoneNumber",
                        "4259879090")));

    TrustedIntermediaryGroupModel trustedIntermediaryGroup =
        repo.getTrustedIntermediaryGroup(
                profileUtils.currentUserProfile(requestBuilder.build()).get())
            .get();
    Result result = tiController.addClient(trustedIntermediaryGroup.id, requestBuilder.build());
    assertThat(result.status()).isEqualTo(OK);
    Optional<ApplicantModel> testApplicant =
        repo.lookupApplicantByEmail("testUpdate@fake.com").toCompletableFuture().join();
    AccountModel account = testApplicant.get().getAccount();
    account.setManagedByGroup(trustedIntermediaryGroup);
    account.save();
    assertThat(testApplicant.get().getApplicantData().getDateOfBirth().get().toString())
        .isEqualTo("2022-07-18");
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
    Result result2 = tiController.editClient(account.id, request2);

    assertThat(result2.status()).isEqualTo(OK);

    assertThat(repo.lookupAccountByEmail("testUpdate@fake.com")).isEmpty();
    // assert email address
    assertThat(repo.lookupAccountByEmail("emailControllerSam")).isNotEmpty();
    AccountModel accountFinal = repo.lookupAccountByEmail("emailControllerSam").get();
    // assert ti notes
    assertThat(accountFinal.getTiNote()).isEqualTo("unitTest");

    ApplicantModel applicantModel = accountFinal.newestApplicant().get();
    // assert dob,name,phone
    assertThat(applicantModel.getApplicantData().getDateOfBirth().get().toString())
        .isEqualTo("2022-07-07");
    assertThat(applicantModel.getApplicantData().getApplicantFirstName().get())
        .isEqualTo("clientFirst");
    assertThat(applicantModel.getApplicantData().getApplicantMiddleName().get())
        .isEqualTo("clientMiddle");
    assertThat(applicantModel.getApplicantData().getApplicantLastName().get())
        .isEqualTo("clientLast");
    assertThat(applicantModel.getApplicantData().getPhoneNumber().get()).isEqualTo("4259879090");
  }

  @Test
  public void testShowEditClientFormCall() {
    AccountModel account = setupForEditClient("test33@test.com");
    Http.Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result = tiController.showEditClientForm(account.id, request);
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void addClient_WithMissingDob() {
    ApplicantModel managedApplicant = createApplicantWithMockedProfile();
    createTIWithMockedProfile(managedApplicant);
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            Helpers.fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "clientFirst",
                        "middleName",
                        "clientMiddle",
                        "lastName",
                        "clientLast",
                        "dob",
                        "",
                        "emailAddress",
                        "sam2@fake.com",
                        "tiNote",
                        "unitTest",
                        "phoneNumber",
                        "4259879090")));

    TrustedIntermediaryGroupModel trustedIntermediaryGroup =
        repo.getTrustedIntermediaryGroup(
                profileUtils.currentUserProfile(requestBuilder.build()).get())
            .get();
    Result result = tiController.addClient(trustedIntermediaryGroup.id, requestBuilder.build());
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Date of birth required");
  }

  @Test
  public void addClient_WithAllInformation() {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            Helpers.fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "clientFirst",
                        "middleName",
                        "clientMiddle",
                        "lastName",
                        "clientLast",
                        "dob",
                        "2022-07-18",
                        "emailAddress",
                        "sample2@fake.com",
                        "tiNote",
                        "unitTest",
                        "phoneNumber",
                        "4259879090")));

    TrustedIntermediaryGroupModel trustedIntermediaryGroup =
        repo.getTrustedIntermediaryGroup(
                profileUtils.currentUserProfile(requestBuilder.build()).get())
            .get();
    Result result = tiController.addClient(trustedIntermediaryGroup.id, requestBuilder.build());
    assertThat(result.status()).isEqualTo(OK);
    Optional<ApplicantModel> testApplicant =
        repo.lookupApplicantByEmail("sample2@fake.com").toCompletableFuture().join();
    assertThat(testApplicant.get().getApplicantData().getDateOfBirth().get().toString())
        .isEqualTo("2022-07-18");
  }

  private AccountModel setupForEditClient(String email) {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            Helpers.fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "clientFirst",
                        "middleName",
                        "clientMiddle",
                        "lastName",
                        "clientLast",
                        "dob",
                        "2022-07-18",
                        "emailAddress",
                        email,
                        "tiNote",
                        "unitTest",
                        "phoneNumber",
                        "4259879090")));

    TrustedIntermediaryGroupModel trustedIntermediaryGroup =
        repo.getTrustedIntermediaryGroup(
                profileUtils.currentUserProfile(requestBuilder.build()).get())
            .get();
    Result result = tiController.addClient(trustedIntermediaryGroup.id, requestBuilder.build());
    assertThat(result.status()).isEqualTo(OK);
    Optional<ApplicantModel> testApplicant =
        repo.lookupApplicantByEmail(email).toCompletableFuture().join();
    assertThat(testApplicant.get().getApplicantData().getDateOfBirth().get().toString())
        .isEqualTo("2022-07-18");
    AccountModel account = repo.lookupAccountByEmail(email).get();
    return account;
  }
}
