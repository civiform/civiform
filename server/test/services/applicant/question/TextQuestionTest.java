package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.ApplicantModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.MessageKey;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.QuestionAnswerer;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.TextQuestionDefinition;

@RunWith(JUnitParamsRunner.class)
public class TextQuestionTest extends ResetPostgres {
  private static final TextQuestionDefinition textQuestionDefinition =
      new TextQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("question name")
              .setDescription("description")
              .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
              .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
              .setLastModifiedTime(Optional.empty())
              .setId(123L)
              .build());

  private static final TextQuestionDefinition minAndMaxLengthTextQuestionDefinition =
      new TextQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("question name")
              .setDescription("description")
              .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
              .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
              .setValidationPredicates(TextQuestionDefinition.TextValidationPredicates.create(3, 4))
              .setLastModifiedTime(Optional.empty())
              .setId(123L)
              .build());
  ;

  private ApplicantModel applicant;
  private ApplicantData applicantData;
  private Messages messages;

  @Before
  public void setUp() {
    applicant = new ApplicantModel();
    applicantData = applicant.getApplicantData();
    messages = instanceOf(MessagesApi.class).preferred(ImmutableList.of(Lang.defaultLang()));
  }

  @Test
  public void withEmptyApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(textQuestionDefinition, applicantData, Optional.empty());

    TextQuestion textQuestion = new TextQuestion(applicantQuestion);

    assertThat(textQuestion.getValidationErrors().isEmpty()).isTrue();
  }

  @Test
  public void withApplicantData_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(textQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerTextQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "hello");

    TextQuestion textQuestion = new TextQuestion(applicantQuestion);

    assertThat(textQuestion.getTextValue().get()).isEqualTo("hello");
    assertThat(textQuestion.getValidationErrors().isEmpty()).isTrue();
  }

  @Test
  @Parameters({"abc", "abcd"})
  public void withMinAndMaxLength_withValidApplicantData_passesValidation(String value) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            minAndMaxLengthTextQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerTextQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), value);

    TextQuestion textQuestion = new TextQuestion(applicantQuestion);

    assertThat(textQuestion.getTextValue().get()).isEqualTo(value);
    assertThat(textQuestion.getValidationErrors().isEmpty()).isTrue();
  }

  @SuppressWarnings("unused") // Used by @Parameters annotation of test below
  private Object[][] withMinAndMaxLength_withInvalidApplicantData_failsValidation_parameters() {
    return new Object[][] {
      {"", ValidationErrorMessage.create(MessageKey.TEXT_VALIDATION_TOO_SHORT, 3)},
      {"a", ValidationErrorMessage.create(MessageKey.TEXT_VALIDATION_TOO_SHORT, 3)},
      {"abcde", ValidationErrorMessage.create(MessageKey.TEXT_VALIDATION_TOO_LONG, 4)},
    };
  }

  @Test
  @Parameters(method = "withMinAndMaxLength_withInvalidApplicantData_failsValidation_parameters")
  public void withMinAndMaxLength_withInvalidApplicantData_failsValidation(
      String value, ValidationErrorMessage expectedError) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            minAndMaxLengthTextQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerTextQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), value);

    TextQuestion textQuestion = new TextQuestion(applicantQuestion);

    if (textQuestion.getTextValue().isPresent()) {
      assertThat(textQuestion.getTextValue().get()).isEqualTo(value);
    }

    assertThat(textQuestion.getValidationErrors())
        .isEqualTo(ImmutableMap.of(textQuestion.getTextPath(), ImmutableSet.of(expectedError)));
  }
}
