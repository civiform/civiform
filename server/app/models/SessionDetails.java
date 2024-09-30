package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public final class SessionDetails {
  @JsonProperty("creationTimeMs")
  private long creationTimeMs;

  private String idToken;

  @JsonIgnore
  public Instant getCreationTime() {
    return Instant.ofEpochMilli(creationTimeMs);
  }

  @JsonIgnore
  public void setCreationTime(Instant creationTime) {
    this.creationTimeMs = creationTime.toEpochMilli();
  }

  public String getIdToken() {
    return idToken;
  }

  public void setIdToken(String idToken) {
    this.idToken = idToken;
  }
}
