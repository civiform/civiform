package views.dev;

import com.google.common.collect.ImmutableList;
import j2html.tags.ContainerTag;
import models.StoredFile;
import services.cloud.StorageUploadRequest;
import views.HtmlBundle;
import views.ViewUtils;

/**
 * Interface for interacting with different cloud storage providers for the dev file upload view.
 */
public interface CloudStorageDevViewStrategy {

  /** Method for getting the file upload form for different cloud providers. */
  ContainerTag getFileUploadForm(
      ViewUtils viewUtils, StorageUploadRequest storageUploadRequest, HtmlBundle bundle)
      throws RuntimeException;

  ContainerTag renderFiles(ImmutableList<StoredFile> files);

  String getPresignedURL(StoredFile file);
}
