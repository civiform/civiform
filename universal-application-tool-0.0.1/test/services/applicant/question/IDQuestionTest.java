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
import repository.WithPostgresContainer;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.question.types.IDQuestionDefinition;
import support.QuestionAnswerer;

@RunWith(JUnitParamsRunner.class)
public class IDQuestionTest extends WithPostgresContainer {
  private static final IDQuestionDefinition idQuestionDefinition =
      new IDQuestionDefinition(
          OptionalLong.of(1),
          "question name",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"),
          IDQuestionDefinition.IDValidationPredicates.create());

  private static final IDQuestionDefinition minAndMaxLengthIDQuestionDefinition =
      new IDQuestionDefinition(
          OptionalLong.of(1),
          "question name",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"),
          IDQuestionDefinition.IDValidationPredicates.create(3, 4));

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
        new ApplicantQuestion(idQuestionDefinition, applicantData, Optional.empty());

    IDQuestion idQuestion = new IDQuestion(applicantQuestion);

    assertThat(idQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(idQuestion.hasQuestionErrors()).isFalse();
  }

  @Test
  public void withApplicantData_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(idQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerIDQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "12345");

    IDQuestion idQuestion = new IDQuestion(applicantQuestion);

    assertThat(idQuestion.getIDValue().get()).isEqualTo("12345");
    assertThat(idQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(idQuestion.hasQuestionErrors()).isFalse();
  }

  @Test
  @Parameters({"123", "1234"})
  public void withMinAndMaxLength_withValidApplicantData_passesValidation(String value) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            minAndMaxLengthIDQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerIDQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), value);

    IDQuestion idQuestion = new IDQuestion(applicantQuestion);

    assertThat(idQuestion.getIDValue().get()).isEqualTo(value);
    assertThat(idQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(idQuestion.hasQuestionErrors()).isFalse();
  }

  @Test
  @Parameters({
    ",Must contain at least 3 characters.",
    "1,Must contain at least 3 characters.",
    "12334,Must contain at most 4 characters.",
    "abc,Must contain only numbers."
  })
  public void withMinAndMaxLength_withInvalidApplicantData_failsValidation(
      String value, String expectedErrorMessage) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            minAndMaxLengthIDQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerIDQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), value);

    IDQuestion idQuestion = new IDQuestion(applicantQuestion);

    if (idQuestion.getIDValue().isPresent()) {
      assertThat(idQuestion.getIDValue().get()).isEqualTo(value);
    }
    assertThat(idQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(idQuestion.getQuestionErrors()).hasSize(1);
    String errorMessage = idQuestion.getQuestionErrors().iterator().next().getMessage(messages);
    assertThat(errorMessage).isEqualTo(expectedErrorMessage);
  }
}
