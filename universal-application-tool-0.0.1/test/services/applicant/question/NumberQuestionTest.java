package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.NumberQuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;

public class NumberQuestionTest {
  private static final NumberQuestionDefinition numberQuestionDefinition =
      new NumberQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"));

  private Applicant applicant;
  private ApplicantData applicantData;

  @Before
  public void setUp() {
    applicant = new Applicant();
    applicantData = applicant.getApplicantData();
  }

  @Test
  public void numberQuestion_withEmptyApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(numberQuestionDefinition, applicantData);

    assertThat(applicantQuestion.getNumberQuestion()).isInstanceOf(NumberQuestion.class);
    assertThat(applicantQuestion.getQuestionText()).isEqualTo("question?");
    assertThat(applicantQuestion.hasErrors()).isFalse();
  }

  @Test
  public void numberQuestion_withPresentApplicantData() {
    applicantData.putLong(numberQuestionDefinition.getNumberPath(), 800);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(numberQuestionDefinition, applicantData);
    NumberQuestion numberQuestion = applicantQuestion.getNumberQuestion();

    assertThat(numberQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(numberQuestion.getNumberValue().get()).isEqualTo(800);
  }

  @Test
  public void numberQuestion_withPresentApplicantData_failsValidation() throws Exception {
    NumberQuestionDefinition question =
        (NumberQuestionDefinition)
            new QuestionDefinitionBuilder()
                .setQuestionType(QuestionType.NUMBER)
                .setVersion(1L)
                .setName("question name")
                .setPath(Path.create("applicant.my.path.name"))
                .setDescription("description")
                .setQuestionText(ImmutableMap.of(Locale.US, "question?"))
                .setQuestionHelpText(ImmutableMap.of(Locale.US, "help text"))
                .setValidationPredicates(
                    NumberQuestionDefinition.NumberValidationPredicates.create(0, 100))
                .build();
    applicantData.putLong(question.getNumberPath(), 1000000);
    ApplicantQuestion applicantQuestion = new ApplicantQuestion(question, applicantData);
    NumberQuestion numberQuestion = applicantQuestion.getNumberQuestion();

    assertThat(applicantQuestion.hasErrors()).isTrue();
    assertThat(numberQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(numberQuestion.getQuestionErrors())
        .containsOnly(ValidationErrorMessage.numberTooLargeError(100));
    assertThat(numberQuestion.getNumberValue().get()).isEqualTo(1000000);
  }
}
