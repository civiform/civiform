package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

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
import services.applicant.ApplicantData;
import services.question.types.CurrencyQuestionDefinition;
import support.QuestionAnswerer;

@RunWith(JUnitParamsRunner.class)
public class CurrencyQuestionTest {

  private static final CurrencyQuestionDefinition currencyQuestionDefinition =
      new CurrencyQuestionDefinition(
          OptionalLong.of(1),
          "question currency",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"));

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
        new ApplicantQuestion(currencyQuestionDefinition, applicantData, Optional.empty());

    CurrencyQuestion currencyQuestion = new CurrencyQuestion(applicantQuestion);

    assertThat(currencyQuestion.getValue()).isEmpty();
    assertThat(currencyQuestion.getAllTypeSpecificErrors().isEmpty()).isTrue();
    assertThat(currencyQuestion.getQuestionErrors().isEmpty()).isTrue();
  }

  @Test
  @Parameters({
    // Single digit dollars.
    "0, 0", // Zero
    "0.00, 0", // Zero with cents
    "0.45, 45", // Only cents
    "1, 100", // Single dollars
    "1.23, 123", // Single dollars with cents.
    // Large values
    "12345, 1234500",
    "12\\,345, 1234500", // With comma
    "12345.67, 1234567", // With cents.
    "12\\,345.67, 1234567" // With comma and cents.
  })
  public void withValidApplicantData_passesValidation(String dollars, Long cents) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(currencyQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerCurrencyQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), dollars);

    CurrencyQuestion currencyQuestion = applicantQuestion.createCurrencyQuestion();

    assertThat(currencyQuestion.getAllTypeSpecificErrors().isEmpty()).isTrue();
    assertThat(currencyQuestion.getQuestionErrors().isEmpty()).isTrue();
    assertThat(currencyQuestion.getValue().isPresent()).isTrue();
    assertThat(currencyQuestion.getValue().get().getCents()).isEqualTo(cents);
  }
}
