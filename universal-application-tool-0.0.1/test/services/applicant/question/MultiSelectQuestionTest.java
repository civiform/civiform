package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import services.MessageKey;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.QuestionOption;
import services.question.types.CheckboxQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import support.QuestionAnswerer;

public class MultiSelectQuestionTest {

  private static final MultiOptionQuestionDefinition CHECKBOX_QUESTION =
      new CheckboxQuestionDefinition(
          OptionalLong.of(1),
          "name",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"),
          ImmutableList.of(
              QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "valid")),
              QuestionOption.create(2L, LocalizedStrings.of(Locale.US, "ok")),
              QuestionOption.create(3L, LocalizedStrings.of(Locale.US, "third")),
              QuestionOption.create(4L, LocalizedStrings.of(Locale.US, "fourth"))),
          MultiOptionQuestionDefinition.MultiOptionValidationPredicates.builder()
              .setMinChoicesRequired(2)
              .setMaxChoicesAllowed(3)
              .build());

  private ApplicantData applicantData;

  @Before
  public void setUp() {
    applicantData = new ApplicantData();
  }

  @Test
  public void withEmptyApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(CHECKBOX_QUESTION, applicantData, Optional.empty());

    MultiSelectQuestion multiSelectQuestion = new MultiSelectQuestion(applicantQuestion);

    assertThat(multiSelectQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(multiSelectQuestion.hasConditionErrors()).isFalse();
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

    assertThat(multiSelectQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(multiSelectQuestion.hasConditionErrors()).isFalse();
  }

  @Test
  public void tooFewSelected_failsValidation() {

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(CHECKBOX_QUESTION, applicantData, Optional.empty());
    // Put too few selections.
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 0, 0L);

    MultiSelectQuestion multiSelectQuestion = applicantQuestion.createMultiSelectQuestion();

    assertThat(multiSelectQuestion.getQuestionErrors())
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

    assertThat(multiSelectQuestion.getQuestionErrors())
        .containsOnly(
            ValidationErrorMessage.create(MessageKey.MULTI_SELECT_VALIDATION_TOO_MANY, 3));
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

    assertThat(multiSelectQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(multiSelectQuestion.hasConditionErrors()).isFalse();
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
