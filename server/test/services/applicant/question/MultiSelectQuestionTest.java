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
        applicantData, applicantQuestion.getContextualizedPath(), 0, 1L);

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

  @Test
  public void withRemovedOptions_validationFails() {
    // Create question with options where options 3 and 4 are hidden (removed by admin)
    ImmutableList<QuestionOption> optionsWithSomeHidden =
        ImmutableList.of(
            QuestionOption.create(
                1L, 1L, "uno", LocalizedStrings.of(Locale.US, "valid"), Optional.of(true)),
            QuestionOption.create(
                2L, 2L, "dos", LocalizedStrings.of(Locale.US, "ok"), Optional.of(true)),
            QuestionOption.create(
                3L,
                3L,
                "tres",
                LocalizedStrings.of(Locale.US, "third"),
                Optional.of(false)), // Hidden
            QuestionOption.create(
                4L,
                4L,
                "cuatro",
                LocalizedStrings.of(Locale.US, "fourth"),
                Optional.of(false))); // Hidden

    MultiOptionQuestionDefinition questionWithHiddenOptions =
        new MultiOptionQuestionDefinition(
            CONFIG, optionsWithSomeHidden, MultiOptionQuestionType.CHECKBOX);

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            questionWithHiddenOptions, applicant, applicantData, Optional.empty());

    // Applicant tries to submit options including a removed one
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 0, 1L); // Valid
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 1, 3L); // Invalid - removed

    MultiSelectQuestion multiSelectQuestion = applicantQuestion.createMultiSelectQuestion();

    // Should have validation error because option 3 is not displayable
    assertThat(multiSelectQuestion.getValidationErrors()).hasSize(1);
    var errors =
        multiSelectQuestion.getValidationErrors().get(applicantQuestion.getContextualizedPath());
    assertThat(errors).contains(ValidationErrorMessage.create(MessageKey.INVALID_INPUT));
  }

  @Test
  public void withRemovedOptions_onlyValidSelectionsPass() {
    // Create question with options where options 3 and 4 are hidden (removed by admin)
    ImmutableList<QuestionOption> optionsWithSomeHidden =
        ImmutableList.of(
            QuestionOption.create(
                1L, 1L, "uno", LocalizedStrings.of(Locale.US, "valid"), Optional.of(true)),
            QuestionOption.create(
                2L, 2L, "dos", LocalizedStrings.of(Locale.US, "ok"), Optional.of(true)),
            QuestionOption.create(
                3L,
                3L,
                "tres",
                LocalizedStrings.of(Locale.US, "third"),
                Optional.of(false)), // Hidden
            QuestionOption.create(
                4L,
                4L,
                "cuatro",
                LocalizedStrings.of(Locale.US, "fourth"),
                Optional.of(false))); // Hidden

    MultiOptionQuestionDefinition questionWithHiddenOptions =
        new MultiOptionQuestionDefinition(
            CONFIG, optionsWithSomeHidden, MultiOptionQuestionType.CHECKBOX);

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            questionWithHiddenOptions, applicant, applicantData, Optional.empty());

    // Applicant selects only valid (displayable) options
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 0, 1L);
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 1, 2L);

    MultiSelectQuestion multiSelectQuestion = applicantQuestion.createMultiSelectQuestion();

    // Should have no validation errors
    assertThat(multiSelectQuestion.getValidationErrors()).isEmpty();
  }

  @Test
  public void withLegacyOptions_allSelectionsValid() {
    // Legacy options don't have displayInAnswerOptions set
    ImmutableList<QuestionOption> legacyOptions =
        ImmutableList.of(
            QuestionOption.create(1L, "uno", LocalizedStrings.of(Locale.US, "valid")),
            QuestionOption.create(2L, "dos", LocalizedStrings.of(Locale.US, "ok")),
            QuestionOption.create(3L, "tres", LocalizedStrings.of(Locale.US, "third")),
            QuestionOption.create(4L, "cuatro", LocalizedStrings.of(Locale.US, "fourth")));

    MultiOptionQuestionDefinition legacyQuestion =
        new MultiOptionQuestionDefinition(CONFIG, legacyOptions, MultiOptionQuestionType.CHECKBOX);

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(legacyQuestion, applicant, applicantData, Optional.empty());

    // Select any legacy options (all should be valid)
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 0, 2L);
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 1, 3L);

    MultiSelectQuestion multiSelectQuestion = applicantQuestion.createMultiSelectQuestion();

    // Should have no validation errors - legacy options default to displayable
    assertThat(multiSelectQuestion.getValidationErrors()).isEmpty();
  }
}
