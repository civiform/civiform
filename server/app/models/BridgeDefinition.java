package models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = AutoValue_BridgeDefinition.Builder.class)
public abstract class BridgeDefinition {
  @Nullable
  @JsonProperty("bridgeConfigurationId")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public abstract Long bridgeConfigurationId();

  @JsonProperty("inputFields")
  public abstract ImmutableList<BridgeDefinitionItem> inputFields();

  @JsonProperty("outputFields")
  public abstract ImmutableList<BridgeDefinitionItem> outputFields();

  public static Builder builder() {
    return new AutoValue_BridgeDefinition.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    @JsonProperty("bridgeConfigurationId")
    public abstract Builder bridgeConfigurationId(Long bridgeConfigurationId);

    @JsonProperty("inputFields")
    public abstract Builder inputFields(ImmutableList<BridgeDefinitionItem> inputFields);

    @JsonProperty("outputFields")
    public abstract Builder outputFields(ImmutableList<BridgeDefinitionItem> outputFields);

    abstract ImmutableList.Builder<BridgeDefinitionItem> inputFieldsBuilder();

    abstract ImmutableList.Builder<BridgeDefinitionItem> outputFieldsBuilder();

    public abstract BridgeDefinition build();
  }
}
