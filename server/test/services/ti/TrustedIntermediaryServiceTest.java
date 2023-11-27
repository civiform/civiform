package services.ti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.test.Helpers.fakeRequest;

import auth.ProfileFactory;
import com.google.common.collect.ImmutableMap;
import controllers.WithMockedProfiles;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import forms.UpdateApplicantDobForm;
import java.util.Optional;
import models.AccountModel;
import models.ApplicantModel;
import models.TrustedIntermediaryGroupModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import repository.AccountRepository;
import repository.SearchParameters;
import services.applicant.ApplicantData;
import services.applicant.exception.ApplicantNotFoundException;

public class TrustedIntermediaryServiceTest extends WithMockedProfiles {

  private AccountRepository repo;

  private TrustedIntermediaryService service;
  private FormFactory formFactory;
  private ProfileFactory profileFactory;
  private TrustedIntermediaryGroupModel tiGroup;
  private TrustedIntermediaryGroupModel tiGroup2;

  @Before
  public void setup() {
    repo = instanceOf(AccountRepository.class);
    service = instanceOf(TrustedIntermediaryService.class);
    formFactory = instanceOf(FormFactory.class);
    profileFactory = instanceOf(ProfileFactory.class);
    ApplicantModel managedApplicant = createApplicant();
    createTIWithMockedProfile(managedApplicant);
    ApplicantModel managedApplicant2 = createApplicant();
    createTIWithMockedProfile(managedApplicant2);
    profileFactory.createFakeTrustedIntermediary();
    tiGroup = repo.listTrustedIntermediaryGroups().get(0);
    tiGroup2 = repo.listTrustedIntermediaryGroups().get(1);
  }

  @After
  public void teardown() {
    // Clean up accounts between tests
    tiGroup.getManagedAccounts().stream()
        .forEach(
            acct -> {
              acct.getApplicants().stream().forEach(app -> app.delete());
              acct.delete();
            });
    tiGroup2.getManagedAccounts().stream()
        .forEach(
            acct -> {
              acct.getApplicants().stream().forEach(app -> app.delete());
              acct.delete();
            });
    tiGroup.delete();
    tiGroup2.delete();
  }

  @Test
  public void addClient_withMissingDob() {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
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
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory
            .form(AddApplicantToTrustedIntermediaryGroupForm.class)
            .bindFromRequest(requestBuilder.build());
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm =
        service.addNewClient(form, tiGroup);
    assertThat(returnedForm.error("dob").get().message()).isEqualTo("Date of Birth required");
  }

