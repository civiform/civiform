package services.cloud.azure;

import com.google.auto.value.AutoValue;
import services.cloud.StorageServiceName;
import services.cloud.StorageUploadRequest;

/** This holds the information needed to upload a file to blob storage. */
@AutoValue
public abstract class BlobStorageUploadRequest implements StorageUploadRequest {

  /**
   * Get account name. This is used to build the canonicalized resource part of the signature
   * string.
   */
  public abstract String accountName();

  /**
   * Get container name. This is used to build the canonicalized resource part of the signature
   * string.
   */
  public abstract String containerName();

  /**
   * Get file name for the blob. This is used to build the canonicalized resource part of the
   * signature string.
   */
  public abstract String fileName();

  /** Get the sas token for the resource. */
  public abstract String sasToken();

  /** Get the URL for the resource. */
  public abstract String blobUrl();

  /** Get the success redirect action link. */
  public abstract String successActionRedirect();

  @Override
  public abstract String serviceName();

  public static Builder builder() {
    return new AutoValue_BlobStorageUploadRequest.Builder()
        .setServiceName(StorageServiceName.AZURE_BLOB.getString());
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setFileName(String fileName);

    public abstract Builder setAccountName(String accountName);

    public abstract Builder setContainerName(String containerName);

    public abstract Builder setBlobUrl(String blobUrl);

    public abstract Builder setSasToken(String sasToken);

    public abstract Builder setSuccessActionRedirect(String successActionRedirect);

    abstract Builder setServiceName(String serviceName);

    abstract BlobStorageUploadRequest autoBuild();

    /** Build the request. This is called by the custom public build method. */
    public BlobStorageUploadRequest build() {
      return autoBuild();
    }
  }
}
