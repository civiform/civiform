package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.Applicant;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.types.AddressQuestionDefinition;

@RunWith(JUnitParamsRunner.class)
public class AddressQuestionTest {

  private static final AddressQuestionDefinition addressQuestionDefinition =
      new AddressQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
          Optional.empty(),
          "description",
          LifecycleStage.ACTIVE,
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"));

  private static final AddressQuestionDefinition noPoBoxAddressQuestionDefinition =
      new AddressQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
          Optional.empty(),
          "description",
          LifecycleStage.ACTIVE,
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"),
          AddressQuestionDefinition.AddressValidationPredicates.create(true));

  private Applicant applicant;
  private ApplicantData applicantData;

  @Before
  public void setUp() {
    applicant = new Applicant();
    applicantData = applicant.getApplicantData();
  }

  @Test
  public void withEmptyApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(addressQuestionDefinition, applicantData);

    AddressQuestion addressQuestion = new AddressQuestion(applicantQuestion);

    assertThat(addressQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(addressQuestion.hasQuestionErrors()).isFalse();
  }

  @Test
  public void withValidApplicantData() {
    applicantData.putString(addressQuestionDefinition.getStreetPath(), "85 Pike St");
    applicantData.putString(addressQuestionDefinition.getCityPath(), "Seattle");
    applicantData.putString(addressQuestionDefinition.getStatePath(), "WA");
    applicantData.putString(addressQuestionDefinition.getZipPath(), "98101");
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(addressQuestionDefinition, applicantData);

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(addressQuestion.hasQuestionErrors()).isFalse();
    assertThat(addressQuestion.getStreetValue().get()).isEqualTo("85 Pike St");
    assertThat(addressQuestion.getCityValue().get()).isEqualTo("Seattle");
    assertThat(addressQuestion.getStateValue().get()).isEqualTo("WA");
    assertThat(addressQuestion.getZipValue().get()).isEqualTo("98101");
  }

  @Test
  public void withInvalidApplicantData_missingRequiredFields() {
    applicantData.putString(addressQuestionDefinition.getStreetPath(), "");
    applicantData.putString(addressQuestionDefinition.getCityPath(), "");
    applicantData.putString(addressQuestionDefinition.getStatePath(), "");
    applicantData.putString(addressQuestionDefinition.getZipPath(), "");
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(addressQuestionDefinition, applicantData);

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.hasTypeSpecificErrors()).isTrue();
    assertThat(addressQuestion.getStreetErrors())
        .contains(ValidationErrorMessage.create("Please enter valid street name and number."));
    assertThat(addressQuestion.getCityErrors())
        .contains(ValidationErrorMessage.create("Please enter city."));
    assertThat(addressQuestion.getStateErrors())
        .contains(ValidationErrorMessage.create("Please enter state."));
    assertThat(addressQuestion.getZipErrors())
        .contains(ValidationErrorMessage.create("Please enter valid ZIP code."));
  }

  @Test
  @Parameters({"not a zip code", "123456789", "123ab"})
  public void withInvalidApplicantData_invalidZipCode(String zipValue) {
    applicantData.putString(addressQuestionDefinition.getStreetPath(), "123 A St");
    applicantData.putString(addressQuestionDefinition.getCityPath(), "Seattle");
    applicantData.putString(addressQuestionDefinition.getStatePath(), "WA");
    applicantData.putString(addressQuestionDefinition.getZipPath(), zipValue);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(addressQuestionDefinition, applicantData);

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.hasTypeSpecificErrors()).isTrue();
    assertThat(addressQuestion.getZipErrors())
        .contains(ValidationErrorMessage.create("Please enter valid 5-digit ZIP code."));
    assertThat(addressQuestion.getStreetErrors()).isEmpty();
    assertThat(addressQuestion.getCityErrors()).isEmpty();
    assertThat(addressQuestion.getStateErrors()).isEmpty();
  }

  @Test
  @Parameters({"123 A St", "123 Boxpo Ave", "12345", "1 Box Blvd"})
  public void withNoPoBoxAllowed_withValidApplicantData_passesValidation(String streetValue) {
    applicantData.putString(noPoBoxAddressQuestionDefinition.getStreetPath(), streetValue);
    applicantData.putString(noPoBoxAddressQuestionDefinition.getCityPath(), "Seattle");
    applicantData.putString(noPoBoxAddressQuestionDefinition.getStatePath(), "WA");
    applicantData.putString(noPoBoxAddressQuestionDefinition.getZipPath(), "98107");
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(noPoBoxAddressQuestionDefinition, applicantData);

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(addressQuestion.hasQuestionErrors()).isFalse();
  }

  @Test
  @Parameters({
    "PO Box 123",
    "PO box 123",
    "pO Box 123",
    "po box 123",
    "P.O. Box 123",
    "p.o. box 123",
    "My P.O. Box ABC",
    "po-box 555"
  })
  public void withNoPoBoxAllowed_withInvalidApplicantData_failsValidation(String streetValue) {
    applicantData.putString(noPoBoxAddressQuestionDefinition.getStreetPath(), streetValue);
    applicantData.putString(noPoBoxAddressQuestionDefinition.getCityPath(), "Seattle");
    applicantData.putString(noPoBoxAddressQuestionDefinition.getStatePath(), "WA");
    applicantData.putString(noPoBoxAddressQuestionDefinition.getZipPath(), "98107");
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(noPoBoxAddressQuestionDefinition, applicantData);

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(addressQuestion.getQuestionErrors())
        .containsOnly(
            ValidationErrorMessage.create(
                "Please enter a valid address. We do not accept PO Boxes."));
  }
}
