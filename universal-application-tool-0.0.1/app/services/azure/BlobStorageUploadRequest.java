package services.azure;

import com.google.auto.value.AutoValue;

/**
 * This holds the information needed to upload a blob storage request.
 */

@AutoValue
public abstract class BlobStorageUploadRequest {

  public abstract String accountName();

  public abstract String containerName();

  public abstract String fileName();

  public abstract String sasUrl();

  public abstract String successRedirectAction();

  public static Builder builder() {
    return new AutoValue_BlobStorageUploadRequest.Builder();
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

    /**
     * Get the URL for the resource with the SAS token attached.
     */
    abstract String sasUrl();

    public abstract Builder setSasUrl(String sasUrl);

    /**
     * Get the success redirect action link.
     */
    abstract String successRedirectAction();

    public abstract Builder setSuccessRedirectAction(String successRedirectAction);

    /**
     * Build the request. This is called by the custom public build method.
     */
    abstract BlobStorageUploadRequest autoBuild();

    public BlobStorageUploadRequest build() {
      return autoBuild();
    }
  }
}