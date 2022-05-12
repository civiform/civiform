package models;

import auth.ApiKeyGrants;
import io.ebean.annotation.DbJsonB;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import java.time.Instant;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.Table;

/** An EBean mapped class that represents an API key in CiviForm. */
@Entity
@Table(name = "api_keys")
public class ApiKey extends BaseModel {

  @WhenCreated private Instant createTime;
  @WhenModified private Instant updateTime;
  private Instant expiration;
  private String createdBy;
  private String name;
  private String keyId;
  private String saltedKeySecret;
  private String subnet;
  private String lastCallIpAddress;
  private Long callCount;
  private Instant retiredTime;
  private String retiredBy;

  /** Permissions granted to this ApiKey by the admin. */
  @DbJsonB private ApiKeyGrants grants;

  public ApiKey(ApiKeyGrants grants) {
    super();
    this.callCount = 0L;
    this.grants = grants;
  }

  public ApiKey() {
    super();
    this.callCount = 0L;
    this.grants = new ApiKeyGrants();
  }

  public ApiKeyGrants getGrants() {
    return grants;
  }

  public ApiKey setGrants(ApiKeyGrants grants) {
    this.grants = grants;
    return this;
  }

  /**
   * Retires the key, setting who retired it and the retired time to now. Throws a runtime exception
   * if it is already retired.
   *
   * @param retiredBy the authority_id of the account who retired the key.
   */
  public ApiKey retire(String retiredBy) {
    if (retiredTime != null) {
      throw new RuntimeException(String.format("ApiKey %s is already retired", id));
    }

    this.retiredBy = retiredBy;
    this.retiredTime = Instant.now();
    return this;
  }

  /** True if the ApiKey is retired. */
  public boolean isRetired() {
    return this.retiredTime != null;
  }

  /** The time when the ApiKey was retired or empty if it is not retired. */
  public Optional<Instant> getRetiredTime() {
    return Optional.ofNullable(retiredTime);
  }

  /** The authority ID of the account that retired the ApiKey or empty if it is not retired. */
  public Optional<String> getRetiredBy() {
    return Optional.ofNullable(retiredBy);
  }

  /** Timestamp of when the the ApiKey was created. */
  public Instant getCreateTime() {
    return createTime;
  }

  /** Timestamp of when the the ApiKey was last modified. */
  public Instant getUpdateTime() {
    return updateTime;
  }

  /** Timestamp of when the ApiKey is no longer valid. */
  public Instant getExpiration() {
    return expiration;
  }

  /**
   * Timestamp of when the ApiKey is no longer valid. Expiration should be immutable after creation.
   */
  public ApiKey setExpiration(Instant expiration) {
    this.expiration = expiration;
    return this;
  }

  /** The {@code authorityId} of the account that created the ApiKey. */
  public String getCreatedBy() {
    return createdBy;
  }

  /** The {@code authorityId} of the account that created the ApiKey. */
  public ApiKey setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /** Human readable name of the ApiKey. */
  public String getName() {
    return name;
  }

  /** Human readable name of the ApiKey. OK to modify after creation. */
  public ApiKey setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Unique identifier for the ApiKey. Paired with the password, comprises the credentials for using
   * the ApiKey. For more information see https://en.wikipedia.org/wiki/Basic_access_authentication
   */
  public String getKeyId() {
    return keyId;
  }

  /** Unique identifier for the ApiKey. */
  public ApiKey setKeyId(String keyId) {
    this.keyId = keyId;
    return this;
  }

  /**
   * The salted key secret (a.k.a. password) of the ApiKey. Created by signing the password with the
   * API secret using the SHA-HMAC-256 algorithm.
   */
  public String getSaltedKeySecret() {
    return saltedKeySecret;
  }

  /** The salted key secret (a.k.a. password) of the ApiKey. */
  public ApiKey setSaltedKeySecret(String saltedKeySecret) {
    this.saltedKeySecret = saltedKeySecret;
    return this;
  }

  /**
   * An allowlist of IPv4 addresses that are permitted to authenticate with this ApiKey. Specified
   * using CIDR notation: https://en.wikipedia.org/wiki/Classless_Inter-Domain_Routing
   */
  public String getSubnet() {
    return subnet;
  }

  /**
   * An allowlist of IPv4 addresses that are permitted to authenticate with this ApiKey. Specified
   * using CIDR notation: https://en.wikipedia.org/wiki/Classless_Inter-Domain_Routing
   */
  public ApiKey setSubnet(String subnet) {
    this.subnet = subnet;
    return this;
  }

  /**
   * The client IPv4 address of the last request to successfully auth with the ApiKey. Empty if the
   * ApiKey has never been used.
   */
  public Optional<String> getLastCallIpAddress() {
    return Optional.ofNullable(lastCallIpAddress);
  }

  /** The client IPv4 address of the last request to successfully auth with the ApiKey. */
  public ApiKey setLastCallIpAddress(String lastCallIpAddress) {
    this.lastCallIpAddress = lastCallIpAddress;
    return this;
  }

  /** The number of requests that have been attempted using this API key. */
  public Long getCallCount() {
    return callCount;
  }

  /** Increment the number of requests that have been attempted using this API key. */
  public ApiKey incrementCallCount() {
    this.callCount++;
    return this;
  }
}
