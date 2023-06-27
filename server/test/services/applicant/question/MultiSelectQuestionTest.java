package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import services.MessageKey;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.program.ProgramQuestionDefinition;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionValidationPredicates;
import services.question.types.MultiOptionQuestionDefinitionConfig;
import services.question.types.MultiOptionQuestionDefinitionConfig.MultiOptionQuestionType;
import support.QuestionAnswerer;

public class MultiSelectQuestionTest {

  private static final MultiOptionQuestionDefinitionConfig CONFIG =
      MultiOptionQuestionDefinitionConfig.builder()
          .setMultiOptionQuestionType(MultiOptionQuestionType.CHECKBOX)
          .setId(OptionalLong.of(1))
          .setName("name")
          .setDescription("description")
          .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
          .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
          .setQuestionOptions(
              ImmutableList.of(
                  QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "valid")),
                  QuestionOption.create(2L, LocalizedStrings.of(Locale.US, "ok")),
                  QuestionOption.create(3L, LocalizedStrings.of(Locale.US, "third")),
                  QuestionOption.create(4L, LocalizedStrings.of(Locale.US, "fourth"))))
          .setValidationPredicates(
              MultiOptionValidationPredicates.builder()
                  .setMinChoicesRequired(2)
                  .setMaxChoicesAllowed(3)
                  .build())
          .setLastModifiedTime(Optional.empty())
          .build();
  private static final MultiOptionQuestionDefinition CHECKBOX_QUESTION =
      new MultiOptionQuestionDefinition(
          CONFIG.questionDefinitionConfig(),
          CONFIG.questionOptions(),
          CONFIG.multiOptionQuestionType());

  private ApplicantData applicantData;

  @Before
  public void setUp() {
    applicantData = new ApplicantData();
  }

  @Test
  public void withEmptyApplicantData_optionalQuestion() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            ProgramQuestionDefinition.create(CHECKBOX_QUESTION, Optional.empty()).setOptional(true),
            applicantData,
            Optional.empty());

    MultiSelectQuestion multiSelectQuestion = new MultiSelectQuestion(applicantQuestion);

    assertThat(multiSelectQuestion.getValidationErrors().isEmpty()).isTrue();
  }

  @Test
  public void withEmptyApplicantData_requiredQuestion_failsValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            ProgramQuestionDefinition.create(CHECKBOX_QUESTION, Optional.empty()),
            applicantData,
            Optional.empty());

    MultiSelectQuestion multiSelectQuestion = new MultiSelectQuestion(applicantQuestion);

    ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors =
        multiSelectQuestion.getValidationErrors();
    assertThat(validationErrors.size()).isEqualTo(1);
    assertThat(
            validationErrors.getOrDefault(
                applicantQuestion.getContextualizedPath(), ImmutableSet.of()))
        .containsOnly(ValidationErrorMessage.create(MessageKey.MULTI_SELECT_VALIDATION_TOO_FEW, 2));
  }

  @Test
  public void withValidApplicantData_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(CHECKBOX_QUESTION, applicantData, Optional.empty());
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 0, 1L);
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 1, 2L);

    MultiSelectQuestion multiSelectQuestion = new MultiSelectQuestion(applicantQuestion);

    assertThat(multiSelectQuestion.getValidationErrors().isEmpty()).isTrue();
  }

  @Test
  public void tooFewSelected_failsValidation() {

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(CHECKBOX_QUESTION, applicantData, Optional.empty());
    // Put too few selections.
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 0, 0L);

    MultiSelectQuestion multiSelectQuestion = applicantQuestion.createMultiSelectQuestion();

    ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors =
        multiSelectQuestion.getValidationErrors();
    assertThat(validationErrors.size()).isEqualTo(1);
    assertThat(
            validationErrors.getOrDefault(
                applicantQuestion.getContextualizedPath(), ImmutableSet.of()))
        .containsOnly(ValidationErrorMessage.create(MessageKey.MULTI_SELECT_VALIDATION_TOO_FEW, 2));
  }

  @Test
  public void tooManySelected_failsValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(CHECKBOX_QUESTION, applicantData, Optional.empty());
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

    ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors =
        multiSelectQuestion.getValidationErrors();
    assertThat(validationErrors.size()).isEqualTo(1);
    assertThat(
            validationErrors.getOrDefault(
                applicantQuestion.getContextualizedPath(), ImmutableSet.of()))
        .containsOnly(
            ValidationErrorMessage.create(MessageKey.MULTI_SELECT_VALIDATION_TOO_MANY, 4));
  }

  @Test
  public void selectedInvalidOptions_typeErrors_hasNoTypeErrors() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(CHECKBOX_QUESTION, applicantData, Optional.empty());
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 0, 1L);
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 1, 2L);

    MultiSelectQuestion multiSelectQuestion = applicantQuestion.createMultiSelectQuestion();

    assertThat(multiSelectQuestion.getValidationErrors().isEmpty()).isTrue();
  }

  @Test
  public void getOptions_defaultsIfLangUnsupported() {
    applicantData.setPreferredLocale(Locale.CHINESE);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(CHECKBOX_QUESTION, applicantData, Optional.empty());

    MultiSelectQuestion multiSelectQuestion = applicantQuestion.createMultiSelectQuestion();

    assertThat(multiSelectQuestion.getOptions()).isNotEmpty();
    assertThat(multiSelectQuestion.getOptions().get(0).locale())
        .isEqualTo(LocalizedStrings.DEFAULT_LOCALE);
  }
}
