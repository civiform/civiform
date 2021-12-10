package modules;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.AbstractModule;
import com.typesafe.config.Config;
import play.Environment;
import services.cloud.StorageClient;
import services.cloud.aws.SimpleStorage;
import services.cloud.azure.BlobStorage;

public class CloudStorageModule extends AbstractModule {


  private final Environment environment;
  private final Config config;

  private enum Storage {
    AWS_S3("aws-s3"),
    AZURE_BLOB("azure-blob"),
    ;
    private final String storageString;

    Storage(String storageString) {
      this.storageString = storageString;
    }

    String getString() {
      return storageString;
    }
  }

  public CloudStorageModule(Environment environment, Config config) {
    this.environment = environment;
    this.config = config;
  }

  @Override
  protected void configure() {
    // cloud.storage = "azure-blob"
    // cloud.storage = "aws-s3"
    final String storageProvider = checkNotNull(config).getString("cloud.storage");
    if (storageProvider == Storage.AWS_S3.getString()) {
      bind(StorageClient.class).to(SimpleStorage.class);
    } else if (storageProvider == Storage.AZURE_BLOB.getString()) {
      bind(StorageClient.class).to(BlobStorage.class);
    } else {
      // default to S3 for now
      bind(StorageClient.class).to(SimpleStorage.class);
    }
  }


}
