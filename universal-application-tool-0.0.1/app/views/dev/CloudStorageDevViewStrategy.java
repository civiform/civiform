package views.dev;

import j2html.tags.ContainerTag;
import services.cloud.StorageUploadRequest;
import views.HtmlBundle;
import views.ViewUtils;

/**
 * Interface for interacting with different cloud storage providers for the dev file upload view.
 */
public interface CloudStorageDevViewStrategy {

  /** Method for getting the file upload form for different cloud providers. */
  ContainerTag getFileUploadForm(
      ViewUtils viewUtils, StorageUploadRequest storageUploadRequest, HtmlBundle bundle);
}
