package services.azure;

import static com.google.common.base.Preconditions.checkNotNull;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.sas.SasProtocol;
import com.typesafe.config.Config;
import java.net.URL;
import java.time.Duration;
import java.time.OffsetDateTime;
import javax.inject.Singleton;
import play.Environment;

/**
 * SimpleStorage provides methods to create federated links for users of CiviForm to upload and
 * download files directly to and from AWS Simple Storage Service (S3).
 */
@Singleton
public class BlobStorage {

  public static final String AZURE_STORAGE_ACCT_CONF_PATH = "azure.blob.account";
  public static final String AZURE_CONTAINER_CONF_PATH = "azure.blob.container";
  public static final Duration AZURE_SAS_TOKEN_DURATION = Duration.ofMinutes(10);
  public static final Duration AZURE_USER_DELEGATION_KEY_DURATION = Duration.ofMinutes(60);

  private final Credentials credentials;
  private final String container;
  private final Client client;
  private final String accountName;
  private final String blobEndpoint;


  @Singleton
  public BlobStorage(
      Credentials credentials,
      Config config,
      Environment environment) {

    this.credentials = checkNotNull(credentials);
    this.container = checkNotNull(config).getString(AZURE_CONTAINER_CONF_PATH);
    this.accountName = checkNotNull(config).getString(AZURE_STORAGE_ACCT_CONF_PATH);
    this.blobEndpoint = String.format("https://%s.blob.core.windows.net", accountName);

    // TODO: Set to NullClient for test environment
    if (environment.isDev()) {
      client = new AzuriteClient(config);
    } else {
      client = new AzureBlobClient();
    }
  }

  public URL getPresignedUrl(String fileName) {
    String signedUrl = client.getPresignedUrl(fileName);

    try {
      return new URL(signedUrl);
    } catch (java.net.MalformedURLException e) {
      throw new RuntimeException(e);
    }

  }

  //TODO: Implement UploadRequest class

  interface Client {
    String getPresignedUrl(String fileName);

  }

  class AzureBlobClient implements Client {

    private final BlobServiceClient blobServiceClient;
    private UserDelegationKey userDelegationKey;

    AzureBlobClient() {
      blobServiceClient = new BlobServiceClientBuilder()
          .endpoint(blobEndpoint)
          .credential(credentials.getCredentials())
          .buildClient();
      userDelegationKey = getUserDelegationKey();
    }

    private UserDelegationKey getUserDelegationKey() {
      OffsetDateTime tokenExpiration = OffsetDateTime.now().plus(AZURE_SAS_TOKEN_DURATION);
      OffsetDateTime keyExpiration = userDelegationKey.getSignedExpiry();
      if (userDelegationKey == null || keyExpiration.isBefore(tokenExpiration)) {
        userDelegationKey = blobServiceClient.getUserDelegationKey(
            OffsetDateTime.now().minus(Duration.ofMinutes(5)),
            OffsetDateTime.now().plus(AZURE_USER_DELEGATION_KEY_DURATION));
      }
      return userDelegationKey;
    }

    @Override
    public String getPresignedUrl(String filePath) {
      BlobClient blobClient = blobServiceClient
          .getBlobContainerClient(container)
          .getBlobClient(filePath);

      BlobSasPermission blobSasPermission = new BlobSasPermission()
          .setReadPermission(true)
          .setWritePermission(true);

      BlobServiceSasSignatureValues signatureValues = new BlobServiceSasSignatureValues(
          OffsetDateTime.now().plus(AZURE_SAS_TOKEN_DURATION), blobSasPermission)
          .setProtocol(SasProtocol.HTTPS_ONLY);

      String sas = blobClient.generateUserDelegationSas(signatureValues, getUserDelegationKey());
      return String.format("%s?%s", blobClient.getBlobUrl(), sas);
    }

  }


  class AzuriteClient implements Client {
    // Using Account SAS because Azurite emulator does not support User Delegation SAS yet
    // See https://github.com/Azure/Azurite/issues/656

    private static final String AZURE_LOCAL_CONNECTION_STRING_CONF_PATH = "azure.local.connection";

    private final String connectionString;
    private final BlobServiceClient blobServiceClient;
    private final BlobContainerClient blobContainerClient;

    AzuriteClient(Config config) {
      this.connectionString = checkNotNull(config)
          .getString(AZURE_LOCAL_CONNECTION_STRING_CONF_PATH);
      this.blobServiceClient = new BlobServiceClientBuilder()
          .connectionString(connectionString)
          .buildClient();
      this.blobContainerClient = blobServiceClient.getBlobContainerClient(container);
      if (!blobContainerClient.exists()) {
        blobContainerClient.create();
      }

    }

    @Override
    public String getPresignedUrl(String filePath) {

      BlobClient blobClient = blobContainerClient.getBlobClient(filePath);

      BlobSasPermission blobSasPermission = new BlobSasPermission()
          .setReadPermission(true)
          .setWritePermission(true)
          .setListPermission(true);

      BlobServiceSasSignatureValues signatureValues = new BlobServiceSasSignatureValues(
          OffsetDateTime.now().plus(AZURE_SAS_TOKEN_DURATION), blobSasPermission)
          .setProtocol(SasProtocol.HTTPS_ONLY);

      String sas = blobClient.generateSas(signatureValues);
      return String.format("%s?%s", blobClient.getBlobUrl(), sas);
    }

  }

// TODO: Create a Null client for mocking
}
