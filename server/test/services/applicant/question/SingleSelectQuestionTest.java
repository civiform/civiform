package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.question.LocalizedQuestionOption;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.QuestionDefinitionConfig;
import support.QuestionAnswerer;

public class SingleSelectQuestionTest {

  private static final QuestionDefinitionConfig CONFIG =
      QuestionDefinitionConfig.builder()
          .setName("question name")
          .setDescription("description")
          .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
          .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
          .setValidationPredicates(
              MultiOptionQuestionDefinition.MultiOptionValidationPredicates.create())
          .setId(OptionalLong.of(1))
          .setLastModifiedTime(Optional.empty())
          .build();

  private static final ImmutableList<QuestionOption> QUESTION_OPTIONS =
      ImmutableList.of(
          QuestionOption.create(
              1L, LocalizedStrings.of(Locale.US, "option 1", Locale.FRANCE, "un")),
          QuestionOption.create(
              2L, LocalizedStrings.of(Locale.US, "option 2", Locale.FRANCE, "deux")));

  private static final MultiOptionQuestionDefinition dropdownQuestionDefinition =
      new MultiOptionQuestionDefinition(CONFIG, QUESTION_OPTIONS, MultiOptionQuestionType.DROPDOWN);

  private Applicant applicant;
  private ApplicantData applicantData;

  @Before
  public void setUp() {
    applicant = new Applicant();
    applicantData = applicant.getApplicantData();
  }

  @Test
  public void withEmptyApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(dropdownQuestionDefinition, applicantData, Optional.empty());

    SingleSelectQuestion singleSelectQuestion = new SingleSelectQuestion(applicantQuestion);

    assertThat(singleSelectQuestion.getOptions())
        .containsOnly(
            LocalizedQuestionOption.create(1L, 1L, "option 1", Locale.US),
            LocalizedQuestionOption.create(2L, 2L, "option 2", Locale.US));
    assertThat(applicantQuestion.hasErrors()).isFalse();
  }

  @Test
  public void withPresentApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(dropdownQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerSingleSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 1L);

    SingleSelectQuestion singleSelectQuestion = applicantQuestion.createSingleSelectQuestion();

    assertThat(singleSelectQuestion.getValidationErrors().isEmpty()).isTrue();
    assertThat(singleSelectQuestion.getSelectedOptionValue())
        .hasValue(LocalizedQuestionOption.create(1L, 1L, "option 1", Locale.US));
  }

  @Test
  public void withPresentApplicantData_selectedInvalidOption_hasErrors() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(dropdownQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerSingleSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 9L);

    SingleSelectQuestion singleSelectQuestion = applicantQuestion.createSingleSelectQuestion();

    assertThat(singleSelectQuestion.getValidationErrors().isEmpty()).isTrue();
    assertThat(singleSelectQuestion.getSelectedOptionValue()).isEmpty();
  }

  @Test
  public void getOptions_defaultsIfLangUnsupported() {
    applicantData.setPreferredLocale(Locale.CHINESE);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(dropdownQuestionDefinition, applicantData, Optional.empty());

    SingleSelectQuestion singleSelectQuestion = applicantQuestion.createSingleSelectQuestion();

    assertThat(singleSelectQuestion.getOptions()).isNotEmpty();
    assertThat(singleSelectQuestion.getOptions().get(0).locale())
        .isEqualTo(LocalizedStrings.DEFAULT_LOCALE);
  }
}
