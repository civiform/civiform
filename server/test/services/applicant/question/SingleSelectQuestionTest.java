package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
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
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.LocalizedQuestionOption;
import services.question.QuestionAnswerer;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.QuestionDefinitionConfig;

@RunWith(JUnitParamsRunner.class)
public class SingleSelectQuestionTest extends ResetPostgres {

  private static final QuestionDefinitionConfig CONFIG =
      QuestionDefinitionConfig.builder()
          .setName("question name")
          .setDescription("description")
          .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
          .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
          .setId(OptionalLong.of(1))
          .setLastModifiedTime(Optional.empty())
          .build();

  private static final ImmutableList<QuestionOption> QUESTION_OPTIONS =
      ImmutableList.of(
          QuestionOption.create(
              1L, "opt1", LocalizedStrings.of(Locale.US, "option 1", Locale.FRANCE, "un")),
          QuestionOption.create(
              2L, "opt2", LocalizedStrings.of(Locale.US, "option 2", Locale.FRANCE, "deux")));

  private static final MultiOptionQuestionDefinition dropdownQuestionDefinition =
      new MultiOptionQuestionDefinition(CONFIG, QUESTION_OPTIONS, MultiOptionQuestionType.DROPDOWN);

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
        new ApplicantQuestion(
            dropdownQuestionDefinition, applicant, applicantData, Optional.empty());

    SingleSelectQuestion singleSelectQuestion = new SingleSelectQuestion(applicantQuestion);

