package views.fileupload;

import views.applicant.ApplicantFileUploadRenderer;

/**
 * Class to render a <form> that supports file upload. Must be subclassed by each cloud storage
 * provider that CiviForm supports.
 *
 * <p>This class supports rendering file upload forms for both applicants *and* admins. See {@link
 * ApplicantFileUploadRenderer} for additional rendering for *applicant* file upload.
 *
 * <p>This class is specific to GCP, but for the time being it does not need to deviate from the AWS
 * version.
 */
public final class GcpFileUploadViewStrategy extends AwsFileUploadViewStrategy {
  // No-op
}
