package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.Applicant;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.TextQuestionDefinition;

@RunWith(JUnitParamsRunner.class)
public class TextQuestionTest {
  private static final TextQuestionDefinition textQuestionDefinition =
      new TextQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
          "description",
          LifecycleStage.ACTIVE,
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"));

  private static final TextQuestionDefinition minAndMaxLengthTextQuestionDefinition =
      new TextQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
          "description",
          LifecycleStage.ACTIVE,
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"),
          TextQuestionDefinition.TextValidationPredicates.create(3, 4));

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
        new ApplicantQuestion(textQuestionDefinition, applicantData);

    TextQuestion textQuestion = new TextQuestion(applicantQuestion);

    assertThat(textQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(textQuestion.hasQuestionErrors()).isFalse();
  }

  @Test
  public void withApplicantData_passesValidation() {
    applicantData.putString(textQuestionDefinition.getTextPath(), "hello");
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(textQuestionDefinition, applicantData);

    TextQuestion textQuestion = new TextQuestion(applicantQuestion);

    assertThat(textQuestion.getTextValue().get()).isEqualTo("hello");
    assertThat(textQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(textQuestion.hasQuestionErrors()).isFalse();
  }

  @Test
  @Parameters({"abc", "abcd"})
  public void withMinAndMaxLength_withValidApplicantData_passesValidation(String value) {
    applicantData.putString(minAndMaxLengthTextQuestionDefinition.getTextPath(), value);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(minAndMaxLengthTextQuestionDefinition, applicantData);

    TextQuestion textQuestion = new TextQuestion(applicantQuestion);

    assertThat(textQuestion.getTextValue().get()).isEqualTo(value);
    assertThat(textQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(textQuestion.hasQuestionErrors()).isFalse();
  }

  @Test
  @Parameters({
    // TODO(https://github.com/seattle-uat/civiform/issues/634): Uncomment the following line once
    //  this bug is fixed.
    // ",This answer must be at least 3 characters long.",
    "a,This answer must be at least 3 characters long.",
    "abcde,This answer must be at most 4 characters long."
  })
  public void withMinAndMaxLength_withInValidApplicantData_failsValidation(
      String value, String expectedErrorMessage) {
    applicantData.putString(minAndMaxLengthTextQuestionDefinition.getTextPath(), value);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(minAndMaxLengthTextQuestionDefinition, applicantData);

    TextQuestion textQuestion = new TextQuestion(applicantQuestion);

    if (textQuestion.getTextValue().isPresent()) {
      assertThat(textQuestion.getTextValue().get()).isEqualTo(value);
    }
    assertThat(textQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(textQuestion.getQuestionErrors())
        .containsOnly(ValidationErrorMessage.create(expectedErrorMessage));
  }
}
