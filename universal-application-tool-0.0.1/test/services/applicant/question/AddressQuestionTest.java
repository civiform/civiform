package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.i18n.Lang;
import play.i18n.Messages;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.types.AddressQuestionDefinition;
import support.QuestionAnswerer;

@RunWith(JUnitParamsRunner.class)
public class AddressQuestionTest {

  private static final AddressQuestionDefinition addressQuestionDefinition =
      new AddressQuestionDefinition(
          "question name",
          Path.create("applicant.my.path.name"),
          Optional.empty(),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"));

  private static final AddressQuestionDefinition noPoBoxAddressQuestionDefinition =
      new AddressQuestionDefinition(
          "question name",
          Path.create("applicant.my.path.name"),
          Optional.empty(),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"),
          AddressQuestionDefinition.AddressValidationPredicates.create(true));

  private final Messages messages = stubMessagesApi().preferred(ImmutableList.of(Lang.defaultLang()));

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
        new ApplicantQuestion(
            addressQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);

    AddressQuestion addressQuestion = new AddressQuestion(applicantQuestion);

    assertThat(addressQuestion.hasTypeSpecificErrors(messages)).isFalse();
    assertThat(addressQuestion.hasQuestionErrors(messages)).isFalse();
  }

  @Test
  public void withValidApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            noPoBoxAddressQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);
    QuestionAnswerer.answerAddressQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        "85 Pike St",
        "Seattle",
        "WA",
        "98101");

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.hasTypeSpecificErrors(messages)).isFalse();
    assertThat(addressQuestion.hasQuestionErrors(messages)).isFalse();
    assertThat(addressQuestion.getStreetValue().get()).isEqualTo("85 Pike St");
    assertThat(addressQuestion.getCityValue().get()).isEqualTo("Seattle");
    assertThat(addressQuestion.getStateValue().get()).isEqualTo("WA");
    assertThat(addressQuestion.getZipValue().get()).isEqualTo("98101");
  }

  @Test
  public void withInvalidApplicantData_missingRequiredFields() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            noPoBoxAddressQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);
    QuestionAnswerer.answerAddressQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "", "", "", "");

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.hasTypeSpecificErrors(messages)).isTrue();
    assertThat(addressQuestion.getStreetErrors(messages)).contains("Please enter valid street name and number.");
    assertThat(addressQuestion.getCityErrors(messages)).contains("Please enter city.");
    assertThat(addressQuestion.getStateErrors(messages)).contains("Please enter state.");
    assertThat(addressQuestion.getZipErrors(messages)).contains("Please enter valid ZIP code.");
  }

  @Test
  @Parameters({"not a zip code", "123456789", "123ab"})
  public void withInvalidApplicantData_invalidZipCode(String zipValue) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            noPoBoxAddressQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);
    QuestionAnswerer.answerAddressQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        "123 A St",
        "Seattle",
        "WA",
        zipValue);

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.hasTypeSpecificErrors(messages)).isTrue();
    assertThat(addressQuestion.getZipErrors(messages)).contains("Please enter valid 5-digit ZIP code.");
    assertThat(addressQuestion.getStreetErrors(messages)).isEmpty();
    assertThat(addressQuestion.getCityErrors(messages)).isEmpty();
    assertThat(addressQuestion.getStateErrors(messages)).isEmpty();
  }

  @Test
  @Parameters({"123 A St", "123 Boxpo Ave", "12345", "1 Box Blvd"})
  public void withNoPoBoxAllowed_withValidApplicantData_passesValidation(String streetValue) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            noPoBoxAddressQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);
    QuestionAnswerer.answerAddressQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        streetValue,
        "Seattle",
        "WA",
        "98107");

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.hasTypeSpecificErrors(messages)).isFalse();
    assertThat(addressQuestion.hasQuestionErrors(messages)).isFalse();
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
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            noPoBoxAddressQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);
    QuestionAnswerer.answerAddressQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        streetValue,
        "Seattle",
        "WA",
        "98107");

    AddressQuestion addressQuestion = applicantQuestion.createAddressQuestion();

    assertThat(addressQuestion.hasTypeSpecificErrors(messages)).isFalse();
    assertThat(addressQuestion.getQuestionErrors(messages)).containsOnly("Please enter a valid address. We do not accept PO Boxes.");
  }
}
