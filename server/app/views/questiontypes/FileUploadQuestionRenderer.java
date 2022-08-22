package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.label;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import org.apache.commons.lang3.RandomStringUtils;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import views.FileUploadViewStrategy;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;
import views.style.Styles;

/**
 * Renders a file upload question.
 *
 * <p>A file upload question requires a different form. See {@code
 * views.applicant.ApplicantProgramBlockEditView#renderFileUploadBlock}.
 */
public class FileUploadQuestionRenderer extends ApplicantQuestionRendererImpl {
  private final FileUploadViewStrategy fileUploadViewStrategy;
  private final FileUploadQuestion fileUploadQuestion;
  // The ID used to associate the file input field with its screen reader label.
  private final String fileInputId;

  public static DivTag renderFileKeyField(
      ApplicantQuestion question, ApplicantQuestionRendererParams params, boolean clearData) {
    FileUploadQuestion fileUploadQuestion = question.createFileUploadQuestion();
    String value = fileUploadQuestion.getFileKeyValue().orElse("");
    if (clearData) {
      value = "";
    }
    return FieldWithLabel.input()
        .setFieldName(fileUploadQuestion.getFileKeyPath().toString())
        .setValue(value)
        .getInputTag();
  }

  public FileUploadQuestionRenderer(
      ApplicantQuestion question, FileUploadViewStrategy fileUploadViewStrategy) {
    super(question);
    this.fileUploadQuestion = question.createFileUploadQuestion();
    this.fileUploadViewStrategy = fileUploadViewStrategy;
    this.fileInputId = RandomStringUtils.randomAlphabetic(8);
  }

  @Override
  protected DivTag renderTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors) {
    return div()
        .with(
            label()
                .withFor(fileInputId)
                .withClass(Styles.SR_ONLY)
                .withText(question.getQuestionText()))
        .with(
            fileUploadViewStrategy.signedFileUploadFields(params, fileUploadQuestion, fileInputId));
  }

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.FILEUPLOAD_QUESTION;
  }
}
