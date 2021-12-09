package services.cloud;

import java.net.URL;

public interface StorageClient {

  URL getPresignedUrl(String fileName);

  StorageUploadRequest getSignedUploadRequest(String fileName,
      String successRedirectActionLink);

}
