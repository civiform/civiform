package views.dev;

import static j2html.TagCreator.a;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.text;
import static j2html.TagCreator.tr;
import static j2html.attributes.Attr.ENCTYPE;

import com.google.common.collect.ImmutableList;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import java.util.Optional;
import javax.inject.Inject;
import models.StoredFile;
import services.cloud.StorageClient;
import services.cloud.StorageUploadRequest;
import services.cloud.aws.SignedS3UploadRequest;
import views.HtmlBundle;
import views.ViewUtils;

/** Strategy class for creating a file upload form for AWS. */
public class AwsStorageDevViewStrategy implements CloudStorageDevViewStrategy {

  private final StorageClient client;

  @Inject
  public AwsStorageDevViewStrategy(StorageClient client) {
    this.client = client;
  }

  @Override
  public ContainerTag getFileUploadForm(
      ViewUtils viewUtils, StorageUploadRequest storageUploadRequest, HtmlBundle bundle)
      throws RuntimeException {
    if (!(storageUploadRequest instanceof SignedS3UploadRequest)) {
      throw new RuntimeException(
          "Trying to upload a file to localhost (AWS emulator) dev file storage using incorrect"
              + " upload request type.");
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

  @Override
  public ContainerTag renderFiles(ImmutableList<StoredFile> files) {
    return table()
        .with(
            tbody(
                each(
                    files,
                    file ->
                        tr(
                            td(String.valueOf(file.id)),
                            td(a(file.getName()).withHref(getPresignedURL(file)))))));
  }

  @Override
  public String getPresignedURL(StoredFile file) {
    return client.getPresignedUrl(file.getName(), Optional.empty()).toString();
  }
}
