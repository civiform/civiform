package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import services.LocalizationUtils;
import services.Path;
import services.applicant.ApplicantData;
import services.question.LocalizedQuestionOption;
import services.question.QuestionOption;
import services.question.types.DropdownQuestionDefinition;
import support.QuestionAnswerer;

public class SingleSelectQuestionTest {

  private static final DropdownQuestionDefinition dropdownQuestionDefinition =
      new DropdownQuestionDefinition(
          "question name",
          Path.create("applicant.my.path.name"),
          Optional.empty(),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"),
          ImmutableList.of(
              QuestionOption.builder()
                  .setId(1L)
                  .setOptionText(ImmutableMap.of(Locale.US, "option 1", Locale.FRANCE, "un"))
                  .build(),
              QuestionOption.builder()
                  .setId(2L)
                  .setOptionText(ImmutableMap.of(Locale.US, "option 2", Locale.FRANCE, "deux"))
                  .build()));

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
        new ApplicantQuestion(
            dropdownQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);

    SingleSelectQuestion singleSelectQuestion = new SingleSelectQuestion(applicantQuestion);

    assertThat(singleSelectQuestion.getOptions())
        .containsOnly(
            LocalizedQuestionOption.create(1L, "option 1", Locale.US),
            LocalizedQuestionOption.create(2L, "option 2", Locale.US));
    assertThat(applicantQuestion.hasErrors()).isFalse();
  }

  @Test
  public void withPresentApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            dropdownQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);
    QuestionAnswerer.answerSingleSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 1L);

    SingleSelectQuestion singleSelectQuestion = applicantQuestion.createSingleSelectQuestion();

    assertThat(singleSelectQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(singleSelectQuestion.hasQuestionErrors()).isFalse();
    assertThat(singleSelectQuestion.getSelectedOptionValue())
        .hasValue(LocalizedQuestionOption.create(1L, "option 1", Locale.US));
  }

  @Test
  public void withPresentApplicantData_selectedInvalidOption_hasErrors() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            dropdownQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);
    QuestionAnswerer.answerSingleSelectQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), 9L);

    SingleSelectQuestion singleSelectQuestion = applicantQuestion.createSingleSelectQuestion();

    assertThat(singleSelectQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(singleSelectQuestion.hasQuestionErrors()).isFalse();
    assertThat(singleSelectQuestion.getSelectedOptionValue()).isEmpty();
  }

  @Test
  public void getOptions_defaultsIfLangUnsupported() {
    applicantData.setPreferredLocale(Locale.CHINESE);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            dropdownQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);

    SingleSelectQuestion singleSelectQuestion = applicantQuestion.createSingleSelectQuestion();

    assertThat(singleSelectQuestion.getOptions()).isNotEmpty();
    assertThat(singleSelectQuestion.getOptions().get(0).locale())
        .isEqualTo(LocalizationUtils.DEFAULT_LOCALE);
  }
}
