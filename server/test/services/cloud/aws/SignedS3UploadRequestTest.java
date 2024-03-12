package services.cloud.aws;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SignedS3UploadRequestTest {
  @Test
  public void uploadPolicyBuilder_key_policyUsesStartWithAndDollarSign() {
    SignedS3UploadRequest.UploadPolicy.Builder uploadPolicyBuilder =
        SignedS3UploadRequest.UploadPolicy.builder()
            .setKeyPrefix("testKeyHere")
            // Irrelevant inputs
            .setExpiration("expiration")
            .setBucket("bucket")
            .setContentLengthRange(1, 100)
            .setSuccessActionRedirect("redirect", /* useSuccessActionRedirectAsPrefix= */ false)
            .setCredential("credential")
            .setAlgorithm("algorithm")
            .setDate("date")
            .setSecurityToken("securityToken");

    String policyString = uploadPolicyBuilder.build().getAsString();

    assertThat(policyString).contains("[\"starts-with\",\"$key\",\"testKeyHere\"]");
  }

  @Test
  public void uploadPolicyBuilder_useSuccessActionRedirectAsPrefixFalse_policyUsesExactMatch() {
    SignedS3UploadRequest.UploadPolicy.Builder uploadPolicyBuilder =
        SignedS3UploadRequest.UploadPolicy.builder()
            .setSuccessActionRedirect(
                "https://civiform.dev/programs/4/blocks/1/updateFile/true",
                /* useSuccessActionRedirectAsPrefix= */ false)
            // Irrelevant inputs
            .setExpiration("expiration")
            .setBucket("bucket")
            .setKeyPrefix("key")
            .setContentLengthRange(1, 100)
            .setCredential("credential")
            .setAlgorithm("algorithm")
            .setDate("date")
            .setSecurityToken("securityToken");

    String policyString = uploadPolicyBuilder.build().getAsString();

    assertThat(policyString)
        .contains(
            "{\"success_action_redirect\":\"https://civiform.dev/programs/4/blocks/1/updateFile/true\"}");
    assertThat(policyString).doesNotContain("[\"starts-with\",\"$success_action_redirect\"");
  }

  @Test
  public void uploadPolicyBuilder_useSuccessActionRedirectAsPrefixTrue_policyUsesStartWith() {
    SignedS3UploadRequest.UploadPolicy.Builder uploadPolicyBuilder =
        SignedS3UploadRequest.UploadPolicy.builder()
            .setSuccessActionRedirect(
                "https://civiform.dev/programs/4/blocks/1/updateFile/true",
                /* useSuccessActionRedirectAsPrefix= */ true)
            // Irrelevant inputs
            .setExpiration("expiration")
            .setBucket("bucket")
            .setKeyPrefix("key")
            .setContentLengthRange(1, 100)
            .setCredential("credential")
            .setAlgorithm("algorithm")
            .setDate("date")
            .setSecurityToken("securityToken");

    String policyString = uploadPolicyBuilder.build().getAsString();

    assertThat(policyString)
        .contains(
            "[\"starts-with\",\"$success_action_redirect\",\"https://civiform.dev/programs/4/blocks/1/updateFile/true\"]");
    assertThat(policyString).doesNotContain("{\"success_action_redirect\":");
  }
}
