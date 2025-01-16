package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
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
        new ApplicantQuestion(
            fileUploadQuestionDefinition, applicant, applicantData, Optional.empty());

    FileUploadQuestion fileUploadQuestion = new FileUploadQuestion(applicantQuestion);

    assertThat(fileUploadQuestion.getValidationErrors()).isEmpty();
  }

  @Test
  public void withApplicantData_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            fileUploadQuestionDefinition, applicant, applicantData, Optional.empty());
    QuestionAnswerer.answerFileQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "file-key");

    FileUploadQuestion fileUploadQuestion = new FileUploadQuestion(applicantQuestion);

    assertThat(fileUploadQuestion.getFileKeyValue().get()).isEqualTo("file-key");
    assertThat(fileUploadQuestion.getValidationErrors()).isEmpty();
  }

  @Test
  public void withApplicantData_multiFile_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            fileUploadQuestionDefinition, applicant, applicantData, Optional.empty());

    FileUploadQuestion fileUploadQuestion = new FileUploadQuestion(applicantQuestion);
    QuestionAnswerer.answerFileQuestionWithMultipleUpload(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        ImmutableList.of("filekey1", "filekey2"));
    assertThat(fileUploadQuestion.getFileKeyListValue().get())
        .containsExactly("filekey1", "filekey2");
    assertThat(fileUploadQuestion.getFileKeyValueForIndex(0).get()).isEqualTo("filekey1");
    assertThat(fileUploadQuestion.getFileKeyValueForIndex(1).get()).isEqualTo("filekey2");
    assertThat(fileUploadQuestion.getValidationErrors()).isEmpty();
  }

  @Test
  public void getOriginalFileName_multiFile_notAnswered_returnsNotPresent() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            fileUploadQuestionDefinition, applicant, applicantData, Optional.empty());

    FileUploadQuestion fileUploadQuestion = new FileUploadQuestion(applicantQuestion);
    assertFalse(fileUploadQuestion.getOriginalFileNameListValue().isPresent());
    assertFalse(fileUploadQuestion.getOriginalFileNameValueForIndex(0).isPresent());
  }

  @Test
  public void getOriginalFileName_multiFile_emptyAnswer_returnsNotPresent() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            fileUploadQuestionDefinition, applicant, applicantData, Optional.empty());

    FileUploadQuestion fileUploadQuestion = new FileUploadQuestion(applicantQuestion);
    QuestionAnswerer.answerFileQuestionWithMultipleUploadOriginalNames(
        applicantData, applicantQuestion.getContextualizedPath(), ImmutableList.of(""));
    assertFalse(fileUploadQuestion.getOriginalFileNameListValue().isPresent());
    assertFalse(fileUploadQuestion.getOriginalFileNameValueForIndex(0).isPresent());
  }

  @Test
  public void getOriginalFileName_multiFile() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            fileUploadQuestionDefinition, applicant, applicantData, Optional.empty());

    FileUploadQuestion fileUploadQuestion = new FileUploadQuestion(applicantQuestion);
    QuestionAnswerer.answerFileQuestionWithMultipleUploadOriginalNames(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        ImmutableList.of("filename1", "filename2"));
    assertThat(fileUploadQuestion.getOriginalFileNameListValue().get())
        .containsExactly("filename1", "filename2");
    assertThat(fileUploadQuestion.getOriginalFileNameValueForIndex(0).get()).isEqualTo("filename1");
    assertThat(fileUploadQuestion.getOriginalFileNameValueForIndex(1).get()).isEqualTo("filename2");
  }

  @Test
  public void getFilename_notAnswered_returnsEmpty() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            fileUploadQuestionDefinition, applicant, applicantData, Optional.empty());

    FileUploadQuestion fileUploadQuestion = new FileUploadQuestion(applicantQuestion);

    assertThat(fileUploadQuestion.getFilename()).isEmpty();
  }

  @Test
  public void getFilename_emptyAnswer_returnsEmpty() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            fileUploadQuestionDefinition, applicant, applicantData, Optional.empty());
    QuestionAnswerer.answerFileQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "");
    FileUploadQuestion fileUploadQuestion = new FileUploadQuestion(applicantQuestion);

    assertThat(fileUploadQuestion.getFilename()).isEmpty();
  }

  @Test
  public void getFilename() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            fileUploadQuestionDefinition, applicant, applicantData, Optional.empty());
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
        new ApplicantQuestion(
            fileUploadQuestionDefinition, applicant, applicantData, Optional.empty());
    QuestionAnswerer.answerFileQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        "applicant-123/program-456/block-789/file%?\\/^&!@");

    FileUploadQuestion fileUploadQuestion = new FileUploadQuestion(applicantQuestion);

    assertThat(fileUploadQuestion.getFilename().get()).isEqualTo("file%?\\/^&!@");
  }

  @Test
  public void canUploadFile() {
    FileUploadQuestionDefinition fileUploadQuestionDefinitionWithMax =
        new FileUploadQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("question name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
                .setId(OptionalLong.of(1))
                .setValidationPredicates(
                    FileUploadQuestionDefinition.FileUploadValidationPredicates.builder()
                        .setMaxFiles(OptionalInt.of(2))
                        .build())
                .setLastModifiedTime(Optional.empty())
                .build());
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            fileUploadQuestionDefinitionWithMax, applicant, applicantData, Optional.empty());
    FileUploadQuestion fileUploadQuestion = new FileUploadQuestion(applicantQuestion);

    assertThat(fileUploadQuestion.canUploadFile()).isTrue();

    QuestionAnswerer.answerFileQuestionWithMultipleUpload(
        applicantData, applicantQuestion.getContextualizedPath(), 0, "filekey1");

    assertThat(fileUploadQuestion.canUploadFile()).isTrue();

    QuestionAnswerer.answerFileQuestionWithMultipleUpload(
        applicantData, applicantQuestion.getContextualizedPath(), 1, "filekey2");

    assertThat(fileUploadQuestion.canUploadFile()).isFalse();
  }

  @Test
  public void canUploadFile_noMaxSet() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            fileUploadQuestionDefinition, applicant, applicantData, Optional.empty());

    FileUploadQuestion fileUploadQuestion = new FileUploadQuestion(applicantQuestion);

    assertThat(fileUploadQuestion.canUploadFile()).isTrue();

    QuestionAnswerer.answerFileQuestionWithMultipleUpload(
        applicantData, applicantQuestion.getContextualizedPath(), ImmutableList.of("filekey1"));

    assertThat(fileUploadQuestion.canUploadFile()).isTrue();
  }
}
