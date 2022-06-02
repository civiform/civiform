package views.questiontypes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.Tag;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import views.FileUploadViewStrategy;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;

/**
 * Renders a file upload question.
 *
 * <p>A file upload question requires a different form. See {@code
 * views.applicant.ApplicantProgramBlockEditView#renderFileUploadBlock}.
 */
public class FileUploadQuestionRenderer extends ApplicantQuestionRendererImpl {
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
        .getContainer();
  }

  public FileUploadQuestionRenderer(
      ApplicantQuestion question, FileUploadViewStrategy fileUploadViewStrategy) {
    super(question);
    this.fileuploadQuestion = question.createFileUploadQuestion();
    this.fileUploadViewStrategy = fileUploadViewStrategy;
  }

  @Override
  protected Tag renderTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors) {
    return fileUploadViewStrategy.signedFileUploadFields(params, fileuploadQuestion);
  }

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.FILEUPLOAD_QUESTION;
  }
}
