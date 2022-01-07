package views.dev;

import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.text;
import static j2html.attributes.Attr.ENCTYPE;

import j2html.TagCreator;
import j2html.tags.ContainerTag;
import services.cloud.StorageUploadRequest;
import services.cloud.aws.SignedS3UploadRequest;
import views.HtmlBundle;
import views.ViewUtils;

/** Strategy class for creating a file upload form for AWS. */
public class AwsStorageDevViewStrategy implements CloudStorageDevViewStrategy {

  @Override
  public ContainerTag getFileUploadForm(
      ViewUtils viewUtils, StorageUploadRequest storageUploadRequest, HtmlBundle bundle) {
    if (!(storageUploadRequest instanceof SignedS3UploadRequest)) {
      return null;
    }
    SignedS3UploadRequest request = (SignedS3UploadRequest) storageUploadRequest;

    ContainerTag formTag =
        form()
            .attr(ENCTYPE, "multipart/form-data")
            .with(input().withType("input").withName("key").withValue(request.key()))
            .with(
                input()
                    .withType("hidden")
                    .withName("success_action_redirect")
                    .withValue(request.successActionRedirect()))
            .with(
                input()
                    .withType("text")
                    .withName("X-Amz-Credential")
                    .withValue(request.credential()));
    if (!request.securityToken().isEmpty()) {
      formTag.with(
          input()
              .withType("hidden")
              .withName("X-Amz-Security-Token")
              .withValue(request.securityToken()));
    }

    return formTag
        .with(input().withType("text").withName("X-Amz-Algorithm").withValue(request.algorithm()))
        .with(input().withType("text").withName("X-Amz-Date").withValue(request.date()))
        .with(input().withType("hidden").withName("Policy").withValue(request.policy()))
        .with(input().withType("hidden").withName("X-Amz-Signature").withValue(request.signature()))
        .with(input().withType("file").withName("file"))
        .with(TagCreator.button(text("Upload to Amazon S3")).withType("submit"))
        .withMethod("post")
        .withAction(request.actionLink());
  }
}
