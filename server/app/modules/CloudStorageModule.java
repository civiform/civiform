package modules;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import play.Environment;
import services.cloud.StorageClient;
import services.cloud.StorageServiceName;
import views.AwsFileUploadViewStrategy;
import views.AzureFileUploadViewStrategy;
import views.BaseHtmlView;
import views.FileUploadViewStrategy;
import views.applicant.ApplicantProgramBlockEditView;
import views.applicant.ApplicantProgramBlockEditViewFactory;

/** Configures and initializes the classes for interacting with file storage backends. */
public class CloudStorageModule extends AbstractModule {

  private static final String AZURE_STORAGE_CLASS_NAME = "services.cloud.azure.BlobStorage";
  private static final String AWS_STORAGE_CLASS_NAME = "services.cloud.aws.SimpleStorage";

  private final Environment environment;
  private final Config config;

  public CloudStorageModule(Environment environment, Config config) {
    this.environment = checkNotNull(environment);
    this.config = checkNotNull(config);
  }

  @Override
  protected void configure() {
    String className = AWS_STORAGE_CLASS_NAME;

    try {
      String storageProvider = checkNotNull(config).getString("cloud.storage");
      className = getStorageProviderClassName(storageProvider);
      bindCloudStorageStrategy(storageProvider);
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

    install(
        new FactoryModuleBuilder()
            .implement(BaseHtmlView.class, ApplicantProgramBlockEditView.class)
            .build(ApplicantProgramBlockEditViewFactory.class));
  }

  private void bindCloudStorageStrategy(String storageProvider) {
    StorageServiceName storageServiceName = StorageServiceName.forString(storageProvider).get();

    switch (storageServiceName) {
      case AZURE_BLOB:
        bind(FileUploadViewStrategy.class).to(AzureFileUploadViewStrategy.class);
        return;
      case AWS_S3:
      default:
        bind(FileUploadViewStrategy.class).to(AwsFileUploadViewStrategy.class);
    }
  }

  private String getStorageProviderClassName(String storageProvider) {
    StorageServiceName storageServiceName = StorageServiceName.forString(storageProvider).get();

    switch (storageServiceName) {
      case AZURE_BLOB:
        return AZURE_STORAGE_CLASS_NAME;
      case AWS_S3:
      default:
        return AWS_STORAGE_CLASS_NAME;
    }
  }
}
