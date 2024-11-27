package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

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
import services.applicant.ApplicantData;
import services.question.PrimaryApplicantInfoTag;
import services.question.QuestionAnswerer;
import services.question.types.EmailQuestionDefinition;
import services.question.types.QuestionDefinitionConfig;

@RunWith(JUnitParamsRunner.class)
public class EmailQuestionTest extends ResetPostgres {
  private static final EmailQuestionDefinition emailQuestionDefinition =
      new EmailQuestionDefinition(
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
        new ApplicantQuestion(emailQuestionDefinition, applicant, applicantData, Optional.empty());

    EmailQuestion emailQuestion = new EmailQuestion(applicantQuestion);

    assertThat(emailQuestion.getValidationErrors()).isEmpty();
  }

  @Test
  public void withApplicantData_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(emailQuestionDefinition, applicant, applicantData, Optional.empty());
    QuestionAnswerer.answerEmailQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "test1@gmail.com");

    EmailQuestion emailQuestion = new EmailQuestion(applicantQuestion);

    assertThat(emailQuestion.getEmailValue().get()).isEqualTo("test1@gmail.com");
    assertThat(emailQuestion.getValidationErrors()).isEmpty();
  }

  @Test
  public void getEmailValue_returnsPAIValueWhenTagged() {

    EmailQuestionDefinition emailQuestionDefinitionWithPAI =
        new EmailQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("question name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
                .setId(OptionalLong.of(1))
                .setLastModifiedTime(Optional.empty())
                // Tag the question as a PAI question
                .setPrimaryApplicantInfoTags(
                    ImmutableSet.of(PrimaryApplicantInfoTag.APPLICANT_EMAIL))
                .build());

    // Save applicant's email to the PAI column
    applicant.setEmailAddress("test@email.com");

    EmailQuestion emailQuestion =
        new ApplicantQuestion(
                emailQuestionDefinitionWithPAI, applicant, applicantData, Optional.empty())
            .createEmailQuestion();

    assertThat(emailQuestion.getEmailValue().get()).isEqualTo(applicant.getEmailAddress().get());
  }
}
