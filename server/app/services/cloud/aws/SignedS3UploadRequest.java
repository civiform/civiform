package services.cloud.aws;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import services.cloud.StorageServiceName;
import services.cloud.StorageUploadRequest;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * SignedS3UploadRequest holds all necessary information to be included in a HTML form for users to
 * upload a file directly from their browsers to CiviForm S3 bucket.
 *
 * <p>SignedS3UploadRequest provides a builder to construct an object from necessary information.
 * That said, users of this class most likely want to obtain an constructed object from {@link
 * AwsApplicantStorage#getSignedUploadRequest}.
 */
@AutoValue
public abstract class SignedS3UploadRequest implements StorageUploadRequest {

  private static final long MB_TO_BYTES = 1L << 20;

  /** The name to use for the success action redirect in the policy JSON. */
  private static final String SUCCESS_ACTION_REDIRECT_NAME = "success_action_redirect";

  private static byte[] HmacSHA256(String data, byte[] key) {
    try {
      String algorithm = "HmacSHA256";
      Mac mac = Mac.getInstance(algorithm);
      mac.init(new SecretKeySpec(key, algorithm));
      return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new RuntimeException(e);
    }
  }

  private static byte[] getSigningKey(
      String secretKey, String dateStamp, String regionName, String serviceName) {
    byte[] kSecret = ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8);
    byte[] kDate = HmacSHA256(dateStamp, kSecret);
    byte[] kRegion = HmacSHA256(regionName, kDate);
    byte[] kService = HmacSHA256(serviceName, kRegion);
    byte[] kSigning = HmacSHA256("aws4_request", kService);
    return kSigning;
  }

  public static Builder builder() {
    return new AutoValue_SignedS3UploadRequest.Builder()
        .setAlgorithm("AWS4-HMAC-SHA256")
        .setServiceName(StorageServiceName.AWS_S3.getString());
  }

  // -- Below should be included in the upload form.

  /** Action link for the form. */
  public abstract String actionLink();

  /** Key of the object in S3 bucket. */
  public abstract String key();

  /** Redirect URL when the upload is successful. */
  public abstract String successActionRedirect();

  /**
   * True if {@link #successActionRedirect()} should be considered just the prefix to the redirect
   * URL, and false if {@link #successActionRedirect()} should be considered an exact match to the
   * redirect URL. This value should be false for most circumstances, and should only be set to true
   * if the redirect URL can vary depending on user interaction.
   *
   * <p>Context: {@link #successActionRedirect()} is embedded in the file upload question `<form>`
   * in two places:
   *
   * <p>1) In the `<input name="success_action_redirect">` element. (See {@link
   * views.fileupload.GenericS3FileUploadViewStrategy#additionalFileUploadFormInputs}.)
   *
   * <p>2) In the `<input name="Policy">` element as part of the encoded policy string. (See {@link
   * Builder#buildPolicy()}.)
   *
   * <p>AWS will verify that the redirect URL in the `<input name="success_action_redirect">`
   * element matches the redirect URL in the `<input name="Policy">` element. If they don't match,
   * AWS will throw a policy error and file upload will fail (see
   * https://github.com/civiform/civiform/issues/6737).
   *
   * <p>Previously, we only saved the user's file upload if they clicked the "Save&next" button. In
   * that case, we always used the same redirect URL, so we could have these two `<input>`s use the
   * same URL with no issues. Now, we want to save the user's file upload when they click any button
   * -- "Save&next", "Review", or "Previous" (see https://github.com/civiform/civiform/issues/6450).
   * This means that the redirect URL needs to be different depending on which button the applicant
   * clicked. So, we can't have the `<input name="success_action_redirect">` element exactly match
   * the redirect URL in the `<input name="Policy">` element. Instead, the Policy should specify
   * that the `<input name="success_action_redirect">` element URL should match a specific
   * **prefix**. Note that this prefix should be as specific as possible to give us the best
   * security verification from AWS.
   *
   * <p>For example, if the Policy specifies that the "success_action_redirect" must start with
   * "https://civiform.dev/programs/4/blocks/1/updateFile/true", then the `<input
   * name="success_action_redirect">` element could use a URL of
   * "https://civiform.dev/programs/4/blocks/1/updateFile/true/REVIEW_PAGE" to redirect the user to
   * the review page and AWS would allow it.
   *
   * <p>See also: https://github.com/civiform/civiform/pull/6744.
   */
  public abstract boolean useSuccessActionRedirectAsPrefix();

  /** Credential of the signed request. */
  public abstract String credential();

  /** AWS session token. This is only needed in a temporary session, usually when run locally. */
  public abstract String securityToken();

  /** Algorithm used to sign the request. */
  public abstract String algorithm();

  /** UTC date in the format of 'yyyyMMddT000000Z'. */
  public abstract String date();

  /**
   * Upload policy encoded in base64 format. Note all fields in the upload form need to be included
   * in the policy.
   */
  public abstract String policy();

  /** Signature of the signed request. */
  public abstract String signature();

  // -- Below is used to build the credential.

  /** AWS access key. */
  public abstract String accessKey();

  // -- Below are required for creating the upload policy.

  /** Expiration time of the signed request, in the format of 'yyyy-MM-ddTkk:mm:ss.SSSZ'. */
  public abstract String expiration();

  /** S3 bucket name. */
  public abstract String bucket();

  // -- Below are required for creating the signing key.

  /** AWS secret access key. */
  public abstract String secretKey();

  /** UTC date in the format of 'yyyyMMdd'. The date should be the same as the date field above. */
  public abstract String dateStamp();

  /** AWS region name. */
  public abstract String regionName();

  /** AWS service name. */
  @Override
  public abstract String serviceName();

  public abstract int fileLimitMb();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setActionLink(String actionLink);

    public abstract Builder setKey(String key);

    /** Get key of the object. This is used to build the policy. */
    abstract String key();

    public abstract Builder setSuccessActionRedirect(String successActionRedirect);

    /** Get successActionRedirect. This is used to build the policy. */
    abstract String successActionRedirect();

    public abstract Builder setUseSuccessActionRedirectAsPrefix(boolean useAsPrefix);

    abstract boolean useSuccessActionRedirectAsPrefix();

    /** Set credential. Credential is autogenerated from other attributes so this is not public. */
    abstract Builder setCredential(String credential);

    /** Get credential. This is used to build the policy. */
    abstract String credential();

    public abstract Builder setSecurityToken(String securityToken);

    /** Get currently set securityToken. This is used to build the policy. */
    abstract Optional<String> securityToken();

    /** Set algorithm. Algorithm always uses default so this is not public. */
    abstract Builder setAlgorithm(String algorithm);

    /** Get algorithm. This is used to build the policy. */
    abstract String algorithm();

    /** Set date. Date is set along with expiration so this is not public. */
    abstract Builder setDate(String date);

    /** Get date. This is used to build the policy. */
    abstract String date();

    /** Set policy. Policy is built from other attributes so this is not public. */
    abstract Builder setPolicy(String policy);

    /** Get policy. This is used when signing the request. */
    abstract String policy();

    /** Set signature. Signature is created when the request is signed so this is not public. */
    abstract Builder setSignature(String signature);

    public abstract Builder setAccessKey(String accessKey);

    /** Get accessKey. This is used to build the credential. */
    abstract String accessKey();

    /** Set expiration. Expiration is calculated from expiration duration so this is not public. */
    abstract Builder setExpiration(String expiration);

    /** Get expiration. This is used to build the policy. */
    abstract String expiration();

    public abstract Builder setBucket(String bucket);

    /** Get bucket. This is used to build the policy. */
    abstract String bucket();

    public abstract Builder setSecretKey(String secretKey);

    /** Get secretKey. This is used to build the signing key. */
    abstract String secretKey();

    /** Set dateStamp. DateStamp is set along with expiration so this is not public. */
    abstract Builder setDateStamp(String dateStamp);

    /** Get dateStamp. This is used to build the credential and the signing key. */
    abstract String dateStamp();

    public abstract Builder setRegionName(String regionName);

    /** Get regionName. This is used to build the credential and the signing key. */
    abstract String regionName();

    /** Set serviceName. ServiceName always uses default so this is not public. */
    abstract Builder setServiceName(String serviceName);

    /** Get serviceName. This is used to build the credential and the signing key. */
    abstract String serviceName();

    /** Set fileLimitMb. This is used to build the credential and the signing key. */
    public abstract Builder setFileLimitMb(int fileLimitMb);

    /** Get fileLimitMb. This is used to build the credential and the signing key. */
    abstract int fileLimitMb();

    /** Build the request. This is called by the custom public build method. */
    abstract SignedS3UploadRequest autoBuild();

    /**
     * Set expiration as duration from now. Date and dateStamp is set to current date along the way.
     */
    public Builder setExpirationDuration(Duration duration) {
      LocalDateTime currentUTCDateTime = LocalDateTime.now(ZoneOffset.UTC);
      String dateString = currentUTCDateTime.toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE);
      setDate(dateString + "T000000Z");
      setDateStamp(dateString);
      LocalDateTime expiration = currentUTCDateTime.plus(duration);
      return setExpiration(
          expiration.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'kk:mm:ss.SSS'Z'")));
    }

    Builder buildCredential() {
      String credential =
          String.join("/", accessKey(), dateStamp(), regionName(), serviceName(), "aws4_request");
      return setCredential(credential);
    }

    /**
     * Build and encode policy in base64 format. All fields in the upload form need to be included
     * in the policy.
     */
    Builder buildPolicy() {
      UploadPolicy.Builder builder =
          UploadPolicy.builder()
              .setExpiration(expiration())
              .setBucket(bucket())
              .setKeyPrefix(key().replace("${filename}", ""))
              .setContentLengthRange(1, fileLimitMb() * MB_TO_BYTES)
              .setSuccessActionRedirect(successActionRedirect(), useSuccessActionRedirectAsPrefix())
              .setCredential(credential())
              .setAlgorithm(algorithm())
              .setDate(date());
      if (securityToken().isPresent()) {
        builder.setSecurityToken(securityToken().get());
      } else {
        setSecurityToken("");
      }
      String policyString = builder.build().getAsString();
      String policyBase64 = BinaryUtils.toBase64(policyString.getBytes(StandardCharsets.UTF_8));
      return setPolicy(policyBase64);
    }

    /** Sign the request, specifically the encoded policy string. */
    Builder sign() {
      String signature =
          BinaryUtils.toHex(
              HmacSHA256(
                  policy(), getSigningKey(secretKey(), dateStamp(), regionName(), serviceName())));
      return setSignature(signature);
    }

    /** Build the request. If a required field is not set, IllegalStateException is thrown. */
    public SignedS3UploadRequest build() {
      return buildCredential().buildPolicy().sign().autoBuild();
    }
  }

  @AutoValue
  abstract static class UploadPolicy {
    private static final ObjectMapper mapper =
        new ObjectMapper().registerModule(new GuavaModule()).registerModule(new Jdk8Module());

    static {
      mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    static Builder builder() {
      return new AutoValue_SignedS3UploadRequest_UploadPolicy.Builder();
    }

    @JsonIgnore
    String getAsString() {
      try {
        return mapper.writeValueAsString(this);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    @JsonProperty("expiration")
    abstract String expiration();

    @JsonProperty("conditions")
    abstract ImmutableList<Condition> conditions();

    abstract static class Condition {}

    @AutoValue
    abstract static class StartsWith extends Condition {

      /**
       * Creates a starts-with clause for the AWS policy. See
       * https://docs.aws.amazon.com/AmazonS3/latest/userguide/HTTPPOSTForms.html#HTTPPOSTConstructPolicy.
       *
       * @param element the policy element, like "key" or "acl". Should **not** include the `$` at
       *     the beginning (this class will automatically add it).
       */
      static StartsWith create(String element, String prefix) {
        return new AutoValue_SignedS3UploadRequest_UploadPolicy_StartsWith(element, prefix);
      }

      abstract String element();

      abstract String prefix();

      @JsonValue
      ImmutableList<String> toJson() {
        return ImmutableList.of("starts-with", "$" + element(), prefix());
      }
    }

    @AutoValue
    abstract static class ContentLengthRange extends Condition {

      static ContentLengthRange create(long minBytes, long maxBytes) {
        return new AutoValue_SignedS3UploadRequest_UploadPolicy_ContentLengthRange(
            minBytes, maxBytes);
      }

      abstract long minBytes();

      abstract long maxBytes();

      @JsonValue
      ImmutableList<Object> toJson() {
        return ImmutableList.of("content-length-range", minBytes(), maxBytes());
      }
    }

    @AutoValue
    abstract static class Bucket extends Condition {

      static Bucket create(String bucket) {
        return new AutoValue_SignedS3UploadRequest_UploadPolicy_Bucket(bucket);
      }

      @JsonProperty("bucket")
      abstract String bucket();
    }

    @AutoValue
    abstract static class SuccessActionRedirect extends Condition {

      static SuccessActionRedirect create(String successActionRedirect) {
        return new AutoValue_SignedS3UploadRequest_UploadPolicy_SuccessActionRedirect(
            successActionRedirect);
      }

      @JsonProperty(SUCCESS_ACTION_REDIRECT_NAME)
      abstract String successActionRedirect();
    }

    @AutoValue
    abstract static class SecurityToken extends Condition {

      static SecurityToken create(String securityToken) {
        return new AutoValue_SignedS3UploadRequest_UploadPolicy_SecurityToken(securityToken);
      }

      @JsonProperty("x-amz-security-token")
      abstract String securityToken();
    }

    @AutoValue
    abstract static class Credential extends Condition {

      static Credential create(String credential) {
        return new AutoValue_SignedS3UploadRequest_UploadPolicy_Credential(credential);
      }

      @JsonProperty("x-amz-credential")
      abstract String credential();
    }

    @AutoValue
    abstract static class Algorithm extends Condition {

      static Algorithm create(String algorithm) {
        return new AutoValue_SignedS3UploadRequest_UploadPolicy_Algorithm(algorithm);
      }

      @JsonProperty("x-amz-algorithm")
      abstract String algorithm();
    }

    @AutoValue
    abstract static class Date extends Condition {

      static Date create(String date) {
        return new AutoValue_SignedS3UploadRequest_UploadPolicy_Date(date);
      }

      @JsonProperty("x-amz-date")
      abstract String date();
    }

    @AutoValue.Builder
    abstract static class Builder {

      abstract Builder setExpiration(String expiration);

      abstract ImmutableList.Builder<Condition> conditionsBuilder();

      Builder setKeyPrefix(String prefix) {
        conditionsBuilder().add(StartsWith.create("key", prefix));
        return this;
      }

      Builder setContentLengthRange(long minBytes, long maxBytes) {
        conditionsBuilder().add(ContentLengthRange.create(minBytes, maxBytes));
        return this;
      }

      Builder setBucket(String bucket) {
        conditionsBuilder().add(Bucket.create(bucket));
        return this;
      }

      Builder setSuccessActionRedirect(
          String successActionRedirect, boolean useSuccessActionRedirectAsPrefix) {
        if (useSuccessActionRedirectAsPrefix) {
          conditionsBuilder()
              .add(StartsWith.create(SUCCESS_ACTION_REDIRECT_NAME, successActionRedirect));
        } else {
          conditionsBuilder().add(SuccessActionRedirect.create(successActionRedirect));
        }
        return this;
      }

      Builder setSecurityToken(String securityToken) {
        conditionsBuilder().add(SecurityToken.create(securityToken));
        return this;
      }

      Builder setCredential(String credential) {
        conditionsBuilder().add(Credential.create(credential));
        return this;
      }

      Builder setAlgorithm(String algorithm) {
        conditionsBuilder().add(Algorithm.create(algorithm));
        return this;
      }

      Builder setDate(String date) {
        conditionsBuilder().add(Date.create(date));
        return this;
      }

      abstract UploadPolicy build();
    }
  }
}
