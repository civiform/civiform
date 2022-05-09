package views;

import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.attributes.Attr.FORM;
import static j2html.attributes.Attr.HREF;
import static views.BaseHtmlView.makeCsrfTokenInputTag;
import static views.BaseHtmlView.submitButton;

import controllers.applicant.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import play.i18n.Messages;
import play.mvc.Http.HttpVerbs;
import services.MessageKey;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import views.applicant.ApplicantProgramBlockEditView.Params;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.questiontypes.FileUploadQuestionRenderer;
import views.style.ApplicantStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.Styles;

/**
 * A strategy pattern that abstracts out the logic of file upload/download into the different cloud
 * providers.
 */
public abstract class FileUploadViewStrategy  extends ApplicationBaseView{

  static final String MIME_TYPES_IMAGES_AND_PDF = "image/*,.pdf";
  final String BLOCK_FORM_ID = "cf-block-form";
  final String FILEUPLOAD_CONTINUE_FORM_ID = "cf-fileupload-continue-form";
  final String FILEUPLOAD_DELETE_FORM_ID = "cf-fileupload-delete-form";
  final String FILEUPLOAD_SUBMIT_FORM_ID = "cf-block-submit";
  final String FILEUPLOAD_DELETE_BUTTON_ID = "fileupload-delete-button";
  final String FILEUPLOAD_SKIP_BUTTON_ID = "fileupload-skip-button";
  final String FILEUPLOAD_CONTINUE_BUTTON_ID = "fileupload-continue-button";


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

  /**
   * Renders a form submit button for delete form if the file upload question is optional.
   *
   * <p>If an uploaded file is present, render the button text as delete. Otherwise, skip.
   *
   * <p>See {@link renderDeleteAndContinueFileUploadForms}.
   */
  protected abstract Optional<ContainerTag> maybeRenderSkipOrDeleteButton(Params params);

  protected Tag renderQuestion(
      ApplicantQuestion question,
      ApplicantQuestionRendererParams params,
      ApplicantQuestionRendererFactory applicantQuestionRendererFactory) {
    return applicantQuestionRendererFactory.getRenderer(question).render(params);
  }

  /**
   * Renders a form submit button for continue form if an uploaded file is present.
   *
   * <p>See {@link renderDeleteAndContinueFileUploadForms}.
   */
  protected Optional<Tag> maybeRenderContinueButton(Params params) {
    if (!hasUploadedFile(params)) {
      return Optional.empty();
    }
    Tag button =
        submitButton(params.messages().at(MessageKey.BUTTON_KEEP_FILE.getKeyName()))
            .attr(FORM, FILEUPLOAD_CONTINUE_FORM_ID)
            .withClasses(ApplicantStyles.BUTTON_BLOCK_NEXT)
            .withId(FILEUPLOAD_CONTINUE_BUTTON_ID);
    return Optional.of(button);
  }

  /**
   * Returns two hidden forms for navigating through a file upload block without uploading a file.
   *
   * <p>Delete form sends an update with an empty file key. An empty file key erases the existing
   * file key if one is present. In either case, the file upload question is marked as seen but
   * unanswered, namely skipping the file upload. This is only allowed for an optional question.
   *
   * <p>Continue form sends an update with the currently stored file key, the same behavior as an
   * applicant re-submits a form without changing their answer. Continue form is only used when an
   * existing file (and file key) is present.
   */
  protected Tag renderDeleteAndContinueFileUploadForms(Params params) {
    String formAction =
        routes.ApplicantProgramBlocksController.update(
                params.applicantId(), params.programId(), params.block().getId(), params.inReview())
            .url();
    ApplicantQuestionRendererParams rendererParams =
        ApplicantQuestionRendererParams.builder()
            .setMessages(params.messages())
            .setErrorDisplayMode(params.errorDisplayMode())
            .build();

    Tag continueForm =
        form()
            .withId(FILEUPLOAD_CONTINUE_FORM_ID)
            .withAction(formAction)
            .withMethod(HttpVerbs.POST)
            .with(makeCsrfTokenInputTag(params.request()))
            .with(
                each(
                    params.block().getQuestions(),
                    question -> renderFileKeyField(question, rendererParams)));
    Tag deleteForm =
        form()
            .withId(FILEUPLOAD_DELETE_FORM_ID)
            .withAction(formAction)
            .withMethod(HttpVerbs.POST)
            .with(makeCsrfTokenInputTag(params.request()))
            .with(
                each(
                    params.block().getQuestions(),
                    question -> renderEmptyFileKeyField(question, rendererParams)));
    return div(continueForm, deleteForm).withClasses(Styles.HIDDEN);
  }

  protected Tag renderUploadButton(Params params) {
    String styles = ApplicantStyles.BUTTON_BLOCK_NEXT;
    if (hasUploadedFile(params)) {
      styles = ApplicantStyles.BUTTON_REVIEW;
    }
    return submitButton(params.messages().at(MessageKey.BUTTON_UPLOAD.getKeyName()))
        .attr(FORM, BLOCK_FORM_ID)
        .withClasses(styles)
        .withId(FILEUPLOAD_SUBMIT_FORM_ID);
  }


  protected Tag renderFileKeyField(
      ApplicantQuestion question, ApplicantQuestionRendererParams params) {
    return FileUploadQuestionRenderer.renderFileKeyField(question, params, false);
  }

  protected Tag renderEmptyFileKeyField(
      ApplicantQuestion question, ApplicantQuestionRendererParams params) {
    return FileUploadQuestionRenderer.renderFileKeyField(question, params, true);
  }

  protected boolean hasUploadedFile(Params params) {
    return params.block().getQuestions().stream()
        .map(ApplicantQuestion::createFileUploadQuestion)
        .map(FileUploadQuestion::getFileKeyValue)
        .anyMatch(maybeValue -> maybeValue.isPresent());
  }

  protected boolean hasAtLeastOneRequiredQuestion(Params params) {
    return params.block().getQuestions().stream().anyMatch(question -> !question.isOptional());
  }

  protected String acceptFileTypes() {
    return MIME_TYPES_IMAGES_AND_PDF;
  }

  protected ContainerTag errorDiv(Messages messages, FileUploadQuestion fileUploadQuestion) {
    return div(fileUploadQuestion.fileRequiredMessage().getMessage(messages))
        .withClasses(
            ReferenceClasses.FILEUPLOAD_ERROR, BaseStyles.FORM_ERROR_TEXT_BASE, Styles.HIDDEN);
  }
}
