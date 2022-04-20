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
import services.question.types.IdQuestionDefinition;
import support.QuestionAnswerer;

@RunWith(JUnitParamsRunner.class)
public class IdQuestionTest extends ResetPostgres {
  private static final IdQuestionDefinition idQuestionDefinition =
      new IdQuestionDefinition(
          OptionalLong.of(1),
          "question name",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"),
          IdQuestionDefinition.IdValidationPredicates.create());

  private static final IdQuestionDefinition minAndMaxLengthIdQuestionDefinition =
      new IdQuestionDefinition(
          OptionalLong.of(1),
          "question name",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"),
          IdQuestionDefinition.IdValidationPredicates.create(3, 4));

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

    IdQuestion idQuestion = new IdQuestion(applicantQuestion);

    assertThat(idQuestion.getAllTypeSpecificErrors().isEmpty()).isTrue();
    assertThat(idQuestion.getQuestionErrors().isEmpty()).isTrue();
  }

  @Test
  public void withApplicantData_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(idQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerIdQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "12345");

    IdQuestion idQuestion = new IdQuestion(applicantQuestion);

    assertThat(idQuestion.getIdValue().get()).isEqualTo("12345");
    assertThat(idQuestion.getAllTypeSpecificErrors().isEmpty()).isTrue();
    assertThat(idQuestion.getQuestionErrors().isEmpty()).isTrue();
  }

  @Test
  @Parameters({"012", "0123"})
  public void withMinAndMaxLength_withValidApplicantData_passesValidation(String value) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(minAndMaxLengthIdQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerIdQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), value);

    IdQuestion idQuestion = new IdQuestion(applicantQuestion);

    assertThat(idQuestion.getIdValue().get()).isEqualTo(value);
    assertThat(idQuestion.getAllTypeSpecificErrors().isEmpty()).isTrue();
    assertThat(idQuestion.getQuestionErrors().isEmpty()).isTrue();
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
        new ApplicantQuestion(minAndMaxLengthIdQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerIdQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), value);

    IdQuestion idQuestion = new IdQuestion(applicantQuestion);

    if (idQuestion.getIdValue().isPresent()) {
      assertThat(idQuestion.getIdValue().get()).isEqualTo(value);
    }
    assertThat(idQuestion.getQuestionErrors().isEmpty()).isTrue();
    assertThat(idQuestion.getAllTypeSpecificErrors()).hasSize(1);
    String errorMessage = idQuestion.getAllTypeSpecificErrors().iterator().next().getMessage(messages);
    assertThat(errorMessage).isEqualTo(expectedErrorMessage);
  }
}
