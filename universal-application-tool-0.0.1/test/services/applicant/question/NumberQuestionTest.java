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
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.types.NumberQuestionDefinition;
import support.QuestionAnswerer;

@RunWith(JUnitParamsRunner.class)
public class NumberQuestionTest extends ResetPostgres {

  private static final NumberQuestionDefinition numberQuestionDefinition =
      new NumberQuestionDefinition(
          OptionalLong.of(1),
          "question name",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"),
          NumberQuestionDefinition.NumberValidationPredicates.create());

  private static final NumberQuestionDefinition minAndMaxNumberQuestionDefinition =
      new NumberQuestionDefinition(
          OptionalLong.of(1),
          "question name",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"),
          NumberQuestionDefinition.NumberValidationPredicates.create(50, 100));

  private Applicant applicant;
  private ApplicantData applicantData;
  private Messages messages;

  @Before
  public void setUp() {
    applicant = new Applicant();
    applicantData = applicant.getApplicantData();
    messages = instanceOf(MessagesApi.class).preferred(ImmutableList.of(Lang.defaultLang()));
  }

  @Test
  public void withEmptyApplicantData_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(numberQuestionDefinition, applicantData, Optional.empty());

    NumberQuestion numberQuestion = new NumberQuestion(applicantQuestion);

    assertThat(numberQuestion.getValidationErrors().isEmpty()).isTrue();
  }

  @Test
  public void withEmptyValueAtPath_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(numberQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerNumberQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "");

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    assertThat(numberQuestion.getAllTypeSpecificErrors().isEmpty()).isTrue();
    assertThat(numberQuestion.getNumberValue()).isEmpty();
  }

  @Test
  public void withValidValue_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(numberQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerNumberQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 800);

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    assertThat(numberQuestion.getAllTypeSpecificErrors().isEmpty()).isTrue();
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

    assertThat(numberQuestion.getValidationErrors().isEmpty()).isTrue();
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
        new ApplicantQuestion(minAndMaxNumberQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerNumberQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), value);

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors = numberQuestion.getValidationErrors();
    assertThat(validationErrors.size()).isEqualTo(1);
    ImmutableSet<ValidationErrorMessage> numberErrors = validationErrors.getOrDefault(numberQuestion.getNumberPath(), ImmutableSet.of());
    assertThat(numberErrors).hasSize(1);
    String errorMessage = numberErrors.iterator().next().getMessage(messages);
    assertThat(errorMessage).isEqualTo(expectedErrorMessage);
    assertThat(numberQuestion.getNumberValue().get()).isEqualTo(value);
  }

  @Test
  public void withMinAndMaxValue_withEmptyValueAtPath_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(minAndMaxNumberQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerNumberQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "");

    NumberQuestion numberQuestion = applicantQuestion.createNumberQuestion();

    assertThat(numberQuestion.getValidationErrors().isEmpty()).isTrue();
  }
}
