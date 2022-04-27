package models;

import auth.ApiKeyGrants;
import io.ebean.annotation.DbJsonB;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.Table;
import play.data.validation.Constraints;

/**
 * An EBean mapped class that represents an API key in CiviForm.
 */
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

  public ApiKeyGrants getGrants() {
    return grants;
  }

  public ApiKey setGrants(ApiKeyGrants grants) {
    this.grants = grants;
    return this;
  }

  @DbJsonB private ApiKeyGrants grants;

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
}
