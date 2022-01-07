package views.dev;

import j2html.tags.ContainerTag;
import services.cloud.StorageUploadRequest;
import services.cloud.aws.SignedS3UploadRequest;
import services.cloud.azure.BlobStorageUploadRequest;
import views.HtmlBundle;
import views.ViewUtils;

/**
 * Interface for interacting with different cloud storage providers for the dev file upload view. We
 * currently support Azure and AWS.
 */
public interface CloudStorageDevViewStrategy {

  /**
   * Method for getting the file upload form for different cloud providers. This currently supports
   * AWS, which takes in a {@link SignedS3UploadRequest} and Azure, which takes in a {@link
   * BlobStorageUploadRequest}.
   */
  ContainerTag getFileUploadForm(
      ViewUtils viewUtils, StorageUploadRequest storageUploadRequest, HtmlBundle bundle);
}
