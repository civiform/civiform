package services.ti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileFactory;
import com.google.common.collect.ImmutableMap;
import controllers.WithMockedProfiles;
import forms.TiClientInfoForm;
import io.ebean.Model;
import java.util.HashMap;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.AccountModel;
import models.ApplicantModel;
import models.TrustedIntermediaryGroupModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.mvc.Http;
import repository.AccountRepository;
import repository.SearchParameters;
import services.applicant.exception.ApplicantNotFoundException;

@RunWith(JUnitParamsRunner.class)
public class TrustedIntermediaryServiceTest extends WithMockedProfiles {

  private static final ImmutableMap<String, String> CLIENT_DATA =
      ImmutableMap.of(
          "firstName",
          "clientFirst",
          "middleName",
          "middle",
          "lastName",
          "ClientLast",
          "dob",
          "2022-07-07",
          "emailAddress",
          "fake@email.com",
          "tiNote",
          "unitTest",
          "phoneNumber",
          "4259879090");

  private AccountRepository repo;

  private TrustedIntermediaryService service;
  private FormFactory formFactory;
  private TrustedIntermediaryGroupModel tiGroup;
  private TrustedIntermediaryGroupModel tiGroup2;
  AccountModel testAccount;
  ApplicantModel testApplicant;
  private MessagesApi messagesApi;

  @Before
  public void setup() {
    repo = instanceOf(AccountRepository.class);
    service = instanceOf(TrustedIntermediaryService.class);
    formFactory = instanceOf(FormFactory.class);
    ProfileFactory profileFactory = instanceOf(ProfileFactory.class);
    ApplicantModel managedApplicant = createApplicant();
    // Note that this results in a blank managed account being added to tiGroup and tiGroup2
    createTIWithMockedProfile(managedApplicant);
    ApplicantModel managedApplicant2 = createApplicant();
    createTIWithMockedProfile(managedApplicant2);
    profileFactory.createFakeTrustedIntermediary();
    tiGroup = repo.listTrustedIntermediaryGroups().get(0);
    tiGroup2 = repo.listTrustedIntermediaryGroups().get(1);
    testAccount = setupTiClientAccount("email2123", tiGroup);
    testApplicant = setTiClientApplicant(testAccount, "clientFirst", "2021-12-12");
    messagesApi = instanceOf(MessagesApi.class);
  }

  @After
  public void teardown() {
    // Clean up accounts between tests
    tiGroup
        .getManagedAccounts()
        .forEach(
            acct -> {
              acct.getApplicants().forEach(Model::delete);
              acct.delete();
            });
    tiGroup2
        .getManagedAccounts()
        .forEach(
            acct -> {
              acct.getApplicants().forEach(Model::delete);
              acct.delete();
            });
    tiGroup.delete();
    tiGroup2.delete();
  }

