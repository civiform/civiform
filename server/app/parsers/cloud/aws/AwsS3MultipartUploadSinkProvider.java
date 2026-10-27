package parsers.cloud.aws;

import com.typesafe.config.Config;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.http.javadsl.model.ContentType;
import org.apache.pekko.http.javadsl.model.ContentTypes;
import org.apache.pekko.stream.connectors.s3.MultipartUploadResult;
import org.apache.pekko.stream.connectors.s3.S3Headers;
import org.apache.pekko.stream.connectors.s3.javadsl.S3;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.util.ByteString;
import parsers.FileTypeValidation;
import parsers.StreamingMultipartUploadResult;
import parsers.cloud.GenericMultipartUploadSinkProvider;
import services.cloud.BucketType;
import services.cloud.StorageServiceName;

public final class AwsS3MultipartUploadSinkProvider
    extends GenericMultipartUploadSinkProvider<MultipartUploadResult> {
  private final Config config;

  public AwsS3MultipartUploadSinkProvider(Config config) {
    super(StorageServiceName.AWS_S3);
    this.config = config;
  }

  // Get the base sink for AWS S3 multipart upload, which will be composed with additional stages
  // below. This helps with testability, since we can mock this method.
  @Override
  protected Sink<ByteString, CompletionStage<MultipartUploadResult>> getBaseSink(
      BucketType bucketType, String fileKey, int chunkSize) {
    return S3.multipartUpload(getBucketName(bucketType), fileKey);
  }

  @Override
  protected Sink<ByteString, CompletionStage<MultipartUploadResult>> getBaseSink(
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

    S3Headers s3Headers = S3Headers.create();
    if (contentDisposition.isPresent()) {
      s3Headers =
          s3Headers.withCustomHeaders(
              scala.collection.immutable.Map$.MODULE$.from(
                  scala.jdk.javaapi.CollectionConverters.asScala(
                      Map.of("Content-Disposition", contentDisposition.get()))));
    }

    return S3.multipartUpload(getBucketName(bucketType), fileKey, pekkoContentType, s3Headers);
  }

  // Get the composed sink for AWS S3 multipart upload, which maps the MultipartUploadResult to the
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
                        awsResult -> {
                          return StreamingMultipartUploadResult.builder()
                              .setStatus(StreamingMultipartUploadResult.Status.SUCCESS)
                              .setStorageServiceName(StorageServiceName.AWS_S3)
                              .setStoredFilePath(Optional.of(awsResult.getKey()))
                              .build();
                        })
                    .exceptionally(
                        throwable -> {
                          return failedResult(throwable);
                        }));
  }

  private String getBucketName(BucketType bucketType) {
    return switch (bucketType) {
      case PRIVATE_BUCKET -> config.getString("aws.s3.bucket");
      case PUBLIC_BUCKET -> config.getString("aws.s3.public_bucket");
    };
  }
}
