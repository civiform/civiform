package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.input;

import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import views.FileUploadViewStrategy;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;

/**
 * Renders a file upload question.
 *
 * <p>A file upload question requires a different form. See {@code
 * views.applicant.ApplicantProgramBlockEditView#renderFileUploadBlockSubmitForms}.
 */
public class FileUploadQuestionRenderer extends ApplicantQuestionRendererImpl {
  private static final String IMAGES_AND_PDF = "image/*,.pdf";

  private final FileUploadViewStrategy fileUploadViewStrategy;
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

  public FileUploadQuestionRenderer(
      ApplicantQuestion question, FileUploadViewStrategy fileUploadViewStrategy) {
    super(question);
    this.fileuploadQuestion = question.createFileUploadQuestion();
    this.fileUploadViewStrategy = fileUploadViewStrategy;
  }

  @Override
  protected boolean shouldDisplayQuestionErrors() {
      return true;
  }

  @Override
  protected Tag renderTag(ApplicantQuestionRendererParams params) {
    return fileUploadFields(params);
  }

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.FILEUPLOAD_QUESTION;
  }

  private ContainerTag fileUploadFields(ApplicantQuestionRendererParams params) {
    if (params.isSample()) {
      return fileUploadFieldsPreview();
    }
    return fileUploadViewStrategy.signedFileUploadFields(params, fileuploadQuestion);
  }

  private ContainerTag fileUploadFieldsPreview() {
    return div()
        .with(input().withType("file").withName("file").attr(Attr.ACCEPT, acceptFileTypes()));
  }

  private String acceptFileTypes() {
    return IMAGES_AND_PDF;
  }
}
