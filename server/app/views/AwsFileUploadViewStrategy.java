package views;

import static j2html.TagCreator.input;

import controllers.applicant.routes;
import j2html.attributes.Attr;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.FormTag;
import java.util.Optional;
import play.mvc.Http.HttpVerbs;
import services.MessageKey;
import services.applicant.question.FileUploadQuestion;
import services.cloud.FileNameFormatter;
import com.google.common.collect.ImmutableList;
import services.cloud.StorageUploadRequest;
import services.cloud.aws.SignedS3UploadRequest;

public final class AwsFileUploadViewStrategy extends FileUploadViewStrategy {

  @Override
  protected ImmutableList<InputTag> extraFileUploadFields(StorageUploadRequest request) {
    SignedS3UploadRequest signedRequest = castStorageRequest(request);
    ImmutableList.Builder<InputTag> builder = ImmutableList.builder();
    builder.add(
        input().withType("hidden").withName("key").withValue(signedRequest.key()),
        input()
            .withType("hidden")
            .withName("success_action_redirect")
            .withValue(signedRequest.successActionRedirect()),
        input()
            .withType("hidden")
            .withName("X-Amz-Credential")
            .withValue(signedRequest.credential()),
        input().withType("hidden").withName("X-Amz-Algorithm").withValue(signedRequest.algorithm()),
        input().withType("hidden").withName("X-Amz-Date").withValue(signedRequest.date()),
        input().withType("hidden").withName("Policy").withValue(signedRequest.policy()),
        input()
            .withType("hidden")
            .withName("X-Amz-Signature")
            .withValue(signedRequest.signature()));

    if (!signedRequest.securityToken().isEmpty()) {
      builder.add(
          input()
              .withType("hidden")
              .withName("X-Amz-Security-Token")
              .withValue(signedRequest.securityToken()));
    }
    return builder.build();
  }

  @Override
  protected FormTag renderFileUploadFormElement(Params params, StorageUploadRequest request) {
    SignedS3UploadRequest signedRequest = castStorageRequest(request);
    return super.renderFileUploadFormElement(params, request)
        .withAction(signedRequest.actionLink());
  }

  private SignedS3UploadRequest castStorageRequest(StorageUploadRequest request) {
    if (!(request instanceof SignedS3UploadRequest)) {
      throw new RuntimeException(
          "Tried to upload a file to AWS S3 storage using incorrect request type");
    }
    return (SignedS3UploadRequest) request;
  }
}
