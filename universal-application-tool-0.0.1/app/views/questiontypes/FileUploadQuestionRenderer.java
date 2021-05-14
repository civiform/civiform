package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.input;

import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import services.aws.SignedS3UploadRequest;
import views.BaseHtmlView;
import views.style.ReferenceClasses;
import views.style.Styles;

public class FileUploadQuestionRenderer extends ApplicantQuestionRenderer {

  public FileUploadQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    FileUploadQuestion fileUploadQuestion = question.createFileUploadQuestion();

    return div()
        .withId(question.getContextualizedPath().toString())
        .withClasses(Styles.MX_AUTO, Styles.W_MAX)
        .with(
            div()
                .withClasses(ReferenceClasses.APPLICANT_QUESTION_TEXT)
                .withText(question.getQuestionText()),
            div()
                .withClasses(
                    ReferenceClasses.APPLICANT_QUESTION_HELP_TEXT,
                    Styles.TEXT_BASE,
                    Styles.FONT_THIN,
                    Styles.MB_2)
                .withText(question.getQuestionHelpText()),
            fileUploadFields(params),
            BaseHtmlView.fieldErrors(params.messages(), fileUploadQuestion.getQuestionErrors()));
  }

  private ContainerTag fileUploadFields(ApplicantQuestionRendererParams params) {
    if (params.isSample()) {
      return fileUploadFieldsPreview();
    }
    return signedFileUploadFields(params.signedFileUploadRequest().get());
  }

  private ContainerTag fileUploadFieldsPreview() {
    return div().with(input().withType("file").withName("file"));
  }

  private ContainerTag signedFileUploadFields(SignedS3UploadRequest request) {
    ContainerTag fieldsTag =
        div()
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
        .with(input().withType("file").withName("file"));
  }
}
