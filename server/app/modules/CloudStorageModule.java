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
import services.cloud.aws.AwsStorageUtils;
import services.cloud.azure.AzureApplicantStorage;
import services.cloud.azure.AzurePublicStorage;
import services.cloud.gcp.GcpApplicantStorage;
import services.cloud.gcp.GcpPublicStorage;
import services.cloud.gcp.GcpStorageUtils;
import services.cloud.generic_s3.AbstractS3StorageUtils;
import services.cloud.generic_s3.GenericS3Client;
import services.cloud.generic_s3.GenericS3ClientWrapper;
import views.BaseHtmlView;
import views.applicant.ApplicantProgramBlockEditView;
import views.applicant.ApplicantProgramBlockEditViewFactory;
import views.fileupload.AwsFileUploadViewStrategy;
import views.fileupload.AzureFileUploadViewStrategy;
import views.fileupload.FileUploadViewStrategy;
import views.fileupload.GcpFileUploadViewStrategy;

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
      case S3:
      case AWS_S3:
        bind(AbstractS3StorageUtils.class).to(AwsStorageUtils.class);
        bind(ApplicantStorageClient.class).to(AwsApplicantStorage.class);
        bind(PublicStorageClient.class).to(AwsPublicStorage.class);
        bind(FileUploadViewStrategy.class).to(AwsFileUploadViewStrategy.class);
        bind(GenericS3ClientWrapper.class).to(GenericS3Client.class);
        break;
      case GCP_S3:
        bind(AbstractS3StorageUtils.class).to(GcpStorageUtils.class);
        bind(ApplicantStorageClient.class).to(GcpApplicantStorage.class);
        bind(PublicStorageClient.class).to(GcpPublicStorage.class);
        bind(FileUploadViewStrategy.class).to(GcpFileUploadViewStrategy.class);
        bind(GenericS3ClientWrapper.class).to(GenericS3Client.class);
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
