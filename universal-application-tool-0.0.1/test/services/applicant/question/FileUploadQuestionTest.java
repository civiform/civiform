package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.i18n.Lang;
import play.i18n.Messages;
import services.Path;
import services.applicant.ApplicantData;
import services.question.types.FileUploadQuestionDefinition;
import support.QuestionAnswerer;

@RunWith(JUnitParamsRunner.class)
public class FileUploadQuestionTest {
  private static final FileUploadQuestionDefinition fileUploadQuestionDefinition =
      new FileUploadQuestionDefinition(
          "question name",
          Path.create("applicant.my.path.name"),
          Optional.empty(),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"));

  private final Messages messages =
      stubMessagesApi().preferred(ImmutableList.of(Lang.defaultLang()));

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
            fileUploadQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);

    FileUploadQuestion fileUploadQuestion = new FileUploadQuestion(applicantQuestion);

    assertThat(fileUploadQuestion.hasTypeSpecificErrors(messages)).isFalse();
    assertThat(fileUploadQuestion.hasQuestionErrors(messages)).isFalse();
  }

  @Test
  public void withApplicantData_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            fileUploadQuestionDefinition, applicantData, ApplicantData.APPLICANT_PATH);
    QuestionAnswerer.answerFileQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "file-key");

    FileUploadQuestion fileUploadQuestion = new FileUploadQuestion(applicantQuestion);

    assertThat(fileUploadQuestion.getFileKeyValue().get()).isEqualTo("file-key");
    assertThat(fileUploadQuestion.hasTypeSpecificErrors(messages)).isFalse();
    assertThat(fileUploadQuestion.hasQuestionErrors(messages)).isFalse();
  }
}
