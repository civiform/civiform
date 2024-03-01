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
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.MessageKey;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.program.ProgramQuestionDefinition;
import services.question.QuestionAnswerer;
import services.question.types.PhoneQuestionDefinition;
import services.question.types.QuestionDefinitionConfig;

@RunWith(JUnitParamsRunner.class)
public class PhoneQuestionTest extends ResetPostgres {
  private static final PhoneQuestionDefinition phoneQuestionDefinition =
      new PhoneQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("applicant phone")
              .setDescription("The applicant Phone Number")
              .setQuestionText(LocalizedStrings.of(Locale.US, "What is your phone number?"))
              .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
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
  public void withEmptyApplicantData_optionalQuestion() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            ProgramQuestionDefinition.create(phoneQuestionDefinition, Optional.empty())
                .setOptional(true),
            applicantData,
            Optional.empty());

    PhoneQuestion phoneQuestion = new PhoneQuestion(applicantQuestion);

    assertThat(phoneQuestion.getValidationErrors().isEmpty()).isTrue();
  }

  @Test
  public void withValidData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(phoneQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerPhoneQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "US", "(615) 717-1234");
    PhoneQuestion phoneQuestion = applicantQuestion.createPhoneQuestion();

    assertThat(phoneQuestion.getValidationErrors().isEmpty()).isTrue();
    assertThat(phoneQuestion.getPhoneNumberValue().get()).isEqualTo("6157171234");
    assertThat(phoneQuestion.getCountryCodeValue().get()).isEqualTo("US");
  }

  @Test
  public void withInvalidApplicantData_missingRequiredFields() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(phoneQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerPhoneQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "", "");
    PhoneQuestion phoneQuestion = applicantQuestion.createPhoneQuestion();
    assertThat(phoneQuestion.getValidationErrors())
        .isEqualTo(
            ImmutableMap.of(
                phoneQuestion.getPhoneNumberPath(),
                ImmutableSet.of(
                    ValidationErrorMessage.create(MessageKey.PHONE_VALIDATION_NUMBER_REQUIRED))));
  }

  @Test
  @Parameters({"5552123333", "1231234567", "123123459a03", "123td25342"})
  public void withInvalidApplicantData_invalidPhoneNumber(String number) {
    Path phonePath =
        ApplicantData.APPLICANT_PATH
            .join(phoneQuestionDefinition.getQuestionPathSegment())
            .join(Scalar.PHONE_NUMBER);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(phoneQuestionDefinition, applicantData, Optional.empty());
    applicantData.setFailedUpdates(ImmutableMap.of(phonePath, "invalid_input"));
    QuestionAnswerer.answerPhoneQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "US", number);

    PhoneQuestion phoneQuestion = applicantQuestion.createPhoneQuestion();

    assertThat(phoneQuestion.getValidationErrors())
        .isEqualTo(
            ImmutableMap.of(
                phoneQuestion.getPhoneNumberPath(),
                ImmutableSet.of(
                    ValidationErrorMessage.create(
                        MessageKey.PHONE_VALIDATION_INVALID_PHONE_NUMBER))));
    assertThat(phoneQuestion.getValidationErrors().isEmpty()).isFalse();
  }

  @Test
  @Parameters({"7782123334", "2505550199"})
  public void withInvalidApplicantData_numberNotInCountry(String number) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(phoneQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerPhoneQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "US", number);

    PhoneQuestion phoneQuestion = applicantQuestion.createPhoneQuestion();

    assertThat(phoneQuestion.getValidationErrors())
        .isEqualTo(
            ImmutableMap.of(
                phoneQuestion.getPhoneNumberPath(),
                ImmutableSet.of(
                    ValidationErrorMessage.create(
                        MessageKey.PHONE_VALIDATION_NUMBER_NOT_IN_COUNTRY))));
  }

  @Test
  @Parameters({"2505550199"})
  public void withInvalidApplicantData_validCANumber(String number) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(phoneQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerPhoneQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "CA", number);

    PhoneQuestion phoneQuestion = applicantQuestion.createPhoneQuestion();

    assertThat(phoneQuestion.getValidationErrors().isEmpty()).isTrue();
    assertThat(phoneQuestion.getPhoneNumberValue().get()).isEqualTo("2505550199");
    assertThat(phoneQuestion.getCountryCodeValue().get()).isEqualTo("CA");
    ;
  }
}
