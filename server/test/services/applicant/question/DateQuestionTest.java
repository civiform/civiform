package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import junitparams.JUnitParamsRunner;
import models.ApplicantModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.MessageKey;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.PrimaryApplicantInfoTag;
import services.question.QuestionAnswerer;
import services.question.types.DateQuestionDefinition;
import services.question.types.QuestionDefinitionConfig;

@RunWith(JUnitParamsRunner.class)
public class DateQuestionTest extends ResetPostgres {
  private static final DateQuestionDefinition dateQuestionDefinition =
      new DateQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("question name")
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
        new ApplicantQuestion(dateQuestionDefinition, applicant, applicantData, Optional.empty());

    DateQuestion dateQuestion = new DateQuestion(applicantQuestion);

    assertThat(dateQuestion.getValidationErrors()).isEmpty();
  }

  @Test
  public void withApplicantData_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(dateQuestionDefinition, applicant, applicantData, Optional.empty());
    QuestionAnswerer.answerDateQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "2021-05-10");

    DateQuestion dateQuestion = new DateQuestion(applicantQuestion);

    assertThat(dateQuestion.getDateValue().get()).isEqualTo("2021-05-10");
    assertThat(dateQuestion.getValidationErrors()).isEmpty();
    assertThat(dateQuestion.getYearValue().get()).isEqualTo(2021);
    assertThat(dateQuestion.getMonthValue().get()).isEqualTo(5);
    assertThat(dateQuestion.getDayValue().get()).isEqualTo(10);
  }

  @Test
  public void withApplicantData_failsValidationOnYearLessThanAllowableYear() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(dateQuestionDefinition, applicant, applicantData, Optional.empty());
    QuestionAnswerer.answerDateQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "0049-05-10");

    DateQuestion dateQuestion = new DateQuestion(applicantQuestion);

    assertThat(dateQuestion.getValidationErrors()).hasSize(1);
    assertThat(dateQuestion.getValidationErrors())
        .isEqualTo(
            ImmutableMap.of(
                dateQuestion.getDatePath(),
                ImmutableSet.of(
                    ValidationErrorMessage.create(
                        MessageKey.DATE_VALIDATION_DATE_BEYOND_ALLOWABLE_YEARS_IN_PAST,
                        DateQuestion.ALLOWABLE_YEAR_FOR_DATE_VALIDATION))));
  }

  @Test
  public void withApplicantData_failsValidationOnYearMoreThanAllowableYear() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(dateQuestionDefinition, applicant, applicantData, Optional.empty());
    QuestionAnswerer.answerDateQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "2549-05-10");

    DateQuestion dateQuestion = new DateQuestion(applicantQuestion);

    assertThat(dateQuestion.getValidationErrors()).hasSize(1);
    assertThat(dateQuestion.getValidationErrors())
        .isEqualTo(
            ImmutableMap.of(
                dateQuestion.getDatePath(),
                ImmutableSet.of(
                    ValidationErrorMessage.create(
                        MessageKey.DATE_VALIDATION_DATE_BEYOND_ALLOWABLE_YEARS_IN_FUTURE,
                        DateQuestion.ALLOWABLE_YEAR_FOR_DATE_VALIDATION))));
  }

  @Test
  public void withMisformattedDate() {
    Path datePath =
        ApplicantData.APPLICANT_PATH
            .join(dateQuestionDefinition.getQuestionPathSegment())
            .join(Scalar.DATE);
    applicantData.setFailedUpdates(ImmutableMap.of(datePath, "invalid_input"));
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(dateQuestionDefinition, applicant, applicantData, Optional.empty());

    DateQuestion dateQuestion = applicantQuestion.createDateQuestion();

    assertThat(dateQuestion.getValidationErrors())
        .isEqualTo(
            ImmutableMap.of(
                dateQuestion.getDatePath(),
                ImmutableSet.of(
                    ValidationErrorMessage.create(
                        MessageKey.DATE_VALIDATION_INVALID_DATE_FORMAT))));
    assertThat(dateQuestion.getDateValue().isPresent()).isFalse();
  }

  @Test
  public void getDateValue_returnsPAIValueWhenTagged() {
    DateQuestionDefinition dateQuestionDefinitionWithPAI =
        new DateQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("question name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
                .setId(OptionalLong.of(1))
                .setLastModifiedTime(Optional.empty())
                // Tag the question as a PAI question
                .setPrimaryApplicantInfoTags(ImmutableSet.of(PrimaryApplicantInfoTag.APPLICANT_DOB))
                .build());

    // Save applicant's dob to the PAI column
    applicant.setDateOfBirth("2001-01-01");

    DateQuestion dateQuestion =
        new ApplicantQuestion(
                dateQuestionDefinitionWithPAI, applicant, applicantData, Optional.empty())
            .createDateQuestion();

    assertThat(dateQuestion.getDateValue().get()).isEqualTo(applicant.getDateOfBirth().get());
  }
}
