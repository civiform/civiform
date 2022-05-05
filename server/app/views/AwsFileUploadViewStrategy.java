package views;

import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.attributes.Attr.ENCTYPE;
import static j2html.attributes.Attr.FORM;

import controllers.applicant.routes;
import j2html.TagCreator;
import j2html.attributes.Attr;

import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.ButtonTag;

import java.util.Optional;
import play.mvc.Http.HttpVerbs;
import services.MessageKey;
import services.applicant.question.FileUploadQuestion;
import services.cloud.FileNameFormatter;
import services.cloud.StorageUploadRequest;
import services.cloud.aws.SignedS3UploadRequest;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.style.ApplicantStyles;
import views.style.Styles;

public class AwsFileUploadViewStrategy extends FileUploadViewStrategy {

  @Override
  public DivTag signedFileUploadFields(
      ApplicantQuestionRendererParams params, FileUploadQuestion fileUploadQuestion) {
    StorageUploadRequest genericRequest = params.signedFileUploadRequest().get();
    SignedS3UploadRequest request = castStorageRequest(genericRequest);
    Optional<String> uploaded =
        fileUploadQuestion.getFilename().map(f -> String.format("File uploaded: %s", f));
    DivTag fieldsTag =
        div()
            .with(div().withText(uploaded.orElse("")))
            .with(input().attr("type", "hidden").attr("name", "key").attr("value", request.key()))
            .with(
                input()
                    .attr("type", "hidden")
                    .attr("name", "success_action_redirect")
                    .attr("value", request.successActionRedirect()))
            .with(
                input()
                    .attr("type", "hidden")
                    .attr("name", "X-Amz-Credential")
                    .attr("value", request.credential()));
    if (!request.securityToken().isEmpty()) {
      fieldsTag.with(
          input()
              .attr("type", "hidden")
              .attr("name", "X-Amz-Security-Token")
              .attr("value", request.securityToken()));
    }
    return fieldsTag
        .with(input().attr("type", "hidden").attr("name", "X-Amz-Algorithm").attr("value", request.algorithm()))
        .with(input().attr("type", "hidden").attr("name", "X-Amz-Date").attr("value", request.date()))
        .with(input().attr("type", "hidden").attr("name", "Policy").attr("value", request.policy()))
        .with(input().attr("type", "hidden").attr("name", "X-Amz-Signature").attr("value", request.signature()))
        .with(input().attr("type", "file").attr("name", "file").attr(Attr.ACCEPT, acceptFileTypes()))
        .with(errorDiv(params.messages(), fileUploadQuestion));
  }

  @Override
  public DivTag renderFileUploadBlockSubmitForms(
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

    SignedS3UploadRequest signedRequest = castStorageRequest(request);

    ApplicantQuestionRendererParams rendererParams =
        ApplicantQuestionRendererParams.builder()
            .setMessages(params.messages())
            .setSignedFileUploadRequest(signedRequest)
            .setErrorDisplayMode(params.errorDisplayMode())
            .build();

    FormTag uploadForm =
        form()
            .withId(BLOCK_FORM_ID)
            .attr(ENCTYPE, "multipart/form-data")
            .attr("action", signedRequest.actionLink())
            .withMethod(HttpVerbs.POST)
            .with(
                each(
                    params.block().getQuestions(),
                    question ->
                        renderQuestion(
                            question, rendererParams, applicantQuestionRendererFactory)));
    DivTag skipForms = renderDeleteAndContinueFileUploadForms(params);
    DivTag buttons = renderFileUploadBottomNavButtons(params);
    return div(uploadForm, skipForms, buttons);
  }

  @Override
  protected Optional<ButtonTag> maybeRenderSkipOrDeleteButton(Params params) {
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
    ButtonTag button =
        button(buttonText)
            .attr("type", "submit")
            .attr(FORM, FILEUPLOAD_DELETE_FORM_ID)
            .withClasses(ApplicantStyles.BUTTON_REVIEW)
            .withId(buttonId);
    return Optional.of(button);
  }

  private SignedS3UploadRequest castStorageRequest(StorageUploadRequest request) {
    if (!(request instanceof SignedS3UploadRequest)) {
      throw new RuntimeException(
          "Tried to upload a file to AWS S3 storage using incorrect request type");
    }
    return (SignedS3UploadRequest) request;
  }

  DivTag renderFileUploadBottomNavButtons(Params params) {
    Optional<ButtonTag> maybeContinueButton = maybeRenderContinueButton(params);
    Optional<ButtonTag> maybeSkipOrDeleteButton = maybeRenderSkipOrDeleteButton(params);
    DivTag ret =
        div()
            .withClasses(ApplicantStyles.APPLICATION_NAV_BAR)
            // An empty div to take up the space to the left of the buttons.
            .with(div().withClasses(Styles.FLEX_GROW))
            .with(renderReviewButton(params))
            .with(renderPreviousButton(params));
    if (maybeSkipOrDeleteButton.isPresent()) {
      ret.with(maybeSkipOrDeleteButton.get());
    }
    ret.with(renderUploadButton(params));
    if (maybeContinueButton.isPresent()) {
      ret.with(maybeContinueButton.get());
    }
    return ret;
  }
}
