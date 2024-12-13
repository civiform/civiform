package views.fileupload;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.stubMessagesApi;
import static views.fileupload.FileUploadViewStrategy.FILE_INPUT_HINT_ID_PREFIX;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.ScriptTag;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import models.ApplicantModel;
import org.junit.Test;
import play.api.libs.json.JsArray;
import play.api.libs.json.JsString;
import play.i18n.Lang;
import play.i18n.Messages;
import play.libs.Scala;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import services.cloud.StorageUploadRequest;
import services.question.QuestionAnswerer;
import services.question.types.FileUploadQuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import views.style.ReferenceClasses;

public class FileUploadViewStrategyTest {
  private static final int FILE_LIMIT_MB = 4;

  private final Messages messages =
      stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));

  private class StubViewStrategy extends FileUploadViewStrategy {
    @Override
    public ImmutableList<InputTag> additionalFileUploadFormInputs(
        Optional<StorageUploadRequest> request) {
      throw new UnsupportedOperationException(
          "Unimplemented method 'additionalFileUploadFormInputs'");
    }

    @Override
    public ImmutableMap<String, String> additionalFileUploadFormInputFields(
        Optional<StorageUploadRequest> request) {
      throw new UnsupportedOperationException(
          "Unimplemented method 'additionalFileUploadFormInputFields'");
    }

    @Override
    public String getUploadFormClass() {
      throw new UnsupportedOperationException("Unimplemented method 'getUploadFormClass'");
    }

    @Override
    public String getMultiFileUploadFormClass() {
      throw new UnsupportedOperationException("Unimplemented method 'getMultiFileUploadFormClass'");
    }

    @Override
    protected ImmutableList<ScriptTag> extraScriptTags() {
      throw new UnsupportedOperationException("Unimplemented method 'extraScriptTags'");
    }
  }

  @Test
  public void createUswdsFileInputFormElement_hasAriaDescribedByLabels() {
    DivTag uswdsForm =
        FileUploadViewStrategy.createUswdsFileInputFormElement(
            /* id= */ "fakeId",
            /* acceptedMimeTypes= */ "image/*",
            /* hints= */ ImmutableList.of("hint0", "hint1", "hint2"),
            /* disabled= */ false,
            FILE_LIMIT_MB,
            messages);

    String expectedAriaDescribedBy =
        FILE_INPUT_HINT_ID_PREFIX
            + "0 "
            + FILE_INPUT_HINT_ID_PREFIX
            + "1 "
            + FILE_INPUT_HINT_ID_PREFIX
            + "2";
    assertThat(uswdsForm.render())
        .containsPattern("<input type=\"file\".*aria-describedby=\"" + expectedAriaDescribedBy);
  }

  @Test
  public void createUswdsFileInputFormElement_hasFileTooLargeError() {
    DivTag uswdsForm =
        FileUploadViewStrategy.createUswdsFileInputFormElement(
            /* id= */ "fakeId",
            /* acceptedMimeTypes= */ "image/*",
            /* hints= */ ImmutableList.of("hint0", "hint1", "hint2"),
            /* disabled= */ false,
            FILE_LIMIT_MB,
            messages);

    assertThat(uswdsForm.render())
        .contains(String.format("id=\"%s\"", ReferenceClasses.FILEUPLOAD_TOO_LARGE_ERROR_ID));
  }

  @Test
  public void createUswdsFileInputFormElement_inputHasFileLimitAsAttr() {
    DivTag uswdsForm =
        FileUploadViewStrategy.createUswdsFileInputFormElement(
            /* id= */ "fakeId",
            /* acceptedMimeTypes= */ "image/*",
            /* hints= */ ImmutableList.of("hint0", "hint1", "hint2"),
            /* disabled= */ false,
            /* fileLimitMb= */ 5,
            messages);

    assertThat(uswdsForm.render())
        .containsPattern("<input type=\"file\".*data-file-limit-mb=\"5\"");
  }

  @Test
  public void getUploadedFileData() {

    FileUploadQuestionDefinition fileUploadQuestionDefinition =
        new FileUploadQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("question name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
                .setId(OptionalLong.of(1))
                .setLastModifiedTime(Optional.empty())
                .build());

    ApplicantData applicantData = new ApplicantData();
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            fileUploadQuestionDefinition, new ApplicantModel(), applicantData, Optional.empty());

    QuestionAnswerer.answerFileQuestionWithMultipleUpload(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        ImmutableList.of("test", "test2", "test3"));

    FileUploadQuestion fileUploadQuestion = applicantQuestion.createFileUploadQuestion();

    FileUploadViewStrategy viewStrategy = new StubViewStrategy();

    JsArray expectedArray =
        new JsArray(
            Scala.toSeq(
                ImmutableList.of(
                    new JsString("test"), new JsString("test2"), new JsString("test3"))));
    assertThat(viewStrategy.getUploadedFileData(fileUploadQuestion)).isEqualTo(expectedArray);
  }
}
