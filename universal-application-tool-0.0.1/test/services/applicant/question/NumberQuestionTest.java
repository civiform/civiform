package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.types.NumberQuestionDefinition;
import support.QuestionAnswerer;

@RunWith(JUnitParamsRunner.class)
public class NumberQuestionTest {

  private static final NumberQuestionDefinition numberQuestionDefinition =
      new NumberQuestionDefinition(
          "question name",
          Path.create("applicant.my.path.name"),
          Optional.empty(),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"));

  private static final NumberQuestionDefinition minAndMaxNumberQuestionDefinition =
      new NumberQuestionDefinition(
          "question name",
          Path.create("applicant.my.path.name"),
          Optional.empty(),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"),
          NumberQuestionDefinition.NumberValidationPredicates.create(50, 100));

  private Applicant applicant;
  private ApplicantData applicantData;

  @Before
  public void setUp() {
    applicant = new Applicant();
    applicantData = applicant.getApplicantData();
  }

  @Test
  public void withEmptyApplicantData_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(numberQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);

    NumberQuestion numberQuestion = new NumberQuestion(applicantQuestion);

    assertThat(numberQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(numberQuestion.hasQuestionErrors()).isFalse();
  }

  @Test
  public void withEmptyValueAtPath_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(numberQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);
    QuestionAnswerer.answerNumberQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "");

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    assertThat(numberQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(numberQuestion.getNumberValue()).isEmpty();
  }

  @Test
  public void withValidValue_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(numberQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);
    QuestionAnswerer.answerNumberQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 800);

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    assertThat(numberQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(numberQuestion.getNumberValue().get()).isEqualTo(800);
  }

  @Test
  @Parameters({"50", "75", "100"})
  public void withMinAndMaxValue_withValidValue_passesValidation(long value) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(minAndMaxNumberQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);
    QuestionAnswerer.answerNumberQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), value);

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    assertThat(numberQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(numberQuestion.hasQuestionErrors()).isFalse();
    assertThat(numberQuestion.getNumberValue().get()).isEqualTo(value);
  }

  @Test
  @Parameters({
    "-1,Must be at least 50.",
    "0,Must be at least 50.",
    "49,Must be at least 50.",
    "101,Must be at most 100.",
    "999,Must be at most 100."
  })
  public void withMinAndMaxValue_withInvalidValue_failsValidation(
      long value, String expectedErrorMessage) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(minAndMaxNumberQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);
    QuestionAnswerer.answerNumberQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), value);

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    assertThat(numberQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(numberQuestion.getQuestionErrors())
        .containsOnly(ValidationErrorMessage.create(expectedErrorMessage));
    assertThat(numberQuestion.getNumberValue().get()).isEqualTo(value);
  }

  @Test
  public void withMinAndMaxValue_withEmptyValueAtPath_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(minAndMaxNumberQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);
    QuestionAnswerer.answerNumberQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "");

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    assertThat(numberQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(numberQuestion.hasQuestionErrors()).isFalse();
  }
}
