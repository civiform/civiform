package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
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
import services.question.NumberQuestionDefinition;

@RunWith(JUnitParamsRunner.class)
public class NumberQuestionTest {
  private static final NumberQuestionDefinition numberQuestionDefinition =
      new NumberQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
          "description",
          LifecycleStage.ACTIVE,
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"));

  private static final NumberQuestionDefinition minAndMaxNumberQuestionDefinition =
      new NumberQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
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
  public void withEmptyApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(numberQuestionDefinition, applicantData);

    NumberQuestion numberQuestion = new NumberQuestion(applicantQuestion);

    assertThat(numberQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(numberQuestion.hasQuestionErrors()).isFalse();
  }

  @Test
  public void withValidApplicantData() {
    applicantData.putLong(numberQuestionDefinition.getNumberPath(), 800);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(numberQuestionDefinition, applicantData);

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    assertThat(numberQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(numberQuestion.getNumberValue().get()).isEqualTo(800);
  }

  @Test
  @Parameters({"50", "75", "100"})
  public void withMinAndMaxValue_withValidApplicantData_passesValidation(long value) {
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
    "-1,This answer must be at least 50.",
    "0,This answer must be at least 50.",
    "49,This answer must be at least 50.",
    "999,This answer cannot be larger than 100."
  })
  public void withMinAndMaxValue_withInvalidApplicantData_failsValidation(
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
}
