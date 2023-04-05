package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import junitparams.JUnitParamsRunner;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.program.ProgramQuestionDefinition;
import services.question.types.PhoneQuestionDefinition;
import support.QuestionAnswerer;

@RunWith(JUnitParamsRunner.class)
public class PhoneQuestionTest {
  private static final PhoneQuestionDefinition phoneQuestionDefinition =
      new PhoneQuestionDefinition(
          OptionalLong.of(1),
          "applicant phone",
          Optional.empty(),
          "The applicant Phone Number",
          LocalizedStrings.of(Locale.US, "What is your phone number?"),
          LocalizedStrings.of(Locale.US, "This is sample help text."),
          PhoneQuestionDefinition.PhoneValidationPredicates.create(),
          /* lastModifiedTime= */ Optional.empty());

  private Applicant applicant;
  private ApplicantData applicantData;

  @Before
  public void setUp() {
    applicant = new Applicant();
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
}
