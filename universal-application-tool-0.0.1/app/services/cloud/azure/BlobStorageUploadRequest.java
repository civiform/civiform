package services.cloud.azure;

import com.google.auto.value.AutoValue;
import services.cloud.StorageServiceName;
import services.cloud.StorageUploadRequest;

/** This holds the information needed to upload a blob storage request. */
@AutoValue
public abstract class BlobStorageUploadRequest implements StorageUploadRequest {

  public abstract String accountName();

  public abstract String containerName();

  public abstract String fileName();

  public abstract String sasToken();

  public abstract String blobUrl();

  public abstract String successActionRedirect();

  @Override
  public abstract String serviceName();

  public static Builder builder() {
    return new AutoValue_BlobStorageUploadRequest.Builder()
        .setServiceName(StorageServiceName.AZURE_BLOB.getString());
  }

  @AutoValue.Builder
  public abstract static class Builder {

    /**
     * Get file name for the blob. This is used to build the canonicalized resource part of the
     * signature string.
     */
    abstract String fileName();

    /**
     * Get account name. This is used to build the canonicalized resource part of the signature
     * string.
     */
    public abstract Builder setFileName(String fileName);

    /**
     * Get account name. This is used to build the canonicalized resource part of the signature
     * string.
     */
    abstract String accountName();

    public abstract Builder setAccountName(String accountName);

    /**
     * Get container name. This is used to build the canonicalized resource part of the signature
     * string.
     */
    abstract String containerName();

    public abstract Builder setContainerName(String containerName);

    /** Get the URL for the resource. */
    abstract String blobUrl();

    public abstract Builder setBlobUrl(String blobUrl);

    /** Get the sas token for the resource. */
    abstract String sasToken();

    public abstract Builder setSasToken(String sasToken);

    /** Get the success redirect action link. */
    abstract String successActionRedirect();

    public abstract Builder setSuccessActionRedirect(String successActionRedirect);

    /** Get the service name (this is always set to "azure-blob" so the setter is private */
    abstract String serviceName();

    abstract Builder setServiceName(String serviceName);

    /** Build the request. This is called by the custom public build method. */
    abstract BlobStorageUploadRequest autoBuild();

    public BlobStorageUploadRequest build() {
      return autoBuild();
    }
  }
}
