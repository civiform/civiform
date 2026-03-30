package parsers.cloud.aws;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.stream.connectors.s3.MultipartUploadResult;
import org.apache.pekko.stream.connectors.s3.javadsl.S3;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.util.ByteString;
import parsers.StreamingMultipartUploadResult;
import parsers.cloud.GenericMultipartUploadSinkProvider;
import services.cloud.StorageServiceName;

public final class AwsS3MultipartUploadSinkProvider
    extends GenericMultipartUploadSinkProvider<MultipartUploadResult> {
  public AwsS3MultipartUploadSinkProvider() {
    super();
    this.storageServiceName = StorageServiceName.AWS_S3;
  }

  // Get the base sink for AWS S3 multipart upload, which will be composed with additional stages
  // below. This helps with testability, since we can mock this method.
  @Override
  protected Sink<ByteString, CompletionStage<MultipartUploadResult>> getBaseSink(
      String bucketName, String fileKey) {
    return S3.multipartUpload(bucketName, fileKey);
  }

  // Get the composed sink for AWS S3 multipart upload, which maps the MultipartUploadResult to the
  // custom result class.
  @Override
  public Sink<ByteString, CompletionStage<StreamingMultipartUploadResult>> getUploadSink(
      String bucketName, String fileKey) {
    return getBaseSink(bucketName, fileKey)
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
}
