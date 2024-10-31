package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import models.ApplicantModel;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.MessageKey;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.program.ProgramQuestionDefinition;
import services.question.QuestionAnswerer;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionValidationPredicates;
import services.question.types.QuestionDefinitionConfig;

public class MultiSelectQuestionTest extends ResetPostgres {

  private static final QuestionDefinitionConfig CONFIG =
      QuestionDefinitionConfig.builder()
          .setName("name")
          .setDescription("description")
          .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
          .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
          .setValidationPredicates(
              MultiOptionValidationPredicates.builder()
                  .setMinChoicesRequired(2)
                  .setMaxChoicesAllowed(3)
                  .build())
          .setLastModifiedTime(Optional.empty())
          .setId(OptionalLong.of(1))
          .build();

  private static final ImmutableList<QuestionOption> QUESTION_OPTIONS =
      ImmutableList.of(
          QuestionOption.create(1L, "uno", LocalizedStrings.of(Locale.US, "valid")),
          QuestionOption.create(2L, "dos", LocalizedStrings.of(Locale.US, "ok")),
          QuestionOption.create(3L, "tres", LocalizedStrings.of(Locale.US, "third")),
          QuestionOption.create(4L, "cuatro", LocalizedStrings.of(Locale.US, "fourth")));

  private static final MultiOptionQuestionDefinition CHECKBOX_QUESTION =
      new MultiOptionQuestionDefinition(CONFIG, QUESTION_OPTIONS, MultiOptionQuestionType.CHECKBOX);

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
            ProgramQuestionDefinition.create(CHECKBOX_QUESTION, Optional.empty()).setOptional(true),
            applicant,
            applicantData,
            Optional.empty());

    MultiSelectQuestion multiSelectQuestion = new MultiSelectQuestion(applicantQuestion);

    assertThat(multiSelectQuestion.getValidationErrors()).isEmpty();
  }

  @Test
  public void withEmptyApplicantData_requiredQuestion_failsValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            ProgramQuestionDefinition.create(CHECKBOX_QUESTION, Optional.empty()),
            applicant,
            applicantData,
            Optional.empty());

    MultiSelectQuestion multiSelectQuestion = new MultiSelectQuestion(applicantQuestion);

    assertThat(multiSelectQuestion.getValidationErrors())
        .isEqualTo(
            ImmutableMap.of(
                applicantQuestion.getContextualizedPath(),
                ImmutableSet.of(
                    ValidationErrorMessage.create(MessageKey.MULTI_SELECT_VALIDATION_TOO_FEW, 2))));
  }

  @Test
  public void withValidApplicantData_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(CHECKBOX_QUESTION, applicant, applicantData, Optional.empty());
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 0, 1L);
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 1, 2L);

    MultiSelectQuestion multiSelectQuestion = new MultiSelectQuestion(applicantQuestion);

    assertThat(multiSelectQuestion.getValidationErrors()).isEmpty();
  }

  @Test
  public void tooFewSelected_failsValidation() {

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(CHECKBOX_QUESTION, applicant, applicantData, Optional.empty());
    // Put too few selections.
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 0, 0L);

    MultiSelectQuestion multiSelectQuestion = applicantQuestion.createMultiSelectQuestion();

    assertThat(multiSelectQuestion.getValidationErrors())
        .isEqualTo(
            ImmutableMap.of(
                applicantQuestion.getContextualizedPath(),
                ImmutableSet.of(
                    ValidationErrorMessage.create(MessageKey.MULTI_SELECT_VALIDATION_TOO_FEW, 2))));
  }

  @Test
  public void tooManySelected_failsValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(CHECKBOX_QUESTION, applicant, applicantData, Optional.empty());
    // Put too many selections.
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 0, 1L);
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 1, 2L);
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 2, 3L);
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 3, 4L);

    MultiSelectQuestion multiSelectQuestion = applicantQuestion.createMultiSelectQuestion();

    assertThat(multiSelectQuestion.getValidationErrors())
        .isEqualTo(
            ImmutableMap.of(
                applicantQuestion.getContextualizedPath(),
                ImmutableSet.of(
                    ValidationErrorMessage.create(
                        MessageKey.MULTI_SELECT_VALIDATION_TOO_MANY, 3))));
  }

  @Test
  public void selectedInvalidOptions_typeErrors_hasNoTypeErrors() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(CHECKBOX_QUESTION, applicant, applicantData, Optional.empty());
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 0, 1L);
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 1, 2L);

    MultiSelectQuestion multiSelectQuestion = applicantQuestion.createMultiSelectQuestion();

    assertThat(multiSelectQuestion.getValidationErrors()).isEmpty();
  }

  @Test
  public void getSelectedOptionAdminNames_getsAdminNames() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(CHECKBOX_QUESTION, applicant, applicantData, Optional.empty());
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 0, 1L);
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 1, 2L);

    Optional<ImmutableList<String>> adminNames =
        applicantQuestion.createMultiSelectQuestion().getSelectedOptionAdminNames();

    assertThat(adminNames).isPresent();
    assertThat(adminNames.get()).containsExactlyInAnyOrder("uno", "dos");
  }

  @Test
  public void getOptions_defaultsIfLangUnsupported() {
    applicantData.setPreferredLocale(Locale.CHINESE);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(CHECKBOX_QUESTION, applicant, applicantData, Optional.empty());

    MultiSelectQuestion multiSelectQuestion = applicantQuestion.createMultiSelectQuestion();

    assertThat(multiSelectQuestion.getOptions()).isNotEmpty();
    assertThat(multiSelectQuestion.getOptions().get(0).locale())
        .isEqualTo(LocalizedStrings.DEFAULT_LOCALE);
  }
}
