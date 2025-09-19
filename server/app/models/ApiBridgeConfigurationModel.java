package models;

import static services.apibridge.ApiBridgeServiceDto.CompatibilityLevel;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import io.ebean.annotation.DbJsonB;
import io.ebean.annotation.NotNull;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.regex.Pattern;
import services.applicant.question.Scalar;

/** Entity representing a bridge configuration. */
@Entity
@Table(name = "api_bridge_configuration")
public class ApiBridgeConfigurationModel extends BaseModel {
  public record ApiBridgeDefinitionItem(
      @JsonProperty("questionName") String questionName,
      @JsonProperty("questionScalar") Scalar questionScalar,
      @JsonProperty("externalName") String externalName) {}

  public record ApiBridgeDefinition(
      @JsonProperty("inputFields") ImmutableList<ApiBridgeDefinitionItem> inputFields,
      @JsonProperty("outputFields") ImmutableList<ApiBridgeDefinitionItem> outputFields) {

    public ApiBridgeDefinition {
      inputFields = inputFields != null ? ImmutableList.copyOf(inputFields) : ImmutableList.of();
      outputFields = outputFields != null ? ImmutableList.copyOf(outputFields) : ImmutableList.of();
    }
  }

  private static final Pattern ADMIN_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9-]*$");

  @NotNull private String hostUrl;
  @NotNull private String urlPath;

  @NotNull
  @Enumerated(EnumType.STRING)
  private CompatibilityLevel compatibilityLevel;

  @NotNull private String adminName;
  @NotNull private String description;
  @NotNull @DbJsonB private String requestSchema;
  @NotNull private String requestSchemaChecksum;
  @NotNull @DbJsonB private String responseSchema;
  @NotNull private String responseSchemaChecksum;
  @DbJsonB private ApiBridgeDefinition globalBridgeDefinition;
  private boolean enabled;
  @NotNull @WhenCreated private Instant createTime;
  @NotNull @WhenModified private Instant updateTime;

  /** Gets the ID. */
  public Long id() {
    return id;
  }

  /** Sets the ID. */
  public ApiBridgeConfigurationModel setId(Long id) {
    this.id = id;
    return this;
  }

  /** Gets the host URL. */
  public String hostUrl() {
    return hostUrl;
  }

  /** Sets the host URL. */
  public ApiBridgeConfigurationModel setHostUrl(String hostUrl) {
    this.hostUrl = hostUrl;
    return this;
  }

  /** Gets the URL path. */
  public String urlPath() {
    return urlPath;
  }

  /** Sets the URL path. */
  public ApiBridgeConfigurationModel setUrlPath(String urlPath) {
    this.urlPath = urlPath;
    return this;
  }

  /** Gets the version. */
  public CompatibilityLevel compatibilityLevel() {
    return compatibilityLevel;
  }

  /** Sets the version. */
  public ApiBridgeConfigurationModel setCompatibilityLevel(CompatibilityLevel version) {
    this.compatibilityLevel = version;
    return this;
  }

  /** Gets the admin name. */
  public String adminName() {
    return adminName;
  }

  /**
   * Sets the admin name. Throws {@link IllegalArgumentException} if invalid string. Must be
   * lowercase. Must start with a letter. Allowed characters alphanumeric and hyphen.
   */
  public ApiBridgeConfigurationModel setAdminName(String adminName) {
    if (!ADMIN_NAME_PATTERN.matcher(adminName).matches()) {
      throw new IllegalArgumentException(
          "Invalid adminName. Must be lowercase. Must start with a letter. Allowed characters"
              + " alphanumeric and hyphen.");
    }
    this.adminName = adminName;
    return this;
  }

  /** Gets the description. */
  public String description() {
    return description;
  }

  /** Sets the description. */
  public ApiBridgeConfigurationModel setDescription(String description) {
    this.description = description;
    return this;
  }

  /** Gets the request schema. */
  public String requestSchema() {
    return requestSchema;
  }

  /** Sets the request schema. */
  public ApiBridgeConfigurationModel setRequestSchema(String requestSchema) {
    this.requestSchema = requestSchema;
    return this;
  }

  /** Gets the request schema checksum. */
  public String requestSchemaChecksum() {
    return requestSchemaChecksum;
  }

  /** Sets the request schema checksum. */
  public ApiBridgeConfigurationModel setRequestSchemaChecksum(String requestSchemaChecksum) {
    this.requestSchemaChecksum = requestSchemaChecksum;
    return this;
  }

  /** Gets the response schema. */
  public String responseSchema() {
    return responseSchema;
  }

  /** Sets the response schema. */
  public ApiBridgeConfigurationModel setResponseSchema(String responseSchema) {
    this.responseSchema = responseSchema;
    return this;
  }

  /** Gets the response schema checksum. */
  public String responseSchemaChecksum() {
    return responseSchemaChecksum;
  }

  /** Sets the response schema checksum. */
  public ApiBridgeConfigurationModel setResponseSchemaChecksum(String responseSchemaChecksum) {
    this.responseSchemaChecksum = responseSchemaChecksum;
    return this;
  }

  /** Gets the global bridge definition. */
  public ApiBridgeDefinition globalBridgeDefinition() {
    return globalBridgeDefinition;
  }

  /** Sets the global bridge definition. */
  public ApiBridgeConfigurationModel setGlobalBridgeDefinition(
      ApiBridgeDefinition globalBridgeDefinition) {
    this.globalBridgeDefinition = globalBridgeDefinition;
    return this;
  }

  /** Checks if the configuration is enabled. */
  public boolean enabled() {
    return enabled;
  }

  /** Sets whether the configuration is enabled. */
  public ApiBridgeConfigurationModel setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /** Gets the creation time. */
  public Instant createTime() {
    return createTime;
  }

  /** Gets the update time. */
  public Instant updateTime() {
    return updateTime;
  }

  /** Gets the formatted url of the hostUrl and urlPath combined */
  public String getFullHostUrlWithPath() {
    return "%s%s".formatted(hostUrl, urlPath);
  }
}