  @Test
  public void addClient_withInvalidDob() {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
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
                        "1865-07-07")));
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory
            .form(AddApplicantToTrustedIntermediaryGroupForm.class)
            .bindFromRequest(requestBuilder.build());
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm =
        service.addNewClient(form, tiGroup);
    assertThat(returnedForm.error("dob").get().message())
        .isEqualTo("Date of Birth should be less than 150 years ago");
  }

  @Test
  public void addClient_withUnformattedDob() {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
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
                        "20-20-20")));
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory
            .form(AddApplicantToTrustedIntermediaryGroupForm.class)
            .bindFromRequest(requestBuilder.build());
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm =
        service.addNewClient(form, tiGroup);
    assertThat(returnedForm.error("dob").get().message())
        .isEqualTo("Date of Birth must be in MM/dd/yyyy format");
  }

  @Test
  public void addClient_withInvalidLastName() {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "first",
                        "middleName",
                        "middle",
                        "lastName",
                        "",
                        "emailAddress",
                        "sample1@fake.com",
                        "dob",
                        "2022-07-07")));
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory
            .form(AddApplicantToTrustedIntermediaryGroupForm.class)
            .bindFromRequest(requestBuilder.build());
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm =
        service.addNewClient(form, tiGroup);
    assertThat(returnedForm.error("lastName").get().message()).isEqualTo("Last name required");
  }

  @Test
  public void addClient_WithInvalidFirstName() {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "",
                        "middleName",
                        "middle",
                        "lastName",
                        "Last",
                        "emailAddress",
                        "sample1@fake.com",
                        "dob",
                        "2012-07-07")));
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory
            .form(AddApplicantToTrustedIntermediaryGroupForm.class)
            .bindFromRequest(requestBuilder.build());
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm =
        service.addNewClient(form, tiGroup);
    assertThat(returnedForm.error("firstName").get().message()).isEqualTo("First name required");
  }

  @Test
  public void addClient_WithEmailAddressExistsError() {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "First",
                        "middleName",
                        "middle",
                        "lastName",
                        "Last",
                        "emailAddress",
                        "sample@fake.com",
                        "dob",
                        "2012-07-07")));
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory
            .form(AddApplicantToTrustedIntermediaryGroupForm.class)
            .bindFromRequest(requestBuilder.build());
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm1 =
        service.addNewClient(form, tiGroup);
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm2 =
        service.addNewClient(form, tiGroup);
    // The first form is successful
    assertThat(returnedForm1).isEqualTo(form);
    // The second form has the same emailAddress, so it errors
    assertThat(returnedForm2.error("emailAddress").get().message())
        .isEqualTo(
            "Email address already in use. Cannot create applicant if an account already exists.");
  }

  @Test
  public void addClient_WithEmptyEmailAddress() {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "No",
                        "middleName",
                        "middle",
                        "lastName",
                        "Email",
                        "emailAddress",
                        "",
                        "dob",
                        "2011-11-11")));
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory
            .form(AddApplicantToTrustedIntermediaryGroupForm.class)
            .bindFromRequest(requestBuilder.build());
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm =
        service.addNewClient(form, tiGroup);
    assertThat(returnedForm.errors()).isEmpty();
    AccountModel account =
        tiGroup.getManagedAccounts().stream()
            .filter(acct -> acct.getApplicantName().equals("Email, No"))
            .findFirst()
            .get();
    assertThat(account.getApplicants().get(0).getApplicantData().getDateOfBirth().get().toString())
        .isEqualTo("2011-11-11");
    assertThat(account.getEmailAddress()).isNull();
  }

  @Test
  public void addClient_WithAllInformation() {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(
            fakeRequest()
                .bodyForm(
                    ImmutableMap.of(
                        "firstName",
                        "First",
                        "middleName",
                        "middle",
                        "lastName",
                        "Last",
                        "emailAddress",
                        "add1@fake.com",
                        "dob",
                        "2022-07-07")));
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory
            .form(AddApplicantToTrustedIntermediaryGroupForm.class)
            .bindFromRequest(requestBuilder.build());
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm =
        service.addNewClient(form, tiGroup);
    assertThat(returnedForm).isEqualTo(form);
    AccountModel account = repo.lookupAccountByEmail("add1@fake.com").get();

    assertThat(account.getApplicants().get(0).getApplicantData().getDateOfBirth().get().toString())
        .isEqualTo("2022-07-07");
  }

  @Test
  public void getManagedAccounts_SearchByDob() {
    setupTIAccount("First", "2022-07-08", "email1", tiGroup);
    setupTIAccount("Second", "2022-07-08", "email2", tiGroup);
    setupTIAccount("Third", "2022-12-12", "email3", tiGroup);
    SearchParameters searchParameters =
        SearchParameters.builder()
            .setNameQuery(Optional.empty())
            .setDateQuery(Optional.of("2022-12-12"))
            .build();
    TrustedIntermediarySearchResult tiResult =
        service.getManagedAccounts(searchParameters, tiGroup);
    assertThat(tiResult.getAccounts().get().size()).isEqualTo(1);
    assertThat(tiResult.getAccounts().get().get(0).getEmailAddress()).isEqualTo("email3");
  }

  @Test
  public void getManagedAccounts_SearchByName() {
    setupTIAccount("First", "2022-07-08", "email10", tiGroup);
    setupTIAccount("Emily", "2022-07-08", "email20", tiGroup);
    setupTIAccount("Third", "2022-07-10", "email30", tiGroup);
    SearchParameters searchParameters =
        SearchParameters.builder()
            .setNameQuery(Optional.of("Emily"))
            .setDateQuery(Optional.empty())
            .build();
    TrustedIntermediarySearchResult tiResult =
        service.getManagedAccounts(searchParameters, tiGroup);
    assertThat(tiResult.getAccounts().get().size()).isEqualTo(1);
    assertThat(tiResult.getAccounts().get().get(0).getEmailAddress()).isEqualTo("email20");
  }

  @Test
  public void getManagedAccounts_ExpectUnformattedDobException() {
    setupTIAccount("First", "2022-07-08", "email11", tiGroup);
    setupTIAccount("Second", "2022-10-10", "email21", tiGroup);
    setupTIAccount("Third", "2022-07-10", "email31", tiGroup);
    SearchParameters searchParameters =
        SearchParameters.builder()
            .setNameQuery(Optional.empty())
            .setDateQuery(Optional.of("22-22-22"))
            .build();
    TrustedIntermediarySearchResult tiResult =
        service.getManagedAccounts(searchParameters, tiGroup);
    assertThat(tiResult.getAccounts().get().size()).isEqualTo(tiGroup.getManagedAccounts().size());
    assertThat(tiResult.getErrorMessage().get())
        .isEqualTo("Please enter date in MM/dd/yyyy format");
  }

  private void setupTIAccount(
      String firstName, String dob, String email, TrustedIntermediaryGroupModel tiGroup) {
    AccountModel account = new AccountModel();
    account.setEmailAddress(email);
    account.setManagedByGroup(tiGroup);
    account.save();
    ApplicantModel applicant = new ApplicantModel();
    applicant.setAccount(account);
    ApplicantData applicantData = applicant.getApplicantData();
    applicantData.setUserName(firstName, "", "Last");
    applicantData.setDateOfBirth(dob);
    applicant.save();
  }

  @Test
  public void updateApplicantDateOfBirth_throwsApplicantNotFoundException() {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(fakeRequest().bodyForm(ImmutableMap.of("dob", "2022-07-07")));
    Form<UpdateApplicantDobForm> form =
        formFactory.form(UpdateApplicantDobForm.class).bindFromRequest(requestBuilder.build());
    assertThatThrownBy(() -> service.updateApplicantDateOfBirth(tiGroup, (long) 0, form))
        .isInstanceOf(ApplicantNotFoundException.class)
        .hasMessage("Applicant not found for ID 0");
  }

  @Test
  public void updateApplicantDateOfBirth_throwsApplicantNotFoundExceptionDueToIncorrectTIGroup() {
    setupTIAccount("First", "2021-11-11", "fake@email.com", tiGroup);
    Http.RequestBuilder requestBuilder =
        addCSRFToken(fakeRequest().bodyForm(ImmutableMap.of("dob", "2022-07-07")));
    Form<UpdateApplicantDobForm> form =
        formFactory.form(UpdateApplicantDobForm.class).bindFromRequest(requestBuilder.build());
    AccountModel account = tiGroup.getManagedAccounts().stream().findAny().get();
    assertThatThrownBy(() -> service.updateApplicantDateOfBirth(tiGroup2, account.id, form))
        .isInstanceOf(ApplicantNotFoundException.class)
        .hasMessage("Applicant not found for ID " + account.id);
  }

  @Test
  public void updateApplicantDateOfBirth_unformattedDate() throws ApplicantNotFoundException {
    setupTIAccount("First", "2021-11-11", "fake@email.com", tiGroup);
    Http.RequestBuilder requestBuilder =
        addCSRFToken(fakeRequest().bodyForm(ImmutableMap.of("dob", "2022-20-20")));
    Form<UpdateApplicantDobForm> form =
        formFactory.form(UpdateApplicantDobForm.class).bindFromRequest(requestBuilder.build());
    AccountModel account = repo.lookupAccountByEmail("fake@email.com").get();
    Form<UpdateApplicantDobForm> returnedForm =
        service.updateApplicantDateOfBirth(tiGroup, account.id, form);
    assertThat(returnedForm.hasErrors()).isTrue();
    assertThat(returnedForm.error("dob").get().message())
        .isEqualTo("Date of Birth must be in MM/dd/yyyy format");
  }

  @Test
  public void updateApplicantDateOfBirth_ApplicantDobUpdated() throws ApplicantNotFoundException {
    setupTIAccount("First", "2021-11-11", "fake@email.com", tiGroup);
    Http.RequestBuilder requestBuilder =
        addCSRFToken(fakeRequest().bodyForm(ImmutableMap.of("dob", "2021-09-09")));
    Form<UpdateApplicantDobForm> form =
        formFactory.form(UpdateApplicantDobForm.class).bindFromRequest(requestBuilder.build());
    AccountModel account = repo.lookupAccountByEmail("fake@email.com").get();
    Form<UpdateApplicantDobForm> returnedForm =
        service.updateApplicantDateOfBirth(tiGroup, account.id, form);
    assertThat(returnedForm.hasErrors()).isFalse();
    assertThat(account.newestApplicant().get().getApplicantData().getDateOfBirth().get().toString())
        .isEqualTo("2021-09-09");
  }
}
