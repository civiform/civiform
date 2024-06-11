package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import junitparams.JUnitParamsRunner;
import models.ApplicantModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.question.QuestionAnswerer;
import services.question.types.FileUploadQuestionDefinition;
import services.question.types.QuestionDefinitionConfig;

@RunWith(JUnitParamsRunner.class)
public class FileUploadQuestionTest extends ResetPostgres {
  private static final FileUploadQuestionDefinition fileUploadQuestionDefinition =
      new FileUploadQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("question name")
              .setDescription("description")
              .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
              .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
              .setId(OptionalLong.of(1))
              .setLastModifiedTime(Optional.empty())
              .build());
  ;

  private ApplicantModel applicant;
  private ApplicantData applicantData;

  @Before
  public void setUp() {
    applicant = new ApplicantModel();
    applicantData = applicant.getApplicantData();
  }

  @Test
  public void withEmptyApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(fileUploadQuestionDefinition, applicantData, Optional.empty());

    FileUploadQuestion fileUploadQuestion = new FileUploadQuestion(applicantQuestion);

    assertThat(fileUploadQuestion.getValidationErrors()).isEmpty();
  }

  @Test
  public void withApplicantData_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(fileUploadQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerFileQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "file-key");

    FileUploadQuestion fileUploadQuestion = new FileUploadQuestion(applicantQuestion);

    assertThat(fileUploadQuestion.getFileKeyValue().get()).isEqualTo("file-key");
    assertThat(fileUploadQuestion.getValidationErrors()).isEmpty();
  }

  @Test
  public void getFilename_notAnswered_returnsEmpty() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(fileUploadQuestionDefinition, applicantData, Optional.empty());

    FileUploadQuestion fileUploadQuestion = new FileUploadQuestion(applicantQuestion);

    assertThat(fileUploadQuestion.getFilename()).isEmpty();
  }

  @Test
  public void getFilename_emptyAnswer_returnsEmpty() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(fileUploadQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerFileQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "");
    FileUploadQuestion fileUploadQuestion = new FileUploadQuestion(applicantQuestion);

    assertThat(fileUploadQuestion.getFilename()).isEmpty();
  }

  @Test
  public void getFilename() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(fileUploadQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerFileQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        "applicant-123/program-456/block-789/filename");

    FileUploadQuestion fileUploadQuestion = new FileUploadQuestion(applicantQuestion);

    assertThat(fileUploadQuestion.getFilename().get()).isEqualTo("filename");
  }

  @Test
  public void getFilename_specialCharacters() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(fileUploadQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerFileQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        "applicant-123/program-456/block-789/file%?\\/^&!@");

    FileUploadQuestion fileUploadQuestion = new FileUploadQuestion(applicantQuestion);

    assertThat(fileUploadQuestion.getFilename().get()).isEqualTo("file%?\\/^&!@");
  }
}
