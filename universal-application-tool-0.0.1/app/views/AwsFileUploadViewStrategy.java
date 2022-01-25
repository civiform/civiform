package views;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.attributes.Attr.ENCTYPE;
import static j2html.attributes.Attr.FORM;
import static views.BaseHtmlView.makeCsrfTokenInputTag;
import static views.BaseHtmlView.submitButton;

import controllers.applicant.routes;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import play.i18n.Messages;
import play.mvc.Http.HttpVerbs;
import services.MessageKey;
import services.applicant.question.FileUploadQuestion;
import services.cloud.FileNameFormatter;
import services.cloud.StorageUploadRequest;
import services.cloud.aws.SignedS3UploadRequest;
import views.applicant.ApplicantProgramBlockEditView.Params;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.style.ApplicantStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.Styles;

public class AwsFileUploadViewStrategy extends FileUploadViewStrategy {

  @Override
  public ContainerTag signedFileUploadFields(
      ApplicantQuestionRendererParams params, FileUploadQuestion fileUploadQuestion) {
    StorageUploadRequest genericRequest = params.signedFileUploadRequest().get();
    SignedS3UploadRequest request = (SignedS3UploadRequest) genericRequest;
    Optional<String> uploaded =
        fileUploadQuestion.getFilename().map(f -> String.format("File uploaded: %s", f));
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
        .with(errorDiv(params.messages(), fileUploadQuestion));
  }

  @Override
  public Tag renderFileUploadBlockSubmitForms(
      Params params, ApplicantQuestionRendererFactory applicantQuestionRendererFactory) {
    String key = FileNameFormatter.formatFileUploadQuestionFilename(params);
    String onSuccessRedirectUrl =
        params.baseUrl()
            + routes.ApplicantProgramBlocksController.updateFile(
                    params.applicantId(),
                    params.programId(),
                    params.block().getId(),
                    params.inReview())
                .url();

    StorageUploadRequest request =
        params.storageClient().getSignedUploadRequest(key, onSuccessRedirectUrl);

    if (!(request instanceof SignedS3UploadRequest)) {
      throw new RuntimeException(
          "Tried to upload a file to AWS S3 storage using incorrect request type");
    }

    SignedS3UploadRequest signedRequest = (SignedS3UploadRequest) request;
    ApplicantQuestionRendererParams rendererParams =
        ApplicantQuestionRendererParams.builder()
            .setMessages(params.messages())
            .setSignedFileUploadRequest(signedRequest)
            .build();

    Tag uploadForm =
        form()
            .withId(BLOCK_FORM_ID)
            .attr(ENCTYPE, "multipart/form-data")
            .withAction(signedRequest.actionLink())
            .withMethod(HttpVerbs.POST)
            .with(
                each(
                    params.block().getQuestions(),
                    question ->
                        renderQuestion(
                            question, rendererParams, applicantQuestionRendererFactory)));
    Tag skipForms = renderDeleteAndContinueFileUploadForms(params);
    Tag buttons = renderFileUploadBottomNavButtons(params);
    return div(uploadForm, skipForms, buttons);
  }

  Tag renderFileUploadBottomNavButtons(Params params) {
    Optional<Tag> maybeContinueButton = maybeRenderContinueButton(params);
    Optional<Tag> maybeSkipOrDeleteButton = maybeRenderSkipOrDeleteButton(params);
    ContainerTag ret =
        div()
            .withClasses(ApplicantStyles.APPLICATION_NAV_BAR)
            // An empty div to take up the space to the left of the buttons.
            .with(div().withClasses(Styles.FLEX_GROW))
            .with(renderReviewButton(params));
    if (maybeSkipOrDeleteButton.isPresent()) {
      ret.with(maybeSkipOrDeleteButton.get());
    }
    ret.with(renderUploadButton(params));
    if (maybeContinueButton.isPresent()) {
      ret.with(maybeContinueButton.get());
    }
    return ret;
  }

  /**
   * Renders a form submit button for continue form if an uploaded file is present.
   *
   * <p>See {@link renderDeleteAndContinueFileUploadForms}.
   */
  Optional<Tag> maybeRenderContinueButton(Params params) {
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
   * Renders a form submit button for delete form if the file upload question is optional.
   *
   * <p>If an uploaded file is present, render the button text as delete. Otherwise, skip.
   *
   * <p>See {@link renderDeleteAndContinueFileUploadForms}.
   */
  private Optional<Tag> maybeRenderSkipOrDeleteButton(Params params) {
    if (hasAtLeastOneRequiredQuestion(params)) {
      // If the file question is required, skip or delete is not allowed.
      return Optional.empty();
    }
    String buttonText = params.messages().at(MessageKey.BUTTON_SKIP_FILEUPLOAD.getKeyName());
    String buttonId = FILEUPLOAD_SKIP_BUTTON_ID;
    if (hasUploadedFile(params)) {
      buttonText = params.messages().at(MessageKey.BUTTON_DELETE_FILE.getKeyName());
      buttonId = FILEUPLOAD_DELETE_BUTTON_ID;
    }
    Tag button =
        submitButton(buttonText)
            .attr(FORM, FILEUPLOAD_DELETE_FORM_ID)
            .withClasses(ApplicantStyles.BUTTON_REVIEW)
            .withId(buttonId);
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
  Tag renderDeleteAndContinueFileUploadForms(Params params) {
    String formAction =
        routes.ApplicantProgramBlocksController.update(
                params.applicantId(), params.programId(), params.block().getId(), params.inReview())
            .url();
    ApplicantQuestionRendererParams rendererParams =
        ApplicantQuestionRendererParams.builder().setMessages(params.messages()).build();

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

  private String acceptFileTypes() {
    return MIME_TYPES_IMAGES_AND_PDF;
  }

  private ContainerTag errorDiv(Messages messages, FileUploadQuestion fileUploadQuestion) {
    return div(fileUploadQuestion.fileRequiredMessage().getMessage(messages))
        .withClasses(
            ReferenceClasses.FILEUPLOAD_ERROR, BaseStyles.FORM_ERROR_TEXT_BASE, Styles.HIDDEN);
  }
}
