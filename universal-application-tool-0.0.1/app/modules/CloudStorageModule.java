package modules;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.AbstractModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import play.Environment;
import services.cloud.StorageClient;
import services.cloud.StorageServiceName;

public class CloudStorageModule extends AbstractModule {

  private static final String AZURE_STORAGE_CLASS_NAME = "services.cloud.azure.BlobStorage";
  private static final String AWS_STORAGE_CLASS_NAME = "services.cloud.aws.SimpleStorage";

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
    String className = AWS_STORAGE_CLASS_NAME;
    try {
      final String storageProvider = checkNotNull(config).getString("cloud.storage");
      if (storageProvider.equals(StorageServiceName.AWS_S3.getString())) {
        className = AWS_STORAGE_CLASS_NAME;
      } else if (storageProvider.equals(StorageServiceName.AZURE_BLOB.getString())) {
        className = AZURE_STORAGE_CLASS_NAME;
      }
    } catch (ConfigException ex) {
      // Ignore missing config and default to S3 for now
    }
    try {
      Class<? extends StorageClient> boundClass =
          environment.classLoader().loadClass(className).asSubclass(StorageClient.class);
      bind(StorageClient.class).to(boundClass);
    } catch (ClassNotFoundException ex) {
      throw new RuntimeException(
          String.format("Failed to load storage client class: %s", className));
    }
  }
}
