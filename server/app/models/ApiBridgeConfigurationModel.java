package models;

import io.ebean.annotation.DbJsonB;
import io.ebean.annotation.NotNull;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/** Entity representing a bridge configuration. */
@Entity
@Table(name = "bridge_configuration")
public class ApiBridgeConfigurationModel extends BaseModel {

  @NotNull private String hostUri;

  @NotNull private String uriPath;

  @NotNull private String compatibilityLevel;

  @NotNull private String adminName;

  @NotNull private String description;

  @NotNull @DbJsonB private String requestSchema;

  @NotNull private String requestSchemaChecksum;

  @NotNull @DbJsonB private String responseSchema;

  @NotNull private String responseSchemaChecksum;

  @DbJsonB private String globalBridgeDefinition;

  @NotNull private boolean enabled;

  @NotNull @WhenCreated private Instant createTime;

  @NotNull @WhenModified private Instant updateTime;

  /** Gets the ID. */
  public Long getId() {
    return id;
  }

  /** Sets the ID. */
  public ApiBridgeConfigurationModel setId(Long id) {
    this.id = id;
    return this;
  }

  /** Gets the host URI. */
  public String getHostUri() {
    return hostUri;
  }

  /** Sets the host URI. */
  public ApiBridgeConfigurationModel setHostUri(String hostUri) {
    this.hostUri = hostUri;
    return this;
  }

  /** Gets the URI path. */
  public String getUriPath() {
    return uriPath;
  }

  /** Sets the URI path. */
  public ApiBridgeConfigurationModel setUriPath(String uriPath) {
    this.uriPath = uriPath;
    return this;
  }

  /** Gets the version. */
  public String getCompatibilityLevel() {
    return compatibilityLevel;
  }

  /** Sets the version. */
  public ApiBridgeConfigurationModel setCompatibilityLevel(String version) {
    this.compatibilityLevel = version;
    return this;
  }

  /** Gets the admin name. */
  public String getAdminName() {
    return adminName;
  }

  /** Sets the admin name. */
  public ApiBridgeConfigurationModel setAdminName(String adminName) {
    this.adminName = adminName;
    return this;
  }

  /** Gets the description. */
  public String getDescription() {
    return description;
  }

  /** Sets the description. */
  public ApiBridgeConfigurationModel setDescription(String description) {
    this.description = description;
    return this;
  }

  /** Gets the request schema. */
  public String getRequestSchema() {
    return requestSchema;
  }

  /** Sets the request schema. */
  public ApiBridgeConfigurationModel setRequestSchema(String requestSchema) {
    this.requestSchema = requestSchema;
    return this;
  }

  /** Gets the request schema checksum. */
  public String getRequestSchemaChecksum() {
    return requestSchemaChecksum;
  }

  /** Sets the request schema checksum. */
  public ApiBridgeConfigurationModel setRequestSchemaChecksum(String requestSchemaChecksum) {
    this.requestSchemaChecksum = requestSchemaChecksum;
    return this;
  }

  /** Gets the response schema. */
  public String getResponseSchema() {
    return responseSchema;
  }

  /** Sets the response schema. */
  public ApiBridgeConfigurationModel setResponseSchema(String responseSchema) {
    this.responseSchema = responseSchema;
    return this;
  }

  /** Gets the response schema checksum. */
  public String getResponseSchemaChecksum() {
    return responseSchemaChecksum;
  }

  /** Sets the response schema checksum. */
  public ApiBridgeConfigurationModel setResponseSchemaChecksum(String responseSchemaChecksum) {
    this.responseSchemaChecksum = responseSchemaChecksum;
    return this;
  }

  /** Gets the global bridge definition. */
  public String getGlobalBridgeDefinition() {
    return globalBridgeDefinition;
  }

  /** Sets the global bridge definition. */
  public ApiBridgeConfigurationModel setGlobalBridgeDefinition(String globalBridgeDefinition) {
    this.globalBridgeDefinition = globalBridgeDefinition;
    return this;
  }

  /** Checks if the configuration is enabled. */
  public boolean isEnabled() {
    return enabled;
  }

  /** Sets whether the configuration is enabled. */
  public ApiBridgeConfigurationModel setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /** Gets the creation time. */
  public Instant getCreateTime() {
    return createTime;
  }

  /** Gets the update time. */
  public Instant getUpdateTime() {
    return updateTime;
  }
}
