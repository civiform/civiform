package models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = AutoValue_BridgeDefinitionItem.Builder.class)
public abstract class BridgeDefinitionItem {
  @JsonProperty("externalName")
  public abstract String externalName();

  @JsonProperty("questionName")
  public abstract String questionName();

  public static Builder builder() {
    return new AutoValue_BridgeDefinitionItem.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    @JsonProperty("externalName")
    public abstract Builder externalName(String externalName);

    @JsonProperty("questionName")
    public abstract Builder questionName(String questionName);

    public abstract BridgeDefinitionItem build();
  }
}