  @Test
  @Parameters({
    ", Date of birth required",
    "1850-07-07, Date of Birth should be less than 150 years ago",
    "20-20-20, Please enter a date in the correct format"
  })
  public void addClient_withDobError(String dob, String wantError) {
    var clientData = new HashMap<>(CLIENT_DATA);
    clientData.put("dob", dob);
    Http.RequestBuilder requestBuilder = fakeRequestBuilder().bodyForm(clientData);
    Form<TiClientInfoForm> form =
        formFactory.form(TiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    Form<TiClientInfoForm> returnedForm =
        service
            .addNewClient(form, tiGroup, messagesApi.preferred(requestBuilder.build()))
            .getForm();
    assertThat(returnedForm.error("dob").orElseThrow().message()).isEqualTo(wantError);
  }

  @Test
  public void addClient_withInvalidLastName() {
    var clientData = new HashMap<>(CLIENT_DATA);
    clientData.put("lastName", "");
    Http.RequestBuilder requestBuilder = fakeRequestBuilder().bodyForm(clientData);
    Form<TiClientInfoForm> form =
        formFactory.form(TiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    Form<TiClientInfoForm> returnedForm =
        service
            .addNewClient(form, tiGroup, messagesApi.preferred(requestBuilder.build()))
            .getForm();
    assertThat(returnedForm.error("lastName").orElseThrow().message())
        .isEqualTo("Last name required");
  }

  @Test
  public void addClient_withInvalidFirstName() {
    var clientData = new HashMap<>(CLIENT_DATA);
    clientData.put("firstName", "");
    Http.RequestBuilder requestBuilder = fakeRequestBuilder().bodyForm(clientData);
    Form<TiClientInfoForm> form =
        formFactory.form(TiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    Form<TiClientInfoForm> returnedForm =
        service
            .addNewClient(form, tiGroup, messagesApi.preferred(requestBuilder.build()))
            .getForm();
    assertThat(returnedForm.error("firstName").orElseThrow().message())
        .isEqualTo("First name required");
  }

  @Test
  public void addClient_withEmailAddressExistsError() {
    Http.RequestBuilder requestBuilder = fakeRequestBuilder().bodyForm(CLIENT_DATA);
    Form<TiClientInfoForm> form =
        formFactory.form(TiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    Form<TiClientInfoForm> returnedForm1 =
        service
            .addNewClient(form, tiGroup, messagesApi.preferred(requestBuilder.build()))
            .getForm();
    Form<TiClientInfoForm> returnedForm2 =
        service
            .addNewClient(form, tiGroup, messagesApi.preferred(requestBuilder.build()))
            .getForm();
    // The first form is successful
    assertThat(returnedForm1).isEqualTo(form);
    // The second form has the same emailAddress, so it errors
    assertThat(returnedForm2.error("emailAddress").orElseThrow().message())
        .isEqualTo(
            "Email address already in use. Cannot create applicant if an account already exists.");
  }

  @Test
  public void addClient_withEmptyEmailAddress() {
    var clientData = new HashMap<>(CLIENT_DATA);
    clientData.put("emailAddress", "");
    Http.RequestBuilder requestBuilder = fakeRequestBuilder().bodyForm(clientData);
    Form<TiClientInfoForm> form =
        formFactory.form(TiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    AddNewApplicantReturnObject returnObject =
        service.addNewClient(form, tiGroup, messagesApi.preferred(requestBuilder.build()));
    Form<TiClientInfoForm> returnedForm = returnObject.getForm();
    assertThat(returnedForm.errors()).isEmpty();
    String wantName = String.join(", ", clientData.get("lastName"), clientData.get("firstName"));
    AccountModel account =
        tiGroup.getManagedAccounts().stream()
            .filter(acct -> acct.getApplicantDisplayName().equals(wantName))
            .findFirst()
            .orElseThrow();
    assertThat(account.getApplicants().get(0).getDateOfBirth().orElseThrow().toString())
        .isEqualTo("2022-07-07");
    assertThat(account.newestApplicant().orElseThrow().id).isEqualTo(returnObject.getApplicantId());
    ApplicantModel applicant = account.getApplicants().get(0);
    assertThat(applicant.getDateOfBirth().orElseThrow().toString()).isEqualTo("2022-07-07");
    assertThat(account.getEmailAddress()).isNull();
    assertThat(applicant.getEmailAddress()).isEmpty();
  }

  @Test
  public void addClient_withAllInformation() {
    Http.RequestBuilder requestBuilder = fakeRequestBuilder().bodyForm(CLIENT_DATA);
    Form<TiClientInfoForm> form =
        formFactory.form(TiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    AddNewApplicantReturnObject returnObject =
        service.addNewClient(form, tiGroup, messagesApi.preferred(requestBuilder.build()));
    Form<TiClientInfoForm> returnedForm = returnObject.getForm();
    assertThat(returnedForm).isEqualTo(form);
    AccountModel account = repo.lookupAccountByEmail(CLIENT_DATA.get("emailAddress")).orElseThrow();

    ApplicantModel applicant = account.getApplicants().get(0);
    assertThat(applicant.getDateOfBirth().orElseThrow().toString()).isEqualTo("2022-07-07");
    assertThat(account.newestApplicant().orElseThrow().id).isEqualTo(returnObject.getApplicantId());
  }

  @Test
  public void getManagedAccounts_searchByDob() {
    setupTiClientAccountWithApplicant("First", "2022-07-08", "email1", tiGroup);
    setupTiClientAccountWithApplicant("Second", "2022-07-08", "email2", tiGroup);
    setupTiClientAccountWithApplicant("Third", "2022-12-12", "email3", tiGroup);
    SearchParameters searchParameters =
        SearchParameters.builder()
            .setNameQuery(Optional.empty())
            .setDayQuery(Optional.of("12"))
            .setMonthQuery(Optional.of("12"))
            .setYearQuery(Optional.of("2022"))
            .build();
    TrustedIntermediarySearchResult tiResult =
        service.getManagedAccounts(searchParameters, tiGroup);
    assertThat(tiResult.accounts().size()).isEqualTo(1);
    assertThat(tiResult.accounts().get(0).getEmailAddress()).isEqualTo("email3");
  }

  @Test
  public void getManagedAccounts_searchByName() {
    setupTiClientAccountWithApplicant("First", "2022-07-08", "email10", tiGroup);
    setupTiClientAccountWithApplicant("Emily", "2022-07-08", "email20", tiGroup);
    setupTiClientAccountWithApplicant("Third", "2022-07-10", "email30", tiGroup);
    SearchParameters searchParameters =
        SearchParameters.builder().setNameQuery(Optional.of("Emily")).build();
    TrustedIntermediarySearchResult tiResult =
        service.getManagedAccounts(searchParameters, tiGroup);
    assertThat(tiResult.accounts().size()).isEqualTo(1);
    assertThat(tiResult.accounts().get(0).getEmailAddress()).isEqualTo("email20");
  }

  @Test
  public void getManagedAccounts_searchWithEmptyStringNameAndDob_returnsFullList() {
    setupTiClientAccountWithApplicant("Bobo", "2022-07-08", "bobo@clown.test", tiGroup);
    SearchParameters searchParameters =
        SearchParameters.builder()
            .setNameQuery(Optional.of(""))
            .setDayQuery(Optional.of(""))
            .setMonthQuery(Optional.of(""))
            .setYearQuery(Optional.of(""))
            .build();
    TrustedIntermediarySearchResult tiResult =
        service.getManagedAccounts(searchParameters, tiGroup);
    // The size is 3 because two other accounts are added to the tiGroup in setup()
    assertThat(tiResult.accounts().size()).isEqualTo(3);
  }

  @Test
  public void getManagedAccounts_searchWithEmptyOptionalNameAndDob_returnsFullList() {
    setupTiClientAccountWithApplicant("Bobo", "2022-07-08", "bobo@clown.test", tiGroup);
    SearchParameters searchParameters =
        SearchParameters.builder()
            .setNameQuery(Optional.empty())
            .setDayQuery(Optional.empty())
            .setMonthQuery(Optional.empty())
            .setYearQuery(Optional.empty())
            .build();
    TrustedIntermediarySearchResult tiResult =
        service.getManagedAccounts(searchParameters, tiGroup);
    // The size is 3 because two other accounts are added to the tiGroup in setup()
    assertThat(tiResult.accounts().size()).isEqualTo(3);
  }

  @Test
  // In this case, getManagedAccounts returns an empty client list and the view displays an error
  // message
  public void getManagedAccounts_searchWithEmptyStringNameAndPartialDob_returnsNoClients() {
    setupTiClientAccountWithApplicant("Bobo", "2022-07-08", "bobo@clown.test", tiGroup);
    SearchParameters searchParameters =
        SearchParameters.builder()
            .setNameQuery(Optional.of(""))
            .setDayQuery(Optional.of("16"))
            .setMonthQuery(Optional.of(""))
            .setYearQuery(Optional.of(""))
            .build();
    TrustedIntermediarySearchResult tiResult =
        service.getManagedAccounts(searchParameters, tiGroup);
    assertThat(tiResult.accounts().size()).isEqualTo(0);
  }

  @Test
  // In this case, getManagedAccounts searches by name and the partial DOB is ignored
  public void getManagedAccounts_searchWithNameAndPartialDob_returnsClientsByName() {
    setupTiClientAccountWithApplicant("Bobo", "2022-07-08", "bobo@clown.test", tiGroup);
    SearchParameters searchParameters =
        SearchParameters.builder()
            .setNameQuery(Optional.of("Bobo"))
            .setDayQuery(Optional.of("16"))
            .setMonthQuery(Optional.of(""))
            .setYearQuery(Optional.of(""))
            .build();
    TrustedIntermediarySearchResult tiResult =
        service.getManagedAccounts(searchParameters, tiGroup);
    assertThat(tiResult.accounts().size()).isEqualTo(1);
    assertThat(tiResult.accounts().get(0).getApplicantDisplayName()).contains("Bobo");
  }

  @Test
  public void getManagedAccounts_ExpectUnformattedDobException() {
    setupTiClientAccountWithApplicant("First", "2022-07-08", "email11", tiGroup);
    setupTiClientAccountWithApplicant("Second", "2022-10-10", "email21", tiGroup);
    setupTiClientAccountWithApplicant("Third", "2022-07-10", "email31", tiGroup);
    SearchParameters searchParameters =
        SearchParameters.builder()
            .setNameQuery(Optional.empty())
            .setDayQuery(Optional.of("2"))
            .setMonthQuery(Optional.of("Feb"))
            .setYearQuery(Optional.of("2"))
            .build();
    TrustedIntermediarySearchResult tiResult =
        service.getManagedAccounts(searchParameters, tiGroup);
    assertThat(tiResult.accounts().size()).isEqualTo(tiGroup.getManagedAccounts().size());
    assertThat(tiResult.errorMessage().orElseThrow())
        .isEqualTo("Please enter a " + "valid birth date.");
  }

  @Test
  public void editTiClientInfo_AllPass_NameEmailUpdate() throws ApplicantNotFoundException {
    AccountModel account = setupTiClientAccount("emailOld", tiGroup);
    ApplicantModel applicant = setTiClientApplicant(account, "clientFirst", "2021-12-12");

    Http.RequestBuilder requestBuilder = fakeRequestBuilder().bodyForm(CLIENT_DATA);
    Form<TiClientInfoForm> form =
        formFactory.form(TiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    Form<TiClientInfoForm> returnForm =
        service.updateClientInfo(
            form, tiGroup, account.id, messagesApi.preferred(requestBuilder.build()));

    assertThat(returnForm).isEqualTo(form);
    AccountModel accountFinal = repo.lookupAccount(account.id).orElseThrow();
    ApplicantModel applicantFinal = repo.lookupApplicantSync(applicant.id).orElseThrow();

    assertThat(accountFinal.getTiNote()).isEqualTo(CLIENT_DATA.get("tiNote"));
    assertThat(applicantFinal.getDateOfBirth().orElseThrow().toString())
        .isEqualTo(CLIENT_DATA.get("dob"));
    assertThat(applicantFinal.getPhoneNumber()).hasValue(CLIENT_DATA.get("phoneNumber"));
    assertThat(applicantFinal.getApplicantName()).hasValue("ClientLast, clientFirst");
    assertThat(accountFinal.getEmailAddress()).isEqualTo(CLIENT_DATA.get("emailAddress"));
  }

  @Test
  @Parameters({
    "42598790, This phone number is invalid",
    "0000000000, This phone number is invalid",
    "42598790UI, Phone number cannot contain non-number characters",
  })
  public void editTiClientInfo_PhoneLengthValidationFail(String testNumber, String wantErrorMsg)
      throws ApplicantNotFoundException {
    var clientData = new HashMap<>(CLIENT_DATA);
    clientData.put("phoneNumber", testNumber);
    Http.RequestBuilder requestBuilder = fakeRequestBuilder().bodyForm(clientData);
    Form<TiClientInfoForm> form =
        formFactory.form(TiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    Form<TiClientInfoForm> returnForm =
        service.updateClientInfo(
            form, tiGroup, testAccount.id, messagesApi.preferred(requestBuilder.build()));
    assertThat(returnForm.error("phoneNumber").orElseThrow().message()).isEqualTo(wantErrorMsg);
  }

  @Test
  @Parameters({"phoneNumber", "emailAddress", "tiNote"})
  public void editTiClientInfo_AllowedEmptyFieldsDoesNotFail(String field)
      throws ApplicantNotFoundException {
    var clientData = new HashMap<>(CLIENT_DATA);
    clientData.put(field, "");
    Http.RequestBuilder requestBuilder = fakeRequestBuilder().bodyForm(clientData);
    Form<TiClientInfoForm> form =
        formFactory.form(TiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    Form<TiClientInfoForm> returnForm =
        service.updateClientInfo(
            form, tiGroup, testAccount.id, messagesApi.preferred(requestBuilder.build()));
    assertThat(returnForm).isEqualTo(form);
  }

  @Test
  public void editTiClientInfo_futureDOB_validationFails() throws ApplicantNotFoundException {
    AccountModel account = setupTiClientAccount("email1123", tiGroup);
    ApplicantModel applicant = setTiClientApplicant(account, "clientFirst", "2021-12-12");
    var clientData = new HashMap<>(CLIENT_DATA);
    clientData.put("dob", "2040-07-07");

    Http.RequestBuilder requestBuilder = fakeRequestBuilder().bodyForm(clientData);
    Form<TiClientInfoForm> form =
        formFactory.form(TiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    Form<TiClientInfoForm> returnForm =
        service.updateClientInfo(
            form, tiGroup, account.id, messagesApi.preferred(requestBuilder.build()));
    assertThat(returnForm.error("dob").orElseThrow().message())
        .isEqualTo("Date of Birth should be in the past");
    assertThat(applicant.getDateOfBirth()).isNotEmpty();
  }

  @Test
  public void editTiClientInfo_missingFirstName_validationFail() throws ApplicantNotFoundException {
    AccountModel account = setupTiClientAccount("email121", tiGroup);
    ApplicantModel applicant = setTiClientApplicant(account, "clientFirst", "2021-12-12");
    var clientData = new HashMap<>(CLIENT_DATA);
    clientData.put("firstName", "");
    Http.RequestBuilder requestBuilder = fakeRequestBuilder().bodyForm(clientData);
    Form<TiClientInfoForm> form =
        formFactory.form(TiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    Form<TiClientInfoForm> returnForm =
        service.updateClientInfo(
            form, tiGroup, account.id, messagesApi.preferred(requestBuilder.build()));
    var error = returnForm.error("firstName");
    assertThat(error).isPresent();
    assertThat(error.get().message()).isEqualTo("First name required");
    assertThat(applicant.getFirstName()).isNotEmpty();
  }

  @Test
  public void editTiClientInfo_missingLastNameValidationFail() throws ApplicantNotFoundException {
    AccountModel account = setupTiClientAccount("email121", tiGroup);
    ApplicantModel applicant = setTiClientApplicant(account, "clientFirst", "2021-12-12");
    var clientData = new HashMap<>(CLIENT_DATA);
    clientData.put("lastName", "");
    Http.RequestBuilder requestBuilder = fakeRequestBuilder().bodyForm(clientData);
    Form<TiClientInfoForm> form =
        formFactory.form(TiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    Form<TiClientInfoForm> returnForm =
        service.updateClientInfo(
            form, tiGroup, account.id, messagesApi.preferred(requestBuilder.build()));
    var error = returnForm.error("lastName");
    assertThat(error).isPresent();
    assertThat(error.get().message()).isEqualTo("Last name required");
    assertThat(applicant.getLastName()).isNotEmpty();
  }

  @Test
  public void editTiClientInfo_throwsException() {
    // There is no Account in the system, so there is no Account to update.
    Http.RequestBuilder requestBuilder = fakeRequestBuilder().bodyForm(CLIENT_DATA);
    Form<TiClientInfoForm> form =
        formFactory.form(TiClientInfoForm.class).bindFromRequest(requestBuilder.build());
    long MISSING_ACCOUNT_ID = 1L;
    assertThatThrownBy(
            () ->
                service.updateClientInfo(
                    form,
                    tiGroup,
                    MISSING_ACCOUNT_ID,
                    messagesApi.preferred(requestBuilder.build())))
        .isInstanceOf(ApplicantNotFoundException.class)
        .hasMessage("Applicant not found for ID 1");
  }

  private void setupTiClientAccountWithApplicant(
      String firstName, String dob, String email, TrustedIntermediaryGroupModel tiGroup) {
    AccountModel account = setupTiClientAccount(email, tiGroup);
    setTiClientApplicant(account, firstName, dob);
  }

  private AccountModel setupTiClientAccount(String email, TrustedIntermediaryGroupModel tiGroup) {
    AccountModel account = new AccountModel();
    account.setEmailAddress(email);
    account.setManagedByGroup(tiGroup);
    account.save();
    return account;
  }

  private ApplicantModel setTiClientApplicant(AccountModel account, String firstName, String dob) {
    ApplicantModel applicant = new ApplicantModel();
    applicant.setAccount(account);
    applicant.setUserName(
        firstName,
        /* middleName= */ Optional.empty(),
        Optional.of("Last"),
        /* nameSuffix= */ Optional.empty());
    applicant.setDateOfBirth(dob);
    applicant.save();
    account.save();
    return applicant;
  }
}
