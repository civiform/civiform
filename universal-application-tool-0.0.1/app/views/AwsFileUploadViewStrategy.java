package views;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.attributes.Attr.ENCTYPE;

import controllers.applicant.routes;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import play.mvc.Http.HttpVerbs;
import services.applicant.question.FileUploadQuestion;
import services.cloud.FileNameFormatter;
import services.cloud.StorageUploadRequest;
import services.cloud.aws.SignedS3UploadRequest;
import views.applicant.ApplicantProgramBlockEditView.Params;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.style.ApplicantStyles;
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
}
