package services.program;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import services.program.predicate.PredicateDefinition;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Defines eligibility configuration on questions in a program.
 *
 * <p>Eligibility rules use the {@link PredicateDefinition} format, and indicate when an applicant
 * is eligible to submit an application based on their answers so far.
 *
 * <p>The {@link PredicateDefinition} condition structure is identical to how visibility predicate
 * data is handled.
 */
@JsonDeserialize(builder = AutoValue_EligibilityDefinition.Builder.class)
@AutoValue
public abstract class EligibilityDefinition {

  /** Indicates the shape of the predicate's AST so view code can render the appropriate
   * UI. */
  public enum PredicateFormat {
    // A single leaf node
    SINGLE_QUESTION,
    // A top level conjunction with only AND child nodes,
    // each of which has only leaf nodes.
    SINGLE_LAYER_AND;
  }

  public static Builder builder() {
    return new AutoValue_EligibilityDefinition.Builder();
  }

  /** A {@link PredicateDefinition} that determines whether the applicant is eligible. */
  @JsonProperty("predicate")
  public abstract PredicateDefinition predicate();

  /** Indicates the shape of the predicate's AST so view code can render the appropriate
   * UI. */
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonProperty("predicateType")
  public abstract Optional<PredicateFormat> predicateFormat();

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract EligibilityDefinition build();

    @JsonProperty("predicate")
    public abstract Builder setPredicate(PredicateDefinition def);

    @JsonProperty("predicateType")
    public abstract Builder setPredicateFormat(Optional<PredicateFormat> predicateFormat);

    public Builder setPredicateFormat(@Nullable PredicateFormat predicateFormat) {
      return this.setPredicateFormat(Optional.ofNullable(predicateFormat));
    }
  }
}
