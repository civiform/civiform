package services.cloud.azure;

import static com.google.common.base.Preconditions.checkNotNull;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobCorsRule;
import com.azure.storage.blob.models.BlobServiceProperties;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.sas.SasProtocol;
import com.typesafe.config.Config;
import java.net.URL;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import play.Environment;
import services.cloud.StorageClient;
import services.cloud.StorageServiceName;

/**
 * BlobStorage provides methods to create federated links for users of CiviForm to upload and
 * download files directly to and from Azure Blob Storage.
 */
@Singleton
public class BlobStorage implements StorageClient {

  public static final String AZURE_STORAGE_ACCT_CONF_PATH = "azure.blob.account";
  public static final String AZURE_CONTAINER_CONF_PATH = "azure.blob.container";
  public static final String AZURE_REGION_CONF_PATH = "java.time.zoneid";
  public static final Duration AZURE_SAS_TOKEN_DURATION = Duration.ofMinutes(10);

  // A User Delegation Key is used to sign SAS tokens without having to store the Account Keu
  // alongside the application.The key needs to be rotated and this duration is the interval
  // at which it's rotated. More info here:
  // https://docs.microsoft.com/en-us/rest/api/storageservices/create-user-delegation-sas
  public static final Duration AZURE_USER_DELEGATION_KEY_DURATION = Duration.ofMinutes(60);

  private final Credentials credentials;
  private final String container;
  private final Client client;
  private final String accountName;
  private final String blobEndpoint;
  private final ZoneId zoneId;

  @Inject
  public BlobStorage(Credentials credentials, Config config, Environment environment) {

    this.credentials = checkNotNull(credentials);
    this.container = checkNotNull(config).getString(AZURE_CONTAINER_CONF_PATH);
    this.accountName = checkNotNull(config).getString(AZURE_STORAGE_ACCT_CONF_PATH);
    this.zoneId = ZoneId.of(checkNotNull(config).getString(AZURE_REGION_CONF_PATH));
    this.blobEndpoint = String.format("https://%s.blob.core.windows.net", accountName);

    if (environment.isDev()) {
      client = new AzuriteClient(config);
    } else if (environment.isTest()) {
      client = new NullClient();
    } else {
      client = new AzureBlobClient();
    }
  }

