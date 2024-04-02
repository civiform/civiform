package support.cloud;

import java.util.Optional;
import play.mvc.Http;
import services.cloud.ApplicantStorageClient;
import services.cloud.StorageServiceName;
import services.cloud.StorageUploadRequest;

public class FakeApplicantStorageClient implements ApplicantStorageClient {
  @Override
  public int getFileLimitMb() {
    return 1;
  }

  @Override
  public String getPresignedUrlString(String fileKey) {
    return getPresignedUrlString(fileKey, Optional.empty());
  }

  @Override
  public String getPresignedUrlString(String fileKey, Optional<String> prefixedOriginalFileName) {
    return "presigned-url";
  }

  @Override
  public StorageUploadRequest getSignedUploadRequest(
      String fileKey, String successActionRedirectUrl, Http.Request request) {
    return new StorageUploadRequest() {
      @Override
      public String serviceName() {
        return "serviceName";
      }
    };
  }

  @Override
  public StorageServiceName getStorageServiceName() {
    return StorageServiceName.AWS_S3;
  }
}
