package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

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
import services.applicant.ApplicantData;
import services.program.ProgramQuestionDefinition;
import services.question.PrimaryApplicantInfoTag;
import services.question.QuestionAnswerer;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinitionConfig;

@RunWith(JUnitParamsRunner.class)
public class NameQuestionTest extends ResetPostgres {
  private static final NameQuestionDefinition nameQuestionDefinition =
      new NameQuestionDefinition(
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
  public void withEmptyApplicantData_optionalQuestion() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            ProgramQuestionDefinition.create(nameQuestionDefinition, Optional.empty())
                .setOptional(true),
            applicantData,
            Optional.empty());

    NameQuestion nameQuestion = new NameQuestion(applicantQuestion);

    assertThat(nameQuestion.getFirstNameValue()).isEmpty();
    assertThat(nameQuestion.getMiddleNameValue()).isEmpty();
    assertThat(nameQuestion.getLastNameValue()).isEmpty();
    assertThat(nameQuestion.getNameSuffixValue()).isEmpty();
    assertThat(nameQuestion.getValidationErrors()).isEmpty();
  }

  @Test
  @Parameters({"Wendel,Middle Name,Patric,Jr.", "Wendel,,Patrick,"})
  public void withValidApplicantData_passesValidation(
      String firstName, String middleName, String lastName, String suffix) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(nameQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerNameQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        firstName,
        middleName,
        lastName,
        suffix);

    NameQuestion nameQuestion = applicantQuestion.createNameQuestion();

    assertThat(nameQuestion.getValidationErrors()).isEmpty();
    assertThat(nameQuestion.getFirstNameValue().get()).isEqualTo(firstName);
    if (nameQuestion.getMiddleNameValue().isPresent()) {
      assertThat(nameQuestion.getMiddleNameValue().get()).isEqualTo(middleName);
    }
    assertThat(nameQuestion.getLastNameValue().get()).isEqualTo(lastName);
    if (nameQuestion.getNameSuffixValue().isPresent()) {
      assertThat(nameQuestion.getNameSuffixValue().get()).isEqualTo(suffix);
    }
  }

  @Test
  @Parameters({",,,", ",Middle Name,,", "Wendel,,,", ",,Patrick,"})
  public void withInvalidApplicantData_failsValidation(
      String firstName, String middleName, String lastName, String suffix) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(nameQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerNameQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        firstName,
        middleName,
        lastName,
        suffix);

    NameQuestion nameQuestion = applicantQuestion.createNameQuestion();

    assertThat(nameQuestion.getValidationErrors().isEmpty()).isFalse();
    assertThat(
            nameQuestion
                .getValidationErrors()
                .containsKey(applicantQuestion.getContextualizedPath()))
        .isFalse();
  }

  @Test
  public void getNameValue_returnsPAIValueWhenTagged() {
    NameQuestionDefinition nameQuestionDefinitionWithPaiTag =
        new NameQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("question name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
                .setId(OptionalLong.of(1))
                .setLastModifiedTime(Optional.empty())
                // Tag the question as a PAI question
                .setPrimaryApplicantInfoTags(
                    ImmutableSet.of(PrimaryApplicantInfoTag.APPLICANT_NAME))
                .build());

    // Save applicant's name to the PAI column
    applicant.setFirstName("First");
    applicant.setMiddleName("Middle");
    applicant.setLastName("Last");
    applicant.setSuffix("Suffix");

    NameQuestion nameQuestion =
        new ApplicantQuestion(nameQuestionDefinitionWithPaiTag, applicantData, Optional.empty())
            .createNameQuestion();

    assertThat(nameQuestion.getFirstNameValue().get()).isEqualTo(applicant.getFirstName().get());
    assertThat(nameQuestion.getMiddleNameValue().get()).isEqualTo(applicant.getMiddleName().get());
    assertThat(nameQuestion.getLastNameValue().get()).isEqualTo(applicant.getLastName().get());
    assertThat(nameQuestion.getNameSuffixValue().get()).isEqualTo(applicant.getSuffix().get());
  }
}
