package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

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
import services.LocalizedStrings;
import services.MessageKey;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.QuestionAnswerer;
import services.question.types.CurrencyQuestionDefinition;
import services.question.types.QuestionDefinitionConfig;

@RunWith(JUnitParamsRunner.class)
public class CurrencyQuestionTest {

  private static final CurrencyQuestionDefinition currencyQuestionDefinition =
      new CurrencyQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("question currency")
              .setDescription("description")
              .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
              .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
              .setId(OptionalLong.of(1))
              .setLastModifiedTime(Optional.empty())
              .build());

  private ApplicantModel applicant;
  private ApplicantData applicantData;

  @Before
  public void setUp() {
    applicant = new ApplicantModel();
    applicantData = applicant.getApplicantData();
  }

  @Test
  public void withEmptyApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            currencyQuestionDefinition, applicant, applicantData, Optional.empty());

    CurrencyQuestion currencyQuestion = new CurrencyQuestion(applicantQuestion);

    assertThat(currencyQuestion.getCurrencyValue()).isEmpty();
    assertThat(currencyQuestion.getValidationErrors()).isEmpty();
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
        new ApplicantQuestion(
            currencyQuestionDefinition, applicant, applicantData, Optional.empty());
    QuestionAnswerer.answerCurrencyQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), dollars);

    CurrencyQuestion currencyQuestion = applicantQuestion.createCurrencyQuestion();

    assertThat(currencyQuestion.getValidationErrors()).isEmpty();
    assertThat(currencyQuestion.getCurrencyValue().isPresent()).isTrue();
    assertThat(currencyQuestion.getCurrencyValue().get().getCents()).isEqualTo(cents);
  }

  @Test
  public void withMisformattedCurrency() {
    Path currencyPath =
        ApplicantData.APPLICANT_PATH
            .join(currencyQuestionDefinition.getQuestionPathSegment())
            .join(Scalar.CURRENCY_CENTS);
    applicantData.setFailedUpdates(ImmutableMap.of(currencyPath, "invalid_input"));
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            currencyQuestionDefinition, applicant, applicantData, Optional.empty());

    CurrencyQuestion currencyQuestion = applicantQuestion.createCurrencyQuestion();

    assertThat(currencyQuestion.getValidationErrors())
        .isEqualTo(
            ImmutableMap.of(
                currencyQuestion.getCurrencyPath(),
                ImmutableSet.of(
                    ValidationErrorMessage.create(MessageKey.CURRENCY_VALIDATION_MISFORMATTED))));
    assertThat(currencyQuestion.getCurrencyValue().isPresent()).isFalse();
  }
}
