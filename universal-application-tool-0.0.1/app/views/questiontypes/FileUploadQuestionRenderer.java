package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.input;

import com.google.inject.Inject;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import views.FileUploadStrategy;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;

/**
 * Renders a file upload question.
 *
 * <p>A file upload question requires a different form. See {@code
 * views.applicant.ApplicantProgramBlockEditView#renderFileUploadBlockSubmitForms}.
 */
public class FileUploadQuestionRenderer extends ApplicantQuestionRenderer {
  private static final String IMAGES_AND_PDF = "image/*,.pdf";

  @Inject FileUploadStrategy fileUploadStrategy;

  private final FileUploadQuestion fileuploadQuestion;

  public static Tag renderFileKeyField(
      ApplicantQuestion question, ApplicantQuestionRendererParams params, boolean clearData) {
    FileUploadQuestion fileuploadQuestion = question.createFileUploadQuestion();
    String value = fileuploadQuestion.getFileKeyValue().orElse("");
    if (clearData) {
      value = "";
    }
    return FieldWithLabel.input()
        .setFieldName(fileuploadQuestion.getFileKeyPath().toString())
        .setValue(value)
        .setFieldErrors(params.messages(), fileuploadQuestion.getQuestionErrors())
        .getContainer();
  }

  @Inject
  public FileUploadQuestionRenderer(ApplicantQuestion question) {
    super(question);
    this.fileuploadQuestion = question.createFileUploadQuestion();
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    return renderInternal(params.messages(), fileUploadFields(params));
  }

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.FILEUPLOAD_QUESTION;
  }

  private ContainerTag fileUploadFields(ApplicantQuestionRendererParams params) {
    if (params.isSample()) {
      return fileUploadFieldsPreview();
    }
    return fileUploadStrategy.signedFileUploadFields(params, fileuploadQuestion);
  }

  private ContainerTag fileUploadFieldsPreview() {
    return div()
        .with(input().withType("file").withName("file").attr(Attr.ACCEPT, acceptFileTypes()));
  }

  private String acceptFileTypes() {
    return IMAGES_AND_PDF;
  }
}
