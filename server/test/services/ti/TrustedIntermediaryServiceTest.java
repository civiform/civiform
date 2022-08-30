package services.ti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.test.Helpers.fakeRequest;

import auth.ProfileFactory;
import com.google.common.collect.ImmutableMap;
import controllers.WithMockedProfiles;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import forms.UpdateApplicantDobForm;
import java.util.Optional;
import models.Account;
import models.Applicant;
import models.TrustedIntermediaryGroup;
import org.junit.Before;
import org.junit.Test;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import repository.SearchParameters;
import repository.UserRepository;
import services.applicant.ApplicantData;
import services.applicant.exception.ApplicantNotFoundException;

public class TrustedIntermediaryServiceTest extends WithMockedProfiles {

  private UserRepository repo;

  private TrustedIntermediaryService service;
  private FormFactory formFactory;
  private ProfileFactory profileFactory;
  private TrustedIntermediaryGroup tiGroup;
  private TrustedIntermediaryGroup tiGroup2;

  @Before
  public void setup() {
    repo = instanceOf(UserRepository.class);
    service = instanceOf(TrustedIntermediaryService.class);
    formFactory = instanceOf(FormFactory.class);
    profileFactory = instanceOf(ProfileFactory.class);
    Applicant managedApplicant = createApplicant();
    createTIWithMockedProfile(managedApplicant);
    Applicant managedApplicant2 = createApplicant();
    createTIWithMockedProfile(managedApplicant2);
    repo = instanceOf(UserRepository.class);
    profileFactory.createFakeTrustedIntermediary();
    tiGroup = repo.listTrustedIntermediaryGroups().get(0);
    tiGroup2 = repo.listTrustedIntermediaryGroups().get(1);
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
  public void addClient_WithInvalidEmailAddress() {
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
                        "",
                        "dob",
                        "2012-07-07")));
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory
            .form(AddApplicantToTrustedIntermediaryGroupForm.class)
            .bindFromRequest(requestBuilder.build());
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm =
        service.addNewClient(form, tiGroup);
    assertThat(returnedForm.error("emailAddress").get().message())
        .isEqualTo("Email Address required");
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
    Account account = repo.lookupAccountByEmail("add1@fake.com").get();

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
      String firstName, String dob, String email, TrustedIntermediaryGroup tiGroup) {
    Account account = new Account();
    account.setEmailAddress(email);
    account.setManagedByGroup(tiGroup);
    account.save();
    Applicant applicant = new Applicant();
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
    assertThrows(
        ApplicantNotFoundException.class,
        () -> service.updateApplicantDateOfBirth(tiGroup, (long) 0, form));
  }

  @Test
  public void updateApplicantDateOfBirth_throwsApplicantNotFoundExceptionDueToIncorrectTIGroup() {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(fakeRequest().bodyForm(ImmutableMap.of("dob", "2022-07-07")));
    Form<UpdateApplicantDobForm> form =
        formFactory.form(UpdateApplicantDobForm.class).bindFromRequest(requestBuilder.build());
    Account account = tiGroup.getManagedAccounts().stream().findAny().get();
    assertThrows(
        ApplicantNotFoundException.class,
        () -> service.updateApplicantDateOfBirth(tiGroup2, account.id, form));
  }

  @Test
  public void updateApplicantDateOfBirth_unformattedDate() throws ApplicantNotFoundException {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(fakeRequest().bodyForm(ImmutableMap.of("dob", "2022-20-20")));
    Form<UpdateApplicantDobForm> form =
        formFactory.form(UpdateApplicantDobForm.class).bindFromRequest(requestBuilder.build());
    Account account = repo.lookupAccountByEmail("email30").get();
    Form<UpdateApplicantDobForm> returnedForm =
        service.updateApplicantDateOfBirth(tiGroup, account.id, form);
    assertThat(returnedForm.hasErrors()).isTrue();
    assertThat(returnedForm.error("dob").get().message())
        .isEqualTo("Date of Birth must be in MM/dd/yyyy format");
  }

  @Test
  public void updateApplicantDateOfBirth_ApplicantDobUpdated() throws ApplicantNotFoundException {
    Http.RequestBuilder requestBuilder =
        addCSRFToken(fakeRequest().bodyForm(ImmutableMap.of("dob", "2021-09-09")));
    Form<UpdateApplicantDobForm> form =
        formFactory.form(UpdateApplicantDobForm.class).bindFromRequest(requestBuilder.build());
    Account account = repo.lookupAccountByEmail("email30").get();
    Form<UpdateApplicantDobForm> returnedForm =
        service.updateApplicantDateOfBirth(tiGroup, account.id, form);
    assertThat(returnedForm.hasErrors()).isFalse();
    assertThat(account.newestApplicant().get().getApplicantData().getDateOfBirth().get().toString())
        .isEqualTo("2021-09-09");
  }
}
