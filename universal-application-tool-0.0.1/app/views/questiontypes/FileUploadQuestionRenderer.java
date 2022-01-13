package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.input;

import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import play.i18n.Messages;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import services.cloud.aws.SignedS3UploadRequest;
import views.components.FieldWithLabel;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.Styles;

/**
 * Renders a file upload question.
 *
 * <p>A file upload question requires a different form. See {@code
 * views.applicant.ApplicantProgramBlockEditView#renderFileUploadBlockSubmitForms}.
 */
public class FileUploadQuestionRenderer extends ApplicantQuestionRenderer {
  private static final String IMAGES_AND_PDF = "image/*,.pdf";

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
    return signedFileUploadFields(params);
  }

  private ContainerTag fileUploadFieldsPreview() {
    return div()
        .with(input().withType("file").withName("file").attr(Attr.ACCEPT, acceptFileTypes()));
  }

  private ContainerTag signedFileUploadFields(ApplicantQuestionRendererParams params) {
    SignedS3UploadRequest request = params.signedFileUploadRequest().get();
    Optional<String> uploaded =
        fileuploadQuestion.getFilename().map(f -> String.format("File uploaded: %s", f));
    ContainerTag fieldsTag =
        div()
            .with(div().withText(uploaded.orElse("")))
            .with(input().withType("hidden").withName("key").withValue(request.key()))
            .with(
                input()
                    .withType("hidden")
                    .withName("success_action_redirect")
                    .withValue(request.successActionRedirect()))
            .with(
                input()
                    .withType("hidden")
                    .withName("X-Amz-Credential")
                    .withValue(request.credential()));
    if (!request.securityToken().isEmpty()) {
      fieldsTag.with(
          input()
              .withType("hidden")
              .withName("X-Amz-Security-Token")
              .withValue(request.securityToken()));
    }
    return fieldsTag
        .with(input().withType("hidden").withName("X-Amz-Algorithm").withValue(request.algorithm()))
        .with(input().withType("hidden").withName("X-Amz-Date").withValue(request.date()))
        .with(input().withType("hidden").withName("Policy").withValue(request.policy()))
        .with(input().withType("hidden").withName("X-Amz-Signature").withValue(request.signature()))
        .with(input().withType("file").withName("file").attr(Attr.ACCEPT, acceptFileTypes()))
        .with(errorDiv(params.messages()));
  }

  private String acceptFileTypes() {
    return IMAGES_AND_PDF;
  }

  private ContainerTag errorDiv(Messages messages) {
    return div(fileuploadQuestion.fileRequiredMessage().getMessage(messages))
        .withClasses(
            ReferenceClasses.FILEUPLOAD_ERROR, BaseStyles.FORM_ERROR_TEXT_BASE, Styles.HIDDEN);
  }
}