    assertThat(singleSelectQuestion.getOptions())
        .containsOnly(
            LocalizedQuestionOption.create(
                /* id= */ 1L,
                /* order= */ 1L,
                /* adminName= */ "opt1",
                /* optionText= */ "option 1",
                /* displayInAnswerOptions= */ Optional.empty(),
                /* locale= */ Locale.US),
            LocalizedQuestionOption.create(
                /* id= */ 2L,
                /* order= */ 2L,
                /* adminName= */ "opt2",
                /* optionText= */ "option 2",
                /* displayInAnswerOptions= */ Optional.empty(),
                /* locale= */ Locale.US));
    assertThat(applicantQuestion.hasErrors()).isFalse();
  }

  @Test
  public void withPresentApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            dropdownQuestionDefinition, applicant, applicantData, Optional.empty());
    QuestionAnswerer.answerSingleSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 1L);

    SingleSelectQuestion singleSelectQuestion = applicantQuestion.createSingleSelectQuestion();

    assertThat(singleSelectQuestion.getValidationErrors()).isEmpty();
    assertThat(singleSelectQuestion.getSelectedOptionValue())
        .hasValue(
            LocalizedQuestionOption.create(
                /* id= */ 1L,
                /* order= */ 1L,
                /* adminName= */ "opt1",
                /* optionText= */ "option 1",
                /* displayInAnswerOptions= */ Optional.empty(),
                /* locale= */ Locale.US));
  }

  // The question has 2 options 1-2, Options are 1 based so 0 is not valid.
  @Test
  @Parameters({"0", "3", "-1", "1.1", "11111", "Not a Number", "&nbsp;"})
  public void withPresentApplicantData_selectedInvalidOption_hasErrors(String inputValue) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            dropdownQuestionDefinition, applicant, applicantData, Optional.empty());
    QuestionAnswerer.answerSingleSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), inputValue);

    SingleSelectQuestion singleSelectQuestion = applicantQuestion.createSingleSelectQuestion();

    assertThat(singleSelectQuestion.getValidationErrors()).hasSize(1);
    var errors =
        singleSelectQuestion
            .getValidationErrors()
            .get(singleSelectQuestion.getContextualizedPath());
    assertThat(errors).containsOnly(ValidationErrorMessage.create(MessageKey.INVALID_INPUT));

    assertThat(singleSelectQuestion.getSelectedOptionValue()).isEmpty();
  }

  @Test
  public void getSelectedOptionAdminName_getsAdminName() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            dropdownQuestionDefinition, applicant, applicantData, Optional.empty());
    QuestionAnswerer.answerSingleSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 2L);

    Optional<String> adminNames =
        applicantQuestion.createSingleSelectQuestion().getSelectedOptionAdminName();

    assertThat(adminNames).isPresent();
    assertThat(adminNames.get()).isEqualTo("opt2");
  }

  @Test
  public void getOptions_defaultsIfLangUnsupported() {
    applicantData.setPreferredLocale(Locale.CHINESE);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            dropdownQuestionDefinition, applicant, applicantData, Optional.empty());

    SingleSelectQuestion singleSelectQuestion = applicantQuestion.createSingleSelectQuestion();

    assertThat(singleSelectQuestion.getOptions()).isNotEmpty();
    assertThat(singleSelectQuestion.getOptions().get(0).locale())
        .isEqualTo(LocalizedStrings.DEFAULT_LOCALE);
  }

  @Test
  public void withRemovedOption_validationFails() {
    // Create question with options where option 2 is hidden (removed by admin)
    ImmutableList<QuestionOption> optionsWithOneHidden =
        ImmutableList.of(
            QuestionOption.create(
                1L,
                1L,
                "opt1",
                LocalizedStrings.of(Locale.US, "option 1"),
                Optional.of(true)), // Displayable
            QuestionOption.create(
                2L,
                2L,
                "opt2",
                LocalizedStrings.of(Locale.US, "option 2"),
                Optional.of(false))); // Hidden/removed by admin

    MultiOptionQuestionDefinition questionWithHiddenOption =
        new MultiOptionQuestionDefinition(
            CONFIG, optionsWithOneHidden, MultiOptionQuestionType.RADIO_BUTTON);

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(questionWithHiddenOption, applicant, applicantData, Optional.empty());

    // Applicant tries to submit option 2 (which was removed)
    QuestionAnswerer.answerSingleSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 2L);

    SingleSelectQuestion singleSelectQuestion = applicantQuestion.createSingleSelectQuestion();

    // Should have validation error because option 2 is not displayable
    assertThat(singleSelectQuestion.getValidationErrors()).hasSize(1);
    var errors =
        singleSelectQuestion
            .getValidationErrors()
            .get(singleSelectQuestion.getContextualizedPath());
    assertThat(errors).containsOnly(ValidationErrorMessage.create(MessageKey.INVALID_INPUT));
  }

  @Test
  public void withRemovedOption_displayableOptionStillValid() {
    // Create question with options where option 2 is hidden (removed by admin)
    ImmutableList<QuestionOption> optionsWithOneHidden =
        ImmutableList.of(
            QuestionOption.create(
                1L,
                1L,
                "opt1",
                LocalizedStrings.of(Locale.US, "option 1"),
                Optional.of(true)), // Displayable
            QuestionOption.create(
                2L,
                2L,
                "opt2",
                LocalizedStrings.of(Locale.US, "option 2"),
                Optional.of(false))); // Hidden/removed by admin

    MultiOptionQuestionDefinition questionWithHiddenOption =
        new MultiOptionQuestionDefinition(
            CONFIG, optionsWithOneHidden, MultiOptionQuestionType.DROPDOWN);

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(questionWithHiddenOption, applicant, applicantData, Optional.empty());

    // Applicant selects option 1 (which is still displayable)
    QuestionAnswerer.answerSingleSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 1L);

    SingleSelectQuestion singleSelectQuestion = applicantQuestion.createSingleSelectQuestion();

    // Should have no validation errors
    assertThat(singleSelectQuestion.getValidationErrors()).isEmpty();
    assertThat(singleSelectQuestion.getSelectedOptionValue()).isPresent();
    assertThat(singleSelectQuestion.getSelectedOptionValue().get().id()).isEqualTo(1L);
  }

  @Test
  public void withLegacyOptions_allOptionsStillValid() {
    // Legacy options don't have displayInAnswerOptions set
    ImmutableList<QuestionOption> legacyOptions =
        ImmutableList.of(
            QuestionOption.create(1L, "opt1", LocalizedStrings.of(Locale.US, "option 1")),
            QuestionOption.create(2L, "opt2", LocalizedStrings.of(Locale.US, "option 2")));

    MultiOptionQuestionDefinition legacyQuestion =
        new MultiOptionQuestionDefinition(CONFIG, legacyOptions, MultiOptionQuestionType.DROPDOWN);

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(legacyQuestion, applicant, applicantData, Optional.empty());

    // Applicant selects any option (both should be valid for legacy questions)
    QuestionAnswerer.answerSingleSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 2L);

    SingleSelectQuestion singleSelectQuestion = applicantQuestion.createSingleSelectQuestion();

    // Should have no validation errors - legacy options default to displayable
    assertThat(singleSelectQuestion.getValidationErrors()).isEmpty();
    assertThat(singleSelectQuestion.getSelectedOptionValue()).isPresent();
  }
}
