package models;

import auth.ApiKeyGrants;
import io.ebean.annotation.DbJsonB;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.Table;

/** An EBean mapped class that represents an API key in CiviForm. */
@Entity
@Table(name = "api_keys")
public class ApiKey extends BaseModel {

  /** Timestamp of when the the ApiKey was created. */
  @WhenCreated private Instant createTime;

  /** Timestamp of when the the ApiKey was last modified. */
  @WhenModified private Instant updateTime;

  /** Timestamp of when the ApiKey is no longer valid. */
  private Instant expiration;

  /** The {@code authorityId} of the account that created the ApiKey. */
  private String createdBy;

  /** Human readable name of the ApiKey. */
  private String name;

  /**
   * Unique identifier for the ApiKey. Paired with the password, comprises the credentials for using
   * the ApiKey. For more information see https://en.wikipedia.org/wiki/Basic_access_authentication
   */
  private String keyId;

  /**
   * The salted key secret (a.k.a. password) of the ApiKey. Created by signing the password with the
   * API secret using the SHA-HMAC-256 algorithm.
   */
  private String saltedKeySecret;

  /**
   * An allowlist of IPv4 addresses specified using CIDR notation.
   * https://en.wikipedia.org/wiki/Classless_Inter-Domain_Routing
   */
  private String subnet;

  /** The client IPv4 address of the last request to successfully auth with the ApiKey. */
  private String lastCallIpAddress;

  /** Permissions granted to this ApiKey by the admin. */
  @DbJsonB private ApiKeyGrants grants;

  public ApiKeyGrants getGrants() {
    return grants;
  }

  public ApiKey setGrants(ApiKeyGrants grants) {
    this.grants = grants;
    return this;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public Instant getUpdateTime() {
    return updateTime;
  }

  public Instant getExpiration() {
    return expiration;
  }

  public ApiKey setExpiration(Instant expiration) {
    this.expiration = expiration;
    return this;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public ApiKey setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  public String getName() {
    return name;
  }

  public ApiKey setName(String name) {
    this.name = name;
    return this;
  }

  public String getKeyId() {
    return keyId;
  }

  public ApiKey setKeyId(String keyId) {
    this.keyId = keyId;
    return this;
  }

  public String getSaltedKeySecret() {
    return saltedKeySecret;
  }

  public ApiKey setSaltedKeySecret(String saltedKeySecret) {
    this.saltedKeySecret = saltedKeySecret;
    return this;
  }

  public String getSubnet() {
    return subnet;
  }

  public ApiKey setSubnet(String subnet) {
    this.subnet = subnet;
    return this;
  }

  public String getLastCallIpAddress() {
    return lastCallIpAddress;
  }

  public String setLastCallIpAddress(String lastCallIpAddress) {
    this.lastCallIpAddress = lastCallIpAddress;
    return this;
  }
}
