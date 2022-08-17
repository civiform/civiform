package services.ti;

import auth.ProfileFactory;
import com.google.common.collect.ImmutableMap;
import controllers.WithMockedProfiles;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import static play.api.test.CSRFTokenHelper.addCSRFToken;

import models.Account;
import models.Applicant;
import models.TrustedIntermediaryGroup;
import org.junit.Before;
import org.junit.Test;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;

import repository.UserRepository;
import services.DateConverter;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeRequest;

public class TrustedIntermediaryServiceTest extends WithMockedProfiles {

  UserRepository repo;

  TrustedIntermediaryService service;
  FormFactory formFactory;
  ProfileFactory profileFactory;

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
  public void testAddClientWithInvalidDOB() {
    Http.RequestBuilder requestBuilder =
      addCSRFToken(fakeRequest()
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
      formFactory.form(AddApplicantToTrustedIntermediaryGroupForm.class).bindFromRequest(requestBuilder.build());
    TIClientCreationResult tiClientCreationResult = service.addNewClient(form, tiGroup);
    assertThat(tiClientCreationResult.getForm().get().error("dob").get().message())
      .isEqualTo("Date of Birth required");

  }
  @Test
  public void testAddClientWithUnformattedDOB() {
    Http.RequestBuilder requestBuilder =
      addCSRFToken(fakeRequest()
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
      formFactory.form(AddApplicantToTrustedIntermediaryGroupForm.class).bindFromRequest(requestBuilder.build());
    TIClientCreationResult tiClientCreationResult = service.addNewClient(form, tiGroup);
    assertThat(tiClientCreationResult.getForm().get().error("dob").get().message())
      .isEqualTo("Date of Birth must be in MM-dd-yyyy format");
  }
  @Test
  public void testAddClientWithInvalidLastName() {
    Http.RequestBuilder requestBuilder =
      addCSRFToken(fakeRequest()
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
      formFactory.form(AddApplicantToTrustedIntermediaryGroupForm.class).bindFromRequest(requestBuilder.build());
    TIClientCreationResult tiClientCreationResult = service.addNewClient(form, tiGroup);
    assertThat(tiClientCreationResult.getForm().get().error("lastName").get().message())
      .isEqualTo("Last name required");
  }
  @Test
  public void testAddClientWithInvalidFirstName() {
    Http.RequestBuilder requestBuilder =
      addCSRFToken(fakeRequest()
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
      formFactory.form(AddApplicantToTrustedIntermediaryGroupForm.class).bindFromRequest(requestBuilder.build());
    TIClientCreationResult tiClientCreationResult = service.addNewClient(form, tiGroup);
    assertThat(tiClientCreationResult.getForm().get().error("firstName").get().message())
      .isEqualTo("First name required");
  }
  @Test
  public void testAddClientWithInvalidEmailAddress() {
    Http.RequestBuilder requestBuilder =
      addCSRFToken(fakeRequest()
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
      formFactory.form(AddApplicantToTrustedIntermediaryGroupForm.class).bindFromRequest(requestBuilder.build());
    TIClientCreationResult tiClientCreationResult = service.addNewClient(form, tiGroup);
    assertThat(tiClientCreationResult.getForm().get().error("emailAddress").get().message())
      .isEqualTo("Email Address required");
  }
  @Test
  public void testAddClientWithAllInformation() {
    Http.RequestBuilder requestBuilder =
      addCSRFToken(fakeRequest()
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
      formFactory.form(AddApplicantToTrustedIntermediaryGroupForm.class).bindFromRequest(requestBuilder.build());
    TIClientCreationResult tiClientCreationResult = service.addNewClient(form, tiGroup);
    assertThat(tiClientCreationResult.getForm()).isEqualTo(Optional.empty());
    Account account = repo.lookupAccountByEmail("sample1@fake.com").get();

    assertThat(account.getApplicants().get(0).getApplicantData().getDateOfBirth().get().toString()).isEqualTo("2022-07-07");
  }


}
