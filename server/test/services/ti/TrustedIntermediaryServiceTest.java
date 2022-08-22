package services.ti;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.test.Helpers.fakeRequest;

import auth.ProfileFactory;
import com.google.common.collect.ImmutableMap;
import controllers.WithMockedProfiles;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import models.Account;
import models.Applicant;
import models.TrustedIntermediaryGroup;
import org.junit.Before;
import org.junit.Test;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import repository.UserRepository;

public class TrustedIntermediaryServiceTest extends WithMockedProfiles {

  private UserRepository repo;

  private TrustedIntermediaryService service;
  private FormFactory formFactory;
  private ProfileFactory profileFactory;

  @Before
  public void setup() {
    repo = instanceOf(UserRepository.class);
    service = instanceOf(TrustedIntermediaryService.class);
    formFactory = instanceOf(FormFactory.class);
    profileFactory = instanceOf(ProfileFactory.class);
    Applicant managedApplicant = createApplicant();
    createTIWithMockedProfile(managedApplicant);
    repo = instanceOf(UserRepository.class);
    profileFactory.createFakeTrustedIntermediary();
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
    TrustedIntermediaryGroup tiGroup = repo.listTrustedIntermediaryGroups().get(0);
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory
            .form(AddApplicantToTrustedIntermediaryGroupForm.class)
            .bindFromRequest(requestBuilder.build());
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm =
        service.addNewClient(form, tiGroup);
    assertThat(returnedForm.error("dob").get().message()).isEqualTo("Date of Birth required");
  }

  @Test
  public void addClient_withUnformmatedDob() {
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
    TrustedIntermediaryGroup tiGroup = repo.listTrustedIntermediaryGroups().get(0);
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
    TrustedIntermediaryGroup tiGroup = repo.listTrustedIntermediaryGroups().get(0);
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
    TrustedIntermediaryGroup tiGroup = repo.listTrustedIntermediaryGroups().get(0);
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
    TrustedIntermediaryGroup tiGroup = repo.listTrustedIntermediaryGroups().get(0);
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
    TrustedIntermediaryGroup tiGroup = repo.listTrustedIntermediaryGroups().get(0);
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
                        "sample1@fake.com",
                        "dob",
                        "2022-07-07")));
    TrustedIntermediaryGroup tiGroup = repo.listTrustedIntermediaryGroups().get(0);
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory
            .form(AddApplicantToTrustedIntermediaryGroupForm.class)
            .bindFromRequest(requestBuilder.build());
    Form<AddApplicantToTrustedIntermediaryGroupForm> returnedForm =
        service.addNewClient(form, tiGroup);
    assertThat(returnedForm).isEqualTo(form);
    Account account = repo.lookupAccountByEmail("sample1@fake.com").get();

    assertThat(account.getApplicants().get(0).getApplicantData().getDateOfBirth().get().toString())
        .isEqualTo("2022-07-07");
  }
}
