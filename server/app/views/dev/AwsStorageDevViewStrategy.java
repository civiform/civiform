package views.dev;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
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
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.TableTag;
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
    this.client = checkNotNull(client);
  }

  @Override
  public DivTag getFileUploadForm(
      ViewUtils viewUtils, StorageUploadRequest storageUploadRequest, HtmlBundle bundle)
      throws RuntimeException {
    if (!(storageUploadRequest instanceof SignedS3UploadRequest)) {
      throw new RuntimeException(
          "Trying to upload a file to localhost (AWS emulator) dev file storage using incorrect"
              + " upload request type.");
    }
    SignedS3UploadRequest request = (SignedS3UploadRequest) storageUploadRequest;

    FormTag formTag =
        form()
            .attr(ENCTYPE, "multipart/form-data")
            .with(input().attr("type", "input").attr("name", "key").attr("value", request.key()))
            .with(
                input()
                    .attr("type", "hidden")
                    .attr("name", "success_action_redirect")
                    .attr("value", request.successActionRedirect()))
            .with(
                input()
                    .attr("type", "text")
                    .attr("name", "X-Amz-Credential")
                    .attr("value", request.credential()));
    if (!request.securityToken().isEmpty()) {
      formTag.with(
          input()
              .attr("type", "hidden")
              .attr("name", "X-Amz-Security-Token")
              .attr("value", request.securityToken()));
    }

    formTag
        .with(
            input()
                .attr("type", "text")
                .attr("name", "X-Amz-Algorithm")
                .attr("value", request.algorithm()))
        .with(input().attr("type", "text").attr("name", "X-Amz-Date").attr("value", request.date()))
        .with(input().attr("type", "hidden").attr("name", "Policy").attr("value", request.policy()))
        .with(
            input()
                .attr("type", "hidden")
                .attr("name", "X-Amz-Signature")
                .attr("value", request.signature()))
        .with(input().attr("type", "file").attr("name", "file"))
        .with(TagCreator.button(text("Upload to Amazon S3")).attr("type", "submit"))
        .withMethod("post")
        .attr("action", request.actionLink());

    return div(formTag).withId("aws-upload-form-component");
  }

  @Override
  public TableTag renderFiles(ImmutableList<StoredFile> files) {
    return table()
        .with(
            tbody(
                each(
                    files,
                    file ->
                        tr(
                            td(String.valueOf(file.id)),
                            td(a(file.getName()).withHref(getPresignedUrl(file)))))));
  }

  @Override
  public String getPresignedUrl(StoredFile file) {
    return client.getPresignedUrlString(file.getName());
  }
}
