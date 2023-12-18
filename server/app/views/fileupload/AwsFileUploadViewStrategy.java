package views.fileupload;

import static j2html.TagCreator.div;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import j2html.tags.Tag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.ScriptTag;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import services.cloud.StorageUploadRequest;
import services.cloud.aws.SignedS3UploadRequest;

public final class AwsFileUploadViewStrategy extends FileUploadViewStrategy {

  @Override
  public ImmutableList<Tag<?>> fileUploadFormInputs(
      Optional<StorageUploadRequest> request,
      String acceptedMimeTypes,
      String fileInputId,
      ImmutableList<String> ariaDescribedByIds,
      boolean hasErrors) {
    if (request.isEmpty()) {
      return ImmutableList.of();
    }
    SignedS3UploadRequest signedRequest = castStorageRequest(request.get());
    ImmutableList.Builder<Tag<?>> builder = ImmutableList.builder();
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
            div().withClasses("usa-form-group", "mb-2")
                    //.with(label().withFor(fileInputId).withClass("usa-label"))
                    .with(span("File size must be at most 500 KB.").withId("file-input-size-hint").withClass("usa-hint"))
                            .with(
                                    input()
                                            .withId(fileInputId)
                                            .condAttr(hasErrors, "aria-invalid", "true")
                                            .attr("aria-describedby", "file-input-size-hint")
                                          //  .condAttr(
                                           //         !ariaDescribedByIds.isEmpty(),
                                          //          "aria-describedby",
                                          //          StringUtils.join(ariaDescribedByIds, " "))
                                            .withType("file")
                                            .withName("file")
                                            .withClasses("usa-file-input", "w-full")
                                            .withAccept(acceptedMimeTypes))
                            )
       ;
    return builder.build();
  }

  @Override
  public FormTag renderFileUploadFormElement(StorageUploadRequest request) {
    SignedS3UploadRequest signedRequest = castStorageRequest(request);
    return super.renderFileUploadFormElement(request).withAction(signedRequest.actionLink());
  }

  private SignedS3UploadRequest castStorageRequest(StorageUploadRequest request) {
    if (!(request instanceof SignedS3UploadRequest)) {
      throw new RuntimeException(
          "Tried to upload a file to AWS S3 storage using incorrect request type");
    }
    return (SignedS3UploadRequest) request;
  }

  @Override
  protected String getUploadFormClass() {
    return "aws-upload";
  }

  @Override
  public ImmutableList<ScriptTag> extraScriptTags() {
    return ImmutableList.of();
  }
}
