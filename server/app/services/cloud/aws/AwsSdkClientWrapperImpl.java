package services.cloud.aws;

import com.google.inject.Inject;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.net.URI;

/** A real implementation of {@link AwsSdkClientWrapper} that delegates to the real {@link S3Client}. */
public class AwsSdkClientWrapperImpl implements AwsSdkClientWrapper {
    @Inject
    public AwsSdkClientWrapperImpl() {}
    @Override
    public void deleteObject(Credentials credentials, Region region,
                             URI endpoint, DeleteObjectRequest request) {
        if (request.key().isBlank()) {
            // TODO & TODO bucket
        }
        try (S3Client s3Client =
                     S3Client.builder()
                             .credentialsProvider(credentials.credentialsProvider())
                             .region(region)
                             // Override the endpoint so that Localstack works correctly.
                             // See https://docs.localstack.cloud/user-guide/integrations/sdks/java/#s3-service and
                             // https://github.com/localstack/localstack/issues/5209#issuecomment-1004395805.
                             .endpointOverride(endpoint)
                             .build()) {
            s3Client.deleteObject(request);
        }
    }
}
