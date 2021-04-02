package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.Applicant;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.Path;
import services.applicant.ApplicantData;
import services.question.NameQuestionDefinition;

@RunWith(JUnitParamsRunner.class)
public class NameQuestionTest {
  private static final NameQuestionDefinition nameQuestionDefinition =
      new NameQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
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

    assertThat(nameQuestion.hasFirstNameValue()).isFalse();
    assertThat(nameQuestion.hasMiddleNameValue()).isFalse();
    assertThat(nameQuestion.hasLastNameValue()).isFalse();
    assertThat(nameQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(nameQuestion.hasQuestionErrors()).isFalse();
  }

  @Test
  @Parameters({"Wendel,Middle Name,Patric", "Wendel,,Patrick"})
  public void withValidApplicantData(String firstName, String middleName, String lastName) {
    applicantData.putString(nameQuestionDefinition.getFirstNamePath(), firstName);
    applicantData.putString(nameQuestionDefinition.getMiddleNamePath(), middleName);
    applicantData.putString(nameQuestionDefinition.getLastNamePath(), lastName);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(nameQuestionDefinition, applicantData);

    NameQuestion nameQuestion = applicantQuestion.getNameQuestion();

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
  public void nameQuestion_withInvalidApplicantData(
      String firstName, String middleName, String lastName) {
    applicantData.putString(nameQuestionDefinition.getFirstNamePath(), firstName);
    applicantData.putString(nameQuestionDefinition.getMiddleNamePath(), middleName);
    applicantData.putString(nameQuestionDefinition.getLastNamePath(), lastName);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(nameQuestionDefinition, applicantData);

    NameQuestion nameQuestion = applicantQuestion.getNameQuestion();

    assertThat(nameQuestion.hasQuestionErrors()).isFalse();
    assertThat(nameQuestion.hasTypeSpecificErrors()).isTrue();
  }
}
