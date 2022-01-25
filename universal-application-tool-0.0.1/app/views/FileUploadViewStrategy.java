package views;

import static j2html.TagCreator.a;
import static j2html.attributes.Attr.FORM;
import static j2html.attributes.Attr.HREF;
import static views.BaseHtmlView.submitButton;

import controllers.applicant.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import services.MessageKey;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import views.applicant.ApplicantProgramBlockEditView.Params;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.questiontypes.FileUploadQuestionRenderer;
import views.style.ApplicantStyles;

/**
 * A strategy pattern that abstracts out the logic of file upload/download into the different cloud
 * providers.
 */
public abstract class FileUploadViewStrategy {

  static final String MIME_TYPES_IMAGES_AND_PDF = "image/*,.pdf";
  final String BLOCK_FORM_ID = "cf-block-form";
  final String FILEUPLOAD_CONTINUE_FORM_ID = "cf-fileupload-continue-form";
  final String FILEUPLOAD_DELETE_FORM_ID = "cf-fileupload-delete-form";
  final String FILEUPLOAD_SUBMIT_FORM_ID = "cf-block-submit";
  final String FILEUPLOAD_DELETE_BUTTON_ID = "fileupload-delete-button";
  final String FILEUPLOAD_SKIP_BUTTON_ID = "fileupload-skip-button";
  final String FILEUPLOAD_CONTINUE_BUTTON_ID = "fileupload-continue-button";
  final String REVIEW_APPLICATION_BUTTON_ID = "review-application-button";

  /**
   * Method to generate the field tags for the file upload view form.
   *
   * @param params the fields necessary to render applicant questions.
   * @param fileUploadQuestion The question that requires a file upload.
   * @return a container tag with the necessary fields
   */
  public abstract ContainerTag signedFileUploadFields(
      ApplicantQuestionRendererParams params, FileUploadQuestion fileUploadQuestion);

  /**
   * Method to render the submit form for uploading a file.
   *
   * @param params the information needed to render a file upload view
   * @param applicantQuestionRendererFactory a class for rendering applicant questions.
   * @return a container tag with the submit view
   */
  public abstract Tag renderFileUploadBlockSubmitForms(
      Params params, ApplicantQuestionRendererFactory applicantQuestionRendererFactory);

  Tag renderQuestion(
      ApplicantQuestion question,
      ApplicantQuestionRendererParams params,
      ApplicantQuestionRendererFactory applicantQuestionRendererFactory) {
    return applicantQuestionRendererFactory.getRenderer(question).render(params);
  }

  Tag renderFileKeyField(ApplicantQuestion question, ApplicantQuestionRendererParams params) {
    return FileUploadQuestionRenderer.renderFileKeyField(question, params, false);
  }

  Tag renderEmptyFileKeyField(ApplicantQuestion question, ApplicantQuestionRendererParams params) {
    return FileUploadQuestionRenderer.renderFileKeyField(question, params, true);
  }

  Tag renderReviewButton(Params params) {
    String reviewUrl =
        routes.ApplicantProgramReviewController.review(params.applicantId(), params.programId())
            .url();
    return a().attr(HREF, reviewUrl)
        .withText(params.messages().at(MessageKey.BUTTON_REVIEW.getKeyName()))
        .withId(REVIEW_APPLICATION_BUTTON_ID)
        .withClasses(ApplicantStyles.BUTTON_REVIEW);
  }

  Tag renderUploadButton(Params params) {
    String styles = ApplicantStyles.BUTTON_BLOCK_NEXT;
    if (hasUploadedFile(params)) {
      styles = ApplicantStyles.BUTTON_REVIEW;
    }
    return submitButton(params.messages().at(MessageKey.BUTTON_UPLOAD.getKeyName()))
        .attr(FORM, BLOCK_FORM_ID)
        .withClasses(styles)
        .withId(FILEUPLOAD_SUBMIT_FORM_ID);
  }

  boolean hasUploadedFile(Params params) {
    return params.block().getQuestions().stream()
        .map(ApplicantQuestion::createFileUploadQuestion)
        .map(FileUploadQuestion::getFileKeyValue)
        .anyMatch(maybeValue -> maybeValue.isPresent());
  }

  boolean hasAtLeastOneRequiredQuestion(Params params) {
    return params.block().getQuestions().stream().anyMatch(question -> !question.isOptional());
  }
}
