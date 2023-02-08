package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.LocalizedStrings;
import services.MessageKey;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.program.ProgramQuestionDefinition;
import services.question.types.AddressQuestionDefinition;
import support.QuestionAnswerer;

@RunWith(JUnitParamsRunner.class)
public class AddressQuestionTest {

  private static final AddressQuestionDefinition addressQuestionDefinition =
      new AddressQuestionDefinition(
          OptionalLong.of(1),
          "question name",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"),
          AddressQuestionDefinition.AddressValidationPredicates.create(),
          /* lastModifiedTime= */ Optional.empty());

  private static final AddressQuestionDefinition noPoBoxAddressQuestionDefinition =
      new AddressQuestionDefinition(
          OptionalLong.of(1),
          "question name",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"),
          AddressQuestionDefinition.AddressValidationPredicates.create(true),
          /* lastModifiedTime= */ Optional.empty());

  private Applicant applicant;
  private ApplicantData applicantData;

  @Before
  public void setUp() {
    applicant = new Applicant();
    applicantData = applicant.getApplicantData();
  }

  @Test
  public void withEmptyApplicantData_optionalQuestion() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            ProgramQuestionDefinition.create(addressQuestionDefinition, Optional.empty())
                .setOptional(true),
            applicantData,
            Optional.empty());

    AddressQuestion addressQuestion = new AddressQuestion(applicantQuestion);

    assertThat(addressQuestion.getValidationErrors().isEmpty()).isTrue();
  }

  @Test
  public void withValidApplicantDataAndNoGeoValues() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(addressQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerAddressQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        "PO Box 123",
        "Line 2",
        "Seattle",
        "WA",
        "98101");

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.getValidationErrors().isEmpty()).isTrue();
    assertThat(addressQuestion.getStreetValue().get()).isEqualTo("PO Box 123");
    assertThat(addressQuestion.getLine2Value().get()).isEqualTo("Line 2");
    assertThat(addressQuestion.getCityValue().get()).isEqualTo("Seattle");
    assertThat(addressQuestion.getStateValue().get()).isEqualTo("WA");
    assertThat(addressQuestion.getZipValue().get()).isEqualTo("98101");
  }

  @Test
  public void withValidApplicantDataWithGeoValues() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(addressQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerAddressQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        "PO Box 123",
        "Line 2",
        "Seattle",
        "WA",
        "98101",
        "true",
        10.1,
        -20.1,
        1000L);

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.getValidationErrors().isEmpty()).isTrue();
    assertThat(addressQuestion.getStreetValue().get()).isEqualTo("PO Box 123");
    assertThat(addressQuestion.getLine2Value().get()).isEqualTo("Line 2");
    assertThat(addressQuestion.getCityValue().get()).isEqualTo("Seattle");
    assertThat(addressQuestion.getStateValue().get()).isEqualTo("WA");
    assertThat(addressQuestion.getZipValue().get()).isEqualTo("98101");
    assertThat(addressQuestion.getCorrectedValue().get()).isEqualTo("true");
    assertThat(addressQuestion.getLatitudeValue().get()).isEqualTo(10.1);
    assertThat(addressQuestion.getLongitudeValue().get()).isEqualTo(-20.1);
    assertThat(addressQuestion.getWellKnownIdValue().get()).isEqualTo(1000L);
  }

  @Test
  public void withInvalidApplicantData_missingRequiredFields() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(noPoBoxAddressQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerAddressQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "", "", "", "", "");
    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.getValidationErrors())
        .isEqualTo(
            ImmutableMap.of(
                addressQuestion.getStreetPath(),
                    ImmutableSet.of(
                        ValidationErrorMessage.create(
                            MessageKey.ADDRESS_VALIDATION_STREET_REQUIRED)),
                addressQuestion.getCityPath(),
                    ImmutableSet.of(
                        ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_CITY_REQUIRED)),
                addressQuestion.getStatePath(),
                    ImmutableSet.of(
                        ValidationErrorMessage.create(
                            MessageKey.ADDRESS_VALIDATION_STATE_REQUIRED)),
                addressQuestion.getZipPath(),
                    ImmutableSet.of(
                        ValidationErrorMessage.create(
                            MessageKey.ADDRESS_VALIDATION_INVALID_ZIPCODE))));
  }

  @Test
  @Parameters({"not a zip code", "123456789", "123ab"})
  public void withInvalidApplicantData_invalidZipCode(String zipValue) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(noPoBoxAddressQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerAddressQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        "123 A St",
        "Unit B",
        "Seattle",
        "WA",
        zipValue);

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.getValidationErrors())
        .isEqualTo(
            ImmutableMap.of(
                addressQuestion.getZipPath(),
                ImmutableSet.of(
                    ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_INVALID_ZIPCODE))));
  }

  @Test
  @Parameters({"123 A St", "123 Boxpo Ave", "12345", "1 Box Blvd"})
  public void withNoPoBoxAllowed_withValidApplicantData_passesValidation(String streetValue) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(noPoBoxAddressQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerAddressQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        streetValue,
        "Unit B",
        "Seattle",
        "WA",
        "98107");

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.getValidationErrors().isEmpty()).isTrue();
  }

  @Test
  @Parameters({
    "PO Box 123,Line 2",
    "PO box 123,Line 2",
    "pO Box 123,Line 2",
    "po box 123,Line 2",
    "P.O. Box 123,Line 2",
    "p.o. box 123,Line 2",
    "My P.O. Box ABC,Line 2",
    "po-box 555,Line 2",
    "Sesame St,PO Box 123"
  })
  public void withNoPoBoxAllowed_withInvalidApplicantData_failsValidation(
      String streetValue, String line2Value) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(noPoBoxAddressQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerAddressQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        streetValue,
        line2Value,
        "Seattle",
        "WA",
        "98107");

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors =
        addressQuestion.getValidationErrors();
    assertThat(validationErrors.size()).isEqualTo(1);
    assertThat(
            validationErrors.getOrDefault(
                applicantQuestion.getContextualizedPath(), ImmutableSet.of()))
        .containsOnly(ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_NO_PO_BOX));
  }

  @Test
  @Parameters
  public void getAnswerString(
      String streetValue,
      String line2Value,
      String cityValue,
      String stateValue,
      String zipValue,
      String expected) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(noPoBoxAddressQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerAddressQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        streetValue,
        line2Value,
        cityValue,
        stateValue,
        zipValue);

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.getAnswerString()).isEqualTo(expected);
  }

  private Object[] parametersForGetAnswerString() {
    return new Object[] {
      new Object[] {
        "111 A St", "Unit B", "Seattle", "WA", "98111", "111 A St\nUnit B\nSeattle, WA 98111"
      },
      new Object[] {"111 A St", "", "Seattle", "WA", "98111", "111 A St\nSeattle, WA 98111"},
      new Object[] {"111 A St", "", "", "WA", "98111", "111 A St\nWA 98111"},
      new Object[] {"111 A St", "", "Seattle", "", "98111", "111 A St\nSeattle, 98111"},
      new Object[] {"111 A St", "Unit B", "", "", "", "111 A St\nUnit B"}
    };
  }
}
