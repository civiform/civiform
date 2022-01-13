package services.cloud;

/**
 * Interface for classes that store the information necessary to complete an upload request
 * client-side.
 */
public interface StorageUploadRequest {

  /**
   * Getter which returns the name of the service being used, i.e "azure-blob" or "s3"
   */
  String serviceName();
}
