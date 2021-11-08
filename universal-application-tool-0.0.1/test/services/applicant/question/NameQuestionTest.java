package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.question.types.NameQuestionDefinition;
import support.QuestionAnswerer;

@RunWith(JUnitParamsRunner.class)
public class NameQuestionTest {
  private static final NameQuestionDefinition nameQuestionDefinition =
      new NameQuestionDefinition(
          OptionalLong.of(1),
          "question name",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"),
          NameQuestionDefinition.NameValidationPredicates.create());

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
        new ApplicantQuestion(nameQuestionDefinition, applicantData, Optional.empty());

    NameQuestion nameQuestion = new NameQuestion(applicantQuestion);

    assertThat(nameQuestion.getFirstNameValue()).isEmpty();
    assertThat(nameQuestion.getMiddleNameValue()).isEmpty();
    assertThat(nameQuestion.getLastNameValue()).isEmpty();
    assertThat(nameQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(nameQuestion.hasConditionErrors()).isFalse();
  }

  @Test
  @Parameters({"Wendel,Middle Name,Patric", "Wendel,,Patrick"})
  public void withValidApplicantData_passesValidation(
      String firstName, String middleName, String lastName) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(nameQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerNameQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), firstName, middleName, lastName);

    NameQuestion nameQuestion = applicantQuestion.createNameQuestion();

    assertThat(nameQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(nameQuestion.hasConditionErrors()).isFalse();
    assertThat(nameQuestion.getFirstNameValue().get()).isEqualTo(firstName);
    if (nameQuestion.getMiddleNameValue().isPresent()) {
      assertThat(nameQuestion.getMiddleNameValue().get()).isEqualTo(middleName);
    }
    assertThat(nameQuestion.getLastNameValue().get()).isEqualTo(lastName);
  }

  @Test
  @Parameters({",,", ",Middle Name,", "Wendel,,", ",,Patrick"})
  public void withInvalidApplicantData_failsValidation(
      String firstName, String middleName, String lastName) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(nameQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerNameQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), firstName, middleName, lastName);

    NameQuestion nameQuestion = applicantQuestion.createNameQuestion();

    assertThat(nameQuestion.hasConditionErrors()).isFalse();
    assertThat(nameQuestion.hasTypeSpecificErrors()).isTrue();
  }
}
