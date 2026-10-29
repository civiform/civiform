package parsers.cloud.gcp;

import com.typesafe.config.Config;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.http.javadsl.model.ContentType;
import org.apache.pekko.http.javadsl.model.ContentTypes;
import org.apache.pekko.stream.connectors.googlecloud.storage.StorageObject;
import org.apache.pekko.stream.connectors.googlecloud.storage.javadsl.GCStorage;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.util.ByteString;
import parsers.FileTypeValidation;
import parsers.StreamingMultipartUploadResult;
import parsers.cloud.GenericMultipartUploadSinkProvider;
import services.cloud.BucketType;
import services.cloud.StorageServiceName;

public class GcpMultipartUploadSinkProvider
    extends GenericMultipartUploadSinkProvider<StorageObject> {
  private final Config config;

  public GcpMultipartUploadSinkProvider(Config config) {
    super(StorageServiceName.GCP_S3);
    this.config = config;
  }

  // Get the base sink for GCP resumable upload, which will be composed with additional stages
  // below. This helps with testability, since we can mock this method.
  @Override
  protected Sink<ByteString, CompletionStage<StorageObject>> getBaseSink(
      BucketType bucketType, String fileKey, int chunkSize) {
    return GCStorage.resumableUpload(
        getBucketName(bucketType), fileKey, ContentTypes.TEXT_PLAIN_UTF8, chunkSize);
  }

  @Override
  protected Sink<ByteString, CompletionStage<StorageObject>> getBaseSink(
      BucketType bucketType,
      String fileKey,
      Optional<String> contentType,
      Optional<String> contentDisposition,
      int chunkSize) {
    if (contentType.isEmpty() && contentDisposition.isEmpty()) {
      return getBaseSink(bucketType, fileKey, chunkSize);
    }

    ContentType pekkoContentType =
        contentType
            .or(() -> FileTypeValidation.determineMimeType(fileKey))
            .map(ContentTypes::parse)
            .orElse(ContentTypes.APPLICATION_OCTET_STREAM);

    return GCStorage.resumableUpload(
        getBucketName(bucketType), fileKey, pekkoContentType, chunkSize);
  }

  // Get the composed sink for the GCP resumable upload, which maps the StoredObject to the
  // custom result class.
  @Override
  public Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> getUploadSink(
      BucketType bucketType,
      String fileKey,
      Optional<String> contentType,
      Optional<String> contentDisposition,
      int chunkSize) {
    return getBaseSink(bucketType, fileKey, contentType, contentDisposition, chunkSize)
        .mapMaterializedValue(
            completionStage ->
                completionStage
                    .thenApply(
                        gcpResult -> {
                          return StreamingMultipartUploadResult.builder()
                              .setStatus(StreamingMultipartUploadResult.Status.SUCCESS)
                              .setStorageServiceName(storageServiceName)
                              .setStoredFilePath(Optional.of(gcpResult.name()))
                              .build();
                        })
                    .exceptionally(
                        throwable -> {
                          return failedResult(throwable);
                        }));
  }

  private String getBucketName(BucketType bucketType) {
    return switch (bucketType) {
      case PRIVATE_BUCKET -> config.getString("gcp.s3.bucket");
      case PUBLIC_BUCKET -> config.getString("gcp.s3.public_bucket");
    };
  }
}
