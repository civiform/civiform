package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import junitparams.JUnitParamsRunner;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.question.types.EmailQuestionDefinition;
import support.QuestionAnswerer;

@RunWith(JUnitParamsRunner.class)
public class EmailQuestionTest extends ResetPostgres {
  private static final EmailQuestionDefinition emailQuestionDefinition =
      new EmailQuestionDefinition(
          OptionalLong.of(1),
          "question name",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"));

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
        new ApplicantQuestion(emailQuestionDefinition, applicantData, Optional.empty());

    EmailQuestion emailQuestion = new EmailQuestion(applicantQuestion);

    assertThat(emailQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(emailQuestion.hasConditionErrors()).isFalse();
  }

  @Test
  public void withApplicantData_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(emailQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerEmailQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "test1@gmail.com");

    EmailQuestion emailQuestion = new EmailQuestion(applicantQuestion);

    assertThat(emailQuestion.getEmailValue().get()).isEqualTo("test1@gmail.com");
    assertThat(emailQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(emailQuestion.hasConditionErrors()).isFalse();
  }
}
