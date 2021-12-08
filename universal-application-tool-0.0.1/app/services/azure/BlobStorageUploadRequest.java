package services.azure;


import com.azure.storage.blob.models.UserDelegationKey;
import com.google.auto.value.AutoValue;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * This holds the information needed to upload a blob storage request.
 */

@AutoValue
public abstract class BlobStorageUploadRequest {


  private static final String DEFAULT_SIGNED_PERMISSIONS = "rw";
  private static final String DEFAULT_SIGNED_PROTOCOL = "https";
  private static final String DEFAULT_SIGNED_RESOURCE = "b";
  private static final String DEFAULT_SIGNED_VERSION = "2020-12-06";


  private static byte[] HmacSHA256(String data, byte[] key) {
    try {
      String algorithm = "HmacSHA256";
      Mac mac = Mac.getInstance(algorithm);
      mac.init(new SecretKeySpec(key, algorithm));
      return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new RuntimeException(e);
    }
  }

  // -- Below should be included in the upload form.

  public abstract String signature();

  public abstract String signedStart();

  public abstract String signedExpiry();

  public abstract String signedKeyStart();

  public abstract String signedKeyExpiry();

  public abstract String signedKeyObjectId();

  public abstract String signedKeyTenantId();

  public abstract String signedKeyVersion();

  public abstract String signedKeyService();

  public abstract String signedProtocol();

  public abstract String signedPermissions();

  public abstract String signedResource();

  public abstract String signedVersion();

  // -- Below are required for creating the signature.

  abstract String accountName();

  abstract String containerName();

  abstract String fileName();

  abstract String signedKeyValue();


  public static Builder builder() {
    return new AutoValue_BlobStorageUploadRequest.Builder()
        .setSignedProtocol(DEFAULT_SIGNED_PROTOCOL)
        .setSignedPermissions(DEFAULT_SIGNED_PERMISSIONS)
        .setSignedVersion(DEFAULT_SIGNED_VERSION)
        .setSignedResource(DEFAULT_SIGNED_RESOURCE);
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

    public abstract Builder setContainerName(String accountName);

    /**
     * Set the start time for the SAS key. Value should be an ISO-formatted offset date time
     * string.
     */
    public abstract String signedStart();

    public abstract Builder setSignedStart(String signedStart);

    /**
     * Set the expiry time for the SAS key. Value should be an ISO-formatted offset date time
     * string.
     */
    public abstract String signedExpiry();

    public abstract Builder setSignedExpiry(String signedExpiry);

    /**
     * Set signedProtocol. signedProtocol always uses default so this is not public.
     */
    abstract Builder setSignedProtocol(String signedProtocol);

    abstract String signedProtocol();

    /**
     * Set signedPermissions. signedPermissions always uses default so this is not public.
     */
    abstract Builder setSignedPermissions(String signedPermissions);

    abstract String signedPermissions();

    /**
     * Set signedResource. signedResource always uses default so this is not public.
     */
    abstract Builder setSignedResource(String signedResource);

    abstract String signedResource();

    /**
     * Set signedVersion. signedVersion always uses default so this is not public.
     */
    abstract Builder setSignedVersion(String signedVersion);

    abstract String signedVersion();


    /**
     * Set signature. Signature is created when the request is signed so this is not public.
     */
    abstract Builder setSignature(String signature);

    /**
     * The following values are set when setUserDelegationKey is called, so they are not public.
     */
    abstract Builder setSignedKeyStart(String signedKeyStart);

    abstract String signedKeyStart();

    abstract Builder setSignedKeyExpiry(String signedKeyExpiry);

    abstract String signedKeyExpiry();

    abstract Builder setSignedKeyObjectId(String signedKeyObjectId);

    abstract String signedKeyObjectId();

    abstract Builder setSignedKeyTenantId(String signedKeyTenantId);

    abstract String signedKeyTenantId();

    abstract Builder setSignedKeyVersion(String signedKeyVersion);

    abstract String signedKeyVersion();

    abstract Builder setSignedKeyService(String signedKeyService);

    abstract String signedKeyService();

    abstract Builder setSignedKeyValue(String signedKeyValue);

    abstract String signedKeyValue();


    /**
     * SetUserDelegationKey extracts, formats,and sets the signedObjectId, signedTenantId,
     * signedService, signedVersion, signedKeyStart, and signedKeyExpiry from the provided
     * UserDelegationKey
     */
    public Builder setUserDelegationKey(UserDelegationKey userDelegationKey) {
      return this.setSignedKeyService(userDelegationKey.getSignedService())
          .setSignedKeyObjectId(userDelegationKey.getSignedObjectId())
          .setSignedKeyTenantId(userDelegationKey.getSignedTenantId())
          .setSignedKeyVersion(userDelegationKey.getSignedVersion())
          .setSignedKeyStart(userDelegationKey.getSignedStart().format(
              DateTimeFormatter.ISO_OFFSET_DATE_TIME))
          .setSignedKeyExpiry(userDelegationKey.getSignedExpiry().format(
              DateTimeFormatter.ISO_OFFSET_DATE_TIME))
          .setSignedKeyValue(userDelegationKey.getValue());
    }

    /**
     * Sign the request.
     */
    Builder sign() {
      String canonicalizedResource = String
          .format("blob/%s/%s/%s", accountName(), containerName(), fileName());
      String stringToSign = signedPermissions() + "\n" +
          signedStart() + "\n" +
          signedExpiry() + "\n" +
          canonicalizedResource + "\n" +
          signedKeyObjectId() + "\n" +
          signedKeyTenantId() + "\n" +
          signedKeyStart() + "\n" +
          signedKeyExpiry() + "\n" +
          signedKeyService() + "\n" +
          signedKeyVersion() + "\n" +
          signedProtocol() + "\n" +
          signedVersion() + "\n" +
          signedResource() + "\n";
      byte[] signature = HmacSHA256(stringToSign, Base64.getDecoder().decode(signedKeyValue()));
      return setSignature(Base64.getEncoder().encodeToString(signature));
    }

    /**
     * Build the request. If a required field is not set, IllegalStateException is thrown.
     */
    public BlobStorageUploadRequest build() {
      return sign().autoBuild();
    }

    /**
     * Build the request. This is called by the custom public build method.
     */
    abstract BlobStorageUploadRequest autoBuild();

  }
}