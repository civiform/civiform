package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.label;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import org.apache.commons.lang3.RandomStringUtils;
import play.i18n.Messages;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import views.applicant.ApplicantFileUploadRenderer;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;

/**
 * Renders a file upload question.
 *
 * <p>A file upload question requires a different form. See {@code
 * views.applicant.ApplicantProgramBlockEditView#renderFileUploadBlock}.
 */
public class FileUploadQuestionRenderer extends ApplicantSingleQuestionRenderer {
  private final ApplicantFileUploadRenderer applicantFileUploadRenderer;
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
      ApplicantQuestion question, ApplicantFileUploadRenderer applicantFileUploadRenderer) {
    super(question);
    this.fileUploadQuestion = question.createFileUploadQuestion();
    this.applicantFileUploadRenderer = applicantFileUploadRenderer;
    this.fileInputId = RandomStringUtils.randomAlphabetic(8);
  }

  @Override
  protected DivTag renderInputTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      ImmutableList<String> ariaDescribedByIds,
      boolean isOptional) {
    Messages messages = params.messages();
    boolean hasErrors = !validationErrors.isEmpty();
    return div()
        .with(
            label()
                .withFor(fileInputId)
                .withClass("sr-only")
                .withText(applicantQuestion.getQuestionTextForScreenReader()))
        .with(
            applicantFileUploadRenderer.signedFileUploadFields(
                params, fileUploadQuestion, fileInputId, ariaDescribedByIds, hasErrors))
        .with(
            label()
                .withFor(fileInputId)
                .with(
                    span()
                        .attr("role", "button")
                        .attr("tabindex", 0)
                        .withText(messages.at(MessageKey.BUTTON_CHOOSE_FILE.getKeyName()))
                        .withClasses(
                            ButtonStyles.OUTLINED_TRANSPARENT, "w-44", "mt-2", "cursor-pointer")));
  }

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.FILEUPLOAD_QUESTION;
  }
}
