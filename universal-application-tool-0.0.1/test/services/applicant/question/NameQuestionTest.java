package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.Path;
import services.applicant.ApplicantData;
import services.question.types.NameQuestionDefinition;

@RunWith(JUnitParamsRunner.class)
public class NameQuestionTest {
  private static final NameQuestionDefinition nameQuestionDefinition =
      new NameQuestionDefinition(
          "question name",
          Path.create("applicant.my.path.name"),
          Optional.empty(),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"));

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
        new ApplicantQuestion(nameQuestionDefinition, applicantData);

    NameQuestion nameQuestion = new NameQuestion(applicantQuestion);

    assertThat(nameQuestion.getFirstNameValue()).isEmpty();
    assertThat(nameQuestion.getMiddleNameValue()).isEmpty();
    assertThat(nameQuestion.getLastNameValue()).isEmpty();
    assertThat(nameQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(nameQuestion.hasQuestionErrors()).isFalse();
  }

  @Test
  @Parameters({"Wendel,Middle Name,Patric", "Wendel,,Patrick"})
  public void withValidApplicantData_passesValidation(
      String firstName, String middleName, String lastName) {
    applicantData.putString(nameQuestionDefinition.getFirstNamePath(), firstName);
    applicantData.putString(nameQuestionDefinition.getMiddleNamePath(), middleName);
    applicantData.putString(nameQuestionDefinition.getLastNamePath(), lastName);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(nameQuestionDefinition, applicantData);

    NameQuestion nameQuestion = applicantQuestion.createNameQuestion();

    assertThat(nameQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(nameQuestion.hasQuestionErrors()).isFalse();
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
    applicantData.putString(nameQuestionDefinition.getFirstNamePath(), firstName);
    applicantData.putString(nameQuestionDefinition.getMiddleNamePath(), middleName);
    applicantData.putString(nameQuestionDefinition.getLastNamePath(), lastName);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(nameQuestionDefinition, applicantData);

    NameQuestion nameQuestion = applicantQuestion.createNameQuestion();

    assertThat(nameQuestion.hasQuestionErrors()).isFalse();
    assertThat(nameQuestion.hasTypeSpecificErrors()).isTrue();
  }
}
