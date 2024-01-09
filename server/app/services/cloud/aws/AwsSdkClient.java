package services.cloud.aws;

import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;

import java.net.URI;

/**
 * A thin wrapper around {@link software.amazon.awssdk.services.s3.S3Client} so that we can fake it out in tests.
 *
 * See https://aws.amazon.com/sdk-for-java/.
 */
public interface AwsSdkClient {
    public void deleteObject(Credentials credentials, Region region,
                                URI endpoint, String bucket, String fileKey, Logger logger)
        throws DeletionFailedException;
}
