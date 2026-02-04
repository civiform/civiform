package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
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
import services.question.types.DateQuestionDefinition.DateValidationOption;
import services.question.types.DateQuestionDefinition.DateValidationOption.DateType;
import services.question.types.DateQuestionDefinition.DateValidationPredicates;
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

  @SuppressWarnings({"TimeInStaticInitializer", "JavaTimeDefaultTimeZone"})
  private static final LocalDate TODAY = LocalDate.now(Clock.systemDefaultZone());

  @SuppressWarnings({"TimeInStaticInitializer", "JavaTimeDefaultTimeZone"})
  private static final LocalDate TOMORROW = TODAY.plusDays(1);

  @SuppressWarnings({"TimeInStaticInitializer", "JavaTimeDefaultTimeZone"})
  private static final LocalDate YESTERDAY = TODAY.minusDays(1);

  private static final DateValidationOption MIN_DATE_TODAY =
      DateValidationOption.builder().setDateType(DateType.APPLICATION_DATE).build();
  private static final DateValidationOption MAX_DATE_TODAY =
      DateValidationOption.builder().setDateType(DateType.APPLICATION_DATE).build();
  private static final DateValidationOption MIN_DATE_CUSTOM_YESTERDAY =
      DateValidationOption.builder()
          .setDateType(DateType.CUSTOM)
          .setCustomDate(Optional.of(YESTERDAY))
          .build();
  private static final DateValidationOption MAX_DATE_CUSTOM_TOMORROW =
      DateValidationOption.builder()
          .setDateType(DateType.CUSTOM)
          .setCustomDate(Optional.of(TOMORROW))
          .build();

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

  @SuppressWarnings("unused")
  private Object[] passingDateValidationCases() {
    return new Object[] {
      new Object[] {MIN_DATE_TODAY, MAX_DATE_TODAY, TODAY.format(DateTimeFormatter.ISO_LOCAL_DATE)},
      new Object[] {
        MIN_DATE_CUSTOM_YESTERDAY,
        MAX_DATE_CUSTOM_TOMORROW,
        TODAY.format(DateTimeFormatter.ISO_LOCAL_DATE)
      },
      new Object[] {
        MIN_DATE_TODAY, MAX_DATE_CUSTOM_TOMORROW, TOMORROW.format(DateTimeFormatter.ISO_LOCAL_DATE)
      },
      new Object[] {
        MIN_DATE_CUSTOM_YESTERDAY,
        MAX_DATE_TODAY,
        YESTERDAY.format(DateTimeFormatter.ISO_LOCAL_DATE)
      },
    };
  }

  @Test
  @Parameters(method = "passingDateValidationCases")
  public void withApplicantData_withMinMaxDate_passesValidation(
      DateValidationOption minDate, DateValidationOption maxDate, String applicantAnswer) {
    DateQuestionDefinition questionDefinition = createDateQuestionDefinition(minDate, maxDate);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            questionDefinition, new ApplicantModel(), new ApplicantData(), Optional.empty());
    QuestionAnswerer.answerDateQuestion(
        applicantQuestion.getApplicantData(),
        applicantQuestion.getContextualizedPath(),
        applicantAnswer);

    DateQuestion dateQuestion = new DateQuestion(applicantQuestion);

    assertThat(dateQuestion.getValidationErrors()).isEmpty();
  }

  @SuppressWarnings("unused")
  private Object[] failingDateValidationCases() {
    return new Object[] {
      new Object[] {
        MIN_DATE_TODAY,
        MAX_DATE_TODAY,
        YESTERDAY.format(DateTimeFormatter.ISO_LOCAL_DATE),
        MessageKey.DATE_VALIDATION_CURRENT_DATE_REQUIRED,
        new Object[] {},
      },
      new Object[] {
        MIN_DATE_CUSTOM_YESTERDAY,
        MAX_DATE_CUSTOM_TOMORROW,
        TODAY.plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE),
        MessageKey.DATE_VALIDATION_DATE_NOT_IN_RANGE,
        new Object[] {YESTERDAY.toString(), TOMORROW.toString()},
      },
      new Object[] {
        MIN_DATE_TODAY,
        MAX_DATE_CUSTOM_TOMORROW,
        YESTERDAY.format(DateTimeFormatter.ISO_LOCAL_DATE),
        MessageKey.DATE_VALIDATION_FUTURE_DATE_REQUIRED,
        new Object[] {},
      },
      new Object[] {
        MIN_DATE_CUSTOM_YESTERDAY,
        MAX_DATE_TODAY,
        TOMORROW.format(DateTimeFormatter.ISO_LOCAL_DATE),
        MessageKey.DATE_VALIDATION_PAST_DATE_REQUIRED,
        new Object[] {},
      },
      new Object[] {
        MIN_DATE_CUSTOM_YESTERDAY,
        DateValidationOption.builder().setDateType(DateType.ANY).build(),
        YESTERDAY.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
        MessageKey.DATE_VALIDATION_DATE_TOO_FAR_IN_PAST,
        new Object[] {YESTERDAY.toString()},
      },
      new Object[] {
        DateValidationOption.builder().setDateType(DateType.ANY).build(),
        MAX_DATE_CUSTOM_TOMORROW,
        TOMORROW.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
        MessageKey.DATE_VALIDATION_DATE_TOO_FAR_IN_FUTURE,
        new Object[] {TOMORROW.toString()},
      },
    };
  }

  @Test
  @Parameters(method = "failingDateValidationCases")
  public void withApplicantData_withMinMaxDate_failsValidation(
      DateValidationOption minDate,
      DateValidationOption maxDate,
      String applicantAnswer,
      MessageKey expectedMessage,
      Object[] messageArgs) {
    DateQuestionDefinition questionDefinition = createDateQuestionDefinition(minDate, maxDate);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            questionDefinition, new ApplicantModel(), new ApplicantData(), Optional.empty());
    QuestionAnswerer.answerDateQuestion(
        applicantQuestion.getApplicantData(),
        applicantQuestion.getContextualizedPath(),
        applicantAnswer);

    DateQuestion dateQuestion = new DateQuestion(applicantQuestion);

    assertThat(dateQuestion.getValidationErrors())
        .isEqualTo(
            ImmutableMap.of(
                dateQuestion.getDatePath(),
                ImmutableSet.of(ValidationErrorMessage.create(expectedMessage, messageArgs))));
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

  private DateQuestionDefinition createDateQuestionDefinition(
      DateValidationOption minDate, DateValidationOption maxDate) {
    return new DateQuestionDefinition(
        QuestionDefinitionConfig.builder()
            .setName("question name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .setId(OptionalLong.of(1))
            .setLastModifiedTime(Optional.empty())
            .setValidationPredicates(
                DateValidationPredicates.create(Optional.of(minDate), Optional.of(maxDate)))
            .build());
  }
}
