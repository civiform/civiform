package modules;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import play.Environment;
import services.cloud.ApplicantStorageClient;
import services.cloud.PublicStorageClient;
import services.cloud.StorageServiceName;
import services.cloud.aws.AwsApplicantStorage;
import services.cloud.aws.AwsPublicStorage;
import services.cloud.aws.AwsS3Client;
import services.cloud.aws.AwsS3ClientWrapper;
import services.cloud.azure.AzureApplicantStorage;
import services.cloud.azure.AzurePublicStorage;
import views.BaseHtmlView;
import views.applicant.ApplicantProgramBlockEditView;
import views.applicant.ApplicantProgramBlockEditViewFactory;
import views.fileupload.AwsFileUploadViewStrategy;
import views.fileupload.AzureFileUploadViewStrategy;
import views.fileupload.FileUploadViewStrategy;

/** Configures and initializes the classes for interacting with file storage backends. */
public class CloudStorageModule extends AbstractModule {
  private final Config config;

  // Environment must always be provided as a param, even if it's unused.
  public CloudStorageModule(Environment unused, Config config) {
    this.config = checkNotNull(config);
  }

  @Override
  protected void configure() {
    StorageServiceName storageServiceName;
    try {
      String storageProvider = checkNotNull(config).getString("cloud.storage");
      storageServiceName =
          StorageServiceName.forString(storageProvider).orElse(StorageServiceName.AWS_S3);
    } catch (ConfigException ex) {
      // Default to S3 if nothing is configured
      storageServiceName = StorageServiceName.AWS_S3;
    }

    switch (storageServiceName) {
      case AWS_S3:
        bind(ApplicantStorageClient.class).to(AwsApplicantStorage.class);
        bind(PublicStorageClient.class).to(AwsPublicStorage.class);
        bind(FileUploadViewStrategy.class).to(AwsFileUploadViewStrategy.class);
        bind(AwsS3ClientWrapper.class).to(AwsS3Client.class);
        break;
      case AZURE_BLOB:
        bind(ApplicantStorageClient.class).to(AzureApplicantStorage.class);
        bind(PublicStorageClient.class).to(AzurePublicStorage.class);
        bind(FileUploadViewStrategy.class).to(AzureFileUploadViewStrategy.class);
        break;
    }

    install(
        new FactoryModuleBuilder()
            .implement(BaseHtmlView.class, ApplicantProgramBlockEditView.class)
            .build(ApplicantProgramBlockEditViewFactory.class));
  }
}
