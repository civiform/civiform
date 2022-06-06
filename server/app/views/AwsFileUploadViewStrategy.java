package views;

import static j2html.TagCreator.input;

import com.google.common.collect.ImmutableList;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import services.cloud.StorageUploadRequest;
import services.cloud.aws.SignedS3UploadRequest;

public final class AwsFileUploadViewStrategy extends FileUploadViewStrategy {

  @Override
  protected ImmutableList<Tag> fileUploadFields(Optional<StorageUploadRequest> request) {
    if (request.isEmpty()) {
      return ImmutableList.of();
    }
    SignedS3UploadRequest signedRequest = castStorageRequest(request.get());
    ImmutableList.Builder<Tag> builder = ImmutableList.builder();
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

    // It's critical that the "file" field be the last input
    // element for the form since S3 will ignore any fields
    // after that. See #2653 /
    // https://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-HTTPPOSTForms.html
    // for more context.
    builder.add(
        input().withType("file").withName("file").attr(Attr.ACCEPT, MIME_TYPES_IMAGES_AND_PDF));
    return builder.build();
  }

  @Override
  protected ContainerTag renderFileUploadFormElement(Params params, StorageUploadRequest request) {
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
