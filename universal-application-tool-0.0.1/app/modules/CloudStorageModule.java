package modules;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.AbstractModule;
import com.typesafe.config.Config;
import play.Environment;
import services.cloud.StorageClient;
import services.cloud.StorageService;
import services.cloud.aws.SimpleStorage;
import services.cloud.azure.BlobStorage;

public class CloudStorageModule extends AbstractModule {


  private final Environment environment;
  private final Config config;

  public CloudStorageModule(Environment environment, Config config) {
    this.environment = environment;
    this.config = config;
  }

  @Override
  protected void configure() {
    // cloud.storage = "azure-blob"
    // cloud.storage = "s3"
    final String storageProvider = checkNotNull(config).getString("cloud.storage");
    if (storageProvider == StorageService.AWS_S3.getString()) {
      bind(StorageClient.class).to(SimpleStorage.class);
    } else if (storageProvider == StorageService.AZURE_BLOB.getString()) {
      bind(StorageClient.class).to(BlobStorage.class);
    } else {
      // default to S3 for now
      bind(StorageClient.class).to(SimpleStorage.class);
    }
  }


}
