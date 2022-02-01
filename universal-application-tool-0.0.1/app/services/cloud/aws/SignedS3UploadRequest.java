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
 * SimpleStorage#getSignedUploadRequest}.
 */
@AutoValue
public abstract class SignedS3UploadRequest implements StorageUploadRequest {

  private static final long MB_TO_BYTES = 1L << 20;

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

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setActionLink(String actionLink);

    public abstract Builder setKey(String key);

    /** Get key of the object. This is used to build the policy. */
    abstract String key();

    public abstract Builder setSuccessActionRedirect(String successActionRedirect);

    /** Get successActionRedirect. This is used to build the policy. */
    abstract String successActionRedirect();

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
              .setContentLengthRange(1, 1024 * MB_TO_BYTES)
              .setSuccessActionRedirect(successActionRedirect())
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

      static StartsWith create(String element, String prefix) {
        return new AutoValue_SignedS3UploadRequest_UploadPolicy_StartsWith(element, prefix);
      }

      abstract String element();

      abstract String prefix();

      @JsonValue
      ImmutableList<String> toJson() {
        return ImmutableList.of("starts-with", element(), prefix());
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
      ImmutableList<String> toJson() {
        return ImmutableList.of(
            "content-length-range", String.valueOf(minBytes()), String.valueOf(maxBytes()));
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

      @JsonProperty("success_action_redirect")
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
        conditionsBuilder().add(StartsWith.create("$key", prefix));
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

      Builder setSuccessActionRedirect(String successActionRedirect) {
        conditionsBuilder().add(SuccessActionRedirect.create(successActionRedirect));
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
