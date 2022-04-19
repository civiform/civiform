package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.question.types.TextQuestionDefinition;
import support.QuestionAnswerer;

@RunWith(JUnitParamsRunner.class)
public class TextQuestionTest extends ResetPostgres {
  private static final TextQuestionDefinition textQuestionDefinition =
      new TextQuestionDefinition(
          OptionalLong.of(1),
          "question name",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"),
          TextQuestionDefinition.TextValidationPredicates.create());

  private static final TextQuestionDefinition minAndMaxLengthTextQuestionDefinition =
      new TextQuestionDefinition(
          OptionalLong.of(1),
          "question name",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"),
          TextQuestionDefinition.TextValidationPredicates.create(3, 4));

  private Applicant applicant;
  private ApplicantData applicantData;
  private Messages messages;

  @Before
  public void setUp() {
    applicant = new Applicant();
    applicantData = applicant.getApplicantData();
    messages = instanceOf(MessagesApi.class).preferred(ImmutableList.of(Lang.defaultLang()));
  }

  @Test
  public void withEmptyApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(textQuestionDefinition, applicantData, Optional.empty());

    TextQuestion textQuestion = new TextQuestion(applicantQuestion);

    assertThat(textQuestion.getAllTypeSpecificErrors().isEmpty()).isTrue();
    assertThat(textQuestion.getQuestionErrors().isEmpty()).isTrue();
  }

  @Test
  public void withApplicantData_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(textQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerTextQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "hello");

    TextQuestion textQuestion = new TextQuestion(applicantQuestion);

    assertThat(textQuestion.getTextValue().get()).isEqualTo("hello");
    assertThat(textQuestion.getAllTypeSpecificErrors().isEmpty()).isTrue();
    assertThat(textQuestion.getQuestionErrors().isEmpty()).isTrue();
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
    assertThat(textQuestion.getAllTypeSpecificErrors().isEmpty()).isTrue();
    assertThat(textQuestion.getQuestionErrors().isEmpty()).isTrue();
  }

  @Test
  @Parameters({
    ",Must contain at least 3 characters.",
    "a,Must contain at least 3 characters.",
    "abcde,Must contain at most 4 characters."
  })
  public void withMinAndMaxLength_withInvalidApplicantData_failsValidation(
      String value, String expectedErrorMessage) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            minAndMaxLengthTextQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerTextQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), value);

    TextQuestion textQuestion = new TextQuestion(applicantQuestion);

    if (textQuestion.getTextValue().isPresent()) {
      assertThat(textQuestion.getTextValue().get()).isEqualTo(value);
    }
    assertThat(textQuestion.getAllTypeSpecificErrors().isEmpty()).isTrue();
    assertThat(textQuestion.getQuestionErrors()).hasSize(1);
    String errorMessage = textQuestion.getQuestionErrors().iterator().next().getMessage(messages);
    assertThat(errorMessage).isEqualTo(expectedErrorMessage);
  }
}
