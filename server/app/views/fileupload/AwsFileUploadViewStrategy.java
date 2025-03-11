package views.fileupload;

import static j2html.TagCreator.input;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.ScriptTag;
import java.util.Optional;
import java.util.logging.Logger;

import services.cloud.StorageUploadRequest;
import services.cloud.aws.SignedS3UploadRequest;

public final class AwsFileUploadViewStrategy extends FileUploadViewStrategy {

  @Override
  public ImmutableList<InputTag> additionalFileUploadFormInputs(
      Optional<StorageUploadRequest> request) {
    if (request.isEmpty()) {
      return ImmutableList.of();
    }
    SignedS3UploadRequest signedRequest = castStorageRequest(request.get());
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
  public ImmutableMap<String, String> additionalFileUploadFormInputFields(
      Optional<StorageUploadRequest> request) {
    SignedS3UploadRequest signedRequest = castStorageRequest(request.get());
    ImmutableMap.Builder<String, String> mapBuilder =
        new ImmutableMap.Builder<String, String>()
            .put("key", signedRequest.key())
            .put("success_action_redirect", signedRequest.successActionRedirect())
            .put("X-Amz-Credential", signedRequest.credential())
            .put("X-Amz-Algorithm", signedRequest.algorithm())
            .put("X-Amz-Date", signedRequest.date())
            .put("X-Amz-Signature", signedRequest.signature())
            .put("Policy", signedRequest.policy());
    if (!signedRequest.securityToken().isEmpty()) {
      mapBuilder.put("X-Amz-Security-Token", signedRequest.securityToken());
    }
    return mapBuilder.build();
  }

  @Override
  public FormTag renderFileUploadFormElement(StorageUploadRequest request) {
    SignedS3UploadRequest signedRequest = castStorageRequest(request);
    Logger logger = LoggerFactory.getLogger(AwsFileUploadViewStrategy.class);
    logger.warning("XXX: renderFileUploadFormElement::actionLink: {}", signedRequest.actionLink());

    return super.renderFileUploadFormElement(request).withAction(signedRequest.actionLink());
  }

  @Override
  public String formAction(StorageUploadRequest request) {
    SignedS3UploadRequest signedRequest = castStorageRequest(request);
    Logger logger = LoggerFactory.getLogger(AwsFileUploadViewStrategy.class);
    logger.warning("XXX: formAction::actionLink: {}", signedRequest.actionLink());
    return signedRequest.actionLink();
  }

  private SignedS3UploadRequest castStorageRequest(StorageUploadRequest request) {
    if (!(request instanceof SignedS3UploadRequest)) {
      throw new RuntimeException(
          "Tried to upload a file to AWS S3 storage using incorrect request type");
    }
    return (SignedS3UploadRequest) request;
  }

  @Override
  public String getUploadFormClass() {
    return "aws-upload";
  }

  @Override
  public String getMultiFileUploadFormClass() {
    // The TS module for AWS file upload does not need to discern between single file and
    // multi file uploads, so the form class should be the same.
    return getUploadFormClass();
  }

  @Override
  public ImmutableList<ScriptTag> extraScriptTags() {
    return ImmutableList.of();
  }
}
