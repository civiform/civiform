package services.azure;


/**
 * This holds the information needed to upload a blob storage request.
 */

 @AutoValue
 public abstract class BlobStorageUploadRequest {

    public abstract String accountName();

    public abstract Credentials credentials();

    public abstract BlobClient blobClient();

    // UTC yyyymmmdd
    public abstract String dateTimeString();

    public abstract String blobEndpoint();

    public abstract String fileName();

    public abstract String sasKey();

    public abstract String sasQueryParamstring();

    public abstract String sas();
 }