  @Override
  public URL getPresignedUrl(String fileName) {
    String blobUrl = client.getBlobUrl(fileName);
    String sasToken = client.getSasToken(fileName);
    String signedUrl = String.format("%s?%s", blobUrl, sasToken);

    try {
      return new URL(signedUrl);
    } catch (java.net.MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  Client getClient() {
    return client;
  }

  @Override
  public BlobStorageUploadRequest getSignedUploadRequest(
      String fileName, String successActionRedirect) {
    // Azure blob must know the name of a file to generate a SAS for it, so we'll use a UUID
    // TODO: figure out how to map this to user-uploaded filename
    fileName = fileName.replace("${filename}", UUID.randomUUID().toString());

    BlobStorageUploadRequest.Builder builder =
        BlobStorageUploadRequest.builder()
            .setFileName(fileName)
            .setAccountName(accountName)
            .setContainerName(container)
            .setBlobUrl(client.getBlobUrl(fileName))
            .setSasToken(client.getSasToken(fileName))
            .setSuccessActionRedirect(successActionRedirect);
    return builder.build();
  }

  @Override
  public StorageServiceName getStorageServiceName() {
    return StorageServiceName.AZURE_BLOB;
  }

  interface Client {

    String getSasToken(String fileName);

    String getBlobUrl(String fileName);
  }

  /** Class to use for BlobStorage unit tests. */
  static class NullClient implements Client {

    NullClient() {}

    @Override
    public String getSasToken(String fileName) {
      return "sasToken";
    }

    @Override
    public String getBlobUrl(String fileName) {
      return "http://www.blobUrl.com";
    }
  }

  /** Class to use for prod file uploads to Azure blob storage. */
  class AzureBlobClient implements Client {

    private final BlobServiceClient blobServiceClient;
    private UserDelegationKey userDelegationKey;
    private ZoneId zoneId;

    AzureBlobClient() {
      blobServiceClient =
          new BlobServiceClientBuilder()
              .endpoint(blobEndpoint)
              .credential(credentials.getCredentials())
              .buildClient();
      userDelegationKey = getUserDelegationKey();
      zoneId = ZoneId.systemDefault();
    }

    private UserDelegationKey getUserDelegationKey() {
      OffsetDateTime tokenExpiration = OffsetDateTime.now(zoneId).plus(AZURE_SAS_TOKEN_DURATION);
      OffsetDateTime keyExpiration = userDelegationKey.getSignedExpiry();
      if (userDelegationKey == null || keyExpiration.isBefore(tokenExpiration)) {
        userDelegationKey =
            blobServiceClient.getUserDelegationKey(
                OffsetDateTime.now(zoneId).minus(Duration.ofMinutes(5)),
                OffsetDateTime.now(zoneId).plus(AZURE_USER_DELEGATION_KEY_DURATION));
      }
      return userDelegationKey;
    }

    @Override
    public String getSasToken(String fileName) {
      BlobClient blobClient =
          blobServiceClient.getBlobContainerClient(container).getBlobClient(fileName);

      BlobSasPermission blobSasPermission =
          new BlobSasPermission().setReadPermission(true).setWritePermission(true);

      BlobServiceSasSignatureValues signatureValues =
          new BlobServiceSasSignatureValues(
                  OffsetDateTime.now(zoneId).plus(AZURE_SAS_TOKEN_DURATION), blobSasPermission)
              .setProtocol(SasProtocol.HTTPS_ONLY);

      return blobClient.generateUserDelegationSas(signatureValues, getUserDelegationKey());
    }

    @Override
    public String getBlobUrl(String fileName) {
      BlobClient blobClient =
          blobServiceClient.getBlobContainerClient(container).getBlobClient(fileName);
      return blobClient.getBlobUrl();
    }
  }

  /** Class to use for BlobStorage dev environment. */
  class AzuriteClient implements Client {
    // Using Account SAS because Azurite emulator does not support User Delegation SAS yet
    // See https://github.com/Azure/Azurite/issues/656

    private static final String AZURE_LOCAL_CONNECTION_STRING_CONF_PATH = "azure.local.connection";

    private final String connectionString;
    private final BlobServiceClient blobServiceClient;
    private final BlobContainerClient blobContainerClient;

    AzuriteClient(Config config) {
      this.connectionString =
          checkNotNull(config).getString(AZURE_LOCAL_CONNECTION_STRING_CONF_PATH);
      this.blobServiceClient =
          new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
      String baseUrl = checkNotNull(config).getString("base_url");

      setCorsRules(blobServiceClient, baseUrl);
      this.blobContainerClient = blobServiceClient.getBlobContainerClient(container);
      if (!blobContainerClient.exists()) {
        blobContainerClient.create();
      }
    }

    private void setCorsRules(BlobServiceClient blobServiceClient, String baseUrl) {
      BlobServiceProperties properties =
          new BlobServiceProperties()
              .setCors(
                  List.of(
                      new BlobCorsRule()
                          .setAllowedOrigins(baseUrl)
                          // These headers are generated by the azure sdk
                          // and passed along, more info on the headers:
                          // https://docs.microsoft.com/en-us/rest/api/storageservices/put-blob
                          .setAllowedHeaders(
                              "content-type,x-ms-blob-type,x-ms-client-request-id,x-ms-version")
                          .setAllowedMethods("GET,PUT,OPTIONS")
                          .setMaxAgeInSeconds(500)));
      blobServiceClient.setProperties(properties);
    }

    @Override
    public String getSasToken(String fileName) {
      BlobClient blobClient = blobContainerClient.getBlobClient(fileName);

      BlobSasPermission blobSasPermission =
          new BlobSasPermission().setReadPermission(true).setWritePermission(true);

      BlobServiceSasSignatureValues signatureValues =
          new BlobServiceSasSignatureValues(
                  OffsetDateTime.now(zoneId).plus(AZURE_SAS_TOKEN_DURATION), blobSasPermission)
              .setProtocol(SasProtocol.HTTPS_HTTP); // TODO: Get this to work with HTTPS_ONLY

      return blobClient.generateSas(signatureValues);
    }

    @Override
    public String getBlobUrl(String fileName) {
      BlobClient blobClient = blobContainerClient.getBlobClient(fileName);
      String signedUrl = blobClient.getBlobUrl().replace("azurite", "localhost");
      return signedUrl;
    }
  }
}
