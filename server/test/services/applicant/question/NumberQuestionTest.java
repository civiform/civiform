package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.ApplicantModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.MessageKey;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.QuestionAnswerer;
import services.question.types.NumberQuestionDefinition;
import services.question.types.QuestionDefinitionConfig;

@RunWith(JUnitParamsRunner.class)
public class NumberQuestionTest extends ResetPostgres {

  private static final NumberQuestionDefinition numberQuestionDefinition =
      new NumberQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("question name")
              .setDescription("description")
              .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
              .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
              .setId(OptionalLong.of(1))
              .setLastModifiedTime(Optional.empty())
              .build());

  private static final NumberQuestionDefinition minAndMaxNumberQuestionDefinition =
      new NumberQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("question name")
              .setDescription("description")
              .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
              .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
              .setValidationPredicates(
                  NumberQuestionDefinition.NumberValidationPredicates.create(50, 100))
              .setId(OptionalLong.of(1))
              .setLastModifiedTime(Optional.empty())
              .build());

  private ApplicantModel applicant;
  private ApplicantData applicantData;
  private Messages messages;

  @Before
  public void setUp() {
    applicant = new ApplicantModel();
    applicantData = applicant.getApplicantData();
    messages = instanceOf(MessagesApi.class).preferred(ImmutableList.of(Lang.defaultLang()));
  }

  @Test
  public void withEmptyApplicantData_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(numberQuestionDefinition, applicantData, Optional.empty());

    NumberQuestion numberQuestion = new NumberQuestion(applicantQuestion);

    assertThat(numberQuestion.getValidationErrors()).isEmpty();
  }

  @Test
  public void withEmptyValueAtPath_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(numberQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerNumberQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "");

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    assertThat(numberQuestion.getValidationErrors()).isEmpty();
    assertThat(numberQuestion.getNumberValue()).isEmpty();
  }

  @Test
  public void withValidValue_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(numberQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerNumberQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 800);

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    assertThat(numberQuestion.getValidationErrors()).isEmpty();
    assertThat(numberQuestion.getNumberValue().get()).isEqualTo(800);
  }

  @Test
  @Parameters({"50", "75", "100"})
  public void withMinAndMaxValue_withValidValue_passesValidation(long value) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(minAndMaxNumberQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerNumberQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), value);

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    assertThat(numberQuestion.getValidationErrors()).isEmpty();
    assertThat(numberQuestion.getNumberValue().get()).isEqualTo(value);
  }

  @SuppressWarnings("unused") // Used by @Parameters annotation of test below
  private Object[][] withMinAndMaxValue_withInvalidValue_failsValidation_parameters() {
    return new Object[][] {
      {-1, ValidationErrorMessage.create(MessageKey.NUMBER_VALIDATION_NON_INTEGER)},
      {0, ValidationErrorMessage.create(MessageKey.NUMBER_VALIDATION_TOO_SMALL, 50L)},
      {49, ValidationErrorMessage.create(MessageKey.NUMBER_VALIDATION_TOO_SMALL, 50L)},
      {101, ValidationErrorMessage.create(MessageKey.NUMBER_VALIDATION_TOO_BIG, 100L)},
      {999, ValidationErrorMessage.create(MessageKey.NUMBER_VALIDATION_TOO_BIG, 100L)},
    };
  }

  @Test
  @Parameters(method = "withMinAndMaxValue_withInvalidValue_failsValidation_parameters")
  public void withMinAndMaxValue_withInvalidValue_failsValidation(
      long value, ValidationErrorMessage expectedError) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(minAndMaxNumberQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerNumberQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), value);

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    assertThat(numberQuestion.getValidationErrors())
        .isEqualTo(ImmutableMap.of(numberQuestion.getNumberPath(), ImmutableSet.of(expectedError)));
  }

  @Test
  public void withMinAndMaxValue_withEmptyValueAtPath_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(minAndMaxNumberQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerNumberQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "");

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    assertThat(numberQuestion.getValidationErrors()).isEmpty();
  }

  @Test
  public void withMisformattedNumber() {
    Path numberPath =
        ApplicantData.APPLICANT_PATH
            .join(numberQuestionDefinition.getQuestionPathSegment())
            .join(Scalar.NUMBER);
    applicantData.setFailedUpdates(ImmutableMap.of(numberPath, "invalid_input"));
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(numberQuestionDefinition, applicantData, Optional.empty());

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    assertThat(numberQuestion.getValidationErrors())
        .isEqualTo(
            ImmutableMap.of(
                numberQuestion.getNumberPath(),
                ImmutableSet.of(
                    ValidationErrorMessage.create(MessageKey.NUMBER_VALIDATION_NON_INTEGER))));
    assertThat(numberQuestion.getNumberValue().isPresent()).isFalse();
  }
}
