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
import services.question.types.NumberQuestionDefinition;

@RunWith(JUnitParamsRunner.class)
public class NumberQuestionTest {
  private static final NumberQuestionDefinition numberQuestionDefinition =
      new NumberQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
          Optional.empty(),
          "description",
          LifecycleStage.ACTIVE,
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"));

  private static final NumberQuestionDefinition minAndMaxNumberQuestionDefinition =
      new NumberQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
          Optional.empty(),
          "description",
          LifecycleStage.ACTIVE,
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
        new ApplicantQuestion(numberQuestionDefinition, applicantData);

    NumberQuestion numberQuestion = new NumberQuestion(applicantQuestion);

    assertThat(numberQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(numberQuestion.hasQuestionErrors()).isFalse();
  }

  @Test
  public void withEmptyValueAtPath_passesValidation() {
    applicantData.putLong(numberQuestionDefinition.getNumberPath(), "");
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(numberQuestionDefinition, applicantData);

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    assertThat(numberQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(numberQuestion.getNumberValue()).isEmpty();
  }

  @Test
  public void withValidValue_passesValidation() {
    applicantData.putLong(numberQuestionDefinition.getNumberPath(), 800);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(numberQuestionDefinition, applicantData);

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    assertThat(numberQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(numberQuestion.getNumberValue().get()).isEqualTo(800);
  }

  @Test
  @Parameters({"50", "75", "100"})
  public void withMinAndMaxValue_withValidValue_passesValidation(long value) {
    applicantData.putLong(minAndMaxNumberQuestionDefinition.getNumberPath(), value);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(minAndMaxNumberQuestionDefinition, applicantData);

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
    applicantData.putLong(minAndMaxNumberQuestionDefinition.getNumberPath(), value);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(minAndMaxNumberQuestionDefinition, applicantData);

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    assertThat(numberQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(numberQuestion.getQuestionErrors())
        .containsOnly(ValidationErrorMessage.create(expectedErrorMessage));
    assertThat(numberQuestion.getNumberValue().get()).isEqualTo(value);
  }

  @Test
  public void withMinValue_withEmptyValueAtPath_failsValidation() {
    NumberQuestionDefinition.NumberValidationPredicates.Builder numberValidationPredicatesBuilder =
            NumberQuestionDefinition.NumberValidationPredicates.builder();
    numberValidationPredicatesBuilder.setMin(1);
    NumberQuestionDefinition minNumberQuestionDefinition =
            new NumberQuestionDefinition(
                    1L,
                    "question name",
                    Path.create("applicant.my.path.name"),
                    Optional.empty(),
                    "description",
                    LifecycleStage.ACTIVE,
                    ImmutableMap.of(Locale.US, "question?"),
                    ImmutableMap.of(Locale.US, "help text"),
                    numberValidationPredicatesBuilder.build());

    applicantData.putLong(minNumberQuestionDefinition.getNumberPath(), "");
    ApplicantQuestion applicantQuestion =
            new ApplicantQuestion(minNumberQuestionDefinition, applicantData);

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    assertThat(numberQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(numberQuestion.getQuestionErrors())
            .containsOnly(ValidationErrorMessage.numberTooSmallError(1));
  }
}
